# CyberAudit7E — Module M7 : Docker & Synthèse Axiome 7E

## Dockerisation

### Fichiers Docker fournis

| Fichier | Rôle |
|---------|------|
| `docker/Dockerfile` | Build multi-stage (JDK 21 → JRE Alpine) |
| `docker/docker-compose.yml` | Dev (H2) + Prod (PostgreSQL 16) avec profiles |
| `docker/.dockerignore` | Optimise le contexte de build |
| `docker/nginx.conf` | Reverse proxy Nginx (optionnel, config SSE) |

### Copier les fichiers Docker à la racine du projet

```bash
cp docker/Dockerfile <votre-projet>/Dockerfile
cp docker/.dockerignore <votre-projet>/.dockerignore
cp docker/docker-compose.yml <votre-projet>/docker-compose.yml
cp docker/nginx.conf <votre-projet>/nginx.conf
```

### Mode DEV (H2 in-memory)

```bash
# Build + run (première fois)
docker compose up --build

# Runs suivants (image déjà buildée)
docker compose up

# Logs
docker compose logs -f cyberaudit7e

# Vérifier le health
curl -s http://localhost:8080/api/health | jq .runtime
```

### Mode PROD (PostgreSQL)

```bash
# Lancer avec le profile prod
docker compose --profile prod up --build

# PostgreSQL démarre d'abord (healthcheck pg_isready)
# Spring Boot attend que Postgres soit healthy (depends_on condition)
# Flyway crée les tables automatiquement

# Se connecter à PostgreSQL
docker exec -it cyberaudit7e-db psql -U audit -d cyberaudit7e

# Requêtes utiles
SELECT * FROM sites;
SELECT id, site_id, score_global, trend FROM audit_reports ORDER BY audited_at DESC;
SELECT * FROM rule_configs;
SELECT * FROM flyway_schema_history;
```

### Commandes Docker utiles

```bash
# Statut des services
docker compose ps

# Logs d'un service
docker compose logs -f cyberaudit7e-app

# Shell dans le conteneur
docker exec -it cyberaudit7e-app sh

# Taille de l'image
docker images | grep cyberaudit7e

# Arrêter et nettoyer
docker compose down
docker compose down -v  # + supprime les volumes (PostgreSQL data)

# Rebuild après modification du code
docker compose up --build --force-recreate
```

## Ajout M7 : métriques runtime

Le health check retourne maintenant les infos container :

```json
{
  "runtime": {
    "javaVersion": "21.0.x",
    "heapUsedMb": 128,
    "heapMaxMb": 512,
    "heapPercent": 25,
    "availableProcessors": 4,
    "uptimeSeconds": 3600,
    "uptime": "1h00m00s",
    "hostname": "abc123def456"
  }
}
```

Le `hostname` correspond au container ID Docker — utile pour identifier
l'instance dans un environnement multi-réplicas.

## Nouvelle dépendance pom.xml (M7)

Ajouter le driver PostgreSQL pour le profile prod :

```xml
<!-- PostgreSQL driver (runtime only — utilisé uniquement en prod) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Note : cette dépendance est probablement déjà présente si vous avez
sélectionné "PostgreSQL Driver" dans Spring Initializr en M1.

---

## Synthèse complète : Axiome 7E × Spring Boot

### Le cycle 7E dans CyberAudit7E

```
   ┌──────────────────────────────────────────────────────────────────┐
   │                    AXIOME 7E — CYCLE CYBERNÉTIQUE               │
   │                                                                  │
   │  "Les Éléments dans l'Espace Engendrent un État d'Expression    │
   │   Évolutif de l'Environnement"                                  │
   │                                                                  │
   │     ┌─────────┐    ┌─────────┐    ┌─────────┐                  │
   │     │ ÉVALUER  │───→│ÉLABORER │───→│EXÉCUTER │                  │
   │     │  (M3)    │    │  (M4)   │    │  (M4)   │                  │
   │     └─────────┘    └─────────┘    └────┬────┘                  │
   │          ▲                              │                        │
   │          │                              ▼                        │
   │  ┌───────────┐                   ┌─────────┐                   │
   │  │ÉQUILIBRER │                   │EXAMINER │                   │
   │  │  (M5)     │                   │  (M4)   │                   │
   │  └───────────┘                   └────┬────┘                   │
   │          ▲                              │                        │
   │          │                              ▼                        │
   │     ┌─────────┐    ┌─────────┐    ┌─────────┐                  │
   │     │ ÉMETTRE  │←───│ ÉVOLUER │←───│         │                  │
   │     │  (M5)    │    │  (M3)   │    │         │                  │
   │     └─────────┘    └─────────┘    └─────────┘                  │
   └──────────────────────────────────────────────────────────────────┘
```

### Mapping Phase 7E → Concept Spring → Module

| Phase 7E | Classe Spring | Concept démontré | Module |
|----------|--------------|------------------|--------|
| **Évaluer** | `EvaluateService` | `@Service`, IoC, injection constructeur | M2 |
| **Élaborer** | `ElaborateService` + `AuditRule` | Strategy Pattern, `List<Interface>` auto-injectée | M2, M4 |
| **Exécuter** | `AuditEngine` + `HtmlFetcherService` | Jsoup crawl, `@Component` scan, try/catch résilient | M4 |
| **Examiner** | `ScoringService` + `RuleConfigRepository` | JPA, `@Query` JPQL, config dynamique BDD | M3, M4 |
| **Évoluer** | `EvolveService` + `AuditReportRepository` | Spring Data Query Methods, Flyway migrations | M3 |
| **Émettre** | `EmitService` + `SseNotificationService` | `ApplicationEvent`, SSE streaming, `@EventListener` | M5 |
| **Équilibrer** | `FeedbackLoopListener` | `@Async`, `@Transactional`, rétroaction BDD | M5 |

### Mapping Concept Spring → Module de la formation

| Concept Spring | Module | Fichier clé |
|---------------|--------|-------------|
| `@SpringBootApplication` | M1 | `CyberAudit7eApplication.java` |
| `@RestController`, `@GetMapping` | M1 | `HealthController.java` |
| IoC, injection par constructeur | M2 | `AuditEngine.java` |
| Strategy Pattern + `@Component` | M2, M4 | `AuditRule.java` + 13 implémentations |
| Spring Profiles (`dev`/`prod`) | M2 | `application-dev.yml` / `application-prod.yml` |
| `@Entity`, `@Table`, `@ManyToOne` | M3 | `Site.java`, `AuditReport.java` |
| `JpaRepository`, Query Methods | M3 | `SiteRepository.java` |
| Flyway migrations SQL | M3 | `V1__create_schema.sql` → `V4` |
| `@Transactional` | M3 | `AuditOrchestrator.java` |
| JPA `AttributeConverter` | M3 | `RuleResultListConverter.java` |
| DTO pattern (Records Java) | M3, M6 | `SiteDto`, `ReportSummaryDto`, `ApiResponse` |
| Jsoup HTTP crawl | M4 | `HtmlFetcherService.java` |
| Config dynamique en BDD | M4 | `RuleConfig.java` + `RuleConfigRepository` |
| `@Async` + `CompletableFuture` | M5 | `AsyncAuditService.java` |
| `@Scheduled` + cron | M5 | `ScheduledAuditService.java` |
| `ApplicationEvent` (3 types) | M5 | `AuditStartedEvent`, `AuditProgressEvent`, `AuditCompletedEvent` |
| `@EventListener` | M5 | `SseNotificationService`, `AuditMetricsListener`, `FeedbackLoopListener` |
| `SseEmitter` (Server-Sent Events) | M5 | `SseNotificationService.java` |
| `ThreadPoolTaskExecutor` | M5 | `AsyncConfig.java` |
| SpringDoc OpenAPI / Swagger | M6 | `OpenApiConfig.java` |
| `@Operation`, `@Parameter`, `@Tag` | M6 | Tous les controllers |
| Pagination (`Page`, `Pageable`) | M6 | `AuditReportRepository`, `PagedResponse` |
| Validation Jakarta (`@NotBlank`, `@Pattern`) | M6 | `AuditRequestDto`, `CreateSiteRequest` |
| `@RestControllerAdvice` | M6 | `GlobalExceptionHandler.java` |
| Dockerfile multi-stage | M7 | `Dockerfile` |
| Docker Compose + profiles | M7 | `docker-compose.yml` |
| HEALTHCHECK Docker | M7 | `Dockerfile` |
| Nginx reverse proxy (SSE) | M7 | `nginx.conf` |

### Convergence des 3 projets dans CyberAudit7E

| Projet source | Concept repris | Implémentation Spring |
|--------------|----------------|----------------------|
| **GitManager** | Registre d'organes (services) | `SiteRepository` + CRUD REST |
| **GitManager** | Noms biologiques (Moëlle, Cortex) | 7 services nommés par phase 7E |
| **GitManager** | Redis Streams bridge (DB5) | `ApplicationEvent` + `@EventListener` |
| **GitManager** | Dokploy integration | Docker Compose + profiles |
| **AuditAccess** | Moteur 17 règles (Django) | 13 `AuditRule` @Component (Strategy) |
| **AuditAccess** | Scoring RGAA×0.5+WCAG×0.3+DSFR×0.2 | `ScoringService` + `RuleConfig` BDD |
| **AuditAccess** | Celery async tasks | `@Async` + `AsyncAuditService` |
| **AuditAccess** | CRI v2.1 multi-services | Docker Compose (app + db) |
| **AuditAccess** | Crawler Playwright | `HtmlFetcherService` (Jsoup) |
| **Axiome 7E** | 7 phases cybernétiques | 7 `@Service` dans `service/cycle/` |
| **Axiome 7E** | Boucle de rétroaction | `FeedbackLoopListener` ajuste les poids |
| **Axiome 7E** | Observation de 2e ordre | `EvolveService` compare les audits |
| **Axiome 7E** | Expression évolutive | Progression SSE temps réel |

### Statistiques finales du POC

| Métrique | Valeur |
|----------|--------|
| Fichiers Java | 61 |
| Règles d'audit | 13 (4 RGAA + 5 WCAG + 3 DSFR + 1 contraste) |
| Migrations Flyway | 4 |
| Endpoints REST | 22 |
| Événements Spring | 3 |
| Tags OpenAPI | 6 |
| Lignes de code | ~5000 |
| Modules de formation | 7 (8h) |

### Pour aller plus loin

| Direction | Technologie | Effort |
|-----------|------------|--------|
| Tests unitaires + intégration | JUnit 5, Mockito, Testcontainers | 1 jour |
| Sécurité API | Spring Security + JWT | 1/2 journée |
| Dashboard Vue.js | Vue 3 + Pinia + EventSource SSE | 1-2 jours |
| Monitoring | Spring Actuator + Micrometer + Grafana | 1/2 journée |
| CI/CD | Gitea webhooks + Dokploy auto-deploy | 1/2 journée |
| Crawler avancé | Playwright (headless Chrome) au lieu de Jsoup | 1 jour |
| Cybernétique L3 | Causal Discovery Engine + GENESIS_FREEZE | Recherche |
| Multi-tenant | Spring Security + schémas PostgreSQL par tenant | 2 jours |
| Kubernetes | Helm chart + HPA sur les métriques d'audit | 1 jour |
