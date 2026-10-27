package cl.duoc.digitalfix.workorders.service;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.Permisos;
import cl.duoc.digitalfix.workorders.domain.CambioDeEstado;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.domain.OrdenDeTrabajo;
import cl.duoc.digitalfix.workorders.domain.Tecnico;
import cl.duoc.digitalfix.workorders.error.AccesoDenegado;
import cl.duoc.digitalfix.workorders.error.RecursoNoEncontrado;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.error.TransicionInvalida;
import cl.duoc.digitalfix.workorders.repository.CambioDeEstadoRepository;
import cl.duoc.digitalfix.workorders.repository.OrdenDeTrabajoRepository;
import cl.duoc.digitalfix.workorders.repository.TecnicoRepository;
import cl.duoc.digitalfix.workorders.web.dto.CambioDeEstadoDto;
import cl.duoc.digitalfix.workorders.web.dto.MisOrdenesDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenDetalleDto;
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

@Service
public class OrdenDeTrabajoService {

    /** Lo que un TECNICO puede marcar en sus ordenes: el avance en terreno, no cancelar. */
    private static final Set<EstadoOrden> ESTADOS_DEL_TECNICO =
            EnumSet.of(EstadoOrden.EN_DESPLAZAMIENTO, EstadoOrden.EN_EJECUCION, EstadoOrden.CERRADA);

    private final OrdenDeTrabajoRepository repository;
    private final OrdenEventoPublisher publisher;
    private final TecnicoService tecnicos;
    private final TecnicoRepository tecnicoRepository;
    private final CambioDeEstadoRepository historial;

    public OrdenDeTrabajoService(OrdenDeTrabajoRepository repository, OrdenEventoPublisher publisher,
                                 TecnicoService tecnicos, TecnicoRepository tecnicoRepository,
                                 CambioDeEstadoRepository historial) {
        this.repository = repository;
        this.publisher = publisher;
        this.tecnicos = tecnicos;
        this.tecnicoRepository = tecnicoRepository;
        this.historial = historial;
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
        registrarPaso(orden, null, contexto, null);

        emitirEvento(orden, "CREADA", null, contexto, null);

        return mapearADto(orden);
    }

    @Transactional
    public OrdenRespuestaDto cambiarEstado(Long ordenId, EstadoOrden nuevoEstado, ContextoEmpresa contexto) {
        return cambiarEstado(ordenId, nuevoEstado, null, contexto);
    }

    /**
     * Cambia el estado y deja el paso en el historial, con el comentario si
     * viene (es opcional: el tecnico lo usa para anotar lo que hizo).
     */
    @Transactional
    public OrdenRespuestaDto cambiarEstado(Long ordenId, EstadoOrden nuevoEstado, String comentario,
                                           ContextoEmpresa contexto) {
        OrdenDeTrabajo orden = buscarDeLaEmpresa(ordenId, contexto);

        autorizarCambio(orden, nuevoEstado, contexto);
        validarTransicion(orden, nuevoEstado);

        EstadoOrden anterior = orden.getEstado();
        orden.setEstado(nuevoEstado);
        orden = repository.save(orden);
        String nota = limpiar(comentario);
        registrarPaso(orden, anterior, contexto, nota);

        emitirEvento(orden, nuevoEstado.name(), anterior, contexto, nota);

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
        return asignarTecnico(ordenId, tecnicoId, null, contexto);
    }

    @Transactional
    public OrdenRespuestaDto asignarTecnico(Long ordenId, Long tecnicoId, String comentario, ContextoEmpresa contexto) {
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
        String nota = limpiar(comentario);
        registrarPaso(orden, anterior, contexto,
                "Asignada a " + tecnico.getNombre() + (nota == null ? "" : ". " + nota));

        emitirEvento(orden, EstadoOrden.ASIGNADA.name(), anterior, contexto, tecnico, nota);

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

    /**
     * Detalle de una orden con su historial. Quien la puede ver:
     * ADMIN, SUPERVISOR y AUDITOR, cualquiera de su empresa; el CLIENTE, las
     * que creo; el TECNICO, las que tiene asignadas. Si no le corresponde,
     * responde como si no existiera (404).
     */
    @Transactional
    public OrdenDetalleDto detalle(Long ordenId, ContextoEmpresa contexto) {
        OrdenDeTrabajo orden = buscarDeLaEmpresa(ordenId, contexto);
        if (!puedeVer(orden, contexto)) {
            throw new RecursoNoEncontrado("Orden de trabajo no encontrada");
        }
        List<CambioDeEstadoDto> pasos = historial
                .findByEmpresaIdAndOrdenIdOrderByFechaAscIdAsc(contexto.companyId(), orden.getId())
                .stream().map(CambioDeEstadoDto::desde).toList();
        return new OrdenDetalleDto(mapearADto(orden), pasos);
    }

    private boolean puedeVer(OrdenDeTrabajo orden, ContextoEmpresa contexto) {
        if (Permisos.gestiona(contexto) || contexto.tieneRol("AUDITOR")) {
            return true;
        }
        if (contexto.tieneRol("CLIENTE") && contexto.userOid() != null
                && contexto.userOid().equals(orden.getCreadorId())) {
            return true;
        }
        if (contexto.tieneRol(Permisos.TECNICO) && orden.getTecnicoId() != null) {
            return orden.getTecnicoId().equals(tecnicos.delUsuario(contexto).getId());
        }
        return false;
    }

    /**
     * Listado con filtros opcionales (estado y rango de fechas de creacion).
     * El CLIENTE ve solo las ordenes que creo; los demas, todas las de su
     * empresa.
     */
    @Transactional(readOnly = true)
    public List<OrdenRespuestaDto> listar(ContextoEmpresa contexto, EstadoOrden estado,
                                          LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalida("La fecha desde no puede ser posterior a hasta.");
        }
        List<OrdenDeTrabajo> base = contexto.esSoloCliente()
                ? repository.findByEmpresaIdAndCreadorId(contexto.companyId(), contexto.userOid())
                : repository.findByEmpresaId(contexto.companyId());
        return base.stream()
                .filter(o -> estado == null || o.getEstado() == estado)
                .filter(o -> desde == null || (o.getFechaCreacion() != null && !o.getFechaCreacion().isBefore(desde)))
                .filter(o -> hasta == null || (o.getFechaCreacion() != null && !o.getFechaCreacion().isAfter(hasta)))
                .sorted(java.util.Comparator.comparing(OrdenDeTrabajo::getFechaCreacion,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(this::mapearADto)
                .toList();
    }

    private void registrarPaso(OrdenDeTrabajo orden, EstadoOrden anterior, ContextoEmpresa contexto, String comentario) {
        historial.save(new CambioDeEstado(orden, anterior, contexto.userOid(), contexto.nombre(), comentario));
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.length() <= 500 ? limpio : limpio.substring(0, 500);
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

    private void emitirEvento(OrdenDeTrabajo orden, String tipoEvento, EstadoOrden anterior, ContextoEmpresa contexto,
                              String nota) {
        Tecnico tecnico = orden.getTecnicoId() == null ? null
                : tecnicoRepository.findById(orden.getTecnicoId()).orElse(null);
        emitirEvento(orden, tipoEvento, anterior, contexto, tecnico, nota);
    }

    private void emitirEvento(OrdenDeTrabajo orden, String tipoEvento, EstadoOrden anterior,
                              ContextoEmpresa contexto, Tecnico tecnico, String nota) {
        // Generamos un UUID unico para la idempotencia
        String eventKey = UUID.randomUUID().toString();
        // Si la peticion no trajo id de correlacion, el evento usa su propia
        // clave: asi siempre se puede seguir de punta a punta.
        String correlacion = contexto.correlacion() == null || contexto.correlacion().isBlank()
                ? eventKey : contexto.correlacion();

        OrdenEventoDto evento = new OrdenEventoDto(
                eventKey,
                tipoEvento,
                orden.getId(),
                contexto.userOid(),
                contexto.companyId(),
                LocalDateTime.now(),
                correlacion,
                anterior == null ? null : anterior.name(),
                orden.getEstado().name(),
                orden.getServicioId(),
                orden.getCreadorId(),
                orden.getComentarios(),
                tecnico == null ? null : tecnico.getId(),
                tecnico == null ? orden.getAsignadoA() : tecnico.getNombre(),
                tecnico == null ? null : tecnico.getEmail(),
                tecnico == null ? null : tecnico.getTelefono(),
                nota
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