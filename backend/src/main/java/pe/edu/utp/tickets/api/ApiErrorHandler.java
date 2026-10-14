package pe.edu.utp.tickets.api;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger log=LoggerFactory.getLogger(ApiErrorHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> business(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields=new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Revisa los datos ingresados", "fields", fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> invalidRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Solicitud inválida"));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        // Do not log request payloads, exception messages or SQL values.
        log.error("Error inesperado de tipo {}", e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(Map.of("message", "No se pudo completar la operación"));
    }
}
