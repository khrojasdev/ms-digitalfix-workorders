package cl.duoc.digitalfix.workorders.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de PUT /api/workorders/{id}/status. */
public record CambioEstadoDto(
        @NotBlank(message = "El estado (status) es obligatorio")
        String status
) {}
