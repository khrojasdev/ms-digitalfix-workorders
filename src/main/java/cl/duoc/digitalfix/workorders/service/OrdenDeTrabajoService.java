package cl.duoc.digitalfix.workorders.service;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.domain.OrdenDeTrabajo;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.error.TransicionInvalida;
import cl.duoc.digitalfix.workorders.repository.OrdenDeTrabajoRepository;
import cl.duoc.digitalfix.workorders.web.dto.OrdenEventoDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenSolicitudDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrdenDeTrabajoService {

    private final OrdenDeTrabajoRepository repository;
    private final OrdenEventoPublisher publisher;

    public OrdenDeTrabajoService(OrdenDeTrabajoRepository repository, OrdenEventoPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
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

        emitirEvento(orden, "CREADA", contexto);

        return mapearADto(orden);
    }

    @Transactional
    public OrdenRespuestaDto cambiarEstado(Long ordenId, EstadoOrden nuevoEstado, ContextoEmpresa contexto) {
        OrdenDeTrabajo orden = repository.findById(ordenId)
                .orElseThrow(() -> new cl.duoc.digitalfix.workorders.error.RecursoNoEncontrado("Orden de trabajo no encontrada"));

        // Validar aislamiento de datos
        if (!orden.getEmpresaId().equals(contexto.companyId())) {
            // Simulamos 404 por seguridad para no revelar que el ID existe en otra empresa
            throw new cl.duoc.digitalfix.workorders.error.RecursoNoEncontrado("Orden de trabajo no encontrada");
        }

        validarTransicion(orden, nuevoEstado);

        orden.setEstado(nuevoEstado);
        orden = repository.save(orden);

        emitirEvento(orden, nuevoEstado.name(), contexto);

        return mapearADto(orden);
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

    private void emitirEvento(OrdenDeTrabajo orden, String tipoEvento, ContextoEmpresa contexto) {
        // Generamos un UUID único para la idempotencia
        String eventKey = UUID.randomUUID().toString();

        OrdenEventoDto evento = new OrdenEventoDto(
                eventKey,
                tipoEvento,
                orden.getId(),
                contexto.userOid(),
                contexto.companyId(),
                LocalDateTime.now(),
                contexto.correlacion()
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
                orden.getFechaActualizacion()
        );
    }
}