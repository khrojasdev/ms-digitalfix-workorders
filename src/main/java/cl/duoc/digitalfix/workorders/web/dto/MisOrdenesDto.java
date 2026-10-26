package cl.duoc.digitalfix.workorders.web.dto;

import java.util.List;

/** Vista del TECNICO: su ficha y las ordenes que tiene asignadas. */
public record MisOrdenesDto(TecnicoRespuestaDto tecnico, List<OrdenRespuestaDto> ordenes) {}
