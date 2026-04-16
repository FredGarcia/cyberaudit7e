package com.CyberAudit7E.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestion globale des exceptions REST.
 * Produit des réponses JSON cohérentes pour toutes les erreurs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Erreurs de validation Jakarta Bean Validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalide",
                        (a, b) -> a  // en cas de doublon, garder le premier
                ));

        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Validation échouée",
                "details", fieldErrors,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * IllegalArgumentException (URL invalide, paramètre manquant, etc.)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Catch-all pour les erreurs non gérées.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of(
                "status", 500,
                "error", "Erreur interne du serveur",
                "message", ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue",
                "timestamp", Instant.now().toString()
        ));
    }
}
