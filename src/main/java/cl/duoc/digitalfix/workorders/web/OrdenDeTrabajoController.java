package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.ContextoEmpresaHolder;
import cl.duoc.digitalfix.workorders.domain.EstadoOrden;
import cl.duoc.digitalfix.workorders.service.OrdenDeTrabajoService;
import cl.duoc.digitalfix.workorders.web.dto.OrdenRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.OrdenSolicitudDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}/estado")
    public ResponseEntity<OrdenRespuestaDto> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoOrden nuevoEstado) {
        ContextoEmpresa contexto = obtenerContexto();
        OrdenRespuestaDto respuesta = service.cambiarEstado(id, nuevoEstado, contexto);
        return ResponseEntity.ok(respuesta);
    }

    private ContextoEmpresa obtenerContexto() {
        return ContextoEmpresaHolder.obtener()
                .orElseThrow(() -> new cl.duoc.digitalfix.workorders.error.AccesoDenegado("No autorizado: Falta contexto de empresa"));
    }

}