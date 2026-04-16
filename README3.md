# CyberAudit7E — Module M3 : Persistance JPA + H2 + Flyway

## Changements M2 → M3

### Fichiers modifiés

| Fichier | Changement |
|---------|------------|
| `CyberAudit7eApplication.java` | Suppression `exclude = DataSourceAutoConfiguration` |
| `domain/entity/Site.java` | POJO → `@Entity` + `@Table` + `@OneToMany` + lifecycle callbacks |
| `domain/entity/AuditReport.java` | POJO → `@Entity` + `@ManyToOne(LAZY)` + `@Convert` JSON |
| `repository/SiteRepository.java` | `ConcurrentHashMap` → `interface extends JpaRepository` |
| `repository/AuditReportRepository.java` | `ConcurrentHashMap` → `interface extends JpaRepository` |
| `service/AuditOrchestrator.java` | Ajout `@Transactional` + bidirectionnel `site.addReport()` |
| `service/cycle/FeedbackLoopListener.java` | Ajout `@Transactional` + `siteRepository.save()` pour la phase |
| `controller/SiteController.java` | Retourne `SiteDto` au lieu de l'entité + `@Transactional(readOnly)` |
| `controller/AuditController.java` | Retourne `ReportSummaryDto` + endpoint `/alerts` + stats enrichies |
| `application-dev.yml` | H2 datasource + JPA + Flyway activés |
| `application-prod.yml` | PostgreSQL + HikariCP configurés |
| `application.yml` | Jackson config (dates ISO, pas de null) |

### Fichiers ajoutés

| Fichier | Rôle |
|---------|------|
| `domain/entity/RuleResultListConverter.java` | `AttributeConverter` JPA : `List<RuleResultDto>` ↔ JSON CLOB |
| `dto/SiteDto.java` | DTO de réponse pour les sites (évite circular refs) |
| `dto/ReportSummaryDto.java` | DTO de réponse pour les rapports |
| `config/JpaConfig.java` | `@EnableJpaAuditing` + ObjectMapper beans |
| `resources/db/migration/V1__create_schema.sql` | Schéma initial (tables + index) |
| `resources/db/migration/V2__seed_data.sql` | 4 sites de test pré-chargés |

### Fichiers inchangés (21 fichiers)

Enums, Rules (7), DTOs (Request/Response/RuleResult), Services (AuditEngine,
ScoringService, Evaluate, Elaborate, Execute, Examine, Evolve*, Emit),
Event, Config (Async, Web, CORS), HealthController, GlobalExceptionHandler, banner.

> *EvolveService est recopié tel quel — preuve que l'IoC fonctionne :
> le service n'a pas changé alors que l'implémentation du repository a
> été complètement remplacée (ConcurrentHashMap → JPA/SQL).

## Structure du projet M3

```
src/main/java/com/cyberaudit7e/
├── CyberAudit7eApplication.java           # MODIFIÉ : plus d'exclude
│
├── config/
│   ├── AsyncConfig.java                   # inchangé
│   ├── JpaConfig.java                     # NOUVEAU : @EnableJpaAuditing
│   └── WebConfig.java                     # inchangé
│
├── controller/
│   ├── HealthController.java              # inchangé
│   ├── SiteController.java                # MODIFIÉ : SiteDto + @Transactional
│   ├── AuditController.java              # MODIFIÉ : ReportSummaryDto + /alerts + stats
│   └── GlobalExceptionHandler.java        # inchangé
│
├── service/
│   ├── AuditEngine.java                   # inchangé
│   ├── ScoringService.java                # inchangé
│   ├── AuditOrchestrator.java             # MODIFIÉ : @Transactional
│   └── cycle/
│       ├── EvaluateService.java           # inchangé
│       ├── ElaborateService.java          # inchangé
│       ├── ExecuteService.java            # inchangé
│       ├── ExamineService.java            # inchangé
│       ├── EvolveService.java             # inchangé (IoC !)
│       ├── EmitService.java               # inchangé
│       └── FeedbackLoopListener.java      # MODIFIÉ : @Transactional + JPA save
│
├── domain/
│   ├── entity/
│   │   ├── Site.java                      # MODIFIÉ : @Entity JPA
│   │   ├── AuditReport.java              # MODIFIÉ : @Entity + @Convert
│   │   └── RuleResultListConverter.java   # NOUVEAU : JSON ↔ CLOB
│   ├── enums/                             # inchangés
│   └── rule/                              # inchangés (7 règles)
│
├── repository/
│   ├── SiteRepository.java               # MODIFIÉ : interface JpaRepository
│   └── AuditReportRepository.java         # MODIFIÉ : interface JpaRepository
│
├── dto/
│   ├── AuditRequestDto.java               # inchangé
│   ├── AuditResponseDto.java              # inchangé
│   ├── RuleResultDto.java                 # inchangé
│   ├── SiteDto.java                       # NOUVEAU
│   └── ReportSummaryDto.java              # NOUVEAU
│
└── event/
    └── AuditCompletedEvent.java           # inchangé

src/main/resources/
├── application.yml                        # MODIFIÉ : Jackson config
├── application-dev.yml                    # MODIFIÉ : H2 + JPA + Flyway
├── application-prod.yml                   # MODIFIÉ : PostgreSQL + Hikari
├── banner.txt                             # inchangé
└── db/migration/
    ├── V1__create_schema.sql              # NOUVEAU
    └── V2__seed_data.sql                  # NOUVEAU
```

## Concepts Spring démontrés en M3

| Concept | Où dans le code |
|---------|----------------|
| Spring Data JPA | SiteRepository, AuditReportRepository (interfaces) |
| Query Methods | findByUrl, findFirstBySiteIdOrderByAuditedAtDesc, etc. |
| JPQL custom | @Query("SELECT AVG(r.scoreGlobal)...") |
| @Entity + @Table | Site.java, AuditReport.java |
| @ManyToOne / @OneToMany | AuditReport ↔ Site (bidirectionnel) |
| FetchType.LAZY | AuditReport.site — chargé à la demande |
| @PrePersist / @PreUpdate | Timestamps automatiques |
| JPA AttributeConverter | RuleResultListConverter (List → JSON) |
| @Transactional | AuditOrchestrator (write), FeedbackLoopListener (write) |
| @Transactional(readOnly) | Endpoints GET dans les controllers |
| Flyway migrations | V1 (schéma) + V2 (seed data) — versionnées |
| H2 Console | /h2-console en dev pour inspecter la BDD |
| Spring Profiles | dev (H2) vs prod (PostgreSQL) — même code |
| DTO pattern | SiteDto, ReportSummaryDto — jamais d'entité dans l'API |

## Installation

```bash
# Remplacer les fichiers M2 par ceux de M3
# (les fichiers inchangés sont identiques, le remplacement est safe)
cp -r src/ <votre-projet-cyberaudit7e>/

# Vérifier que pom.xml contient bien ces dépendances (Spring Initializr M1) :
#   spring-boot-starter-data-jpa
#   h2 (runtime scope)
#   flyway-core

# Build + run
cd <votre-projet-cyberaudit7e>
mvnw.cmd spring-boot:run
```

## Vérification Flyway au démarrage

Dans les logs, vous devez voir :
```
Flyway Community Edition ...
Successfully validated 2 migrations
Migrating schema "PUBLIC" to version "1 - create schema"
Migrating schema "PUBLIC" to version "2 - seed data"
Successfully applied 2 migrations
```

## Tests curl — Validation M3

```bash
# ═══ 1. Health check ═══
curl -s http://localhost:8080/api/health | jq .

# ═══ 2. Vérifier les sites pré-chargés par V2__seed_data.sql ═══
curl -s http://localhost:8080/api/sites | jq .
# → 4 sites (Service Public, Gouvernement, Légifrance, Example.com)

# ═══ 3. Détail d'un site seed ═══
curl -s http://localhost:8080/api/sites/1 | jq .
# → SiteDto avec auditsCount=0, lastScore=null

# ═══ 4. Recherche par nom ═══
curl -s "http://localhost:8080/api/sites/search?name=gouv" | jq .
# → Gouvernement FR + Légifrance

# ═══ 5. Audit d'un site .gouv.fr (scores DSFR élevés) ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouvernement FR\"}" | jq .
# → Score DSFR élevé car .gouv.fr détecté par DsfrHeaderRule + DsfrFooterRule

# ═══ 6. Audit d'un site non-.gouv.fr ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.example.com\",\"name\":\"Example.com\"}" | jq .
# → Score DSFR bas

# ═══ 7. Deuxième audit du même site (test tendance ÉVOLUER) ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouvernement FR\"}" | jq .
# → trend devrait être "STABLE" (même score simulé)

# ═══ 8. Historique d'un site ═══
curl -s http://localhost:8080/api/audits/site/2 | jq .
# → Liste des rapports du Gouvernement (plus récent en premier)

# ═══ 9. Consulter un rapport ═══
curl -s http://localhost:8080/api/audits/1 | jq .
# → ReportSummaryDto avec détails des 7 règles

# ═══ 10. Alertes (sites sous le seuil) ═══
curl -s "http://localhost:8080/api/audits/alerts?threshold=0.7" | jq .

# ═══ 11. Stats globales ═══
curl -s http://localhost:8080/api/audits/stats | jq .
# → totalSites, totalAudits, averageScore, trends, sitesByPhase

# ═══ 12. Console H2 (navigateur) ═══
# Ouvrir http://localhost:8080/h2-console
# JDBC URL : jdbc:h2:mem:cyberaudit7e
# User : sa / Password : (vide)
# → Inspecter les tables sites et audit_reports
# → Exécuter : SELECT * FROM sites;
# → Exécuter : SELECT * FROM audit_reports;

# ═══ 13. Test validation (400) ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"\",\"name\":\"\"}" | jq .

# ═══ 14. Test not found (404) ═══
curl -s http://localhost:8080/api/sites/999 | jq .

# ═══ 15. Créer un nouveau site + audit immédiat ═══
curl -s -X POST http://localhost:8080/api/sites ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.numerique.gouv.fr\",\"name\":\"DINUM\"}" | jq .

curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.numerique.gouv.fr\",\"name\":\"DINUM\"}" | jq .
# → Score DSFR élevé (.gouv.fr) + persisté en H2
```

## Console H2 : requêtes SQL utiles

```sql
-- Tous les sites avec leur phase
SELECT id, name, url, current_phase, created_at FROM sites;

-- Rapports d'audit avec scores
SELECT r.id, s.name, r.score_rgaa, r.score_wcag, r.score_dsfr,
       r.score_global, r.trend, r.audited_at
FROM audit_reports r
JOIN sites s ON r.site_id = s.id
ORDER BY r.audited_at DESC;

-- Score moyen par site
SELECT s.name, COUNT(r.id) as audits, AVG(r.score_global) as avg_score
FROM sites s
LEFT JOIN audit_reports r ON r.site_id = s.id
GROUP BY s.name;

-- Vérifier la migration Flyway
SELECT * FROM flyway_schema_history;
```

## Transition vers M4 (Moteur d'audit avancé)

M3 est complet. Pour M4 :
1. Remplacer les simulations dans les règles par un vrai crawler HTTP (Jsoup)
2. Ajouter des règles dynamiques chargées depuis la BDD
3. Enrichir le Strategy Pattern avec un système de poids configurables
4. Implémenter la vraie logique de rétroaction dans FeedbackLoopListener
