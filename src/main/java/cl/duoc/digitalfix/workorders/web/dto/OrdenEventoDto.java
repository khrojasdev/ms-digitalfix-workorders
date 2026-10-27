package cl.duoc.digitalfix.workorders.web.dto;

import java.time.LocalDateTime;

/**
 * Evento de negocio que viaja por ordenes.negocio.exchange.
 *
 * Los primeros siete campos son los originales; el resto se agrego para que
 * los consumidores no tengan que consultar a workorders (catalog descuenta
 * stock con servicioId, notify arma el ticket de despacho con los datos del
 * tecnico). Los consumidores ignoran los campos que no conocen.
 */
public record OrdenEventoDto(
        String eventKey,          // Clave unica de idempotencia
        String tipoEvento,        // CREADA, ASIGNADA, EN_DESPLAZAMIENTO, EN_EJECUCION, CERRADA, CANCELADA
        Long ordenId,
        String autorId,           // OID de quien hizo el cambio
        Long empresaId,
        LocalDateTime fecha,
        String correlacionId,
        String estadoAnterior,    // null en CREADA; igual a estadoNuevo en una reasignacion
        String estadoNuevo,
        Long servicioId,
        String creadorId,         // OID de quien creo la orden (el cliente)
        String comentarios,
        Long tecnicoId,
        String tecnicoNombre,
        String tecnicoEmail,
        String tecnicoTelefono,
        String nota               // comentario opcional del cambio (lo escribe quien cambia el estado)
) {}
