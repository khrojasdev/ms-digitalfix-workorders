package cl.duoc.digitalfix.workorders.config;
import java.util.Set;
public record ContextoEmpresa(String userOid, Long companyId, Set<String> roles, String correlacion) {}