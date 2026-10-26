package cl.duoc.digitalfix.workorders.web.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/workorders/{id}/asignacion. */
public record AsignacionDto(
        @NotNull(message = "El tecnico (tecnicoId) es obligatorio")
        Long tecnicoId
) {}
