package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.dto.RuleResultDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

/**
 * Converter JPA : List<RuleResultDto> ↔ JSON String (CLOB).
 *
 * M3 (fix Spring Boot 4) : migration Jackson 2 → Jackson 3.
 *
 * Changements Jackson 3 :
 * - Package : com.fasterxml.jackson.* → tools.jackson.*
 * - ObjectMapper (mutable) → JsonMapper (immutable, builder-based)
 * - readValue/writeValueAsString ne lèvent plus de checked exception
 * (JsonProcessingException → RuntimeException)
 *
 * Le JsonMapper est thread-safe et peut être déclaré en static final.
 *
 * Note : AttributeConverter est instancié par JPA (pas par Spring),
 * donc on ne peut pas @Autowire le JsonMapper auto-configuré.
 * On en instancie un dédié avec les defaults Jackson 3.
 */
@Converter
public class RuleResultListConverter implements AttributeConverter<List<RuleResultDto>, String> {

    private static final Logger log = LoggerFactory.getLogger(RuleResultListConverter.class);

    // JsonMapper thread-safe, ISO-8601 pour les dates par défaut en Jackson 3
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final TypeReference<List<RuleResultDto>> TYPE_REF = new TypeReference<>() {
    };

    /**
     * Java → BDD : sérialise la liste en JSON.
     * writeValueAsString en Jackson 3 ne lève plus de checked exception.
     */
    @Override
    public String convertToDatabaseColumn(List<RuleResultDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
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
        } catch (Exception e) {
            log.error("Erreur désérialisation JSON → RuleResults", e);
            return Collections.emptyList();
        }
    }
}
