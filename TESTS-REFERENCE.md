# CyberAudit7E — Référence rapide des tests curl

> Tous les exemples fonctionnent dans **PowerShell** (Windows 11 natif).
> Pour **Git Bash** ou **WSL**, remplacer `^` par `\` et utiliser les guillemets simples.

## M1 — Bootstrap

```powershell
# Health check
curl -s http://localhost:8080/api/health | jq .

# Index API
curl -s http://localhost:8080/api/ | jq .
```

## M2 — Architecture

```powershell
# Vérifier le nombre de règles chargées (13 attendu)
curl -s http://localhost:8080/api/health | jq .rulesLoaded
```

## M3 — Persistance JPA

```powershell
# Sites seed Flyway (4 sites)
curl -s http://localhost:8080/api/sites | jq .

# Détail d'un site
curl -s http://localhost:8080/api/sites/1 | jq .

# Recherche par nom
curl -s "http://localhost:8080/api/sites/search?name=gouv" | jq .

# Créer un site
curl -s -X POST http://localhost:8080/api/sites ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.numerique.gouv.fr\",\"name\":\"DINUM\"}" | jq .

# Doublon → 409
curl -s -X POST http://localhost:8080/api/sites ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Doublon\"}"

# Supprimer un site
curl -s -X DELETE http://localhost:8080/api/sites/5

# Not found → 404
curl -s http://localhost:8080/api/sites/999
```

## M4 — Moteur Jsoup (13 règles réelles)

```powershell
# Poids de scoring
curl -s http://localhost:8080/api/config/weights | jq .

# ── Audits réels (crawl HTTP) ──

# Service Public (site FR bien structuré)
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Service Public\"}" | jq .

# Site .gouv.fr (DSFR élevé attendu)
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouvernement FR\"}" | jq .

# Site non-gouvernemental (DSFR bas attendu)
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.example.com\",\"name\":\"Example.com\"}" | jq .

# Légifrance
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.legifrance.gouv.fr\",\"name\":\"Légifrance\"}" | jq .

# Site inaccessible (mode dégradé — toutes les règles FAIL)
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://site-inexistant.invalid\",\"name\":\"Test 404\"}" | jq .

# Validation → 400
curl -s -X POST http://localhost:8080/api/audits ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"\",\"name\":\"\"}" | jq .

# ── Poids dynamiques ──

# Modifier un poids
curl -s -X PUT http://localhost:8080/api/config/weights/RGAA ^
  -H "Content-Type: application/json" ^
  -d "{\"weight\":0.6}" | jq .

# Reset aux valeurs par défaut
curl -s -X POST http://localhost:8080/api/config/weights/reset | jq .
```

## M5 — Async, Events & SSE

```powershell
# ── SSE Streaming (ouvrir en premier dans un terminal dédié) ──
curl -N http://localhost:8080/api/audits/stream

# ── Audit asynchrone ──

# Soumettre (retour immédiat avec jobId)
curl -s -X POST http://localhost:8080/api/audits/async ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://www.legifrance.gouv.fr\",\"name\":\"Légifrance\"}" | jq .

# Consulter le statut d'un job
curl -s http://localhost:8080/api/audits/async/1 | jq .

# Lister tous les jobs
curl -s http://localhost:8080/api/audits/async | jq .

# Nettoyer les jobs terminés
curl -s -X DELETE http://localhost:8080/api/audits/async | jq .

# ── Batch (3 audits en parallèle) ──

curl -s -X POST http://localhost:8080/api/audits/batch ^
  -H "Content-Type: application/json" ^
  -d "{\"sites\":[{\"url\":\"https://www.service-public.fr\",\"name\":\"SP\"},{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouv\"},{\"url\":\"https://www.example.com\",\"name\":\"Ex\"}]}" | jq .

# ── Scheduler ──

# Statut du scheduler
curl -s http://localhost:8080/api/audits/schedule | jq .

# Déclenchement manuel (audite tous les sites)
curl -s -X POST http://localhost:8080/api/audits/schedule/trigger | jq .
```

## Stats & Consultation (transversal)

```powershell
# Stats globales (sites, audits, métriques, tendances)
curl -s http://localhost:8080/api/audits/stats | jq .

# Historique des audits d'un site
curl -s http://localhost:8080/api/audits/site/1 | jq .
curl -s http://localhost:8080/api/audits/site/2 | jq .

# Détail d'un rapport
curl -s http://localhost:8080/api/audits/1 | jq .

# Alertes (score sous le seuil)
curl -s "http://localhost:8080/api/audits/alerts?threshold=0.7" | jq .
curl -s "http://localhost:8080/api/audits/alerts?threshold=0.5" | jq .
```

## Console H2 (navigateur)

```
URL      : http://localhost:8080/h2-console
JDBC URL : jdbc:h2:mem:cyberaudit7e
User     : sa
Password : (vide)
```

### Requêtes SQL utiles

```sql
-- Sites avec phase
SELECT id, name, url, current_phase FROM sites;

-- Rapports avec scores
SELECT r.id, s.name, r.score_global, r.trend, r.audited_at
FROM audit_reports r JOIN sites s ON r.site_id = s.id
ORDER BY r.audited_at DESC;

-- Score moyen par site
SELECT s.name, COUNT(r.id) as audits, ROUND(AVG(r.score_global), 2) as avg_score
FROM sites s LEFT JOIN audit_reports r ON r.site_id = s.id
GROUP BY s.name;

-- Poids dynamiques (modifiés par la rétroaction)
SELECT * FROM rule_configs;

-- Migrations Flyway
SELECT version, description, success FROM flyway_schema_history;
```

## SSE depuis JavaScript (futur dashboard Vue.js)

```javascript
const sse = new EventSource('/api/audits/stream');

sse.addEventListener('connected', (e) => {
  console.log('🔗 SSE connecté');
});

sse.addEventListener('audit-started', (e) => {
  const data = JSON.parse(e.data);
  console.log(`🚀 Audit démarré : ${data.siteName}`);
});

sse.addEventListener('audit-progress', (e) => {
  const data = JSON.parse(e.data);
  console.log(`⏳ [${data.step}] ${data.phaseLabel} — ${data.progress}%`);
});

sse.addEventListener('audit-completed', (e) => {
  const data = JSON.parse(e.data);
  console.log(`✅ Score : ${data.scoreGlobal} — Tendance : ${data.trend}`);
});
```

## Récapitulatif des endpoints

| Méthode | Endpoint | Module | Description |
|---------|----------|--------|-------------|
| GET | `/api/` | M1 | Index API (découvrabilité) |
| GET | `/api/health` | M1 | Health check + stats moteur |
| GET | `/api/sites` | M3 | Lister les sites |
| POST | `/api/sites` | M3 | Créer un site |
| GET | `/api/sites/{id}` | M3 | Détail d'un site |
| DELETE | `/api/sites/{id}` | M3 | Supprimer un site |
| GET | `/api/sites/search?name=` | M3 | Recherche par nom |
| POST | `/api/audits` | M3 | Audit synchrone (cycle 7E) |
| GET | `/api/audits/{id}` | M3 | Détail d'un rapport |
| GET | `/api/audits/site/{siteId}` | M3 | Historique d'un site |
| GET | `/api/audits/stats` | M3 | Statistiques globales |
| GET | `/api/audits/alerts` | M3 | Alertes (score < seuil) |
| GET | `/api/config/weights` | M4 | Voir les poids de scoring |
| PUT | `/api/config/weights/{cat}` | M4 | Modifier un poids |
| POST | `/api/config/weights/reset` | M4 | Reset poids par défaut |
| POST | `/api/audits/async` | M5 | Audit asynchrone |
| GET | `/api/audits/async/{jobId}` | M5 | Statut d'un job |
| GET | `/api/audits/async` | M5 | Lister les jobs |
| DELETE | `/api/audits/async` | M5 | Nettoyer les jobs |
| POST | `/api/audits/batch` | M5 | Batch d'audits parallèles |
| GET | `/api/audits/stream` | M5 | Flux SSE temps réel |
| GET | `/api/audits/schedule` | M5 | Info scheduler |
| POST | `/api/audits/schedule/trigger` | M5 | Déclenchement manuel |
