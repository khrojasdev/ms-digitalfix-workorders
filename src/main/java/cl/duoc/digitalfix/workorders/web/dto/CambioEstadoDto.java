package cl.duoc.digitalfix.workorders.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de PUT /api/workorders/{id}/status. El comentario es opcional. */
public record CambioEstadoDto(
        @NotBlank(message = "El estado (status) es obligatorio")
        String status,

        @Size(max = 500, message = "El comentario admite hasta 500 caracteres")
        String comentario
) {
    public CambioEstadoDto(String status) {
        this(status, null);
    }
}
