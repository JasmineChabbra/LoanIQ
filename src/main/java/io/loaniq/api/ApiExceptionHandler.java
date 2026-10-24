package io.loaniq.api;

import io.loaniq.service.ApplicationNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns exceptions into a single, predictable error shape. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ApplicationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body("not_found", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Request failed validation.");
        return ResponseEntity.badRequest().body(body("invalid_request", detail));
    }

    private static Map<String, Object> body(String code, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("detail", detail);
        map.put("at", Instant.now().toString());
        return map;
    }
}
