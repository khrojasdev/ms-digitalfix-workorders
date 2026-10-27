package cl.duoc.digitalfix.workorders.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de PUT /api/workorders/{id}/asignacion. El comentario es opcional. */
public record AsignacionDto(
        @NotNull(message = "El tecnico (tecnicoId) es obligatorio")
        Long tecnicoId,

        @Size(max = 500, message = "El comentario admite hasta 500 caracteres")
        String comentario
) {
    public AsignacionDto(Long tecnicoId) {
        this(tecnicoId, null);
    }
}
