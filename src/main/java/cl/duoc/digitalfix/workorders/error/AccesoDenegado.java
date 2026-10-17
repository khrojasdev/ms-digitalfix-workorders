package cl.duoc.digitalfix.workorders.error;
public class AccesoDenegado extends RuntimeException {
    public AccesoDenegado(String mensaje) { super(mensaje); }
}