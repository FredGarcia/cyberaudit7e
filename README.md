 Nouvel onglet body { background: #FFFFFF; margin: 0; } #backgroundImage { border: none; height: 100%; pointer-events: none; position: fixed; top: 0; visibility: hidden; width: 100%; } \[show-background-image\] #backgroundImage { visibility: visible; }

# CyberAudit7E

**Moteur d'audit d'accessibilité cybernétique — Spring Boot 4 POC**

README unifié construit à partir des documents `README.md`, `README0.md`, `README1.md`, `README2.md`, `README3.md`, `README4.md`, `README5.md`, `README6.md` et `TEST.md`.

Fusion conceptuelle de trois projets :

*   **GitManager** → registre de services (organes) à auditer
*   **AuditAccess** → moteur de règles multi-référentiel (RGAA, WCAG, DSFR) avec scoring pondéré
*   **Axiome 7E** → boucle cybernétique : **Évaluer → Élaborer → Exécuter → Examiner → Évoluer → Émettre → Équilibrer**

- - -

## Sommaire

*   [Vision du projet](#vision-du-projet)
*   [Stack technique](#stack-technique)
*   [Progression pédagogique M1 → M6](#progression-p%C3%A9dagogique-m1--m6)
*   [Architecture globale](#architecture-globale)
*   [Cycle Axiome 7E](#cycle-axiome-7e)
*   [Arborescence du projet](#arborescence-du-projet)
*   [Démarrage rapide](#d%C3%A9marrage-rapide)
*   [API REST](#api-rest)
*   [Scoring](#scoring)
*   [Tests](#tests)
*   [Modules détaillés](#modules-d%C3%A9taill%C3%A9s)
*   [Profils Spring](#profils-spring)
*   [Références et guides](#r%C3%A9f%C3%A9rences-et-guides)
*   [Roadmap / prolongements](#roadmap--prolongements)
*   [Licence](#licence)

- - -

## Vision du projet

CyberAudit7E est un POC Spring Boot centré sur l'audit d'accessibilité web. Il combine :

*   une **architecture modulaire Spring**,
*   un **moteur de règles multi-référentiel**,
*   une **persistance JPA/Flyway**,
*   un **pipeline événementiel asynchrone**,
*   une **documentation OpenAPI**,
*   et une **boucle cybernétique 7E** comme cadre d'orchestration.

Le projet sert à la fois de :

*   **POC technique**,
*   **support de formation Spring Boot**,
*   **base d'industrialisation** vers Docker, PostgreSQL, Testcontainers, monitoring et intégration Dokploy.

- - -

## Stack technique

| Composant | Version | Rôle |
| --- | --- | --- |
| Java | 21+ | Runtime (testé avec JDK 25) |
| Spring Boot | 4.0.5 | Framework applicatif |
| Spring Web MVC | 4.x | API REST |
| Spring Data JPA | 4.x | Persistance ORM |
| Hibernate | 7.2.x | Implémentation JPA |
| Jackson | 3.x (`tools.jackson`) | Sérialisation JSON |
| H2 Database | 2.4.x | Base in-memory en développement |
| PostgreSQL | profil prod | Base cible production |
| Flyway | 11.x | Migrations SQL versionnées |
| Jsoup | 1.18.3 | Crawl HTTP et analyse DOM réelle |
| SpringDoc OpenAPI | 2.8.4 | Swagger UI + spec OpenAPI |
| JUnit Jupiter | 6.x | Framework de tests |
| AssertJ | 3.x | Assertions fluides |
| Mockito | starter test | Mocks |
| Awaitility | 4.2.2 | Tests asynchrones |
| Maven Wrapper | 3.9.x | Build tool |

- - -

## Progression pédagogique M1 → M6

| Module | Thème | Livrable principal |
| --- | --- | --- |
| **M1** | Bootstrap Spring Boot | `/api/health` opérationnel |
| **M2** | Architecture IoC + Strategy Pattern | Structure MVC + 7 règles |
| **M3** | Persistance JPA + H2 + Flyway | CRUD `Site` + `AuditReport` |
| **M4** | Moteur d'audit réel avec Jsoup | 13 règles sur DOM réel |
| **M5** | Async, Events, SSE, Scheduler | Audits asynchrones et streaming |
| **M6** | API REST complète + OpenAPI | Swagger UI, pagination, réponses standardisées |
| **M7** | Docker + synthèse (projection) | Industrialisation et conteneurisation |

Résumé volumétrique issu des documents :

| Module | Concepts | Fichiers | Lignes estimées |
| --- | --- | --- | --- |
| M1  | Bootstrap, `@RestController` | 1   | ~20 |
| M2  | IoC, Strategy, Profiles, Events | 35  | ~1850 |
| M3  | JPA, Flyway, `@Transactional` | 39  | ~2300 |
| M4  | Jsoup, DOM réel, poids dynamiques | 50  | ~3550 |
| M5  | `@Async`, SSE, `@Scheduled`, batch | 57  | ~4400 |
| M6  | OpenAPI, pagination, validation | 60  | ~4750 |

- - -

## Architecture globale

### Couches applicatives

```text
Copy┌──────────────────────────────────────────────────────┐
│                    REST API Layer                    │
│  HealthController │ SiteController │ AuditController │
│               GlobalExceptionHandler                 │
├──────────────────────────────────────────────────────┤
│                    Service Layer                     │
│  AuditOrchestrator ──→ chaîne les 7 phases du cycle │
│  AuditEngine ──→ Strategy Pattern (List<AuditRule>) │
│  ScoringService ──→ RGAA×0.5 + WCAG×0.3 + DSFR×0.2  │
├──────────────────────────────────────────────────────┤
│                    Domain Layer                      │
│  Site │ AuditReport │ Phase7E │ RuleCategory        │
│  AuditRule (interface) + implémentations            │
├──────────────────────────────────────────────────────┤
│                    Data Layer                        │
│  SiteRepository │ AuditReportRepository │ RuleConfig │
│  H2 (dev) │ PostgreSQL (prod) │ Flyway migrations   │
└──────────────────────────────────────────────────────┘
```

### Patterns utilisés

| Pattern | Où  | Pourquoi |
| --- | --- | --- |
| Strategy | `AuditRule` + implémentations | Ajouter une règle sans modifier le moteur |
| IoC / DI | Injection par constructeur | Découplage et testabilité |
| Observer | Events Spring + listeners | Découplage entre exécution, métriques et rétroaction |
| DTO | Records et wrappers API | Éviter les références circulaires JPA |
| Repository | Interfaces Spring Data | Génération automatique des accès SQL |
| Template Method | `AuditOrchestrator.executeFullCycle()` | Enchaînement stable des 7 phases |

- - -

## Cycle Axiome 7E

Chaque audit exécute un cycle complet de sept phases orchestré par `AuditOrchestrator`.

```text
Copy   ┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
   │ ÉVALUER ├───→│ ÉLABORER ├───→│ EXÉCUTER ├───→│ EXAMINER │
   └─────────┘    └──────────┘    └──────────┘    └────┬─────┘
                                                       │
   ┌────────────┐    ┌────────┐    ┌─────────┐    ┌───┴────┐
   │ ÉQUILIBRER │←───┤ ÉMETTRE│←───┤ ÉVOLUER │←───┤        │
   └──────┬─────┘    └────────┘    └─────────┘    └────────┘
          │              ▲
          │    @Async     │ ApplicationEvent
          └──── rétroaction cybernétique (FeedbackLoopListener)
```

En M5, ce cycle devient aussi un pipeline événementiel temps réel :

*   `AuditStartedEvent`
*   `AuditProgressEvent`
*   `AuditCompletedEvent`

Ces événements alimentent :

*   le **streaming SSE**,
*   les **métriques d'exécution**,
*   la **rétroaction cybernétique**,
*   les **audits asynchrones / batch / planifiés**.

- - -

## Arborescence du projet

```text
Copycyberaudit7e/
│
├── pom.xml                                        # Dépendances Maven
├── mvnw / mvnw.cmd                                # Maven Wrapper
├── scripts/
│   └── smoke-test.ps1                             # Smoke test E2E
│
├── src/main/java/com/cyberaudit7e/
│   ├── CyberAudit7eApplication.java
│   ├── config/
│   │   ├── AsyncConfig.java
│   │   ├── JpaConfig.java
│   │   ├── OpenApiConfig.java
│   │   └── WebConfig.java
│   ├── controller/
│   │   ├── HealthController.java
│   │   ├── SiteController.java
│   │   ├── AuditController.java
│   │   ├── ConfigController.java
│   │   └── GlobalExceptionHandler.java
│   ├── service/
│   │   ├── AuditEngine.java
│   │   ├── AuditOrchestrator.java
│   │   ├── AsyncAuditService.java
│   │   ├── ScheduledAuditService.java
│   │   ├── HtmlFetcherService.java
│   │   ├── ScoringService.java
│   │   ├── SseNotificationService.java
│   │   └── cycle/
│   │       ├── EvaluateService.java
│   │       ├── ElaborateService.java
│   │       ├── ExecuteService.java
│   │       ├── ExamineService.java
│   │       ├── EvolveService.java
│   │       ├── EmitService.java
│   │       ├── FeedbackLoopListener.java
│   │       └── AuditMetricsListener.java
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── Site.java
│   │   │   ├── AuditReport.java
│   │   │   ├── RuleConfig.java
│   │   │   └── RuleResultListConverter.java
│   │   ├── enums/
│   │   │   ├── Phase7E.java
│   │   │   └── RuleCategory.java
│   │   └── rule/
│   │       ├── AuditRule.java
│   │       ├── AuditContext.java
│   │       ├── TitlePresenceRule.java
│   │       ├── LangAttributeRule.java
│   │       ├── ImageAltRule.java
│   │       ├── ContrastRule.java
│   │       ├── KeyboardNavRule.java
│   │       ├── HeadingStructureRule.java
│   │       ├── FormLabelRule.java
│   │       ├── AriaLandmarkRule.java
│   │       ├── MetaViewportRule.java
│   │       ├── LinkPurposeRule.java
│   │       ├── DsfrHeaderRule.java
│   │       ├── DsfrFooterRule.java
│   │       └── DsfrBreadcrumbRule.java
│   ├── repository/
│   │   ├── SiteRepository.java
│   │   ├── AuditReportRepository.java
│   │   └── RuleConfigRepository.java
│   ├── dto/
│   │   ├── AuditRequestDto.java
│   │   ├── BatchAuditRequestDto.java
│   │   ├── AuditResponseDto.java
│   │   ├── RuleResultDto.java
│   │   ├── SiteDto.java
│   │   ├── ReportSummaryDto.java
│   │   ├── ApiResponse.java
│   │   └── PagedResponse.java
│   └── event/
│       ├── AuditStartedEvent.java
│       ├── AuditProgressEvent.java
│       └── AuditCompletedEvent.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── banner.txt
│   └── db/migration/
│       ├── V1__create_schema.sql
│       ├── V2__seed_data.sql
│       └── V3__rule_configs.sql
│
└── src/test/java/com/cyberaudit7e/
    ├── service/ScoringServiceTest.java
    ├── domain/enums/Phase7ETest.java
    ├── domain/rule/DsfrHeaderRuleTest.java
    ├── repository/SiteRepositoryTest.java
    ├── controller/HealthControllerTest.java
    └── integration/AuditCycleIntegrationTest.java
Copy
```

- - -

## Démarrage rapide

### Prérequis

* Java 21+
* Maven via le wrapper inclus
* Optionnel : `jq`, H2 Console, Bruno ou HTTPie pour les tests manuels

### Lancer l'application

Sous Windows / DOS
mvn -N io.takari:maven:wrapper -Dmaven=3.9.9

ou mieux :
mvn wrapper:wrapper -Dmaven=3.9.9


Sinon
👍mvn org.apache.maven.plugins:maven-wrapper-plugin:3.2.0:wrapper -Dmaven=3.9.9

puis  :
mvnw clean spring-boot:run

Sous Windows / PowerShell :

```powershell
.\mvnw.cmd clean spring-boot:run
```

Sous Linux / macOS :

```bash
./mvnw clean spring-boot:run
```

Au démarrage, le profil `dev` doit :

*   activer H2 in-memory,
*   exécuter Flyway,
*   charger les règles d'audit,
*   exposer l'API sur `http://localhost:8080`.

### Console H2

Ouvrir :

```text
Copyhttp://localhost:8080/h2-console
```

Paramètres :

*   JDBC URL : `jdbc:h2:mem:cyberaudit7e`
*   User : `sa`
*   Password : _(vide)_

### Swagger UI

Après démarrage :

```text
Copyhttp://localhost:8080/swagger-ui.html
```

### OpenAPI

```text
Copyhttp://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

- - -

## API REST

### Santé

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | Health check complet |
| `GET` | `/api/` | Index des endpoints |

### Sites

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/sites` | Enregistrer un site |
| `GET` | `/api/sites?page=0&size=10` | Liste paginée |
| `GET` | `/api/sites/{id}` | Détail d'un site |
| `GET` | `/api/sites/search?name=xxx` | Recherche par nom |
| `DELETE` | `/api/sites/{id}` | Supprimer un site |

### Audits synchrones

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/audits` | Audit synchrone complet |
| `GET` | `/api/audits/list?page=0&size=10&sortBy=auditedAt&direction=desc` | Liste paginée des rapports |
| `GET` | `/api/audits/{id}` | Détail d'un rapport |
| `GET` | `/api/audits/site/{siteId}?page=0&size=10` | Historique paginé d'un site |
| `GET` | `/api/audits/search?q=gouv` | Recherche full-text |
| `GET` | `/api/audits/alerts?threshold=0.5` | Rapports sous le seuil |
| `GET` | `/api/audits/stats` | Statistiques globales |

### Audits asynchrones

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/audits/async` | Soumettre un audit asynchrone |
| `POST` | `/api/audits/batch` | Batch parallèle |
| `GET` | `/api/audits/async/{jobId}` | Statut d'un job |
| `GET` | `/api/audits/async` | Liste des jobs |
| `DELETE` | `/api/audits/async` | Nettoyage des jobs terminés |
| `GET` | `/api/audits/stream` | Flux SSE temps réel |

### Scheduler

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/audits/schedule` | État du scheduler |
| `POST` | `/api/audits/schedule/trigger` | Déclenchement manuel |

### Configuration

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/config/weights` | Poids de scoring |
| `PUT` | `/api/config/weights/{category}` | Modifier un poids |
| `POST` | `/api/config/weights/reset` | Réinitialiser les poids |

### Exemples de commandes

```powershell
Copy# Health check
curl -s http://localhost:8080/api/health

# Créer un site
curl -X POST http://localhost:8080/api/sites `
  -H "Content-Type: application/json" `
  -d '{"url":"https://www.service-public.fr","name":"Service Public"}'

# Lancer un audit synchrone dans DOS : 
curl -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouvernement FR\"}"
# Pour PowerShell :
curl -X POST http://localhost:8080/api/audits `
  -H "Content-Type: application/json" `
  -d '{"url":"https://www.gouvernement.gouv.fr","name":"Gouvernement FR"}'
  
# Voir les statistiques
curl -s http://localhost:8080/api/audits/stats
```

### SSE depuis JavaScript

```javascript
Copyconst sse = new EventSource('/api/audits/stream');

sse.addEventListener('audit-started', (e) => {
  const data = JSON.parse(e.data);
  console.log(`🚀 Audit démarré : ${data.siteName}`);
});

sse.addEventListener('audit-progress', (e) => {
  const data = JSON.parse(e.data);
  console.log(`⏳ ${data.phaseLabel} — ${data.progress}%`);
});

sse.addEventListener('audit-completed', (e) => {
  const data = JSON.parse(e.data);
  console.log(`✅ Score : ${data.scoreGlobal}`);
});
```

- - -

## Scoring

Formule globale :

```text
Copyscore_global = score_rgaa × 0.5 + score_wcag × 0.3 + score_dsfr × 0.2
```

### Répartition des poids

| Catégorie | Poids par défaut | Exemples de règles |
| --- | --- | --- |
| RGAA 4.1 | 50% | titre, `lang`, alt-text, headings, labels |
| WCAG 2.2 | 30% | contraste, clavier, landmarks, viewport, liens |
| DSFR | 20% | header, footer, breadcrumb |

### Règles M4 (13 règles)

| Priorité | ID  | Catégorie | Description |
| --- | --- | --- | --- |
| 10  | RGAA-8.5 | RGAA | Titre de page pertinent |
| 10  | RGAA-8.3 | RGAA | Attribut `lang` |
| 15  | WCAG-1.4.4 | WCAG | Viewport et zoom |
| 20  | WCAG-1.3.1 | WCAG | Landmarks ARIA |
| 20  | DSFR-HDR-01 | DSFR | En-tête DSFR |
| 25  | DSFR-FTR-01 | DSFR | Pied de page DSFR |
| 30  | RGAA-9.1 | RGAA | Hiérarchie des titres |
| 30  | DSFR-BRD-01 | DSFR | Fil d'Ariane |
| 40  | WCAG-2.1.1 | WCAG | Navigation clavier / skip-nav |
| 50  | RGAA-1.1 | RGAA | Alt-text des images |
| 60  | RGAA-11.1 | RGAA | Labels de formulaire |
| 70  | WCAG-2.4.4 | WCAG | Intitulé des liens |
| 80  | WCAG-1.4.3 | WCAG | Contraste (heuristique) |

En M4+, les poids peuvent être :

*   lus depuis la base via `rule_configs`,
*   modifiés via l'API de configuration,
*   réajustés par la boucle de rétroaction.

- - -

## Tests

### Pyramide de tests Spring

```text
Copy           ╱╲
          ╱  ╲       Niveau 4 : Integration (lent, complet)
         ╱ E2E╲      @SpringBootTest — contexte complet
        ╱──────╲
       ╱        ╲    Niveau 3 : Web slice (moyen)
      ╱   Web    ╲   @WebMvcTest + MockMvc + @MockitoBean
     ╱────────────╲
    ╱              ╲ Niveau 2 : JPA slice (moyen)
   ╱   Repository   ╲ @DataJpaTest — couche JPA isolée
  ╱──────────────────╲
 ╱                    ╲ Niveau 1 : Unit (rapide, pur Java)
╱        Unit          ╲ Pas de Spring — JUnit + AssertJ + Mockito
─────────────────────────
```

**Règle d'or** : beaucoup de tests unitaires rapides à la base, peu de tests d'intégration lents au sommet. Une exécution complète doit rester idéalement sous 30 secondes.

### Dépendances de test

Le starter `spring-boot-starter-test` apporte déjà :

*   JUnit Jupiter,
*   AssertJ,
*   Mockito,
*   Spring Test,
*   JsonPath.

Ajouter si nécessaire :

```xml
Copy<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.2</version>
    <scope>test</scope>
</dependency>
```

### Contenu du pack de tests

| Fichier | Niveau | Cible | Concepts |
| --- | --- | --- | --- |
| `ScoringServiceTest.java` | 1 — Unit | `ScoringService` | AssertJ, `within()` |
| `Phase7ETest.java` | 1 — Unit | `Phase7E` | `@ParameterizedTest`, `@CsvSource` |
| `DsfrHeaderRuleTest.java` | 1 — Unit | règle DSFR | `@Nested`, Strategy |
| `SiteRepositoryTest.java` | 2 — JPA | Repository | `@DataJpaTest`, Flyway, rollback |
| `HealthControllerTest.java` | 3 — Web | endpoint `/health` | `@WebMvcTest`, `@MockitoBean`, MockMvc |
| `AuditCycleIntegrationTest.java` | 4 — Intégration | cycle 7E complet | `@SpringBootTest`, Awaitility |
| `scripts/smoke-test.ps1` | E2E | 11 endpoints | PowerShell |

### Lancer les tests

```powershell
Copy# Tous les tests
.\mvnw.cmd test

# Un test spécifique
.\mvnw.cmd test "-Dtest=ScoringServiceTest"

# Intégration uniquement
.\mvnw.cmd test "-Dtest=*IntegrationTest"

# Smoke test E2E
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

### Couverture JaCoCo

Ajouter dans `pom.xml` :

```xml
Copy<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Stratégie recommandée d'extension

*   **M4** : 1 test unitaire par règle d'audit
*   **M5** : tests Awaitility sur le comportement asynchrone et les listeners
*   **M6** : tests d'erreur exhaustifs sur tous les endpoints
*   **M7** : Testcontainers pour remplacer H2 par un vrai PostgreSQL dans les tests d'intégration
*   **Durcissement** : JMH, Spring Cloud Contract, PIT mutation testing

### Outils complémentaires de test manuel

*   **Bruno** : collections HTTP versionnables, local-first
*   **HTTPie** : CLI plus lisible que `curl`
*   **Spring Boot DevTools** : rechargement à chaud pour accélérer la boucle de feedback

- - -

## Modules détaillés

### M1 — Bootstrap Spring Boot

Objectif : démarrer rapidement une application Spring Boot avec un premier endpoint de santé.

Livrables principaux :

*   `CyberAudit7eApplication.java`
*   `HealthController`
*   structure Maven initiale

- - -

### M2 — Architecture Spring

M2 introduit :

*   l'architecture par couches,
*   le Strategy Pattern pour les règles,
*   les DTOs,
*   la gestion des événements,
*   la base du cycle 7E.

#### Concepts démontrés

| Concept | Où dans le code |
| --- | --- |
| IoC / injection constructeur | `AuditEngine`, `AuditOrchestrator` |
| Strategy Pattern + IoC | `AuditRule` → 7 `@Component` |
| `@RestController` | controllers REST |
| `@RestControllerAdvice` | `GlobalExceptionHandler` |
| `@Service` layering | engine → orchestrator → cycle |
| `@Repository` | repositories in-memory |
| Spring Profiles | `application-dev.yml`, `application-prod.yml` |
| Spring Events | `AuditCompletedEvent` |
| `@Async` | `FeedbackLoopListener` |
| Validation Jakarta | DTOs |
| Java Records | objets de transfert |
| CORS | `WebConfig` |

#### Transition M2 → M3

*   supprimer l'exclusion de `DataSourceAutoConfiguration`
*   annoter `Site` et `AuditReport` avec JPA
*   transformer les repositories en interfaces `JpaRepository`
*   activer datasource/JPA/Flyway

- - -

### M3 — Persistance JPA + H2 + Flyway

M3 remplace les repositories en mémoire par une vraie persistance SQL.

#### Apports principaux

*   `@Entity`, `@Table`, `@ManyToOne`, `@OneToMany`
*   `@Transactional`
*   `AttributeConverter` JSON/CLOB
*   Flyway avec schéma et seed data
*   DTOs de sortie dédiés
*   H2 en dev, PostgreSQL en prod

#### Fichiers notables ajoutés / modifiés

*   `RuleResultListConverter.java`
*   `SiteDto.java`
*   `ReportSummaryDto.java`
*   `JpaConfig.java`
*   `V1__create_schema.sql`
*   `V2__seed_data.sql`

#### Concepts démontrés

| Concept | Où  |
| --- | --- |
| Spring Data JPA | repositories |
| Query Methods | `findByUrl`, etc. |
| JPQL custom | agrégats et stats |
| `@PrePersist` / `@PreUpdate` | timestamps |
| `@Transactional(readOnly = true)` | endpoints de lecture |
| Flyway | versionnement du schéma |

- - -

### M4 — Moteur d'audit Jsoup + Strategy avancée

M4 remplace les simulations par une analyse réelle du DOM.

#### Nouveautés majeures

*   `HtmlFetcherService` avec cache
*   `AuditContext` riche (`url + Optional<Document>`)
*   13 règles sur DOM réel
*   configuration dynamique des poids via `RuleConfig`
*   API de gestion des poids
*   rétroaction réelle en base

#### Flux d'exécution M4

```text
CopyAuditEngine.runAllRules(url)
    ├─ HtmlFetcherService.fetch(url)
    ├─ construction de AuditContext(url, doc)
    ├─ évaluation des règles par priorité
    └─ clearCache()
```

#### Concepts démontrés

| Concept | Où  |
| --- | --- |
| Strategy avancée | `evaluate(AuditContext)` + `priority()` |
| cache applicatif | `HtmlFetcherService` |
| config dynamique en BDD | `RuleConfig` |
| fallback gracieux | mode sans document si crawl échoue |
| gestion d'erreur par règle | `try/catch` dans `AuditEngine` |

- - -

### M5 — Async, Events & Streaming SSE

M5 transforme le projet en système événementiel temps réel.

#### Nouveautés majeures

*   audits asynchrones avec `CompletableFuture`
*   batch parallèle
*   SSE pour progression temps réel
*   scheduler d'audits planifiés
*   métriques runtime thread-safe

#### Événements Spring

| Événement | Publié par | Consommé par |
| --- | --- | --- |
| `AuditStartedEvent` | orchestrateur | SSE, métriques |
| `AuditProgressEvent` | orchestrateur | SSE |
| `AuditCompletedEvent` | `EmitService` | SSE, métriques, feedback |

#### Concepts démontrés

| Concept | Où  |
| --- | --- |
| `@Async` + `CompletableFuture` | `AsyncAuditService` |
| `@Scheduled` + cron | `ScheduledAuditService` |
| `SseEmitter` | `SseNotificationService` |
| `CopyOnWriteArrayList` | gestion des clients SSE |
| `AtomicLong` / `volatile` | métriques thread-safe |
| config externalisée | scheduler et timeouts |

- - -

### M6 — API REST complète + OpenAPI

M6 formalise l'API pour un usage plus industriel et plus ergonomique.

#### Nouveautés majeures

*   documentation Swagger UI
*   OpenAPI JSON/YAML
*   pagination et tri sur les listes
*   réponses standardisées `ApiResponse`
*   wrapper `PagedResponse`
*   erreurs homogènes et documentées

#### Concepts démontrés

| Concept | Où  |
| --- | --- |
| SpringDoc OpenAPI | `OpenApiConfig` + annotations |
| `@Tag`, `@Operation`, `@Parameter` | controllers |
| `@ApiResponses` | documentation des codes retour |
| `Pageable` / `Page<T>` | repositories et endpoints |
| tri dynamique | `Sort.by(...)` |
| erreurs structurées | `GlobalExceptionHandler` |

- - -

## Profils Spring

| Profile | DataSource | Flyway | Logs | Usage |
| --- | --- | --- | --- | --- |
| `dev` (défaut) | H2 in-memory | actif | DEBUG | développement local |
| `prod` | PostgreSQL | actif | INFO | Docker / production |

### Changer de profil

```powershell
Copy$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run
```

ou

```powershell
Copy.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

- - -

## Références et guides

### Documentation de référence

*   [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
*   [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/maven-plugin)
*   [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/maven-plugin/build-image.html)
*   [Spring Web](https://docs.spring.io/spring-boot/4.0.5/reference/web/servlet.html)
*   [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.5/reference/data/sql.html#data.sql.jpa-and-spring-data)
*   [Flyway Migration](https://docs.spring.io/spring-boot/4.0.5/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
*   [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.5/reference/using/devtools.html)
*   [Validation](https://docs.spring.io/spring-boot/4.0.5/reference/io/validation.html)

### Guides pratiques

*   [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
*   [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
*   [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
*   [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
*   [Validation](https://spring.io/guides/gs/validating-form-input/)

### Note Maven Parent overrides

Si le projet hérite d'un parent Maven différent, vérifier les éléments hérités comme `<license>` et `<developers>`. Le POM peut contenir des overrides vides pour neutraliser certains héritages non désirés.

- - -

## Roadmap / prolongements

### Pistes techniques immédiates

*   remplacer H2 par PostgreSQL en intégration via Testcontainers
*   ajouter Dockerfile multi-stage + `docker-compose`
*   intégrer Spring Security + JWT
*   brancher un monitoring Actuator + Micrometer + Grafana
*   connecter Gitea / webhooks pour audit au commit
*   ajouter Redis bridge pour communication inter-services

### Pistes d'architecture avancée

*   Spring WebFlux pour un moteur réactif
*   Spring Modulith pour clarifier les frontières applicatives
*   causal discovery engine piloté par IA
*   circuit-breaker de protection
*   modèles dynamiques de santé par site
*   intégration Dokploy-natif pour l'orchestration

### Stratégie de validation industrielle

*   1 test unitaire par règle M4
*   tests async exhaustifs en M5
*   tests de validation et d'erreurs exhaustifs en M6
*   PostgreSQL dockerisé en M7

- - -

## Licence

POC de formation — usage pédagogique.