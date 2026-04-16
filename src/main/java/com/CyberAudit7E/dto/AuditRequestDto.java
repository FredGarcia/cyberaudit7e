package com.cyberaudit7e.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de requête pour lancer un audit.
 * La validation est assurée par Jakarta Bean Validation.
 *
 * @param url  URL du site à auditer (doit être HTTP/HTTPS)
 * @param name Nom lisible du site
 */
public record AuditRequestDto(

        @NotBlank(message = "L'URL est obligatoire")
        @Pattern(regexp = "^https?://.*", message = "L'URL doit commencer par http:// ou https://")
        String url,

        @NotBlank(message = "Le nom du site est obligatoire")
        String name
) {}
