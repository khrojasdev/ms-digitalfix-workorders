package cl.duoc.digitalfix.workorders.error;

/** El cambio de estado pedido no respeta el ciclo de vida de la orden (409). */
public class TransicionInvalida extends RuntimeException {
    public TransicionInvalida(String mensaje) { super(mensaje); }
}
