package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.error.AccesoDenegado;
import cl.duoc.digitalfix.workorders.error.RecursoNoEncontrado;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.web.dto.RespuestaError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontrado.class)
    public ResponseEntity<RespuestaError> manejarNoEncontrado(RecursoNoEncontrado ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RespuestaError.crear("RECURSO_NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(AccesoDenegado.class)
    public ResponseEntity<RespuestaError> manejarAccesoDenegado(AccesoDenegado ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(RespuestaError.crear("ACCESO_DENEGADO", ex.getMessage()));
    }

    @ExceptionHandler(SolicitudInvalida.class)
    public ResponseEntity<RespuestaError> manejarSolicitudInvalida(SolicitudInvalida ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RespuestaError.crear("SOLICITUD_INVALIDA", ex.getMessage()));
    }

    // Atrapa los errores de validación (ej. @NotNull, @Size en los DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RespuestaError.crear("DATOS_INVALIDOS", mensaje));
    }
}