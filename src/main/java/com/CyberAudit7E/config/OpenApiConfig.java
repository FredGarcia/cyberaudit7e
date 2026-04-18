package com.cyberaudit7e.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI / Swagger.
 *
 * M6 NOUVEAU : génère automatiquement la documentation interactive
 * accessible sur /swagger-ui.html et /v3/api-docs (JSON/YAML).
 *
 * SpringDoc scanne les @RestController et les annotations
 * @Operation, @Parameter, @Schema pour produire le spec OpenAPI 3.0.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cyberAudit7eOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CyberAudit7E API")
                        .version("1.0.0 — M6")
                        .description("""
                                Moteur d'audit d'accessibilité cybernétique basé sur l'Axiome 7E.

                                **Cycle 7E** : Évaluer → Élaborer → Exécuter → Examiner → Évoluer → Émettre → Équilibrer

                                **Référentiels** : RGAA 4.1 (×0.5) + WCAG 2.2 (×0.3) + DSFR (×0.2)

                                **Architecture** : Spring Boot 3.4 + Jsoup + JPA/H2 + Spring Events + SSE

                                Inspiré de GitManager × AuditAccess × Axiome 7E.
                                """)
                        .contact(new Contact()
                                .name("CyberAudit7E")
                                .url("https://cyberaudit7e.local"))
                        .license(new License()
                                .name("POC — Usage formation")
                                .url("https://cyberaudit7e.local/license")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Développement local"),
                        new Server().url("https://cyberaudit7e.local").description("Production (Dokploy)")
                ))
                .tags(List.of(
                        new Tag().name("Santé").description("Health check et index API"),
                        new Tag().name("Sites").description("CRUD des sites à auditer"),
                        new Tag().name("Audits").description("Lancement d'audits synchrones et consultation des rapports"),
                        new Tag().name("Audits Async").description("Audits asynchrones, batch et streaming SSE"),
                        new Tag().name("Scheduler").description("Audits programmés automatiques"),
                        new Tag().name("Configuration").description("Poids de scoring dynamiques")
                ));
    }
}
