package cl.duoc.digitalfix.workorders.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta o edicion de un tecnico. */
public record TecnicoSolicitudDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 150, message = "El correo admite hasta 150 caracteres")
        String email,

        @Size(max = 30, message = "El telefono admite hasta 30 caracteres")
        String telefono,

        @Size(max = 100, message = "La especialidad admite hasta 100 caracteres")
        String especialidad,

        /** Solo en la edicion: permite reactivar a un tecnico desactivado. Null = no cambia. */
        Boolean activo
) {}
