package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.ContextoEmpresaHolder;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.service.OrdenDeTrabajoService;
import cl.duoc.digitalfix.workorders.web.dto.AsignacionDto;
import cl.duoc.digitalfix.workorders.web.dto.CambioEstadoDto;
import cl.duoc.digitalfix.workorders.web.dto.MisOrdenesDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenDetalleDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenSolicitudDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/workorders")
public class OrdenDeTrabajoController {

    private final OrdenDeTrabajoService service;

    public OrdenDeTrabajoController(OrdenDeTrabajoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrdenRespuestaDto> crearOrden(@Valid @RequestBody OrdenSolicitudDto dto) {
        ContextoEmpresa contexto = obtenerContexto();
        OrdenRespuestaDto respuesta = service.crearOrden(dto, contexto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * GET /api/workorders?status=EN_EJECUCION&from=2026-10-01&to=2026-10-31
     * Todos los filtros son opcionales; las fechas son de creacion (from desde
     * las 00:00, to hasta las 23:59:59). El CLIENTE ve solo sus ordenes.
     */
    @GetMapping
    public ResponseEntity<List<OrdenRespuestaDto>> listarOrdenes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ContextoEmpresa contexto = obtenerContexto();
        EstadoOrden estado = status == null || status.isBlank() ? null : interpretar(status);
        List<OrdenRespuestaDto> ordenes = service.listar(contexto, estado,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ordenes);
    }

    /** El TECNICO ve las ordenes que tiene asignadas (y su ficha). */
    @GetMapping("/mias")
    public ResponseEntity<MisOrdenesDto> misOrdenes() {
        return ResponseEntity.ok(service.misOrdenes(obtenerContexto()));
    }

    /** Detalle de la orden con su historial de estados y comentarios. */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<OrdenDetalleDto> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(service.detalle(id, obtenerContexto()));
    }

    /**
     * Cambio de estado del caso: PUT /api/workorders/{id}/status con
     * {"status": "EN_DESPLAZAMIENTO", "comentario": "Salgo desde la bodega"}.
     * El comentario es opcional y queda en el historial de la orden.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrdenRespuestaDto> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoDto cambio) {
        ContextoEmpresa contexto = obtenerContexto();
        OrdenRespuestaDto respuesta = service.cambiarEstado(id, interpretar(cambio.status()), cambio.comentario(), contexto);
        return ResponseEntity.ok(respuesta);
    }

    /** El supervisor asigna un tecnico: PUT /api/workorders/{id}/asignacion con {"tecnicoId": 3}. */
    @PutMapping("/{id}/asignacion")
    public ResponseEntity<OrdenRespuestaDto> asignarTecnico(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionDto asignacion) {
        ContextoEmpresa contexto = obtenerContexto();
        return ResponseEntity.ok(service.asignarTecnico(id, asignacion.tecnicoId(), asignacion.comentario(), contexto));
    }

    /** Forma anterior (?nuevoEstado=), se mantiene para no romper al frontend actual. */
    @PutMapping("/{id}/estado")
    public ResponseEntity<OrdenRespuestaDto> cambiarEstadoPorParametro(
            @PathVariable Long id,
            @RequestParam String nuevoEstado) {
        ContextoEmpresa contexto = obtenerContexto();
        OrdenRespuestaDto respuesta = service.cambiarEstado(id, interpretar(nuevoEstado), contexto);
        return ResponseEntity.ok(respuesta);
    }

    private EstadoOrden interpretar(String texto) {
        EstadoOrden estado = EstadoOrden.desdeTexto(texto);
        if (estado == null) {
            throw new SolicitudInvalida("Estado desconocido: " + texto + ". Validos: "
                    + Arrays.toString(EstadoOrden.values()) + ".");
        }
        return estado;
    }

    private ContextoEmpresa obtenerContexto() {
        return ContextoEmpresaHolder.obtener()
                .orElseThrow(() -> new cl.duoc.digitalfix.workorders.error.AccesoDenegado("No autorizado: Falta contexto de empresa"));
    }

}