package cl.duoc.DisGasBackend.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manejo de errores centralizado (@RestControllerAdvice).
 * En los servicios lanzamos ResponseStatusException(404, 409, ...) y aqui se transforman en un JSON uniforme.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** Errores que lanzamos nosotros: 400, 401, 403, 404, 409... */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarEstado(ResponseStatusException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", ex.getStatusCode().value());
        cuerpo.put("mensaje", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(cuerpo);
    }

    /** Falla alguna validacion del cuerpo (@NotBlank, @Email, @Min...): responde 400 con la lista de campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", 400);
        cuerpo.put("mensaje", "Datos invalidos");
        cuerpo.put("errores", errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
