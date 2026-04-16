# CyberAudit7E — Module M2 : Architecture Spring

## Structure du projet

```
src/main/java/com/cyberaudit7e/
├── CyberAudit7eApplication.java          # Point d'entrée (@SpringBootApplication)
│
├── config/
│   ├── AsyncConfig.java                  # @EnableAsync + ThreadPool (7e-async-*)
│   └── WebConfig.java                    # CORS pour futur dashboard Vue.js
│
├── controller/
│   ├── HealthController.java             # GET /api/health (enrichi M2)
│   ├── SiteController.java               # CRUD sites (POST/GET/DELETE)
│   ├── AuditController.java              # Cycle 7E (POST audit, GET reports)
│   └── GlobalExceptionHandler.java       # @RestControllerAdvice centralisé
│
├── service/
│   ├── AuditEngine.java                  # Moteur — Strategy Pattern + IoC
│   ├── ScoringService.java               # Score pondéré RGAA×0.5+WCAG×0.3+DSFR×0.2
│   ├── AuditOrchestrator.java            # Chaînage du cycle 7E complet
│   └── cycle/                            # ── Les 7 phases Axiome 7E ──
│       ├── EvaluateService.java          # Phase 1 : Collecte métriques
│       ├── ElaborateService.java         # Phase 2 : Plan de remédiation
│       ├── ExecuteService.java           # Phase 3 : Exécution des règles
│       ├── ExamineService.java           # Phase 4 : Scoring pondéré
│       ├── EvolveService.java            # Phase 5 : Tendance (comparaison)
│       ├── EmitService.java              # Phase 6 : Publication événement
│       └── FeedbackLoopListener.java     # Phase 7 : Rétroaction cybernétique
│
├── domain/
│   ├── entity/
│   │   ├── Site.java                     # POJO site (JPA en M3)
│   │   └── AuditReport.java             # POJO rapport (JPA en M3)
│   ├── enums/
│   │   ├── Phase7E.java                  # Les 7 phases + next() cyclique
│   │   └── RuleCategory.java            # RGAA/WCAG/DSFR + poids par défaut
│   └── rule/                             # ── Strategy Pattern ──
│       ├── AuditRule.java                # Interface contrat
│       ├── TitlePresenceRule.java        # RGAA-8.5 : titre de page
│       ├── LangAttributeRule.java        # RGAA-8.3 : attribut lang
│       ├── ImageAltRule.java             # RGAA-1.1 : alt-text images
│       ├── ContrastRule.java             # WCAG-1.4.3 : contraste AA
│       ├── KeyboardNavRule.java          # WCAG-2.1.1 : navigation clavier
│       ├── DsfrHeaderRule.java           # DSFR-HDR-01 : en-tête DSFR
│       └── DsfrFooterRule.java           # DSFR-FTR-01 : pied de page DSFR
│
├── repository/
│   ├── SiteRepository.java              # In-memory ConcurrentHashMap (JPA en M3)
│   └── AuditReportRepository.java       # In-memory ConcurrentHashMap (JPA en M3)
│
├── dto/
│   ├── AuditRequestDto.java             # Record + validation Jakarta
│   ├── AuditResponseDto.java            # Record + factory method
│   └── RuleResultDto.java               # Record + factories success/failure/partial
│
└── event/
    └── AuditCompletedEvent.java          # ApplicationEvent pour la phase ÉMETTRE

src/main/resources/
├── application.yml                       # Config de base (profile: dev)
├── application-dev.yml                   # Profile DEV (logs DEBUG, prêt pour H2)
├── application-prod.yml                  # Profile PROD (prêt pour PostgreSQL)
└── banner.txt                            # Banner ASCII CyberAudit7E
```

## Concepts Spring démontrés en M2

| Concept                    | Où dans le code                              |
|----------------------------|----------------------------------------------|
| IoC / Injection constructeur | AuditEngine, AuditOrchestrator (tous les services) |
| Strategy Pattern + IoC     | AuditRule interface → 7 @Component auto-injectés   |
| @RestController            | HealthController, SiteController, AuditController  |
| @RestControllerAdvice      | GlobalExceptionHandler                             |
| @Service layering          | Engine → Orchestrator → Cycle services             |
| @Repository                | SiteRepository, AuditReportRepository (in-memory)  |
| Spring Profiles            | application-dev.yml / application-prod.yml          |
| Spring Events              | AuditCompletedEvent → FeedbackLoopListener         |
| @Async                     | FeedbackLoopListener.onAuditCompleted()            |
| @EnableAsync/@EnableScheduling | AsyncConfig                                    |
| Jakarta Validation         | @NotBlank, @Pattern sur les DTOs                   |
| Java Records               | AuditRequestDto, AuditResponseDto, RuleResultDto   |
| CORS                       | WebConfig (préparation Vue.js)                     |

## Installation

```bash
# Copier les fichiers dans le projet généré en M1
cp -r src/ <votre-projet-cyberaudit7e>/

# Build + run
cd <votre-projet-cyberaudit7e>
./mvnw spring-boot:run
```
=> pour corriger le dossier target : mvnw.cmd clean spring-boot:run
## Tests curl — Validation M2

```bash
# 1. Health check (enrichi avec le nombre de règles)
curl -s http://localhost:8080/api/health | jq .
# → {"status":"UP","service":"CyberAudit7E","phase":"7E-READY","rulesLoaded":7,...}

# 2. Créer un site
curl -s -X POST http://localhost:8080/api/sites \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.service-public.fr","name":"Service Public"}' | jq .

# 3. Créer un second site
curl -s -X POST http://localhost:8080/api/sites \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.example.com","name":"Example"}' | jq .

# 4. Lister les sites
curl -s http://localhost:8080/api/sites | jq .

# 5. Détail d'un site
curl -s http://localhost:8080/api/sites/1 | jq .

# 6. Lancer un audit complet (cycle 7E)
curl -s -X POST http://localhost:8080/api/audits \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.service-public.fr","name":"Service Public"}' | jq .
# → Observe les 7 phases dans les logs du serveur !

# 7. Lancer un 2e audit (pour tester la tendance ÉVOLUER)
curl -s -X POST http://localhost:8080/api/audits \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.service-public.fr","name":"Service Public"}' | jq .

# 8. Historique des audits du site
curl -s http://localhost:8080/api/audits/site/1 | jq .

# 9. Stats globales
curl -s http://localhost:8080/api/audits/stats | jq .

# 10. Test validation (doit retourner 400)
curl -s -X POST http://localhost:8080/api/audits \
  -H "Content-Type: application/json" \
  -d '{"url":"","name":""}' | jq .

# 11. Test conflit (doit retourner 409)
curl -s -X POST http://localhost:8080/api/sites \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.service-public.fr","name":"Doublon"}' | jq .
```

## Logs attendus lors d'un audit (cycle 7E)

```
══════════════════════════════════════════════════════
  AuditEngine initialisé — 7 règles chargées
  ├─ [RGAA] RGAA-8.5 (Chaque page web a un titre de page pertinent)
  ├─ [RGAA] RGAA-8.3 (Langue par défaut indiquée...)
  ├─ [RGAA] RGAA-1.1 (Chaque image porteuse d'information...)
  ├─ [WCAG] WCAG-1.4.3 (Contraste minimum de 4.5:1...)
  ├─ [WCAG] WCAG-2.1.1 (Toutes les fonctionnalités au clavier)
  ├─ [DSFR] DSFR-HDR-01 (En-tête conforme DSFR)
  └─ [DSFR] DSFR-FTR-01 (Pied de page conforme DSFR)
══════════════════════════════════════════════════════

╔══════════════════════════════════════════════════════╗
║  CYCLE 7E — Démarrage audit pour https://www.service-public.fr
╚══════════════════════════════════════════════════════╝
[7E-ÉVALUER]   Site Service Public prêt pour l'audit
[7E-EXÉCUTER]  Lancement du moteur sur https://www.service-public.fr
[7E-EXÉCUTER]  7 règle(s) exécutée(s) — N réussie(s)
[7E-ÉLABORER]  X violation(s) détectée(s) sur 7 règle(s)
[7E-EXAMINER]  Score global : 0.XX/1.00
[7E-ÉVOLUER]   Premier audit — pas de référence
[7E-ÉMETTRE]   Publication AuditCompletedEvent — rapport #1
╔══════════════════════════════════════════════════════╗
║  CYCLE 7E TERMINÉ — Rapport #1 — Score: 0.XX
╚══════════════════════════════════════════════════════╝

══════════════════════════════════════════════════════
[7E-ÉQUILIBRER] Analyse rétroactive du rapport #1    ← @Async (thread 7e-async-*)
  ├─ Score   : 0.XX/1.00
  └─ Tendance: FIRST
[7E-CYCLE COMPLET] Service Public → phase ÉQUILIBRER
══════════════════════════════════════════════════════
```

## Transition vers M3 (Persistance JPA)

Pour passer en M3, il faudra :
1. Supprimer `exclude = { DataSourceAutoConfiguration.class }` dans `CyberAudit7eApplication`
2. Ajouter les annotations JPA sur `Site` et `AuditReport` (`@Entity`, `@Table`, etc.)
3. Transformer les repositories en interfaces `extends JpaRepository<T, Long>`
4. Décommenter la config datasource dans `application-dev.yml`
5. Créer la migration Flyway `V1__create_schema.sql`
