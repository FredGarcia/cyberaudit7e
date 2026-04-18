package com.cyberaudit7e.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestion globale des exceptions REST.
 * M6 : réponses structurées cohérentes, logging, types d'erreur étendus.
 */
@RestControllerAdvice
@Hidden  // Ne pas afficher dans Swagger
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Erreurs de validation Jakarta (400).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalide",
                        (a, b) -> a
                ));
        log.warn("[VALIDATION] {} champ(s) invalide(s) : {}", fieldErrors.size(), fieldErrors.keySet());
        return ResponseEntity.badRequest().body(errorBody(400, "Validation échouée", fieldErrors));
    }

    /**
     * Paramètres de type incorrect (400).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("Paramètre '%s' : valeur '%s' invalide (attendu : %s)",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "inconnu");
        return ResponseEntity.badRequest().body(errorBody(400, message, null));
    }

    /**
     * IllegalArgumentException (400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(errorBody(400, ex.getMessage(), null));
    }

    /**
     * Ressource non trouvée (404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(
                errorBody(404, "Ressource non trouvée", Map.of("path", ex.getResourcePath())));
    }

    /**
     * Catch-all (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("[ERROR] Exception non gérée : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "Erreur interne du serveur",
                        Map.of("exception", ex.getClass().getSimpleName(),
                               "message", ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue")));
    }

    private Map<String, Object> errorBody(int status, String message, Map<String, ?> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", Map.of("status", status, "message", message));
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
