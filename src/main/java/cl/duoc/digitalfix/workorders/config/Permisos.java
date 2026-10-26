package cl.duoc.digitalfix.workorders.config;

import cl.duoc.digitalfix.workorders.error.AccesoDenegado;

import java.util.Arrays;

/**
 * Chequeo de rol dentro del microservicio. El BFF ya filtra por rol, esto es
 * una segunda barrera para las operaciones del supervisor.
 */
public final class Permisos {

    public static final String[] GESTION = {"ADMIN", "SUPERVISOR"};
    public static final String TECNICO = "TECNICO";

    /** true si gestiona (ADMIN o SUPERVISOR). */
    public static boolean gestiona(ContextoEmpresa contexto) {
        return Arrays.stream(GESTION).anyMatch(contexto::tieneRol);
    }

    private Permisos() {}

    public static void exigirAlguno(ContextoEmpresa contexto, String... roles) {
        boolean tiene = contexto.roles() != null
                && Arrays.stream(roles).anyMatch(rol -> contexto.roles().contains(rol));
        if (!tiene) {
            throw new AccesoDenegado("Esta accion requiere uno de estos roles: " + String.join(", ", roles) + ".");
        }
    }
}
