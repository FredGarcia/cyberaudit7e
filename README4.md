# CyberAudit7E — Module M4 : Moteur d'audit Jsoup + Strategy Pattern avancé

## La grande transition de M4

M4 remplace les **simulations** par un **vrai crawler HTTP**. Les règles analysent
maintenant le DOM réel des sites via Jsoup. C'est le passage du POC prototype au POC fonctionnel.

## Changements M3 → M4

### Nouvelles dépendances (pom.xml)

```xml
<!-- Ajouter dans <dependencies> -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.3</version>
</dependency>
```

### Architecture : le nouveau flux d'exécution

```
AuditEngine.runAllRules(url)
    │
    ├─ 1. HtmlFetcherService.fetch(url)     ← NOUVEAU : crawl HTTP réel
    │      └─ Jsoup.connect(url).get()
    │      └─ Retourne Optional<Document>
    │
    ├─ 2. Construit AuditContext(url, doc)   ← NOUVEAU : contexte riche
    │
    ├─ 3. Pour chaque règle (triée par priorité) :
    │      rule.evaluate(AuditContext)        ← MODIFIÉ : reçoit le DOM
    │      └─ Analyse le Document Jsoup
    │
    └─ 4. htmlFetcher.clearCache()
```

### Fichiers modifiés

| Fichier | Changement |
|---------|------------|
| `domain/rule/AuditRule.java` | `evaluate(String url)` → `evaluate(AuditContext context)` + `priority()` |
| `domain/rule/*.java` (7 règles) | Simulations → analyse DOM réelle via Jsoup |
| `service/AuditEngine.java` | Intègre HtmlFetcherService, tri par priorité, try/catch par règle |
| `service/ScoringService.java` | Poids chargés dynamiquement depuis BDD (table rule_configs) |
| `service/cycle/FeedbackLoopListener.java` | Rétroaction réelle : ajuste les poids en BDD |
| `controller/HealthController.java` | Affiche version M4, mode fetcher, poids dynamiques |

### Fichiers ajoutés

| Fichier | Rôle |
|---------|------|
| `service/HtmlFetcherService.java` | Crawler HTTP Jsoup avec cache par URL |
| `domain/rule/AuditContext.java` | Record portant URL + Optional<Document> |
| `domain/rule/HeadingStructureRule.java` | RGAA 9.1 — hiérarchie h1-h6 |
| `domain/rule/FormLabelRule.java` | RGAA 11.1 — étiquettes de formulaire |
| `domain/rule/AriaLandmarkRule.java` | WCAG 1.3.1 — landmarks sémantiques |
| `domain/rule/MetaViewportRule.java` | WCAG 1.4.4 — viewport et zoom |
| `domain/rule/LinkPurposeRule.java` | WCAG 2.4.4 — intitulé des liens |
| `domain/rule/DsfrBreadcrumbRule.java` | DSFR-BRD-01 — fil d'Ariane |
| `domain/entity/RuleConfig.java` | Entité JPA pour les poids dynamiques |
| `repository/RuleConfigRepository.java` | Repository pour rule_configs |
| `controller/ConfigController.java` | API CRUD pour les poids de scoring |
| `db/migration/V3__rule_configs.sql` | Table + seed des poids par défaut |
| `pom-additions.xml` | Dépendance Jsoup à ajouter |

### Inventaire des règles M4 (13 règles)

| Priorité | ID | Catégorie | Description | Mode |
|----------|-----|-----------|------------|------|
| 10 | RGAA-8.5 | RGAA | Titre de page pertinent | DOM réel |
| 10 | RGAA-8.3 | RGAA | Attribut lang sur html | DOM réel |
| 15 | WCAG-1.4.4 | WCAG | Viewport et zoom | DOM réel |
| 20 | WCAG-1.3.1 | WCAG | Landmarks ARIA | DOM réel |
| 20 | DSFR-HDR-01 | DSFR | En-tête DSFR | DOM réel |
| 25 | DSFR-FTR-01 | DSFR | Pied de page DSFR | DOM réel |
| 30 | RGAA-9.1 | RGAA | Hiérarchie des titres | DOM réel |
| 30 | DSFR-BRD-01 | DSFR | Fil d'Ariane | DOM réel |
| 40 | WCAG-2.1.1 | WCAG | Navigation clavier / skip-nav | DOM réel |
| 50 | RGAA-1.1 | RGAA | Alt-text des images | DOM réel |
| 60 | RGAA-11.1 | RGAA | Étiquettes de formulaires | DOM réel |
| 70 | WCAG-2.4.4 | WCAG | Intitulé des liens | DOM réel |
| 80 | WCAG-1.4.3 | WCAG | Contraste (heuristique) | Partiel |

## Installation

```bash
# 1. Ajouter Jsoup dans pom.xml (voir pom-additions.xml)

# 2. Copier les fichiers M4
cp -r src/ <votre-projet-cyberaudit7e>/

# 3. Build + run
cd <votre-projet-cyberaudit7e>
mvnw.cmd spring-boot:run
```

### Logs attendus au démarrage

```
Flyway: Migrating schema "PUBLIC" to version "3 - rule configs"
Successfully applied 1 migration

AuditEngine M4 initialisé — 13 règles (Jsoup actif)
  ├─ [RGAA]  p10 RGAA-8.5 — Chaque page web a un titre de page pertinent
  ├─ [RGAA]  p10 RGAA-8.3 — Langue par défaut indiquée dans l'élément HTML
  ├─ [WCAG]  p15 WCAG-1.4.4 — Viewport n'empêche pas le zoom utilisateur
  ...
  └─ [WCAG]  p80 WCAG-1.4.3 — Contraste minimum 4.5:1 pour le texte normal
```

## Tests curl — Validation M4

```bash CMD , pas PowerShell !!
# ═══ 1. Health check M4 (version + poids dynamiques) ═══
curl -s http://localhost:8080/api/health | jq .
# → version: "M4", fetcherMode: "Jsoup (HTTP réel)", weights: {...}

# ═══ 2. Voir les poids de scoring actuels ═══
curl -s http://localhost:8080/api/config/weights | jq .
# → RGAA: 0.5, WCAG: 0.3, DSFR: 0.2, normalized: true

# ═══ 3. Audit RÉEL d'un site .gouv.fr (crawl HTTP) ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouvernement FR\"}" | jq .
# → 13 règles évaluées sur le DOM réel !
# → Observe les vrais titres, lang, landmarks, images, liens...

# ═══ 4. Audit d'un site non-gouvernemental ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.example.com\",\"name\":\"Example.com\"}" | jq .
# → Scores DSFR bas (pas de composants DSFR)

# ═══ 5. Audit de service-public.fr ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Service Public\"}" | jq .

# ═══ 6. Audit d'un site accessible connu ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.legifrance.gouv.fr\",\"name\":\"Légifrance\"}" | jq .

# ═══ 7. Vérifier les poids après rétroaction ═══
curl -s http://localhost:8080/api/config/weights | jq .
# → Les poids ont pu bouger si le FeedbackLoopListener a détecté des anomalies

# ═══ 8. Modifier un poids manuellement ═══
curl -s -X PUT http://localhost:8080/api/config/weights/RGAA ^
  -H "Content-Type: application/json" ^
  -d "{\"weight\":0.6}" | jq .

# ═══ 9. Remettre les poids par défaut ═══
curl -s -X POST http://localhost:8080/api/config/weights/reset | jq .

# ═══ 10. Stats globales (après plusieurs audits) ═══
curl -s http://localhost:8080/api/audits/stats | jq .

# ═══ 11. Historique d'un site ═══
curl -s http://localhost:8080/api/audits/site/2 | jq .

# ═══ 12. Alertes — sites sous le seuil ═══
curl -s "http://localhost:8080/api/audits/alerts?threshold=0.6" | jq .

# ═══ 13. Console H2 — vérifier rule_configs ═══
# http://localhost:8080/h2-console
# SQL : SELECT * FROM rule_configs;
# → Voir si les poids ont été ajustés par la rétroaction

# ═══ 14. Test avec un site inaccessible (mode dégradé) ═══
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://site-inexistant.invalid\",\"name\":\"Test 404\"}" | jq .
# → Toutes les règles retournent "Impossible de crawler la page"
```

## Concepts Spring démontrés en M4

| Concept | Où dans le code |
|---------|----------------|
| Strategy Pattern avancé | AuditRule.evaluate(AuditContext) + priority() |
| IoC automatique | 13 @Component → List<AuditRule> dans AuditEngine |
| Service injection | HtmlFetcherService injecté dans AuditEngine |
| Cache applicatif | HtmlFetcherService.cache (ConcurrentHashMap) |
| Record comme contexte | AuditContext(url, Optional<Document>) |
| JPA AttributeConverter | RuleResultListConverter (JSON ↔ CLOB) |
| Config dynamique en BDD | RuleConfig + RuleConfigRepository |
| @Transactional write | FeedbackLoopListener ajuste les poids |
| Error handling par règle | try/catch dans AuditEngine.runAllRules() |
| Fallback gracieux | AuditContext.withoutDocument() si crawl échoue |

## Transition vers M5 (Async & Events avancés)

M4 est le moteur fonctionnel. Pour M5 :
1. Audits asynchrones avec CompletableFuture
2. Notifications par événements (WebSocket ou SSE)
3. Audits programmés (@Scheduled) — audit périodique automatique
4. Parallélisation des règles (ExecutorService)
