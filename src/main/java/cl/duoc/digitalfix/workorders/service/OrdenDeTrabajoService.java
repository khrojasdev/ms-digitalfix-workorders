package cl.duoc.digitalfix.workorders.service;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.domain.OrdenDeTrabajo;
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

        orden.setEstado(nuevoEstado);
        orden = repository.save(orden);

        emitirEvento(orden, nuevoEstado.name(), contexto);

        return mapearADto(orden);
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