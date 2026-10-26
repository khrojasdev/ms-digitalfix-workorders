package cl.duoc.digitalfix.workorders.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ContextoEmpresaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String correlacion = request.getHeader("X-Correlation-Id");
        if (correlacion != null && !correlacion.isBlank()) MDC.put("correlacion", correlacion);
        try {
            String oid = request.getHeader("X-User-Oid");
            String empresaStr = request.getHeader("X-Company-Id");
            String rolesStr = request.getHeader("X-Roles");
            String email = request.getHeader("X-User-Email");
            if (empresaStr != null && !empresaStr.isBlank()) {
                Long companyId = Long.parseLong(empresaStr.trim());
                Set<String> roles = rolesStr == null || rolesStr.isBlank() ? Collections.emptySet() : Arrays.stream(rolesStr.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
                ContextoEmpresaHolder.establecer(new ContextoEmpresa(oid, companyId, roles, correlacion,
                        email == null || email.isBlank() ? null : email.trim().toLowerCase()));
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextoEmpresaHolder.limpiar();
            MDC.remove("correlacion");
        }
    }
}