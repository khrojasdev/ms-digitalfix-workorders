package cl.duoc.digitalfix.workorders.service;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.Permisos;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.domain.OrdenDeTrabajo;
import cl.duoc.digitalfix.workorders.domain.Tecnico;
import cl.duoc.digitalfix.workorders.error.AccesoDenegado;
import cl.duoc.digitalfix.workorders.error.RecursoNoEncontrado;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.error.TransicionInvalida;
import cl.duoc.digitalfix.workorders.repository.OrdenDeTrabajoRepository;
import cl.duoc.digitalfix.workorders.repository.TecnicoRepository;
import cl.duoc.digitalfix.workorders.web.dto.MisOrdenesDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenEventoDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenSolicitudDto;
import cl.duoc.digitalfix.workorders.web.dto.TecnicoRespuestaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrdenDeTrabajoService {

    /** Lo que un TECNICO puede marcar en sus ordenes: el avance en terreno, no cancelar. */
    private static final Set<EstadoOrden> ESTADOS_DEL_TECNICO =
            EnumSet.of(EstadoOrden.EN_DESPLAZAMIENTO, EstadoOrden.EN_EJECUCION, EstadoOrden.CERRADA);

    private final OrdenDeTrabajoRepository repository;
    private final OrdenEventoPublisher publisher;
    private final TecnicoService tecnicos;
    private final TecnicoRepository tecnicoRepository;

    public OrdenDeTrabajoService(OrdenDeTrabajoRepository repository, OrdenEventoPublisher publisher,
                                 TecnicoService tecnicos, TecnicoRepository tecnicoRepository) {
        this.repository = repository;
        this.publisher = publisher;
        this.tecnicos = tecnicos;
        this.tecnicoRepository = tecnicoRepository;
    }

    @Transactional
    public OrdenRespuestaDto crearOrden(OrdenSolicitudDto dto, ContextoEmpresa contexto) {
        OrdenDeTrabajo orden = new OrdenDeTrabajo(
                dto.servicioId(),
                contexto.companyId(),
                contexto.userOid(),
                dto.comentarios()
        );

        orden = repository.save(orden);

        emitirEvento(orden, "CREADA", null, contexto);

        return mapearADto(orden);
    }

    @Transactional
    public OrdenRespuestaDto cambiarEstado(Long ordenId, EstadoOrden nuevoEstado, ContextoEmpresa contexto) {
        OrdenDeTrabajo orden = buscarDeLaEmpresa(ordenId, contexto);

        autorizarCambio(orden, nuevoEstado, contexto);
        validarTransicion(orden, nuevoEstado);

        EstadoOrden anterior = orden.getEstado();
        orden.setEstado(nuevoEstado);
        orden = repository.save(orden);

        emitirEvento(orden, nuevoEstado.name(), anterior, contexto);

        return mapearADto(orden);
    }

    /**
     * El supervisor asigna un tecnico activo de su empresa. La orden pasa a
     * ASIGNADA y se emite el evento ASIGNADA (catalog descuenta stock y notify
     * envia el ticket de despacho). Mientras no salga a terreno se puede
     * reasignar; en ese caso el evento lleva estadoAnterior = ASIGNADA.
     */
    @Transactional
    public OrdenRespuestaDto asignarTecnico(Long ordenId, Long tecnicoId, ContextoEmpresa contexto) {
        Permisos.exigirAlguno(contexto, Permisos.GESTION);
        OrdenDeTrabajo orden = buscarDeLaEmpresa(ordenId, contexto);

        if (!orden.getEstado().admiteAsignacion()) {
            throw new TransicionInvalida("La orden esta " + orden.getEstado()
                    + "; solo se asigna tecnico a ordenes CREADA o ASIGNADA.");
        }

        Tecnico tecnico = tecnicos.buscarDeLaEmpresa(tecnicoId, contexto);
        if (!tecnico.isActivo()) {
            throw new TransicionInvalida("El tecnico " + tecnico.getNombre() + " esta desactivado.");
        }
        if (tecnico.getId().equals(orden.getTecnicoId())) {
            throw new TransicionInvalida("La orden ya esta asignada a " + tecnico.getNombre() + ".");
        }

        EstadoOrden anterior = orden.getEstado();
        orden.asignarA(tecnico);
        orden = repository.save(orden);

        emitirEvento(orden, EstadoOrden.ASIGNADA.name(), anterior, contexto, tecnico);

        return mapearADto(orden);
    }

    /**
     * ADMIN y SUPERVISOR cambian cualquier orden de su empresa. El TECNICO solo
     * las suyas, y solo para avanzar en terreno (no cancela). El resto, no.
     */
    private void autorizarCambio(OrdenDeTrabajo orden, EstadoOrden destino, ContextoEmpresa contexto) {
        if (Permisos.gestiona(contexto)) {
            return;
        }
        if (contexto.tieneRol(Permisos.TECNICO)) {
            Tecnico yo = tecnicos.delUsuario(contexto);
            if (!yo.getId().equals(orden.getTecnicoId())) {
                throw new AccesoDenegado("La orden #" + orden.getId() + " no esta asignada a ti.");
            }
            if (destino != null && !ESTADOS_DEL_TECNICO.contains(destino)) {
                throw new AccesoDenegado("Un tecnico solo marca En desplazamiento, En ejecucion o Cerrada.");
            }
            return;
        }
        throw new AccesoDenegado("Tu rol no permite cambiar el estado de las ordenes.");
    }

    /** Vista del TECNICO: su ficha y sus ordenes, de la mas nueva a la mas antigua. */
    @Transactional
    public MisOrdenesDto misOrdenes(ContextoEmpresa contexto) {
        Tecnico yo = tecnicos.delUsuario(contexto);
        List<OrdenRespuestaDto> ordenes = repository
                .findByEmpresaIdAndTecnicoIdOrderByFechaCreacionDesc(contexto.companyId(), yo.getId())
                .stream().map(this::mapearADto).toList();
        return new MisOrdenesDto(TecnicoRespuestaDto.desde(yo), ordenes);
    }

    /** Orden de la empresa del contexto; si es de otra empresa, 404 para no revelar que existe. */
    private OrdenDeTrabajo buscarDeLaEmpresa(Long ordenId, ContextoEmpresa contexto) {
        OrdenDeTrabajo orden = repository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontrado("Orden de trabajo no encontrada"));
        if (!orden.getEmpresaId().equals(contexto.companyId())) {
            throw new RecursoNoEncontrado("Orden de trabajo no encontrada");
        }
        return orden;
    }

    /**
     * Aplica las reglas del caso: no se salta etapas, ASIGNADA solo se logra
     * asignando un tecnico y las ordenes cerradas o canceladas no cambian.
     */
    private void validarTransicion(OrdenDeTrabajo orden, EstadoOrden destino) {
        if (destino == null) {
            throw new SolicitudInvalida("Falta el estado de destino.");
        }
        EstadoOrden actual = orden.getEstado();
        if (actual == destino) {
            throw new TransicionInvalida("La orden ya esta en " + actual + ".");
        }
        if (actual.esFinal()) {
            throw new TransicionInvalida("La orden esta " + actual + " y ya no puede cambiar de estado.");
        }
        if (destino == EstadoOrden.ASIGNADA) {
            throw new TransicionInvalida("Para pasar a ASIGNADA hay que asignar un tecnico (PUT /api/workorders/"
                    + orden.getId() + "/asignacion).");
        }
        if (!actual.puedePasarA(destino)) {
            if (actual == EstadoOrden.CREADA) {
                throw new TransicionInvalida("No se puede pasar a " + destino + " sin ASIGNAR un tecnico.");
            }
            throw new TransicionInvalida("No se puede pasar de " + actual + " a " + destino
                    + ". Siguientes permitidos: " + actual.siguientes() + ".");
        }
    }

    public List<OrdenRespuestaDto> listarOrdenesPorEmpresa(Long empresaId) {
        return repository.findByEmpresaId(empresaId).stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    private void emitirEvento(OrdenDeTrabajo orden, String tipoEvento, EstadoOrden anterior, ContextoEmpresa contexto) {
        Tecnico tecnico = orden.getTecnicoId() == null ? null
                : tecnicoRepository.findById(orden.getTecnicoId()).orElse(null);
        emitirEvento(orden, tipoEvento, anterior, contexto, tecnico);
    }

    private void emitirEvento(OrdenDeTrabajo orden, String tipoEvento, EstadoOrden anterior,
                              ContextoEmpresa contexto, Tecnico tecnico) {
        // Generamos un UUID unico para la idempotencia
        String eventKey = UUID.randomUUID().toString();

        OrdenEventoDto evento = new OrdenEventoDto(
                eventKey,
                tipoEvento,
                orden.getId(),
                contexto.userOid(),
                contexto.companyId(),
                LocalDateTime.now(),
                contexto.correlacion(),
                anterior == null ? null : anterior.name(),
                orden.getEstado().name(),
                orden.getServicioId(),
                orden.getCreadorId(),
                orden.getComentarios(),
                tecnico == null ? null : tecnico.getId(),
                tecnico == null ? orden.getAsignadoA() : tecnico.getNombre(),
                tecnico == null ? null : tecnico.getEmail(),
                tecnico == null ? null : tecnico.getTelefono()
        );
        publisher.publicarEvento(evento);
    }

    private OrdenRespuestaDto mapearADto(OrdenDeTrabajo orden) {
        return new OrdenRespuestaDto(
                orden.getId(),
                orden.getServicioId(),
                orden.getEstado(),
                orden.getAsignadoA(),
                orden.getComentarios(),
                orden.getFechaCreacion(),
                orden.getFechaActualizacion(),
                orden.getTecnicoId(),
                orden.getCreadorId()
        );
    }
}