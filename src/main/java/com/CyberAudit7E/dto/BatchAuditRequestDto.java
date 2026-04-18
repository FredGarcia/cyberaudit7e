package com.cyberaudit7e.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO pour lancer un batch d'audits asynchrones.
 * M5 NOUVEAU.
 *
 * @param sites Liste des sites à auditer (max 10 pour le POC)
 */
public record BatchAuditRequestDto(

        @NotEmpty(message = "La liste des sites ne peut pas être vide")
        @Size(max = 10, message = "Maximum 10 sites par batch")
        @Valid
        List<AuditRequestDto> sites
) {}
