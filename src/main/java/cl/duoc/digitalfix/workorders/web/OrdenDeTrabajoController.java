package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.ContextoEmpresaHolder;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.service.OrdenDeTrabajoService;
import cl.duoc.digitalfix.workorders.web.dto.AsignacionDto;
import cl.duoc.digitalfix.workorders.web.dto.CambioEstadoDto;
import cl.duoc.digitalfix.workorders.web.dto.MisOrdenesDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenSolicitudDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<List<OrdenRespuestaDto>> listarOrdenes() {
        ContextoEmpresa contexto = obtenerContexto();
        List<OrdenRespuestaDto> ordenes = service.listarOrdenesPorEmpresa(contexto.companyId());
        return ResponseEntity.ok(ordenes);
    }

    /** Cambio de estado del caso: PUT /api/workorders/{id}/status con {"status": "EN_DESPLAZAMIENTO"}. */
    /** El TECNICO ve las ordenes que tiene asignadas (y su ficha). */
    @GetMapping("/mias")
    public ResponseEntity<MisOrdenesDto> misOrdenes() {
        return ResponseEntity.ok(service.misOrdenes(obtenerContexto()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrdenRespuestaDto> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoDto cambio) {
        ContextoEmpresa contexto = obtenerContexto();
        OrdenRespuestaDto respuesta = service.cambiarEstado(id, interpretar(cambio.status()), contexto);
        return ResponseEntity.ok(respuesta);
    }

    /** El supervisor asigna un tecnico: PUT /api/workorders/{id}/asignacion con {"tecnicoId": 3}. */
    @PutMapping("/{id}/asignacion")
    public ResponseEntity<OrdenRespuestaDto> asignarTecnico(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionDto asignacion) {
        ContextoEmpresa contexto = obtenerContexto();
        return ResponseEntity.ok(service.asignarTecnico(id, asignacion.tecnicoId(), contexto));
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