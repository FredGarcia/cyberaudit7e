package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.dto.RuleResultDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Converter JPA : List<RuleResultDto> ↔ JSON String (CLOB).
 *
 * Appliqué automatiquement (@Converter autoApply=false) sur le champ
 * AuditReport.ruleResults via @Convert.
 *
 * Jackson (ObjectMapper) est disponible car spring-boot-starter-web
 * l'inclut dans le classpath.
 *
 * Concept pédagogique M3 :
 * JPA ne sait pas persister des types complexes (List de Records).
 * L'AttributeConverter fait le pont entre le modèle objet Java
 * et la colonne TEXT/CLOB de la BDD, de façon transparente.
 */
@Converter
public class RuleResultListConverter implements AttributeConverter<List<RuleResultDto>, String> {

    private static final Logger log = LoggerFactory.getLogger(RuleResultListConverter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<RuleResultDto>> TYPE_REF = new TypeReference<>() {};

    /**
     * Java → BDD : sérialise la liste en JSON.
     */
    @Override
    public String convertToDatabaseColumn(List<RuleResultDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Erreur sérialisation RuleResults → JSON", e);
            return "[]";
        }
    }

    /**
     * BDD → Java : désérialise le JSON en liste.
     */
    @Override
    public List<RuleResultDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, TYPE_REF);
        } catch (JsonProcessingException e) {
            log.error("Erreur désérialisation JSON → RuleResults", e);
            return Collections.emptyList();
        }
    }
}
