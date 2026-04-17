package com.cyberaudit7e.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration JPA.
 * @EnableJpaAuditing active les callbacks @PrePersist/@PreUpdate.
 *
 * Note : Spring Boot 4.x + Jackson 3 gère automatiquement
 * la sérialisation des dates Java 8 en ISO 8601. Plus besoin
 * de configurer un ObjectMapper bean manuellement.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}