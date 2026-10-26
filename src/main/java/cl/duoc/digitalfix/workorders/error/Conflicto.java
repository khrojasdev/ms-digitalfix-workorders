package cl.duoc.digitalfix.workorders.error;

/** Los datos chocan con otros ya guardados, p.ej. un correo repetido (409). */
public class Conflicto extends RuntimeException {
    public Conflicto(String mensaje) { super(mensaje); }
}
