# CyberAudit7E

**Moteur d’audit d’accessibilité cybernétique — Spring Boot 4**

README unifié consolidé à partir des documents fournis : `README.md`, `README-new.md`, `README7.md`, `Before-ServiceNow-SailPoint.md` et `TESTS-REFERENCE.md`.

---

## Sommaire

- [Vision du projet](#vision-du-projet)
- [Objectifs](#objectifs)
- [Stack technique](#stack-technique)
- [Architecture applicative](#architecture-applicative)
- [Cycle Axiome 7E](#cycle-axiome-7e)
- [Fonctionnalités principales](#fonctionnalités-principales)
- [Progression pédagogique](#progression-pédagogique)
- [Arborescence cible](#arborescence-cible)
- [Démarrage rapide](#démarrage-rapide)
- [Profils Spring](#profils-spring)
- [Dockerisation](#dockerisation)
- [API REST](#api-rest)
- [Scoring et règles d’audit](#scoring-et-règles-daudit)
- [Intégrations ServiceNow × SailPoint](#intégrations-servicenow--sailpoint)
- [Tests et validation](#tests-et-validation)
- [Roadmap](#roadmap)
- [Sources de consolidation](#sources-de-consolidation)
- [Licence](#licence)

---

## Vision du projet

CyberAudit7E est une application Java/Spring Boot conçue comme un moteur d’audit d’accessibilité web structuré autour d’une boucle cybernétique en 7 phases. Le projet fusionne trois intentions :

- **GitManager** : registre de sites ou services à auditer
- **AuditAccess** : moteur de règles multi-référentiel avec scoring pondéré
- **Axiome 7E** : cadre d’orchestration cybernétique de l’audit

Le projet sert à la fois de **POC technique**, de **support de formation Spring Boot**, et de **socle industrialisable** vers Docker, PostgreSQL, monitoring, sécurité et intégrations tierces.

---

## Objectifs

- Auditer des sites web via une API REST.
- Évaluer automatiquement des règles d’accessibilité réelles sur le DOM.
- Produire des scores pondérés RGAA / WCAG / DSFR.
- Comparer les audits dans le temps avec détection de tendance.
- Exposer des traitements synchrones, asynchrones, batch, planifiés et temps réel via SSE.
- Préparer une base extensible pour des intégrations IAM / ITSM comme SailPoint et ServiceNow.

---

## Stack technique

| Composant | Version / profil | Rôle |
|---|---|---|
| Java | 21+ (testé jusqu’à JDK 25) | Runtime |
| Spring Boot | 4.0.5 | Framework applicatif |
| Spring Web MVC | 4.x | API REST |
| Spring Data JPA | 4.x | Persistance ORM |
| Hibernate | 7.2.x | Implémentation JPA |
| Jackson | 3.x | Sérialisation JSON |
| H2 Database | 2.4.x | Base mémoire en développement |
| PostgreSQL | 16 / prod | Base de production |
| Flyway | 11.x | Migrations SQL versionnées |
| Jsoup | 1.18.3 | Crawl HTTP et analyse DOM |
| SpringDoc OpenAPI | 2.8.4 | Swagger UI / OpenAPI |
| Lombok | optionnel | Réduction du boilerplate |
| JUnit Jupiter / Mockito / AssertJ / Awaitility | tests | Tests unitaires, intégration, async |
| Maven Wrapper | 3.9.x | Build reproductible |

---

## Architecture applicative

CyberAudit7E suit une architecture en couches classique et pédagogique.

```text
┌────────────────────────────────────────────────────┐
│                   REST API Layer                    │
│  HealthController | SiteController | AuditController│
│  ConfigController | TicketController               │
│  GlobalExceptionHandler                            │
├────────────────────────────────────────────────────┤
│                    Service Layer                    │
│  AuditOrchestrator | AuditEngine | ScoringService  │
│  AsyncAuditService | ScheduledAuditService         │
│  SseNotificationService | TicketOrchestrator       │
├────────────────────────────────────────────────────┤
│                    Domain Layer                     │
│  Site | AuditReport | RuleConfig | SecurityTicket  │
│  Phase7E | RuleCategory | TicketStatus | Severity  │
│  AuditRule + implémentations                       │
├────────────────────────────────────────────────────┤
│                    Data Layer                       │
│  SiteRepository | AuditReportRepository            │
│  RuleConfigRepository | SecurityTicketRepository   │
│  H2 (dev) / PostgreSQL (prod) + Flyway             │
└────────────────────────────────────────────────────┘
```

### Patterns utilisés

- **Strategy** : `AuditRule` et ses implémentations
- **IoC / DI** : injection par constructeur
- **Observer** : événements Spring (`AuditStartedEvent`, `AuditProgressEvent`, `AuditCompletedEvent`)
- **DTO** : records et objets de réponse API
- **Repository** : interfaces `JpaRepository`
- **Template Method** : orchestration stable du cycle d’audit

---

## Cycle Axiome 7E

Chaque audit suit sept phases :

**Évaluer → Élaborer → Exécuter → Examiner → Évoluer → Émettre → Équilibrer**

```text
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ ÉVALUER ├───→│ ÉLABORER ├───→│ EXÉCUTER ├───→│ EXAMINER │
└─────────┘    └──────────┘    └──────────┘    └────┬─────┘
                                                     │
┌────────────┐    ┌─────────┐    ┌─────────┐        │
│ ÉQUILIBRER │←───│ ÉMETTRE │←───│ ÉVOLUER │←───────┘
└────────────┘    └─────────┘    └─────────┘
```

### Mapping 7E → Spring

| Phase | Classe / rôle principal | Concept illustré |
|---|---|---|
| Évaluer | `EvaluateService` | préparation métier |
| Élaborer | `ElaborateService` + `AuditRule` | Strategy Pattern |
| Exécuter | `AuditEngine` + `HtmlFetcherService` | crawl et analyse DOM |
| Examiner | `ScoringService` | scoring pondéré |
| Évoluer | `AuditReportRepository` | historique et tendance |
| Émettre | `SseNotificationService` | événements et SSE |
| Équilibrer | `FeedbackLoopListener` | rétroaction cybernétique |

---

## Fonctionnalités principales

### Gestion des sites

- CRUD de sites à auditer via `/api/sites`
- recherche par nom
- pagination et tri
- unicité des URL

### Audit d’accessibilité

- audit synchrone
- audit asynchrone avec `jobId`
- batch parallèle
- scheduler par cron / déclenchement manuel
- analyse DOM réelle via Jsoup
- comparaison avec audit précédent : `UP`, `DOWN`, `STABLE`, `FIRST`

### Temps réel

- flux SSE `/api/audits/stream`
- événements `connected`, `audit-started`, `audit-progress`, `audit-completed`

### API complète

- réponses structurées (`ApiResponse`, `PagedResponse`)
- pagination et tri sur les listes
- documentation Swagger UI / OpenAPI

### Industrialisation

- profils `dev` / `prod`
- H2 pour le développement
- PostgreSQL en production
- Flyway pour les migrations
- Docker / Docker Compose
- reverse proxy Nginx compatible SSE

---

## Progression pédagogique

| Module | Thème | Livrables clés |
|---|---|---|
| M1 | Bootstrap Spring Boot | `HealthController`, `/api/health` |
| M2 | Architecture IoC + Strategy | MVC, règles d’audit injectées |
| M3 | Persistance JPA + H2 + Flyway | CRUD `Site`, `AuditReport`, tendances |
| M4 | Moteur d’audit réel avec Jsoup | 13 règles sur DOM réel, poids dynamiques |
| M5 | Async, Events, SSE, Scheduler | `@Async`, batch, planification, streaming |
| M6 | API REST complète + OpenAPI | Swagger UI, validation, pagination |
| M7 | Docker & synthèse | Dockerfile, Compose, runtime metrics |
| Module intégrations | ServiceNow × SailPoint | tickets de sécurité unifiés |

---

## Arborescence cible

```text
cyberaudit7e/
├── pom.xml
├── mvnw / mvnw.cmd
├── Dockerfile
├── docker-compose.yml
├── nginx.conf
├── scripts/
│   ├── smoke-test.ps1
│   ├── test-cyberaudit7e.ps1
│   └── test-cyberaudit7e.sh
├── src/main/java/com/cyberaudit7e/
│   ├── config/
│   ├── controller/
│   ├── service/
│   │   └── cycle/
│   ├── domain/
│   │   ├── entity/
│   │   ├── enums/
│   │   └── rule/
│   ├── repository/
│   ├── dto/
│   ├── event/
│   └── integration/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
└── src/test/java/com/cyberaudit7e/
```

---

## Démarrage rapide

### Prérequis

- Java 21+
- Maven Wrapper inclus
- optionnel : `jq`, Bruno, HTTPie, H2 Console

### Lancer l’application

#### Windows PowerShell

```powershell
.\mvnw.cmd clean spring-boot:run
```

#### Linux / macOS

```bash
./mvnw clean spring-boot:run
```

### Accès utiles

| Accès | URL |
|---|---|
| Index API | `http://localhost:8080/api/` |
| Health check | `http://localhost:8080/api/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Console H2 (dev) | `http://localhost:8080/h2-console` |

### Console H2

- JDBC URL : `jdbc:h2:mem:cyberaudit7e`
- User : `sa`
- Password : vide

---

## Profils Spring

### `dev`

- base H2 en mémoire
- console H2 activée
- démarrage rapide pour la formation et le prototypage

### `prod`

- base PostgreSQL
- migrations Flyway automatiques
- configuration orientée conteneurisation / industrialisation

---

## Dockerisation

### Fichiers fournis

| Fichier | Rôle |
|---|---|
| `Dockerfile` | build multi-stage |
| `docker-compose.yml` | orchestration dev / prod |
| `.dockerignore` | optimisation du contexte |
| `nginx.conf` | reverse proxy optionnel SSE |

### Mode DEV

```bash
docker compose up --build
```

### Mode PROD

```bash
docker compose --profile prod up --build
```

### Commandes utiles

```bash
docker compose ps
docker compose logs -f cyberaudit7e-app
docker exec -it cyberaudit7e-app sh
docker compose down
docker compose down -v
```

### Runtime metrics (M7)

Le health check expose aussi des informations runtime :

- version Java
- heap utilisée / max
- pourcentage de heap
- nombre de processeurs
- uptime
- hostname / container ID

---

## API REST

### Santé et index

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/` | index de l’API |
| GET | `/api/health` | état applicatif + moteur |

### Sites

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/sites` | liste paginée |
| POST | `/api/sites` | créer un site |
| GET | `/api/sites/{id}` | détail d’un site |
| GET | `/api/sites/search?name=...` | recherche par nom |
| DELETE | `/api/sites/{id}` | suppression |

### Audits synchrones et consultation

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/api/audits` | audit synchrone |
| GET | `/api/audits/{id}` | détail d’un rapport |
| GET | `/api/audits/list` | liste paginée des rapports |
| GET | `/api/audits/site/{siteId}` | historique d’un site |
| GET | `/api/audits/search?q=...` | recherche full-text |
| GET | `/api/audits/stats` | statistiques globales |
| GET | `/api/audits/alerts?threshold=...` | alertes sous seuil |

### Configuration

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/config/weights` | voir les poids |
| PUT | `/api/config/weights/{category}` | modifier un poids |
| POST | `/api/config/weights/reset` | reset poids par défaut |

### Asynchrone, batch, SSE, scheduler

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/api/audits/async` | audit asynchrone |
| GET | `/api/audits/async/{jobId}` | statut d’un job |
| GET | `/api/audits/async` | liste des jobs |
| DELETE | `/api/audits/async` | nettoyage des jobs terminés |
| POST | `/api/audits/batch` | batch d’audits |
| GET | `/api/audits/stream` | streaming SSE |
| GET | `/api/audits/schedule` | état du scheduler |
| POST | `/api/audits/schedule/trigger` | déclenchement manuel |

### Exemple de commandes

```powershell
# Health check
curl -s http://localhost:8080/api/health | jq .

# Créer un site
curl -s -X POST http://localhost:8080/api/sites ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.numerique.gouv.fr\",\"name\":\"DINUM\"}" | jq .

# Lancer un audit
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Service Public\"}" | jq .

# Ouvrir le stream SSE
curl -N http://localhost:8080/api/audits/stream
```

---

## Scoring et règles d’audit

### Formule

```text
score_global = score_rgaa × 0.5 + score_wcag × 0.3 + score_dsfr × 0.2
```

Les poids sont stockés en base et peuvent être modifiés par API ou réajustés par la boucle de rétroaction.

### Répartition par catégorie

| Catégorie | Poids par défaut | Exemples |
|---|---|---|
| RGAA | 50 % | titre, langue, alt-text, headings, labels |
| WCAG | 30 % | contraste, clavier, landmarks, viewport, liens |
| DSFR | 20 % | header, footer, breadcrumb |

### Règles principales (M4)

| ID | Catégorie | Description |
|---|---|---|
| RGAA-8.5 | RGAA | titre de page pertinent |
| RGAA-8.3 | RGAA | attribut `lang` |
| RGAA-1.1 | RGAA | textes alternatifs des images |
| RGAA-9.1 | RGAA | hiérarchie des titres |
| RGAA-11.1 | RGAA | labels de formulaire |
| WCAG-1.4.4 | WCAG | viewport et zoom |
| WCAG-1.3.1 | WCAG | landmarks ARIA |
| WCAG-2.1.1 | WCAG | navigation clavier / skip links |
| WCAG-2.4.4 | WCAG | intitulé des liens |
| WCAG-1.4.3 | WCAG | contraste (heuristique) |
| DSFR-HDR-01 | DSFR | en-tête DSFR |
| DSFR-FTR-01 | DSFR | pied de page DSFR |
| DSFR-BRD-01 | DSFR | fil d’Ariane |

---

## Intégrations ServiceNow × SailPoint

Le module d’intégration ajoute un pipeline de tickets de sécurité unifié entre **SailPoint Identity Security Cloud**, **CyberAudit7E** et **ServiceNow ITSM**.

### Architecture de principe

```text
SailPoint ──POST webhook──→ CyberAudit7E ──POST Table API──→ ServiceNow
     ▲                            │                               │
     └──────────── GET API ───────┘                               │
                               callbacks / synchronisation ───────┘
```

### Composants clés

- `SecurityTicket` : entité JPA des tickets
- `TicketOrchestrator` : persistance + orchestration de sync
- `ServiceNowClient` : client REST ServiceNow
- `SailPointClient` : client REST SailPoint
- `TicketController` : API `/api/tickets`
- `SailPointWebhookController` : `/api/webhooks/sailpoint`
- `ServiceNowWebhookController` : `/api/webhooks/servicenow`

### Endpoints intégrations

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/api/tickets` | liste paginée / filtrée |
| GET | `/api/tickets/open` | tickets ouverts |
| GET | `/api/tickets/{id}` | détail ticket |
| GET | `/api/tickets/search?q=...` | recherche |
| GET | `/api/tickets/stats` | statistiques |
| GET | `/api/tickets/integrations` | statut des intégrations |
| POST | `/api/tickets` | création manuelle |
| POST | `/api/tickets/{id}/resolve` | résolution |
| POST | `/api/tickets/sync` | synchronisation vers ServiceNow |
| POST | `/api/webhooks/sailpoint` | webhook SailPoint |
| POST | `/api/webhooks/servicenow` | callback ServiceNow |

### Mode développement

Par défaut, les intégrations peuvent rester désactivées (`enabled: false`) afin de tester le module en mode standalone.

### Configuration production

Prévoir dans `application.yml` :

- instance ServiceNow
- mode d’authentification et identifiants API
- tenant SailPoint
- client ID / secret
- webhook secret partagé

---

## Tests et validation

### Niveaux de tests

- **unitaires** : logique métier, scoring, règles
- **JPA** : repositories et persistance
- **web slice** : contrôleurs REST
- **intégration** : contexte Spring complet
- **async / SSE** : Awaitility et tests événementiels
- **smoke tests** : scripts PowerShell / bash

### Scripts utiles

- `smoke-test.ps1`
- `test-cyberaudit7e.ps1`
- `test-cyberaudit7e.sh`
- scripts de diagnostic / réalignement de packages de tests

### Référence rapide curl

```powershell
# Nombre de règles chargées
curl -s http://localhost:8080/api/health | jq .rulesLoaded

# Voir les poids
curl -s http://localhost:8080/api/config/weights | jq .

# Audit asynchrone
curl -s -X POST http://localhost:8080/api/audits/async ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.legifrance.gouv.fr\",\"name\":\"Légifrance\"}" | jq .

# Liste des jobs
curl -s http://localhost:8080/api/audits/async | jq .

# Batch
curl -s -X POST http://localhost:8080/api/audits/batch ^
  -H "Content-Type: application/json" ^
  -d "{\"sites\":[{\"url\":\"https://www.service-public.fr\",\"name\":\"SP\"},{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouv\"}]}" | jq .
```

### SQL de vérification (H2 / PostgreSQL)

```sql
SELECT id, name, url, current_phase FROM sites;
SELECT id, site_id, score_global, trend, audited_at FROM audit_reports ORDER BY audited_at DESC;
SELECT * FROM rule_configs;
SELECT version, description, success FROM flyway_schema_history;
```

---

## Roadmap

### Court terme

- finaliser la couverture de tests
- fiabiliser PostgreSQL en intégration
- homogénéiser la documentation API
- ajouter la sécurité API

### Moyen terme

- Testcontainers pour les tests d’intégration
- Spring Security + JWT
- monitoring avec Actuator / Micrometer / Grafana
- dashboard front temps réel
- CI/CD et déploiement Dokploy

### Long terme

- Playwright pour audits dynamiques avancés
- architecture multi-tenant
- Kubernetes / Helm
- moteur plus réactif ou event-driven
- enrichissement cybernétique de la boucle d’équilibrage

---

## Sources de consolidation

- `README-new.md`
- `README.md`
- `README7.md`
- `Before-ServiceNow-SailPoint.md`
- `TESTS-REFERENCE.md`

Liens source :

- README-new.md : https://www.genspark.ai/api/files/s/IHYLtIne
- Before-ServiceNow-SailPoint.md : https://www.genspark.ai/api/files/s/CijZH8i1
- README.md : https://www.genspark.ai/api/files/s/wBfOyOiJ
- README7.md : https://www.genspark.ai/api/files/s/yinOqwwj
- TESTS-REFERENCE.md : https://www.genspark.ai/api/files/s/lKRSPPp1

---

## Licence

**POC de formation — usage pédagogique et base d’industrialisation.**

À adapter selon votre politique de diffusion (interne, open source, client, démonstration, etc.).
