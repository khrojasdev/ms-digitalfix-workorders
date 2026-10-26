package cl.duoc.digitalfix.workorders.config;

import java.util.Set;

/**
 * Quien llama, segun las cabeceras que pone el BFF. email sirve para vincular
 * la cuenta de un TECNICO con su ficha la primera vez que entra.
 */
public record ContextoEmpresa(String userOid, Long companyId, Set<String> roles, String correlacion, String email) {

    public ContextoEmpresa(String userOid, Long companyId, Set<String> roles, String correlacion) {
        this(userOid, companyId, roles, correlacion, null);
    }

    public boolean tieneRol(String rol) {
        return roles != null && roles.contains(rol);
    }
}
