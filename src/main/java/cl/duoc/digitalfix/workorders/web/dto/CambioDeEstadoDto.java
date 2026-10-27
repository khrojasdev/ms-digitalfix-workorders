package cl.duoc.digitalfix.workorders.web.dto;

import cl.duoc.digitalfix.workorders.domain.CambioDeEstado;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;

import java.time.LocalDateTime;

/** Un paso del historial de la orden. */
public record CambioDeEstadoDto(
        EstadoOrden estadoAnterior,
        EstadoOrden estadoNuevo,
        String autorId,
        String autorNombre,
        String comentario,
        LocalDateTime fecha
) {
    public static CambioDeEstadoDto desde(CambioDeEstado c) {
        return new CambioDeEstadoDto(c.getEstadoAnterior(), c.getEstadoNuevo(), c.getAutorId(),
                c.getAutorNombre(), c.getComentario(), c.getFecha());
    }
}
