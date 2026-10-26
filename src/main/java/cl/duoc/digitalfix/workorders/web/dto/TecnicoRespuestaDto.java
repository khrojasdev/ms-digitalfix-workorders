package cl.duoc.digitalfix.workorders.web.dto;

import cl.duoc.digitalfix.workorders.domain.Tecnico;

import java.time.LocalDateTime;

public record TecnicoRespuestaDto(
        Long id,
        String nombre,
        String email,
        String telefono,
        String especialidad,
        boolean activo,
        LocalDateTime fechaCreacion,
        /** Ya inicio sesion con su cuenta y quedo vinculado. */
        boolean cuentaVinculada
) {
    public static TecnicoRespuestaDto desde(Tecnico t) {
        return new TecnicoRespuestaDto(t.getId(), t.getNombre(), t.getEmail(), t.getTelefono(),
                t.getEspecialidad(), t.isActivo(), t.getFechaCreacion(), t.getUsuarioOid() != null);
    }
}
