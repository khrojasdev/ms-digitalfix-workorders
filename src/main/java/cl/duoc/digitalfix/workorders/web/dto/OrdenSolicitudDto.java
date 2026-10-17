package cl.duoc.digitalfix.workorders.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrdenSolicitudDto(
        @NotNull(message = "El ID del servicio es obligatorio")
        Long servicioId,

        @Size(max = 500, message = "Los comentarios no pueden superar los 500 caracteres")
        String comentarios
) {}