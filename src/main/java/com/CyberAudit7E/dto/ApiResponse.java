package com.cyberaudit7e.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Enveloppe de réponse API standardisée.
 *
 * M6 NOUVEAU : toutes les réponses suivent le même format,
 * facilitant le parsing côté client (Vue.js, curl, etc.).
 *
 * Succès : { success:true, data:{...}, timestamp:"..." }
 * Erreur : { success:false, error:{status:400, message:"..."}, timestamp:"..." }
 *
 * @param <T> Type du payload data
 */
@Schema(description = "Enveloppe standard des réponses API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(

        @Schema(description = "true si la requête a réussi", example = "true")
        boolean success,

        @Schema(description = "Données de la réponse (absent si erreur)")
        T data,

        @Schema(description = "Détails de l'erreur (absent si succès)")
        Map<String, Object> error,

        @Schema(description = "Horodatage ISO 8601", example = "2026-04-17T22:15:02Z")
        String timestamp
) {

    /**
     * Factory pour une réponse succès.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().toString());
    }

    /**
     * Factory pour une réponse erreur.
     */
    public static <T> ApiResponse<T> fail(int status, String message) {
        return new ApiResponse<>(false, null,
                Map.of("status", status, "message", message),
                Instant.now().toString());
    }

    /**
     * Factory pour une réponse erreur avec détails.
     */
    public static <T> ApiResponse<T> fail(int status, String message, Map<String, ?> details) {
        return new ApiResponse<>(false, null,
                Map.of("status", status, "message", message, "details", details),
                Instant.now().toString());
    }
}
