Analyse du projet CyberAudit7E
1. Présentation générale
CyberAudit7E est une application Java basée sur Spring Boot, conçue comme un moteur d’audit d’accessibilité web s’appuyant sur une boucle cybernétique en 7 phases (Axiome 7E).
Le projet fusionne trois concepts :

GitManager : registre de sites à auditer (organes).

AuditAccess : moteur de règles multi‑référentiel (RGAA, WCAG, DSFR) avec scoring pondéré.

Axiome 7E : cycle Évaluer → Élaborer → Exécuter → Examiner → Évoluer → Émettre → Équilibrer.

Il servira aussi bien de POC technique que de support de formation Spring Boot et de base industrialisable (Docker, PostgreSQL, monitoring).

2. Stack technique
Composant	Version	Rôle
Java	25	Runtime (JDK 25)
Spring Boot	4.0.5	Framework applicatif
Spring Web MVC	4.x	API REST
Spring Data JPA	4.x	Persistance ORM
Hibernate	7.2.x	Implémentation JPA
Jackson	3.x	Sérialisation JSON
H2 Database	2.4.x	Base de données mémoire (profil dev)
PostgreSQL	16	Base de données cible (profil prod)
Flyway	11.x	Migrations SQL versionnées
Jsoup	1.18.3	Crawl HTTP et analyse DOM réelle
SpringDoc OpenAPI	2.8.4	Documentation Swagger / OpenAPI
Lombok	-	Réduction du code boilerplate (getters, etc.)
JUnit Jupiter, Mockito, AssertJ, Awaitility	–	Tests unitaires, d’intégration et asynchrones
Maven Wrapper	3.9.9	Build tool
3. Architecture applicative
L’application respecte une architecture en couches classique Spring :

text
┌────────────────────────────────────────────────────┐
│                   REST API Layer                    │
│  HealthController | SiteController | AuditController│
│               GlobalExceptionHandler                │
├────────────────────────────────────────────────────┤
│                    Service Layer                    │
│  AuditOrchestrator (cycle 7E)                       │
│  AuditEngine (Strategy Pattern – règles d’audit)   │
│  ScoringService (pondération RGAA/WCAG/DSFR)       │
├────────────────────────────────────────────────────┤
│                    Domain Layer                     │
│  Site, AuditReport, Phase7E, RuleCategory          │
│  AuditRule (interface) + 13 implémentations        │
├────────────────────────────────────────────────────┤
│                    Data Layer                       │
│  SiteRepository, AuditReportRepository, RuleConfig  │
│  H2 (dev) / PostgreSQL (prod) + Flyway migrations  │
└────────────────────────────────────────────────────┘
Patterns utilisés :

Strategy : AuditRule + ses implémentations (« plug and play »)

IoC / DI : injection par constructeur (découplage, testabilité)

Observer : événements Spring (AuditStartedEvent, AuditProgressEvent, AuditCompletedEvent)

DTO : records Java pour éviter l’exposition directe des entités JPA

Repository : interfaces JpaRepository pour l’accès aux données

Template Method : AuditOrchestrator.executeFullCycle() qui enchaîne les 7 phases

4. Cycle Axiome 7E
Chaque audit parcourt 7 phases orchestrées par AuditOrchestrator. Des événements Spring sont émis à chaque transition, permettant le streaming SSE (Server‑Sent Events) vers les clients connectés.

text
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │ ÉVALUER │───→│ ÉLABORER│───→│ EXÉCUTER│
   └─────────┘    └─────────┘    └────┬────┘
                                      ▼
   ┌───────────┐                   ┌─────────┐
   │ÉQUILIBRER │                   │EXAMINER │
   └───────────┘                   └────┬────┘
        ▲                                │
        │                                ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │ ÉMETTRE │←───│ ÉVOLUER │←───│         │
   └─────────┘    └─────────┘    └─────────┘
Le FeedbackLoopListener réagit à AuditCompletedEvent pour ajuster dynamiquement les poids de scoring (phase ÉQUILIBRER) – une forme de rétroaction cybernétique.

5. Fonctionnalités principales
Gestion des sites
CRUD complet via API REST (/api/sites)

Recherche par nom, pagination

URL unique garantie

Audit d’accessibilité
13 règles réelles évaluées sur le DOM (Jsoup) couvrant RGAA, WCAG et DSFR.

Scoring pondéré :
score = RGAA×0.5 + WCAG×0.3 + DSFR×0.2
les poids sont modifiables en base de données (rule_configs) et ajustables par la boucle de rétroaction.

Comparaison avec l'audit précédent → tendance UP, DOWN, STABLE, FIRST.

Modes d’exécution
Synchrone : blocant, retourne immédiatement le rapport.

Asynchrone : retourne un jobId, suivi possible par polling ou SSE.

Batch : plusieurs audits en parallèle (max 10).

Programmé : scheduler basé sur une expression cron (ex. tous les jours à 2h).

Streaming temps réel (SSE)
Flux d’événements (/api/audits/stream) : connected, audit‑started, audit‑progress, audit‑completed.

Permet de créer un tableau de bord dynamique (un fichier dashboard.html est fourni dans le projet).

API REST complète (M6)
Pagination et tri sur toutes les listes.

Réponses structurées (ApiResponse, PagedResponse).

Documentation interactive Swagger UI (/swagger‑ui.html).

Persistance et migrations
H2 pour le développement (profil dev).

PostgreSQL pour la production (profil prod).

Flyway avec 4 migrations : schéma initial, données de test (V2__seed_data.sql), table rule_configs et index supplémentaires.

Tests
Pyramide de tests : unitaires (@Nested, paramétrés), JPA (@DataJpaTest), Web (@WebMvcTest), intégration (@SpringBootTest + Awaitility).

Scripts PowerShell et bash fournis (smoke-test.ps1, test-cyberaudit7e.ps1/sh) pour valider tous les endpoints.

6. Organisation des modules (M1 → M7)
Le projet a été construit par étapes pédagogiques :

Module	Thème	Livrables clés
M1	Bootstrap Spring Boot	HealthController, endpoint /api/health
M2	Architecture IoC + Strategy Pattern	Structure MVC, 7 règles simulées
M3	Persistance JPA + H2 + Flyway	CRUD Site, AuditReport avec tendances
M4	Moteur d’audit réel avec Jsoup	13 règles analysant le DOM réel, poids dynamiques
M5	Async, Events, SSE, Scheduler	@Async, CompletableFuture, batch, planification
M6	API REST complète + OpenAPI	Swagger UI, pagination, réponses standardisées
M7	Docker + synthèse	Dockerfile multi‑stage, Docker Compose (profiles dev/prod), Nginx reverse proxy
7. Dockerisation
Fichiers fournis
Dockerfile : multi‑stage (build Maven + runtime JRE 25), utilisateur non‑root, healthcheck.

docker-compose.yml : deux profiles – dev (H2, seul un conteneur app) et prod (app + PostgreSQL 16).

.dockerignore : optimise le contexte de build.

nginx.conf : reverse proxy optionnel, avec configuration adaptée au SSE (désactivation du buffering).

Commandes usuelles
bash
# Développement (H2)
docker compose up --build

# Production (PostgreSQL)
docker compose --profile prod up --build

# Nettoyage
docker compose down -v
8. Scripts de test et d’aide
Le répertoire scripts/ contient :

smoke-test.ps1 : test end‑to‑end de 11 endpoints (health, sites, audits, validation).

test-cyberaudit7e.ps1 / .sh : tests complets couvrant M1 à M5 (synchrones, asynchrones, batch, scheduler, SSE).

diagnose-packages.ps1 : vérifie l’alignement des packages Java (évite les erreurs Spring Test).

realign-test-packages.ps1 : corrige automatiquement les packages des tests.

9. Points d’entrée et documentation
Accès	URL
API index	http://localhost:8080/api/
Health check	http://localhost:8080/api/health
Swagger UI	http://localhost:8080/swagger-ui.html
OpenAPI JSON	http://localhost:8080/v3/api-docs
Console H2 (profil dev)	http://localhost:8080/h2-console (JDBC URL : jdbc:h2:mem:cyberaudit7e, user sa)
10. Synthèse et apports pédagogiques
Le projet illustre un grand nombre de concepts Spring Boot avancés :

Injection de dépendances et Strategy Pattern (les règles sont des @Component injectées dans AuditEngine).

Programmation événementielle avec ApplicationEvent et @EventListener.

Traitements asynchrones (@Async, CompletableFuture) et scheduling (@Scheduled).

Streaming SSE (server‑sent events) via SseEmitter.

Profils Spring (dev / prod) et configuration externalisée.

Gestion des erreurs centralisée (@RestControllerAdvice).

Documentation d’API avec SpringDoc OpenAPI.

Tests à plusieurs niveaux (unitaire, JPA, web, intégration, asynchrone).

Docker et Docker Compose avec profiles.

C’est un excellent support pour une formation pratique à Spring Boot, tout en livrant un moteur d’audit réel (13 règles d’accessibilité) utilisable pour évaluer la conformité de sites web français (RGAA, WCAG, DSFR).

11. Prolongements possibles (mentionnés dans la roadmap)
Remplacer H2 par PostgreSQL dans les tests d’intégration (Testcontainers)

Ajouter Spring Security + JWT

Monitoring avec Actuator / Micrometer / Grafana

Webhooks Gitea pour audit automatique à chaque commit

Moteur réactif (Spring WebFlux)

Intégration Dokploy

Utilisation de Playwright au lieu de Jsoup pour une analyse plus poussée du contraste et des interactions dynamiques

Conclusion : CyberAudit7E est un projet Spring Boot complet, bien structuré, prêt à être industrialisé, qui combine audit d’accessibilité, architecture cybernétique et bonnes pratiques d’entreprise. Il est fourni avec tous les fichiers nécessaires (Java, configuration, Docker, scripts de test) pour être reconstruit et exécuté immédiatement.