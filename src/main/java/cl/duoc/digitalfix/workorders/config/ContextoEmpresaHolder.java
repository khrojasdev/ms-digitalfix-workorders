package cl.duoc.digitalfix.workorders.config;
import java.util.Optional;
public final class ContextoEmpresaHolder {
    private static final ThreadLocal<ContextoEmpresa> CONTEXTO = new ThreadLocal<>();
    private ContextoEmpresaHolder() {}
    public static void establecer(ContextoEmpresa contexto) { CONTEXTO.set(contexto); }
    public static Optional<ContextoEmpresa> obtener() { return Optional.ofNullable(CONTEXTO.get()); }
    public static void limpiar() { CONTEXTO.remove(); }
}