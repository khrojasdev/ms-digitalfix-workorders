package cl.duoc.digitalfix.workorders.domain;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Ciclo de vida de una orden segun el caso DigitalFix:
 *
 * <pre>
 * CREADA -> ASIGNADA -> EN_DESPLAZAMIENTO -> EN_EJECUCION -> CERRADA
 *    \          \               \
 *     +----------+---------------+--> CANCELADA
 * </pre>
 *
 * ASIGNADA solo se alcanza asignando un tecnico (PUT /{id}/asignacion), no con
 * un cambio de estado suelto. CERRADA y CANCELADA son finales.
 */
public enum EstadoOrden {
    CREADA,
    ASIGNADA,
    EN_DESPLAZAMIENTO,
    EN_EJECUCION,
    CERRADA,
    CANCELADA;

    /** Estados a los que se puede pasar con un cambio de estado (sin contar la asignacion). */
    public Set<EstadoOrden> siguientes() {
        return switch (this) {
            case CREADA -> EnumSet.of(CANCELADA);
            case ASIGNADA -> EnumSet.of(EN_DESPLAZAMIENTO, CANCELADA);
            case EN_DESPLAZAMIENTO -> EnumSet.of(EN_EJECUCION, CANCELADA);
            case EN_EJECUCION -> EnumSet.of(CERRADA);
            case CERRADA, CANCELADA -> EnumSet.noneOf(EstadoOrden.class);
        };
    }

    public boolean puedePasarA(EstadoOrden destino) {
        return siguientes().contains(destino);
    }

    public boolean esFinal() {
        return this == CERRADA || this == CANCELADA;
    }

    /** Solo se asigna (o reasigna) tecnico antes de que salga a terreno. */
    public boolean admiteAsignacion() {
        return this == CREADA || this == ASIGNADA;
    }

    /**
     * Interpreta el estado que manda el cliente. Acepta mayusculas o minusculas,
     * con o sin tilde ("EN_EJECUCIÓN") y con espacios en vez de guion bajo.
     * INICIADA es el nombre anterior de EN_EJECUCION y se sigue aceptando.
     *
     * @return el estado, o null si el texto no corresponde a ninguno
     */
    public static EstadoOrden desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if (limpio.equals("INICIADA")) {
            return EN_EJECUCION;
        }
        for (EstadoOrden estado : values()) {
            if (estado.name().equals(limpio)) {
                return estado;
            }
        }
        return null;
    }
}
