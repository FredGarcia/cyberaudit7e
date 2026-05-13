# Auditaccess

**Moteur d'audit d'accessibilite Devops**

> *Les Elements dans l'Espace Engendrent un Etat d'Expression Evolutif de l'Environnement*

Auditaccess est une plateforme construite sur Spring Boot pour l'audit d'accessibilité web qui évalue les sites selon trois référentiels (RGAA 4.1, WCAG 2.2, DSFR), implémente un cycle Devops auto-adaptatif a 7 phases (Évaluer Élaborer Exécuter Examiner Évoluer Émettre Équilibrer), et s'intègre avec ServiceNow (ITSM) et SailPoint (Gouvernance des identités) pour un suivi unifié des tickets de securité et d’accessibilité.

---

## Table des matieres

- [1. Vue d'ensemble](#1-vue-densemble)
- [2. Architecture technique](#2-architecture-technique)
- [3. Le cycle Axiome 7E](#3-le-cycle-axiome-7e)
- [4. Les 13 regles d'audit](#4-les-13-regles-daudit)
- [5. Scoring et seuils configurables](#5-scoring-et-seuils-configurables)
- [6. API REST -- Reference complete](#6-api-rest----reference-complete)
- [7. Integration ServiceNow](#7-integration-servicenow)
- [8. Integration SailPoint](#8-integration-sailpoint)
- [9. Tickets de securite unifies](#9-tickets-de-securite-unifies)
- [10. Dashboards Vue.js](#10-dashboards-vuejs)
- [11. Installation et deploiement](#11-installation-et-deploiement)
- [12. Configuration](#12-configuration)
- [13. Docker](#13-docker)
- [14. Tests](#14-tests)
- [15. Modules de formation M1-M7](#15-modules-de-formation-m1-m7)
- [16. Depannage](#16-depannage)
- [17. Securite](#17-securite)
- [18. Statistiques du projet](#18-statistiques-du-projet)

---

## 1. Vue d'ensemble

### Ce que fait Auditaccess

- **Crawle** les sites web via Jsoup (HTTP reel, analyse du DOM)
- **Evalue** 13 regles d'accessibilite couvrant RGAA 4.1, WCAG 2.2 et DSFR
- **Calcule** un score composite pondere : `score = RGAA x 0.5 + WCAG x 0.3 + DSFR x 0.2`
- **S'auto-adapte** via une boucle de retroaction Devops (poids de scoring ajustes automatiquement)
- **Streame** la progression en temps reel via SSE (Server-Sent Events)
- **Cree automatiquement** des incidents ServiceNow quand le score passe sous un seuil configurable
- **Recoit** les violations SailPoint via webhooks et les transforme en tickets
- **Centralise** tous les evenements de securite dans un modele de ticket unifie

### Flux triangulaire

```
SailPoint                     Auditaccess                     ServiceNow
Identity Cloud                (Spring Boot)                     ITSM
    |                              |                              |
    |--- Event Triggers ---------> |                              |
    |    (webhook POST)            |                              |
    |                              |--- Table API POST ---------> |
    |                              |    (creation incident)       |
    |                              |                              |
    | <--- API V3 GET ----------- |                              |
    |      (enrichissement)        |                              |
    |                              | <--- Business Rule --------- |
    |                              |      (callback statut)       |
    |                              |                              |
    |                         SecurityTicket                      |
    |                         (modele unifie)                     |
```

### Stack technique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Backend | Spring Boot | 3.4.x |
| Java | Eclipse Temurin | 25 LTS |
| Crawler HTML | Jsoup | 1.18.3 |
| Persistance | Spring Data JPA + H2/PostgreSQL | -- |
| Migrations | Flyway (6 migrations) | -- |
| Documentation API | SpringDoc OpenAPI | 2.8.4 |
| Frontend | Vue.js 3 (CDN) | 3.x |
| Conteneurisation | Docker + Docker Compose | -- |
| ITSM | ServiceNow Table API | -- |
| IAM/IGA | SailPoint Identity Security Cloud V3 | -- |

---

## 2. Architecture technique

### Architecture en couches

```
+---------------------------------------------------------------+
|                    COUCHE PRESENTATION                        |
|  Vue.js Dashboards (admin / exploitation / integrations)     |
|  Swagger UI (/swagger-ui.html)                               |
+---------------------------------------------------------------+
|                    COUCHE API REST                            |
|  @RestController                                             |
|  HealthCtrl | SiteCtrl | AuditCtrl | ConfigCtrl | TicketCtrl |
|  SailPointWebhookCtrl | ServiceNowWebhookCtrl               |
+---------------------------------------------------------------+
|                    COUCHE SERVICE                             |
|  AuditEngine | AuditOrchestrator | ScoringService            |
|  AsyncAuditService | ScheduledAuditService                   |
|  SseNotificationService | HtmlFetcherService                 |
|  TicketOrchestrator                                          |
|  Cycle 7E : Evaluate | Elaborate | Execute | Examine         |
|             Evolve | Emit | FeedbackLoopListener             |
+---------------------------------------------------------------+
|                    COUCHE INTEGRATION                         |
|  ServiceNowClient (OAuth 2.0 / Basic Auth)                   |
|  SailPointClient (OAuth 2.0 PAT)                             |
+---------------------------------------------------------------+
|                    COUCHE DOMAINE                             |
|  Entities : Site | AuditReport | RuleConfig | SecurityTicket |
|             SystemSetting                                    |
|  Rules    : AuditRule (interface) -> 13 @Component           |
+---------------------------------------------------------------+
|                    COUCHE DONNEES                             |
|  Spring Data JPA | Flyway (6 migrations)                     |
|  H2 (dev) | PostgreSQL 16 (prod)                             |
+---------------------------------------------------------------+
```

### Modele de donnees

```
+------------------+       +----------------------+
|      sites       |       |    audit_reports      |
+------------------+       +----------------------+
| id          PK   |--+    | id              PK   |
| url         UK   |  |    | site_id         FK   |<--+
| name             |  +--->| score_rgaa           |   |
| current_phase    |       | score_wcag           |   |
| created_at       |       | score_dsfr           |   |
| updated_at       |       | score_global         |   |
+------------------+       | trend                |   |
                           | results_json  (CLOB) |   |
                           | audited_at           |   |
                           +----------------------+   |
                                                      |
+------------------+       +----------------------+   |
|  rule_configs    |       |  security_tickets    |   |
+------------------+       +----------------------+   |
| id          PK   |       | id              PK   |   |
| category    UK   |       | title                |   |
| weight           |       | source               |   |
| enabled          |       | status               |   |
| description      |       | severity             |   |
+------------------+       | sailpoint_*  (refs)  |   |
                           | servicenow_* (refs)  |   |
+------------------+       | audit_report_id FK   |---+
| system_settings  |       | site_url             |
+------------------+       | assigned_to          |
| setting_key  PK  |       | raw_payload   (CLOB) |
| setting_value    |       | created_at           |
| description      |       | resolved_at          |
| updated_at       |       +----------------------+
+------------------+
```

### Flux evenementiel

```
Client POST /api/audits
    |
    v
AuditOrchestrator.executeFullCycle()
    |
    +---> publie AuditStartedEvent
    |         |---> SseNotificationService -> SSE "audit-started"
    |         +---> AuditMetricsListener   -> compteur++
    |
    +---> Phase 1-7 : publie 7x AuditProgressEvent
    |         +---> SseNotificationService -> SSE "audit-progress" (14%->100%)
    |
    +---> EmitService publie AuditCompletedEvent
    |         |---> SseNotificationService -> SSE "audit-completed"
    |         |---> AuditMetricsListener   -> duree, compteurs
    |         +---> FeedbackLoopListener   -> ajuste poids en BDD
    |                   |
    |                   +---> TicketOrchestrator (si score < seuil configurable)
    |                              |
    |                              +---> ServiceNowClient.createIncident()
    |
    +---> retourne AuditResponseDto au client
```

### Arborescence du projet

```
Auditaccess/
+-- pom.xml
+-- Dockerfile
+-- docker-compose.yml
+-- .env
+-- .dockerignore
+-- nginx.conf
+-- tests-Auditaccess.bat
+-- tests-threshold.bat
|
+-- src/main/java/com/Auditaccess/
|   +-- AuditaccessApplication.java
|   +-- config/
|   |   +-- AsyncConfig.java                # ThreadPool 4->10, @EnableAsync
|   |   +-- IntegrationProperties.java      # @ConfigurationProperties SNOW/SP
|   |   +-- JpaConfig.java                  # @EnableJpaAuditing
|   |   +-- OpenApiConfig.java              # Swagger/OpenAPI 3.0
|   |   +-- RestTemplateConfig.java         # HTTP client integrations
|   |   +-- WebConfig.java                  # CORS (Vue.js)
|   +-- controller/
|   |   +-- HealthController.java           # /api/health, /api/
|   |   +-- SiteController.java             # CRUD /api/sites
|   |   +-- AuditController.java            # /api/audits (sync/async/batch/SSE)
|   |   +-- ConfigController.java           # /api/config (weights + settings + threshold)
|   |   +-- GlobalExceptionHandler.java     # @RestControllerAdvice
|   +-- service/
|   |   +-- AuditEngine.java                # 13 regles + Jsoup
|   |   +-- AuditOrchestrator.java          # Chaine 7 phases + events
|   |   +-- ScoringService.java             # Score pondere dynamique (BDD)
|   |   +-- HtmlFetcherService.java         # Crawler HTTP + cache
|   |   +-- AsyncAuditService.java          # @Async + batch
|   |   +-- ScheduledAuditService.java      # @Scheduled + cron
|   |   +-- SseNotificationService.java     # SSE broadcast
|   |   +-- cycle/
|   |       +-- EvaluateService.java        # Phase 1 : Evaluer
|   |       +-- ElaborateService.java       # Phase 2 : Elaborer
|   |       +-- ExecuteService.java         # Phase 3 : Executer
|   |       +-- ExamineService.java         # Phase 4 : Examiner
|   |       +-- EvolveService.java          # Phase 5 : Evoluer
|   |       +-- EmitService.java            # Phase 6 : Emettre
|   |       +-- FeedbackLoopListener.java   # Phase 7 : Equilibrer
|   |       +-- AuditMetricsListener.java   # Metriques performance
|   +-- integration/
|   |   +-- TicketOrchestrator.java         # Pont central (seuil dynamique)
|   |   +-- TicketController.java           # REST /api/tickets
|   |   +-- servicenow/
|   |   |   +-- ServiceNowClient.java       # REST client SNOW
|   |   +-- sailpoint/
|   |   |   +-- SailPointClient.java        # REST client SP
|   |   +-- webhook/
|   |       +-- SailPointWebhookController.java
|   |       +-- ServiceNowWebhookController.java
|   +-- domain/
|   |   +-- entity/
|   |   |   +-- Site.java
|   |   |   +-- AuditReport.java
|   |   |   +-- RuleConfig.java
|   |   |   +-- SecurityTicket.java
|   |   |   +-- SystemSetting.java          # Parametres systeme cle/valeur
|   |   |   +-- RuleResultListConverter.java
|   |   +-- enums/
|   |   |   +-- Phase7E.java
|   |   |   +-- RuleCategory.java
|   |   |   +-- TicketSource.java
|   |   |   +-- TicketStatus.java
|   |   |   +-- TicketSeverity.java
|   |   +-- rule/
|   |       +-- AuditRule.java              # Interface Strategy Pattern
|   |       +-- AuditContext.java           # URL + Document Jsoup
|   |       +-- (13 implementations)
|   +-- repository/
|   |   +-- SiteRepository.java
|   |   +-- AuditReportRepository.java
|   |   +-- RuleConfigRepository.java
|   |   +-- SecurityTicketRepository.java
|   |   +-- SystemSettingRepository.java    # Parametres systeme
|   +-- dto/
|   |   +-- AuditRequestDto / AuditResponseDto / RuleResultDto
|   |   +-- SiteDto / ReportSummaryDto / BatchAuditRequestDto
|   |   +-- ApiResponse / PagedResponse
|   |   +-- integration/TicketDto.java
|   +-- event/
|       +-- AuditStartedEvent / AuditProgressEvent / AuditCompletedEvent
|
+-- src/main/resources/
    +-- application.yml / application-dev.yml / application-prod.yml
    +-- banner.txt
    +-- static/
    |   +-- index.html               # Page d'accueil (3 dashboards)
    |   +-- admin.html               # Dashboard Admin & Tests
    |   +-- exploitation.html        # Dashboard Exploitation
    |   +-- integrations.html        # Dashboard ServiceNow/SailPoint
    +-- db/migration/
        +-- V1__create_schema.sql    # Tables sites + audit_reports
        +-- V2__seed_data.sql        # 4 sites de test
        +-- V3__rule_configs.sql     # Poids de scoring dynamiques
        +-- V4__indexes_pagination.sql # Index pour pagination
        +-- V5__security_tickets.sql # Tickets unifies
        +-- V6__ticket_threshold.sql # Parametres systeme (seuil)
```

---

## 3. Le cycle Axiome 7E

Chaque audit execute un cycle Devops complet en 7 phases. Le systeme s'observe et s'auto-adapte.

```
    +----------+     +----------+     +----------+
    | 1.EVALUER|---->|2.ELABORER|---->|3.EXECUTER|
    +----------+     +----------+     +----+-----+
         ^                                 |
         |                                 |                          
   +------------+                          |                  
   |7.EQUILIBRER|                          |                   
   +------------+                          |                   
         ^                                 |
         |                                 V
    +----------+     +----------+     +-----------+    
    | 6.EMETTRE|<----|5.EVOLUER |<----|4.EXAMINER |
    +----------+     +----------+     +----+------+                          
   
```

### Detail de chaque phase

| Phase | Service Spring | Role | Donnees |
|-------|---------------|------|---------|
| 1. Evaluer | `EvaluateService` | Valide l'URL, prepare le contexte | URL validee |
| 2. Elaborer | `ElaborateService` | Identifie les violations | Liste violations |
| 3. Executer | `ExecuteService` -> `AuditEngine` | Crawle le site + 13 regles | List RuleResultDto |
| 4. Examiner | `ExamineService` -> `ScoringService` | Score pondere composite | Scores RGAA/WCAG/DSFR/Global |
| 5. Evoluer | `EvolveService` | Compare avec audit precedent | Tendance UP/DOWN/STABLE/FIRST |
| 6. Emettre | `EmitService` | Publie AuditCompletedEvent | SSE broadcast |
| 7. Equilibrer | `FeedbackLoopListener` | Ajuste les poids en BDD | Poids mis a jour |

### Retroaction automatique (phase Equilibrer)

| Condition detectee | Action |
|---|---|
| Score RGAA < 0.4 | Poids RGAA +0.03 |
| Score WCAG < 0.6 | Poids WCAG +0.015 |
| Site .gouv.fr avec DSFR < 0.6 | Poids DSFR +0.03 |
| Tendance DOWN | Renforce la categorie la plus faible |
| Apres ajustement | Normalise pour que somme = 1.0 |

---

## 4. Les 13 regles d'audit

### Regles RGAA (5 regles -- poids x0.50)

| ID | Priorite | Description | Analyse DOM |
|----|----------|-------------|-------------|
| RGAA-8.5 | 10 | Titre de page pertinent | Presence, longueur >5 car., non-genericite |
| RGAA-8.3 | 10 | Attribut lang sur html | Presence et format BCP 47 |
| RGAA-1.1 | 50 | Alternative textuelle images | img avec/sans alt, alt="" decoratif |
| RGAA-9.1 | 30 | Hierarchie titres h1-h6 | h1 unique, pas de sauts de niveau |
| RGAA-11.1 | 60 | Etiquettes formulaires | label for, labels implicites, aria-label |

### Regles WCAG (5 regles -- poids x0.30)

| ID | Priorite | Description | Analyse DOM |
|----|----------|-------------|-------------|
| WCAG-1.3.1 | 20 | Landmarks ARIA | header, nav, main, footer + roles ARIA |
| WCAG-1.4.3 | 80 | Contraste minimum 4.5:1 | Heuristique couleurs inline |
| WCAG-1.4.4 | 15 | Viewport et zoom | user-scalable=no, maximum-scale |
| WCAG-2.1.1 | 40 | Navigation clavier | Skip-nav, main, tabindex negatifs |
| WCAG-2.4.4 | 70 | Intitule des liens | Detection textes vagues |

### Regles DSFR (3 regles -- poids x0.20)

| ID | Priorite | Description | Analyse DOM |
|----|----------|-------------|-------------|
| DSFR-HDR-01 | 20 | En-tete DSFR | 5 criteres : fr-header, fr-logo, service-name, nav, dsfr-assets |
| DSFR-FTR-01 | 25 | Pied de page DSFR | 5 criteres : fr-footer, mentions legales, accessibilite, plan du site, RGPD |
| DSFR-BRD-01 | 30 | Fil d'Ariane | fr-breadcrumb, nav, aria-label, structure liste |

---

## 5. Scoring et seuils configurables

### Formule de scoring

```
score_global = score_rgaa x poids_rgaa + score_wcag x poids_wcag + score_dsfr x poids_dsfr
```

Poids par defaut : RGAA = 0.50, WCAG = 0.30, DSFR = 0.20 (somme = 1.0).

Les poids sont stockes dans la table `rule_configs` et ajustables via API ou automatiquement par la boucle de retroaction.

### Lecture des scores

| Plage | Evaluation | Couleur |
|-------|-----------|---------|
| >= 0.70 (70%) | Bon -- conforme ou en bonne voie | Vert |
| 0.40 -- 0.69 | A ameliorer -- corrections necessaires | Orange |
| < 0.40 (40%) | Critique -- non-conformite majeure | Rouge |

### Seuil de creation automatique de tickets

Quand un audit retourne un score global inferieur au seuil configure, un ticket de securite est cree automatiquement et pousse vers ServiceNow.

**Le seuil est configurable de 0% a 100%** via l'API ou les dashboards (slider).

| Seuil | Effet |
|-------|-------|
| 0% | Desactive -- aucun ticket auto |
| 30% | Seuls les sites en non-conformite majeure |
| 50% | Defaut -- sites necessitant des corrections significatives |
| 70% | Seuil eleve -- la plupart des sites non parfaits |
| 100% | Chaque audit cree un ticket |

Le seuil est stocke dans la table `system_settings` (cle: `ticket.auto.threshold`) et lu dynamiquement par le `TicketOrchestrator` a chaque audit.

**API du seuil** :

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/config/settings/ticket-threshold` | Lire le seuil actuel |
| PUT | `/api/config/settings/ticket-threshold` | Modifier (body: `{"threshold": 0.7}`) |
| GET | `/api/config/settings` | Tous les parametres systeme |

**Exemple** :
```bash
# Voir le seuil actuel
curl -s http://localhost:8080/api/config/settings/ticket-threshold

# Passer le seuil a 70%
curl -s -X PUT http://localhost:8080/api/config/settings/ticket-threshold \
  -H "Content-Type: application/json" \
  -d "{\"threshold\":0.7}"
```

Les 3 dashboards incluent un **slider visuel** pour regler ce seuil en temps reel.

---

## 6. API REST -- Reference complete

Base URL : `http://localhost:8080`

Documentation interactive : `http://localhost:8080/swagger-ui.html`

### Sante

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/health` | Health check (runtime, metriques, poids, uptime) |
| GET | `/api/` | Index API avec liste des endpoints |

### Sites

| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/sites` | Enregistrer un site |
| GET | `/api/sites?page=0&size=10` | Liste paginee |
| GET | `/api/sites/{id}` | Detail d'un site |
| GET | `/api/sites/search?name=xxx` | Recherche par nom |
| DELETE | `/api/sites/{id}` | Supprimer (CASCADE) |

### Audits

| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/audits` | Audit synchrone (cycle 7E complet) |
| POST | `/api/audits/async` | Audit asynchrone (retourne un jobId) |
| POST | `/api/audits/batch` | Batch parallele (max 10 sites) |
| GET | `/api/audits/stream` | Flux SSE temps reel |
| GET | `/api/audits/list?page=0&size=10&sortBy=auditedAt&direction=desc` | Rapports pagines |
| GET | `/api/audits/{id}` | Detail rapport |
| GET | `/api/audits/site/{siteId}?page=0&size=10` | Historique site |
| GET | `/api/audits/search?q=xxx` | Recherche full-text |
| GET | `/api/audits/alerts?threshold=0.5` | Alertes (score sous le seuil) |
| GET | `/api/audits/stats` | Statistiques globales |

### Jobs asynchrones

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/audits/async` | Liste tous les jobs |
| GET | `/api/audits/async/{jobId}` | Statut d'un job |
| DELETE | `/api/audits/async` | Nettoyer les jobs termines |

### Scheduler

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/audits/schedule` | Info scheduler |
| POST | `/api/audits/schedule/trigger` | Declenchement manuel de tous les sites |

### Configuration

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/config/weights` | Poids de scoring actuels |
| PUT | `/api/config/weights/{RGAA/WCAG/DSFR}` | Modifier un poids |
| POST | `/api/config/weights/reset` | Reinitialiser (0.50 / 0.30 / 0.20) |
| GET | `/api/config/settings` | Tous les parametres systeme |
| GET | `/api/config/settings/ticket-threshold` | Seuil de ticket auto |
| PUT | `/api/config/settings/ticket-threshold` | Modifier le seuil (0.0 a 1.0) |
| PUT | `/api/config/settings/{key}` | Modifier un parametre generique |

### Tickets de securite

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/tickets?page=0&size=10` | Liste paginee (filtres: source, status, severity) |
| GET | `/api/tickets/open` | Tickets ouverts uniquement |
| GET | `/api/tickets/{id}` | Detail d'un ticket |
| GET | `/api/tickets/search?q=xxx` | Recherche full-text |
| GET | `/api/tickets/stats` | Statistiques (par source, statut, severite) |
| GET | `/api/tickets/integrations` | Statut des connexions SNOW/SP |
| POST | `/api/tickets` | Creer un ticket manuellement |
| POST | `/api/tickets/{id}/resolve` | Resoudre un ticket (+sync ServiceNow) |
| POST | `/api/tickets/sync` | Synchroniser tickets non pousses vers SNOW |

### Webhooks

| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/webhooks/sailpoint` | Recepteur Event Triggers SailPoint |
| GET | `/api/webhooks/sailpoint/test` | Test connectivite webhook SP |
| POST | `/api/webhooks/servicenow` | Callback mise a jour ServiceNow |
| GET | `/api/webhooks/servicenow/test` | Test connectivite webhook SNOW |

---

## 7. Integration ServiceNow

### Vue d'ensemble

L'integration ServiceNow cree automatiquement des incidents quand :

- Un audit retourne un score sous le seuil configurable: 50% par defaut
- SailPoint detecte une violation (transmise via webhook)
- Un ticket est cree manuellement

### Flux

```
Auditaccess                              ServiceNow
    |                                          |
    |  1. getOAuthToken() -------POST--------> | /oauth_token.do
    |     (cache 1h)         <---token-------- |
    |                                          |
    |  2. createIncident() ------POST--------> | /api/now/table/incident
    |     {short_description,    <---result---- | -> {sys_id, number}
    |      priority, category}                 |
    |                                          |
    |  3. resolveIncident() -----PATCH-------> | /api/now/table/incident/{sys_id}
    |     {state:6, close_notes}               |
    |                                          |
    |  4. ServiceNowWebhookCtrl  <---POST----- | Business Rule (on state change)
    |     -> update ticket status               |
```

### Mapping des champs

| Auditaccess (SecurityTicket) | ServiceNow (incident) |
|---|---|
| title | short_description (tronque 160 car.) |
| description (construit) | description |
| severity.snowPriority (1-5) | priority |
| source.label | u_source_system (custom) |
| sailpointIdentityName | u_affected_user (custom) |
| siteUrl | u_affected_resource (custom) |
| config assignmentGroup | assignment_group |

### Authentification

**Basic Auth** (dev) :
```yaml
servicenow:
  auth-method: basic
  username: ${SNOW_USERNAME}
  password: ${SNOW_PASSWORD}
```

**OAuth 2.0** (prod) :
```yaml
servicenow:
  auth-method: oauth
  client-id: ${SNOW_CLIENT_ID}
  client-secret: ${SNOW_CLIENT_SECRET}
```

### Configuration cote ServiceNow

1. **Creer un utilisateur API** : System Administration -> Users -> New, roles `itil` + `rest_service`
2. **Configurer OAuth** (optionnel) : System OAuth -> Application Registry -> Create endpoint
3. **Creer les champs custom** (optionnel) : `u_source_system`, `u_affected_user`, `u_affected_resource`
4. **Configurer le callback** : Business Rule sur `incident` (on state change) qui POST vers `/api/webhooks/servicenow`

---

## 8. Integration SailPoint

### Vue d'ensemble

L'integration SailPoint permet de :

- **Recevoir** les violations (SoD, suppressions, demandes d'acces) via Event Triggers webhooks
- **Interroger** l'API V3 pour enrichir les tickets (details identite, access profiles)

### Flux

```
SailPoint Identity Cloud             Auditaccess
    |                                    |
    |  POST /api/webhooks/sailpoint ---> | SailPointWebhookController
    |  Bearer: {webhook-secret}          |   |
    |  {triggerId, identity, ...}        |   +-> validateToken()
    |                                    |   +-> mapToTicket()
    |                                    |   +-> TicketOrchestrator
    |                                    |        +-> dedoublonnage
    |                                    |        +-> save SecurityTicket
    |                                    |        +-> push ServiceNow (async)
    |  <--- 200 OK {ticketId} --------- |
    |                                    |
    |  GET /v3/identities/{id} <-------- | SailPointClient (enrichissement)
```

### Event Triggers supportes

| Trigger ID | Evenement | Severite | Categorie |
|------------|-----------|----------|-----------|
| idn:policy-violation | Violation SoD | HIGH | sod_violation |
| idn:identity-deleted | Identite supprimee | MEDIUM | orphan_account |
| idn:access-request-pre-approval | Demande d'acces | MEDIUM | access_violation |
| idn:access-request-post-approval | Demande d'acces | MEDIUM | access_violation |
| idn:account-aggregation-completed | Agregation terminee | LOW | account_anomaly |
| idn:certification-signed-off | Certification signee | LOW | certification_issue |

### Dedoublonnage

Si un webhook arrive avec un `sailpointViolationId` deja connu en BDD, le ticket existant est mis a jour au lieu de creer un doublon.

### Configuration cote SailPoint

1. **Creer un Personal Access Token** : Preferences -> Personal Access Tokens -> New Token
2. **Souscrire aux Event Triggers** : Admin -> Event Triggers -> + Subscribe
   - Type : HTTP
   - URL : `https://Auditaccess.domaine.com/api/webhooks/sailpoint`
   - Auth : Bearer Token (valeur = SP_WEBHOOK_SECRET)
3. **Triggers recommandes** : policy-violation (priorite 1), identity-deleted, access-request-pre-approval

---

## 9. Tickets de securite unifies

### Modele SecurityTicket

Le ticket unifie est le pont central. Il maintient les references croisees :

```
Sources :                                           Destinations :
+------------------------+     +-----------------+  +-------------------+
| Audit Auditaccess      |--->|                  |->| ServiceNow        |
| (score < seuil)        |    | SecurityTicket   |  | Incident INC...   |
+------------------------+    |                  |  +-------------------+
| SailPoint              |--->| - sailpoint_*    |
| (Event Trigger webhook)|    | - servicenow_*   |  +-------------------+
+------------------------+    | - audit_report_id|->| Dashboards Vue.js |
| Creation manuelle      |--->|                  |  +-------------------+
+------------------------+     +-----------------+
```

### Cycle de vie

```
NEW --> OPEN --> IN_PROGRESS --> RESOLVED --> CLOSED
                                    |
                               CANCELLED
```

- **NEW** : ticket cree
- **OPEN** : incident ServiceNow cree (sys_id renseigne)
- **IN_PROGRESS** : callback ServiceNow (state=2)
- **RESOLVED** : resolu via Auditaccess ou ServiceNow
- **CLOSED** : ferme definitivement

### Creation automatique

Quand un audit retourne un score < seuil configurable (par defaut 50%), le `TicketOrchestrator` :
1. Lit le seuil depuis `system_settings` (cle `ticket.auto.threshold`)
2. Cree un `SecurityTicket` avec source=AUDIT et severite calculee
3. Pousse vers ServiceNow de facon asynchrone

---

## 10. Dashboards Vue.js

Trois dashboards standalone (Vue 3 CDN) servis par Spring Boot depuis `static/`.

### Dashboard Admin & Tests (admin.html)

Theme dark Devops. 12 pages : Health, Stats, Sites CRUD, Audit sync, Audit async, Batch, Rapports pagines, Alertes, Console SSE, Jobs, Poids scoring + seuil de ticket, Scheduler. Chaque page affiche la reponse JSON brute.

### Dashboard Exploitation (exploitation.html)

Theme clair moderne. 8 pages : Tableau de bord (KPIs + barres ponderations), Temps reel (SSE + barre progression), Lancer un audit, Portefeuille de sites, Historique, Alertes, Ponderations + seuil de ticket, Guide fonctionnel complet.

### Dashboard Integrations (integrations.html)

Theme professionnel. 11 pages : Tableau de bord (KPIs par source/severite/statut + seuil configurable), Chronologie (timeline visuelle), Incidents SNOW (detail sys_id/number/URL), Synchronisation, Evenements SailPoint, Simuler webhooks (4 types), Tous les tickets (filtres), Creer un ticket, Resoudre (liste ouverts cliquables), Connexions (statut SNOW/SP), Statistiques.

### Acces

| URL | Page |
|-----|------|
| `http://localhost:8080/` | Page d'accueil (3 cartes) |
| `http://localhost:8080/admin.html` | Dashboard Admin |
| `http://localhost:8080/exploitation.html` | Dashboard Exploitation |
| `http://localhost:8080/integrations.html` | Dashboard Integrations |
| `http://localhost:8080/swagger-ui.html` | Documentation OpenAPI |
| `http://localhost:8080/h2-console` | Console H2 (dev) |

---

## 11. Installation et deploiement

### Prerequis

- Java 25 (Eclipse Temurin) : `winget install EclipseAdoptium.Temurin.25.JDK`
- Maven (inclus via mvnw)
- curl + jq : `winget install jqlang.jq`

### Lancement

```bash
Sous Windows / DOS
mvn -N io.takari:maven:wrapper -Dmaven=3.9.9
ou :
mvn wrapper:wrapper -Dmaven=3.9.9
Sinon
👍mvn org.apache.maven.plugins:maven-wrapper-plugin:3.2.0:wrapper -Dmaven=3.9.9
puis  :
mvnw clean spring-boot:run
ou
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run

# Verifier
curl http://localhost:8080/api/health
```

### Dependances pom.xml

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
h2 (runtime, dev)
postgresql (runtime, prod)
flyway-core
org.jsoup:jsoup:1.18.3
springdoc-openapi-starter-webmvc-ui:2.8.4
```

---

## 12. Configuration

### application.yml (structure complete)

```yaml
spring:
  application:
    name: Auditaccess
  profiles:
    active: dev

server:
  port: 8080

springdoc:
  swagger-ui:
    enabled: true

Auditaccess:
  scheduler:
    enabled: false
    cron: "0 0 2 * * *"
  integrations:
    servicenow:
      enabled: false
      instance: ${SNOW_INSTANCE}
      auth-method: basic
      username: ${SNOW_USERNAME}
      password: ${SNOW_PASSWORD}
    sailpoint:
      enabled: false
      tenant: ${SP_TENANT}
      client-id: ${SP_CLIENT_ID}
      client-secret: ${SP_CLIENT_SECRET}
      webhook-secret: ${SP_WEBHOOK_SECRET}
```

### Fichier .env

```bash
# ServiceNow
SNOW_INSTANCE=mon-instance.service-now.com
SNOW_USERNAME=api_user
SNOW_PASSWORD=ChangeMe!S3cret
SNOW_CLIENT_ID=
SNOW_CLIENT_SECRET=

# SailPoint
SP_TENANT=mon-tenant
SP_CLIENT_ID=xxxxx
SP_CLIENT_SECRET=xxxxx
SP_WEBHOOK_SECRET=mon-webhook-secret
```

### Profiles Spring

| Profile | BDD | Scheduler | Swagger | Usage |
|---------|-----|-----------|---------|-------|
| dev | H2 in-memory | Desactive | Actif | Developpement |
| prod | PostgreSQL 16 | Active (2h/jour) | Actif | Production Docker |

### Migrations Flyway

| Migration | Contenu |
|-----------|---------|
| V1__create_schema.sql | Tables sites + audit_reports + index |
| V2__seed_data.sql | 4 sites de test |
| V3__rule_configs.sql | Table rule_configs + poids par defaut |
| V4__indexes_pagination.sql | Index pour pagination M6 |
| V5__security_tickets.sql | Table security_tickets + index |
| V6__ticket_threshold.sql | Table system_settings + seuil par defaut 50% |

---

## 13. Docker

### Dockerfile multi-stage

Stage 1 (builder) : JDK 25 Alpine -> mvnw package
Stage 2 (runtime) : JRE 25 Alpine (~200 MB), user non-root, HEALTHCHECK

### Docker Compose

```bash
# Mode dev (H2)
docker compose up --build

# Mode prod (PostgreSQL)
docker compose --profile prod up --build
```

| Service | Port | Profile |
|---------|------|---------|
| Auditaccess | 8080 | dev (defaut) |
| Auditaccess-prod | 8080 | prod |
| postgres | 5432 | prod |

### Nginx (optionnel)

La config nginx.conf inclut le reglage critique SSE : `proxy_buffering off`.

---

## 14. Tests

### Script complet (50 tests)

```bash
# Windows CMD
tests-cyberaudit7e.bat
```

13 sections : sante, sites, audits sync, rapports, async, batch, config, scheduler, tickets, filtrage, resolution, webhooks, statistiques.

### Tests du seuil

```bash
tests-tickets.bat
```

10 tests specifiques : lecture, modification, validation, retour defaut, test avec audit.

---

## 15. Modules de formation M1-M7

Le projet a ete construit incrementalement en 7 modules (1 journee de 8h).

| Module | Horaire | Concepts Spring | Livrable |
|--------|---------|----------------|----------|
| M1 Bootstrap | 08:30-09:30 | @SpringBootApplication, @RestController | /api/health |
| M2 Architecture | 09:30-10:30 | IoC, Strategy Pattern, Profiles | Structure MVC + 7 regles |
| M3 Persistance | 10:45-12:00 | JPA, Flyway, @Transactional | CRUD + H2 |
| M4 Moteur audit | 13:00-14:30 | Jsoup, AuditContext, poids BDD | 13 regles DOM reel |
| M5 Async/Events | 14:30-15:30 | @Async, @Scheduled, SSE | Streaming + batch |
| M6 API complete | 15:45-17:00 | OpenAPI, Pageable, validation | Swagger + pagination |
| M7 Docker | 17:00-17:30 | Dockerfile multi-stage, Compose | POC containerise |

### 30+ concepts Spring couverts

@SpringBootApplication, @RestController, @Service, @Repository, @Component, @Configuration, @Bean, IoC / injection constructeur, Strategy Pattern, Profiles, @Entity / @Table / @ManyToOne / @OneToMany, JpaRepository, Query Methods, JPQL @Query, @Transactional, Flyway, AttributeConverter, DTO (Java Records), @Async / CompletableFuture, @Scheduled / cron, ApplicationEvent / @EventListener, SseEmitter, ThreadPoolTaskExecutor, @ConfigurationProperties, @Valid / Jakarta Validation, @RestControllerAdvice, SpringDoc OpenAPI, Page / Pageable, Dockerfile multi-stage, Docker Compose profiles.

---

## 16. Depannage

| Probleme | Cause probable | Solution |
|----------|---------------|----------|
| L'app ne demarre pas | Java 25 manquant | winget install EclipseAdoptium.Temurin.25.JDK |
| Port 8080 occupe | Autre app sur le port | Changer server.port dans application.yml |
| Swagger UI vide | Dependance manquante | Verifier springdoc-openapi dans pom.xml |
| Score DSFR = 0 sur .gouv.fr | CSS DSFR absent de la page d'accueil | Tester une page interieure |
| Audit tres lent (> 30s) | Site lent ou anti-bot | Verifier le site manuellement |
| SNOW connected=false | Identifiants incorrects | Verifier .env (SNOW_INSTANCE, USERNAME, PASSWORD) |
| SailPoint connected=false | Tenant/token incorrect | Verifier SP_TENANT, SP_CLIENT_ID, SP_CLIENT_SECRET |
| Webhook SP 401 | Token webhook incorrect | Verifier SP_WEBHOOK_SECRET = Bearer Token dans SP |
| Dashboards absents | Fichiers pas dans static/ | Copier les .html dans src/main/resources/static/ |
| Accents casses dans CMD | Encodage | Executer chcp 65001 avant les tests |
| Ticket auto pas cree | Seuil trop bas | Verifier GET /api/config/settings/ticket-threshold |

---

## 17. Securite

- Ne jamais commiter `.env` dans Git (ajouter dans .gitignore)
- Utiliser OAuth 2.0 pour ServiceNow en production
- Renouveler les tokens SailPoint regulierement
- Proteger l'API avec Spring Security + JWT en production
- Activer HTTPS via Nginx ou reverse proxy
- Le webhook SailPoint est protege par Bearer Token
- Desactiver /h2-console en production (fait dans le profil prod)
- Les conteneurs Docker utilisent un user non-root (audit)

---

## 18. Statistiques du projet

| Metrique | Valeur |
|----------|--------|
| Fichiers Java | 68+ |
| Regles d'audit | 13 (5 RGAA + 5 WCAG + 3 DSFR) |
| Endpoints REST | 37+ |
| Migrations Flyway | 6 |
| Evenements Spring | 3 |
| Tags OpenAPI | 7 |
| Lignes de code Java | ~6000 |
| Dashboards Vue.js | 3 (admin + exploitation + integrations) |
| Scripts de tests | 2 (50 + 10 tests) |
| Integrations externes | 2 (ServiceNow + SailPoint) |
| Parametres configurables | poids RGAA/WCAG/DSFR + seuil de ticket auto |

### Convergence des projets source

| Projet | Concept repris | Implementation Auditaccess |
|--------|----------------|----------------------------|
| GitManager | Registre d'organes | SiteRepository + CRUD REST |
| GitManager | Redis Streams bridge | ApplicationEvent + @EventListener |
| GitManager | Dokploy | Docker Compose + profiles |
| AuditAccess | Moteur 17 regles Django | 13 AuditRule @Component (Strategy) |
| AuditAccess | Scoring RGAA x 0.5 + WCAG x 0.3 + DSFR x 0.2 | ScoringService + RuleConfig BDD |
| AuditAccess | Celery async tasks | @Async + AsyncAuditService |
| AuditAccess | Crawler Playwright | HtmlFetcherService (Jsoup) |
| Axiome 7E | 7 phases Devopss | 7 @Service dans service/cycle/ |
| Axiome 7E | Boucle de retroaction | FeedbackLoopListener ajuste les poids |
| Axiome 7E | Observation de 2e ordre | EvolveService compare les audits |

---

*Auditaccess -- Spring Boot -- Mai 2026*
