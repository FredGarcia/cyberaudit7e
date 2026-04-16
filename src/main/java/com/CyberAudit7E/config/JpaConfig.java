package com.CyberAudit7E.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration JPA.
 *
 * M3 (fix Spring Boot 4) : suppression du bean ObjectMapper custom.
 * Jackson 3 est auto-configuré par Spring Boot 4 avec :
 * - Sérialisation ISO-8601 des dates par défaut (plus besoin de JavaTimeModule)
 * - JsonMapper immutable thread-safe
 * - Configuration via spring.jackson.* dans application.yml
 *
 * @EnableJpaAuditing active les callbacks @PrePersist/@PreUpdate
 *                    sur les entités JPA (Site, AuditReport).
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // Pas de bean ObjectMapper : Spring Boot 4 auto-configure JsonMapper (Jackson
    // 3)
}
