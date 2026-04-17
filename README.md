# CyberAudit7E

**Moteur d'audit d'accessibilité cybernétique — Spring Boot 4 POC**

Fusion de trois projets :
- **GitManager** → Registre de services (organes) à auditer
- **AuditAccess** → Moteur de règles multi-référentiel (RGAA, WCAG, DSFR) avec scoring pondéré
- **Axiome 7E** → Boucle cybernétique : Évaluer → Élaborer → Exécuter → Examiner → Évoluer → Émettre → Équilibrer

---

## Stack technique

| Composant | Version | Rôle |
|-----------|---------|------|
| Java | 21+ | Runtime (testé avec JDK 25) |
| Spring Boot | 4.0.5 | Framework applicatif |
| Spring Data JPA | 4.x | Persistance ORM |
| Hibernate | 7.2.x | Implémentation JPA |
| Jackson | 3.x (`tools.jackson`) | Sérialisation JSON |
| H2 Database | 2.4.x | BDD in-memory (dev) |
| Flyway | 11.x | Migrations SQL versionnées |
| JUnit Jupiter | 6.x | Framework de tests |
| AssertJ | 3.x | Assertions fluides |
| Maven | 3.9.x (wrapper) | Build tool |

---

## Arborescence complète du projet

```
cyberaudit7e/
│
├── pom.xml                                        # Configuration Maven + dépendances
├── mvnw.cmd                                       # Maven Wrapper (Windows)
├── mvnw                                           # Maven Wrapper (Linux/Mac)
│
├── scripts/
│   └── smoke-test.ps1                             # Test E2E PowerShell (11 endpoints)
│
├── src/
│   ├── main/
│   │   ├── java/com/cyberaudit7e/
│   │   │   │
│   │   │   ├── CyberAudit7eApplication.java       # Point d'entrée @SpringBootApplication
│   │   │   │
│   │   │   ├── config/                            # ── Configuration Spring ──
│   │   │   │   ├── AsyncConfig.java               #   @EnableAsync + ThreadPool (7e-async-*)
│   │   │   │   ├── JpaConfig.java                 #   @EnableJpaAuditing
│   │   │   │   └── WebConfig.java                 #   CORS (préparation dashboard Vue.js)
│   │   │   │
│   │   │   ├── controller/                        # ── Couche REST API ──
│   │   │   │   ├── HealthController.java          #   GET /api/health
│   │   │   │   ├── SiteController.java            #   CRUD /api/sites
│   │   │   │   ├── AuditController.java           #   /api/audits + /api/audits/stats
│   │   │   │   └── GlobalExceptionHandler.java    #   @RestControllerAdvice (400, 404, 500)
│   │   │   │
│   │   │   ├── service/                           # ── Couche métier ──
│   │   │   │   ├── AuditEngine.java               #   Moteur — Strategy Pattern + IoC
│   │   │   │   ├── ScoringService.java            #   Score pondéré RGAA×0.5+WCAG×0.3+DSFR×0.2
│   │   │   │   ├── AuditOrchestrator.java         #   Chaînage @Transactional du cycle 7E
│   │   │   │   │
│   │   │   │   └── cycle/                         # ── Les 7 phases Axiome 7E ──
│   │   │   │       ├── EvaluateService.java       #     Phase 1 : Collecte métriques
│   │   │   │       ├── ElaborateService.java      #     Phase 2 : Plan de remédiation
│   │   │   │       ├── ExecuteService.java        #     Phase 3 : Exécution des règles
│   │   │   │       ├── ExamineService.java        #     Phase 4 : Scoring pondéré
│   │   │   │       ├── EvolveService.java         #     Phase 5 : Tendance (vs audit précédent)
│   │   │   │       ├── EmitService.java           #     Phase 6 : Publication événement Spring
│   │   │   │       └── FeedbackLoopListener.java  #     Phase 7 : Rétroaction @Async @EventListener
│   │   │   │
│   │   │   ├── domain/                            # ── Couche domaine ──
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Site.java                  #     @Entity — site web à auditer
│   │   │   │   │   ├── AuditReport.java           #     @Entity — rapport d'un cycle 7E
│   │   │   │   │   └── RuleResultListConverter.java #   AttributeConverter List<DTO> ↔ JSON CLOB
│   │   │   │   │
│   │   │   │   ├── enums/
│   │   │   │   │   ├── Phase7E.java               #     Enum des 7 phases + next() cyclique
│   │   │   │   │   └── RuleCategory.java          #     RGAA / WCAG / DSFR + poids par défaut
│   │   │   │   │
│   │   │   │   └── rule/                          # ── Strategy Pattern (règles d'audit) ──
│   │   │   │       ├── AuditRule.java             #     Interface contrat
│   │   │   │       ├── TitlePresenceRule.java     #     RGAA-8.5 : titre de page
│   │   │   │       ├── LangAttributeRule.java     #     RGAA-8.3 : attribut lang
│   │   │   │       ├── ImageAltRule.java          #     RGAA-1.1 : alt-text images
│   │   │   │       ├── ContrastRule.java          #     WCAG-1.4.3 : contraste AA (4.5:1)
│   │   │   │       ├── KeyboardNavRule.java       #     WCAG-2.1.1 : navigation clavier
│   │   │   │       ├── DsfrHeaderRule.java        #     DSFR-HDR-01 : en-tête DSFR
│   │   │   │       └── DsfrFooterRule.java        #     DSFR-FTR-01 : pied de page DSFR
│   │   │   │
│   │   │   ├── repository/                        # ── Couche données (Spring Data JPA) ──
│   │   │   │   ├── SiteRepository.java            #     interface JpaRepository + Query Methods
│   │   │   │   └── AuditReportRepository.java     #     interface JpaRepository + JPQL custom
│   │   │   │
│   │   │   ├── dto/                               # ── Objets de transfert (Java Records) ──
│   │   │   │   ├── AuditRequestDto.java           #     Requête audit (validation Jakarta)
│   │   │   │   ├── AuditResponseDto.java          #     Réponse audit (scores + détails)
│   │   │   │   ├── RuleResultDto.java             #     Résultat d'une règle (score 0-1)
│   │   │   │   ├── SiteDto.java                   #     Vue site (évite circular JSON refs)
│   │   │   │   └── ReportSummaryDto.java          #     Vue rapport (évite lazy loading issues)
│   │   │   │
│   │   │   └── event/
│   │   │       └── AuditCompletedEvent.java       #   ApplicationEvent fin de cycle 7E
│   │   │
│   │   └── resources/
│   │       ├── application.yml                    # Config de base (Jackson 3, port 8080)
│   │       ├── application-dev.yml                # Profile DEV : H2 + JPA + Flyway + H2 Console
│   │       ├── application-prod.yml               # Profile PROD : PostgreSQL + HikariCP
│   │       ├── banner.txt                         # Banner ASCII CyberAudit7E au démarrage
│   │       │
│   │       └── db/migration/                      # ── Flyway (migrations SQL versionnées) ──
│   │           ├── V1__create_schema.sql           #   Tables sites + audit_reports + index
│   │           └── V2__seed_data.sql               #   4 sites de test pré-chargés
│   │
│   └── test/
│       └── java/com/cyberaudit7e/
│           │
│           ├── service/
│           │   └── ScoringServiceTest.java         # Niveau 1 — Unit test scoring pondéré
│           │
│           ├── domain/
│           │   ├── enums/
│           │   │   └── Phase7ETest.java            # Niveau 1 — @ParameterizedTest cycle 7E
│           │   └── rule/
│           │       └── DsfrHeaderRuleTest.java     # Niveau 1 — Test Strategy Pattern @Nested
│           │
│           ├── repository/
│           │   └── SiteRepositoryTest.java         # Niveau 2 — @DataJpaTest slice JPA
│           │
│           ├── controller/
│           │   └── HealthControllerTest.java       # Niveau 3 — @WebMvcTest + MockMvc
│           │
│           └── integration/
│               └── AuditCycleIntegrationTest.java  # Niveau 4 — @SpringBootTest complet
│
└── target/                                         # (généré) Artefacts de build Maven
```

---

## Architecture

### Couches applicatives

```
┌──────────────────────────────────────────────────────┐
│                    REST API Layer                     │
│  HealthController │ SiteController │ AuditController  │
│                 GlobalExceptionHandler                │
├──────────────────────────────────────────────────────┤
│                    Service Layer                      │
│  AuditOrchestrator ──→ chaîne les 7 phases du cycle  │
│  AuditEngine ──→ Strategy Pattern (List<AuditRule>)  │
│  ScoringService ──→ RGAA×0.5 + WCAG×0.3 + DSFR×0.2  │
├──────────────────────────────────────────────────────┤
│                    Domain Layer                       │
│  Site │ AuditReport │ Phase7E │ RuleCategory          │
│  AuditRule (interface) ──→ 7 implémentations @Component│
├──────────────────────────────────────────────────────┤
│                    Data Layer                         │
│  SiteRepository │ AuditReportRepository (JPA)         │
│  H2 (dev) │ PostgreSQL (prod) │ Flyway migrations     │
└──────────────────────────────────────────────────────┘
```

### Cycle Axiome 7E

Chaque audit exécute un cycle complet de 7 phases, orchestré par `AuditOrchestrator` :

```
   ┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
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

### Design Patterns utilisés

| Pattern | Où | Pourquoi |
|---------|-----|---------|
| Strategy | `AuditRule` interface + 7 implémentations | Ajouter une règle = 1 fichier, 0 modif moteur |
| IoC / DI | Constructeur injection partout | Découplage total, testabilité |
| Observer | `AuditCompletedEvent` + `@EventListener` | Phase ÉMETTRE → ÉQUILIBRER découplée |
| DTO | Records Java pour l'API | Évite circular refs JPA + lazy loading |
| Repository | `JpaRepository` interfaces | Spring Data génère le SQL automatiquement |
| Template Method | `AuditOrchestrator.executeFullCycle()` | Chaîne fixe des 7 phases |

---

## Démarrage rapide

### Prérequis

- Java 21+ (`java --version`)
- Maven via le wrapper inclus (`mvnw.cmd`)

### Lancer l'application

```powershell
.\mvnw.cmd spring-boot:run
```

L'application démarre sur `http://localhost:8080` avec :
- Profile `dev` actif (H2 in-memory)
- Flyway applique 2 migrations (schéma + 4 sites seed)
- AuditEngine charge 7 règles d'audit
- Banner ASCII CyberAudit7E affiché

### Console H2 (inspection BDD)

Ouvrir `http://localhost:8080/h2-console` :
- JDBC URL : `jdbc:h2:mem:cyberaudit7e`
- User : `sa`
- Password : _(vide)_

---

## API REST

### Sites

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/health` | Statut du service + nombre de règles |
| `POST` | `/api/sites` | Enregistrer un site `{"url":"...","name":"..."}` |
| `GET` | `/api/sites` | Lister tous les sites |
| `GET` | `/api/sites/{id}` | Détail d'un site |
| `GET` | `/api/sites/search?name=xxx` | Recherche par nom (insensible casse) |
| `DELETE` | `/api/sites/{id}` | Supprimer un site + rapports (CASCADE) |

### Audits

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/audits` | Lancer un audit complet (cycle 7E) |
| `GET` | `/api/audits/{id}` | Consulter un rapport |
| `GET` | `/api/audits/site/{siteId}` | Historique des audits d'un site |
| `GET` | `/api/audits/alerts?threshold=0.5` | Rapports sous le seuil |
| `GET` | `/api/audits/stats` | Statistiques globales |

### Exemples curl (PowerShell)

```powershell
# Health check
curl -s http://localhost:8080/api/health

# Lancer un audit
curl -X POST http://localhost:8080/api/audits `
  -H "Content-Type: application/json" `
  -d '{"url":"https://www.gouvernement.gouv.fr","name":"Gouvernement FR"}'

# Stats
curl -s http://localhost:8080/api/audits/stats
```

---

## Tests

### Pyramide de tests

```
           ╱╲           Niveau 4 : @SpringBootTest (contexte complet)
          ╱  ╲          AuditCycleIntegrationTest
         ╱────╲
        ╱      ╲        Niveau 3 : @WebMvcTest (slice web)
       ╱  Web   ╲       HealthControllerTest
      ╱──────────╲
     ╱            ╲     Niveau 2 : @DataJpaTest (slice JPA)
    ╱  Repository  ╲    SiteRepositoryTest
   ╱────────────────╲
  ╱                  ╲  Niveau 1 : Unit pur (pas de Spring)
 ╱  ScoringService    ╲ Phase7ETest, DsfrHeaderRuleTest
╱──────────────────────╲
```

### Lancer les tests

```powershell
# Tous les tests Maven
.\mvnw.cmd test

# Smoke test E2E (application doit tourner)
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1

# Un test spécifique
.\mvnw.cmd test "-Dtest=ScoringServiceTest"
```

### Contenu des tests

| Fichier | Niveau | Cible | Concepts |
|---------|--------|-------|----------|
| `ScoringServiceTest` | 1 — Unit | Formule de scoring | AssertJ, `within()` |
| `Phase7ETest` | 1 — Unit | Enum cyclique | `@ParameterizedTest`, `@CsvSource` |
| `DsfrHeaderRuleTest` | 1 — Unit | Règle DSFR | `@Nested`, Strategy Pattern |
| `SiteRepositoryTest` | 2 — JPA | Repository + Flyway | `@DataJpaTest`, rollback auto |
| `HealthControllerTest` | 3 — Web | Endpoint /health | `@WebMvcTest`, `@MockitoBean`, MockMvc |
| `AuditCycleIntegrationTest` | 4 — Integration | Cycle 7E complet | `@SpringBootTest`, Awaitility |
| `smoke-test.ps1` | E2E | 11 endpoints HTTP | PowerShell, validation status codes |

---

## Modules de formation

Ce projet est le POC fil conducteur d'une formation Spring Boot d'une journée :

| Module | Horaire | Thème | Livrable |
|--------|---------|-------|----------|
| M1 | 08:30–09:30 | Bootstrap Spring Boot | `/api/health` opérationnel |
| M2 | 09:30–10:30 | Architecture IoC + Strategy Pattern | Structure MVC + 7 règles |
| M3 | 10:45–12:00 | Persistance JPA + Flyway | CRUD Site + AuditReport en H2 |
| M4 | 13:00–14:30 | Moteur d'audit avancé | Engine + scoring pondéré |
| M5 | 14:30–15:30 | Async + Events | FeedbackLoop @Async |
| M6 | 15:45–17:00 | API REST complète | Validation, DTOs, endpoints |
| M7 | 17:00–17:30 | Docker + Synthèse | Dockerfile multi-stage + compose |

---

## Scoring

Formule alignée sur AuditAccess :

```
score_global = score_rgaa × 0.5 + score_wcag × 0.3 + score_dsfr × 0.2
```

Chaque catégorie est la moyenne des scores (0.0–1.0) de ses règles :

| Catégorie | Poids | Règles POC |
|-----------|-------|------------|
| RGAA 4.1 | 50% | RGAA-8.5, RGAA-8.3, RGAA-1.1 |
| WCAG 2.2 | 30% | WCAG-1.4.3, WCAG-2.1.1 |
| DSFR | 20% | DSFR-HDR-01, DSFR-FTR-01 |

---

## Profiles Spring

| Profile | DataSource | Flyway | Logs | Usage |
|---------|-----------|--------|------|-------|
| `dev` (défaut) | H2 in-memory | Actif | DEBUG | Développement local |
| `prod` | PostgreSQL | Actif | INFO | Docker / production |

Changer de profile :
```powershell
# Via variable d'environnement
$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run

# Via argument
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

---

## Pour aller plus loin

### Intégration avec l'écosystème existant

- Remplacer les simulations par un vrai crawler HTTP (Jsoup ou Playwright)
- Connecter à Gitea via webhooks pour déclencher des audits au commit
- Ajouter Redis bridge (Spring Data Redis) pour communiquer avec Django/Laravel
- Spring Security + JWT pour protéger l'API

### Concepts Spring avancés

- Spring WebFlux pour un moteur réactif non-bloquant
- Spring Actuator + Micrometer pour le monitoring (Grafana)
- Testcontainers pour tester avec un vrai PostgreSQL dockerisé
- Spring Modulith pour structurer en modules avec frontières claires

### Vers la cybernétique de 3e ordre (L3)

- Causal Discovery Engine via Spring AI
- Circuit-breaker GENESIS_FREEZE avec Resilience4j
- Health Models dynamiques par site avec apprentissage des seuils
- Orchestrateur Dokploy-natif via `generate-dokploy-engine.sh`

---

## Licence

POC de formation — Usage pédagogique.
