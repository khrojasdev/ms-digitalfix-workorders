package cl.duoc.digitalfix.workorders.web.dto;

import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import java.time.LocalDateTime;

public record OrdenRespuestaDto(
        Long id,
        Long servicioId,
        EstadoOrden estado,
        String asignadoA,
        String comentarios,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        Long tecnicoId,
        String creadorId
) {}
