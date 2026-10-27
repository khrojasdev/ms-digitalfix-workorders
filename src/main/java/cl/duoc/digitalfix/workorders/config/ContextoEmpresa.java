package cl.duoc.digitalfix.workorders.config;

import java.util.Set;

/**
 * Quien llama, segun las cabeceras que pone el BFF. email y nombre salen del
 * token: con ellos se reconoce (o se da de alta) la ficha del TECNICO la
 * primera vez que entra, y el nombre queda en el historial de la orden.
 */
public record ContextoEmpresa(String userOid, Long companyId, Set<String> roles, String correlacion,
                              String email, String nombre) {

    public ContextoEmpresa(String userOid, Long companyId, Set<String> roles, String correlacion) {
        this(userOid, companyId, roles, correlacion, null, null);
    }

    public ContextoEmpresa(String userOid, Long companyId, Set<String> roles, String correlacion, String email) {
        this(userOid, companyId, roles, correlacion, email, null);
    }

    public boolean tieneRol(String rol) {
        return roles != null && roles.contains(rol);
    }

    /** Solo CLIENTE (sin un rol de gestion o lectura de toda la empresa). */
    public boolean esSoloCliente() {
        return tieneRol("CLIENTE") && !tieneRol("ADMIN") && !tieneRol("SUPERVISOR") && !tieneRol("AUDITOR");
    }
}
