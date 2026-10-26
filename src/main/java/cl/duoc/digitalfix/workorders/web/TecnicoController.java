package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.ContextoEmpresaHolder;
import cl.duoc.digitalfix.workorders.error.AccesoDenegado;
import cl.duoc.digitalfix.workorders.service.TecnicoService;
import cl.duoc.digitalfix.workorders.web.dto.TecnicoRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.TecnicoSolicitudDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tecnicos de terreno: GET, POST, PUT y DELETE (baja logica). Solo ADMIN o SUPERVISOR. */
@RestController
@RequestMapping("/api/workorders/tecnicos")
public class TecnicoController {

    private final TecnicoService service;

    public TecnicoController(TecnicoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TecnicoRespuestaDto> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return service.listar(soloActivos, contexto());
    }

    @PostMapping
    public ResponseEntity<TecnicoRespuestaDto> crear(@Valid @RequestBody TecnicoSolicitudDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, contexto()));
    }

    @PutMapping("/{id}")
    public TecnicoRespuestaDto actualizar(@PathVariable Long id, @Valid @RequestBody TecnicoSolicitudDto dto) {
        return service.actualizar(id, dto, contexto());
    }

    @DeleteMapping("/{id}")
    public TecnicoRespuestaDto desactivar(@PathVariable Long id) {
        return service.desactivar(id, contexto());
    }

    private ContextoEmpresa contexto() {
        return ContextoEmpresaHolder.obtener()
                .orElseThrow(() -> new AccesoDenegado("No autorizado: Falta contexto de empresa"));
    }
}
