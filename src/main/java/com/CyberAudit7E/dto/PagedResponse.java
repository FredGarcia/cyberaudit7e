package com.cyberaudit7e.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Réponse paginée standardisée.
 *
 * M6 NOUVEAU : enveloppe les résultats paginés avec métadonnées
 * (page courante, total, nombre de pages, liens prev/next).
 *
 * Utilisé par les endpoints GET qui retournent des listes.
 *
 * @param <T> Type des éléments
 */
@Schema(description = "Réponse paginée avec métadonnées")
public record PagedResponse<T>(

        @Schema(description = "Éléments de la page courante")
        List<T> content,

        @Schema(description = "Numéro de la page (0-based)", example = "0")
        int page,

        @Schema(description = "Taille de la page", example = "10")
        int size,

        @Schema(description = "Nombre total d'éléments", example = "42")
        long totalElements,

        @Schema(description = "Nombre total de pages", example = "5")
        int totalPages,

        @Schema(description = "Est-ce la première page ?")
        boolean first,

        @Schema(description = "Est-ce la dernière page ?")
        boolean last
) {

    /**
     * Factory depuis un objet Page Spring Data.
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Factory depuis une liste complète (pas de pagination).
     */
    public static <T> PagedResponse<T> fromList(List<T> list) {
        return new PagedResponse<>(
                list, 0, list.size(), list.size(), 1, true, true
        );
    }
}
