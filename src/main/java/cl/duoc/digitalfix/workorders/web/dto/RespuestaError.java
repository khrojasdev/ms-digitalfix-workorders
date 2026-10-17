package cl.duoc.digitalfix.workorders.web.dto;

import java.time.Instant;

public record RespuestaError(String codigo, String mensaje, Instant timestamp) {
    public static RespuestaError crear(String codigo, String mensaje) {
        return new RespuestaError(codigo, mensaje, Instant.now());
    }
}