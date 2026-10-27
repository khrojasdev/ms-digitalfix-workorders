package cl.duoc.digitalfix.workorders.web.dto;

import java.util.List;

/** GET /api/workorders/{id}: la orden y su historial, del primer paso al ultimo. */
public record OrdenDetalleDto(OrdenRespuestaDto orden, List<CambioDeEstadoDto> historial) {}
