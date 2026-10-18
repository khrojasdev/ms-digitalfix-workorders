package cl.duoc.digitalfix.workorders.error;
public class SolicitudInvalida extends RuntimeException {
    public SolicitudInvalida(String mensaje) { super(mensaje); }
}