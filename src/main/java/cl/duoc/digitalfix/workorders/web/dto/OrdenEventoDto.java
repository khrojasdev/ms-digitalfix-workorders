package cl.duoc.digitalfix.workorders.web.dto;

import java.time.LocalDateTime;

public record OrdenEventoDto(
        String eventKey,       // Clave única de idempotencia
        String tipoEvento,     // ej: CREADA, ASIGNADA, EN_DESPLAZAMIENTO, EN_EJECUCION, CERRADA, CANCELADA
        Long ordenId,
        String autorId,        // OID del usuario en Azure AD
        Long empresaId,
        LocalDateTime fecha,
        String correlacionId
) {}