@echo off
setlocal EnableDelayedExpansion

REM ============================================================
REM  CyberAudit7E - Script de tests complet (Windows CMD)
REM  Usage : tests-cyberaudit7e.bat
REM  Prerequis : curl, jq (winget install jqlang.jq)
REM  L'application doit tourner sur http://localhost:8080
REM ============================================================

set BASE=http://localhost:8080
set JSON=-H "Content-Type: application/json"

echo.
echo ============================================================
echo   CyberAudit7E - Tests complets
echo   %date% %time:~0,8%
echo ============================================================
echo.

REM ============================================================
REM  1. SANTE ET SYSTEME
REM ============================================================

echo ------------------------------------------------------------
echo   1. SANTE ET SYSTEME
echo ------------------------------------------------------------
echo.

echo [1.1] GET /api/health - Health check complet
curl -s %BASE%/api/health | jq .
echo.

echo [1.2] GET /api/ - Index API (liste des endpoints)
curl -s %BASE%/api/ | jq .
echo.

echo [1.3] GET /v3/api-docs - OpenAPI (5 premiers paths)
curl -s %BASE%/v3/api-docs | jq ".paths | keys[:5]"
echo.
pause

REM ============================================================
REM  2. GESTION DES SITES
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   2. GESTION DES SITES
echo ------------------------------------------------------------
echo.

echo [2.1] GET /api/sites - Lister les sites (seed Flyway)
curl -s "%BASE%/api/sites?page=0&size=10" | jq .
echo.

echo [2.2] POST /api/sites - Creer un nouveau site
curl -s -X POST %BASE%/api/sites %JSON% -d "{\"url\":\"https://www.numerique.gouv.fr\",\"name\":\"DINUM\"}" | jq .
echo.

echo [2.3] GET /api/sites/1 - Detail d'un site
curl -s %BASE%/api/sites/1 | jq .
echo.

echo [2.4] GET /api/sites/search?name=gouv - Recherche par nom
curl -s "%BASE%/api/sites/search?name=gouv" | jq .
echo.

echo [2.5] POST /api/sites - Test conflit (409 attendu)
curl -s -X POST %BASE%/api/sites %JSON% -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Doublon\"}" | jq .
echo.

echo [2.6] POST /api/sites - Test validation (400 attendu)
curl -s -X POST %BASE%/api/sites %JSON% -d "{\"url\":\"\",\"name\":\"\"}" | jq .
echo.
pause

REM ============================================================
REM  3. AUDIT SYNCHRONE
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   3. AUDIT SYNCHRONE (cycle 7E complet)
echo ------------------------------------------------------------
echo.

echo [3.1] POST /api/audits - Audit service-public.fr
echo       (Crawl HTTP reel - peut prendre quelques secondes...)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Service Public\"}" | jq .
echo.

echo [3.2] POST /api/audits - Audit site .gouv.fr (DSFR eleve)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouvernement FR\"}" | jq .
echo.

echo [3.3] POST /api/audits - Audit site non-gouv (DSFR bas)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"https://www.example.com\",\"name\":\"Example.com\"}" | jq .
echo.

echo [3.4] POST /api/audits - 2eme audit meme site (test tendance)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"https://www.service-public.fr\",\"name\":\"Service Public\"}" | jq .
echo.

echo [3.5] POST /api/audits - Test validation (400 attendu)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"\",\"name\":\"\"}" | jq .
echo.

echo [3.6] POST /api/audits - Test site inaccessible (mode degrade)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"https://site-inexistant-xyz.invalid\",\"name\":\"Test 404\"}" | jq .
echo.
pause

REM ============================================================
REM  4. RAPPORTS ET CONSULTATION
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   4. RAPPORTS ET CONSULTATION
echo ------------------------------------------------------------
echo.

echo [4.1] GET /api/audits/list - Rapports pagines (tri date desc)
curl -s "%BASE%/api/audits/list?page=0&size=5&sortBy=auditedAt&direction=desc" | jq .
echo.

echo [4.2] GET /api/audits/list - Rapports tries par score asc
curl -s "%BASE%/api/audits/list?page=0&size=5&sortBy=scoreGlobal&direction=asc" | jq .
echo.

echo [4.3] GET /api/audits/1 - Detail d'un rapport
curl -s %BASE%/api/audits/1 | jq .
echo.

echo [4.4] GET /api/audits/site/1 - Historique du site 1
curl -s "%BASE%/api/audits/site/1?page=0&size=5" | jq .
echo.

echo [4.5] GET /api/audits/search?q=gouv - Recherche full-text
curl -s "%BASE%/api/audits/search?q=gouv&page=0&size=5" | jq .
echo.

echo [4.6] GET /api/audits/alerts - Alertes (score inf 0.7)
curl -s "%BASE%/api/audits/alerts?threshold=0.7&page=0&size=10" | jq .
echo.

echo [4.7] GET /api/audits/stats - Statistiques globales
curl -s %BASE%/api/audits/stats | jq .
echo.

echo [4.8] GET /api/audits/999 - Test 404
curl -s %BASE%/api/audits/999 | jq .
echo.
pause

REM ============================================================
REM  5. AUDIT ASYNCHRONE
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   5. AUDIT ASYNCHRONE
echo ------------------------------------------------------------
echo.

echo [5.1] POST /api/audits/async - Soumettre un audit async
curl -s -X POST %BASE%/api/audits/async %JSON% -d "{\"url\":\"https://www.legifrance.gouv.fr\",\"name\":\"Legifrance\"}" | jq .
echo.

echo      Attente 5 secondes pour laisser le job tourner...
timeout /t 5 /nobreak >nul

echo [5.2] GET /api/audits/async/1 - Statut du job 1
curl -s %BASE%/api/audits/async/1 | jq .
echo.

echo [5.3] GET /api/audits/async - Liste de tous les jobs
curl -s %BASE%/api/audits/async | jq .
echo.
pause

REM ============================================================
REM  6. BATCH PARALLELE
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   6. BATCH PARALLELE
echo ------------------------------------------------------------
echo.

echo [6.1] POST /api/audits/batch - 3 sites en parallele
curl -s -X POST %BASE%/api/audits/batch %JSON% -d "{\"sites\":[{\"url\":\"https://www.service-public.fr\",\"name\":\"SP\"},{\"url\":\"https://www.gouvernement.gouv.fr\",\"name\":\"Gouv\"},{\"url\":\"https://www.example.com\",\"name\":\"Example\"}]}" | jq .
echo.

echo      Attente 10 secondes pour le batch...
timeout /t 10 /nobreak >nul

echo [6.2] GET /api/audits/async - Verifier les jobs du batch
curl -s %BASE%/api/audits/async | jq .
echo.

echo [6.3] DELETE /api/audits/async - Nettoyer les jobs termines
curl -s -X DELETE %BASE%/api/audits/async | jq .
echo.
pause

REM ============================================================
REM  7. CONFIGURATION ET POIDS DE SCORING
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   7. CONFIGURATION ET POIDS DE SCORING
echo ------------------------------------------------------------
echo.

echo [7.1] GET /api/config/weights - Poids actuels
curl -s %BASE%/api/config/weights | jq .
echo.

echo [7.2] PUT /api/config/weights/RGAA - Modifier le poids RGAA
curl -s -X PUT %BASE%/api/config/weights/RGAA %JSON% -d "{\"weight\":0.6}" | jq .
echo.

echo [7.3] GET /api/config/weights - Verifier la modification
curl -s %BASE%/api/config/weights | jq .
echo.

echo [7.4] POST /api/config/weights/reset - Remettre par defaut
curl -s -X POST %BASE%/api/config/weights/reset | jq .
echo.

echo [7.5] GET /api/config/weights - Verifier le reset
curl -s %BASE%/api/config/weights | jq .
echo.
pause

REM ============================================================
REM  8. SCHEDULER
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   8. SCHEDULER
echo ------------------------------------------------------------
echo.

echo [8.1] GET /api/audits/schedule - Info scheduler
curl -s %BASE%/api/audits/schedule | jq .
echo.

echo [8.2] POST /api/audits/schedule/trigger - Declenchement manuel
curl -s -X POST %BASE%/api/audits/schedule/trigger | jq .
echo.
pause

REM ============================================================
REM  9. TICKETS DE SECURITE (Integrations)
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   9. TICKETS DE SECURITE (ServiceNow / SailPoint)
echo ------------------------------------------------------------
echo.

echo [9.1] POST /api/tickets - Creer un ticket manuellement
curl -s -X POST %BASE%/api/tickets %JSON% -d "{\"title\":\"Violation SoD Finance\",\"description\":\"Utilisateur avec acces Finance + Achats incompatibles\",\"severity\":\"HIGH\",\"category\":\"sod_violation\"}" | jq .
echo.

echo [9.2] POST /api/tickets - Creer un ticket CRITICAL
curl -s -X POST %BASE%/api/tickets %JSON% -d "{\"title\":\"Compte admin orphelin detecte\",\"description\":\"Compte AD admin sans proprietaire identifie\",\"severity\":\"CRITICAL\",\"category\":\"orphan_account\",\"siteUrl\":\"https://ad.corp.local\"}" | jq .
echo.

echo [9.3] POST /api/tickets - Creer un ticket LOW
curl -s -X POST %BASE%/api/tickets %JSON% -d "{\"title\":\"Certification trimestrielle completee\",\"description\":\"Campagne Q1 2026 signee par le manager\",\"severity\":\"LOW\",\"category\":\"certification_issue\"}" | jq .
echo.

echo [9.4] Webhook SailPoint - Policy Violation
curl -s -X POST %BASE%/api/webhooks/sailpoint %JSON% -d "{\"_metadata\":{\"triggerId\":\"idn:policy-violation\",\"invocationId\":\"inv-001\"},\"identity\":{\"id\":\"uid-789\",\"name\":\"Marie Martin\",\"type\":\"IDENTITY\"},\"policyName\":\"SoD Comptabilite-Tresorerie\",\"violatingAccessItems\":[{\"name\":\"Comptabilite Admin\"},{\"name\":\"Tresorerie Approbateur\"}]}" | jq .
echo.

echo [9.5] Webhook SailPoint - Identity Deleted
curl -s -X POST %BASE%/api/webhooks/sailpoint %JSON% -d "{\"_metadata\":{\"triggerId\":\"idn:identity-deleted\",\"invocationId\":\"inv-002\"},\"identity\":{\"id\":\"uid-999\",\"name\":\"Pierre Durand\",\"type\":\"IDENTITY\"}}" | jq .
echo.

echo [9.6] Webhook SailPoint - Access Request
curl -s -X POST %BASE%/api/webhooks/sailpoint %JSON% -d "{\"_metadata\":{\"triggerId\":\"idn:access-request-pre-approval\",\"invocationId\":\"inv-003\"},\"requestedFor\":{\"id\":\"uid-111\",\"name\":\"Sophie Bernard\"},\"accessRequestId\":\"ar-555\",\"requestedItems\":[{\"name\":\"VPN Full Access\",\"type\":\"ACCESS_PROFILE\"},{\"name\":\"Serveur Production\",\"type\":\"ROLE\"}]}" | jq .
echo.

echo [9.7] Webhook SailPoint - Certification
curl -s -X POST %BASE%/api/webhooks/sailpoint %JSON% -d "{\"_metadata\":{\"triggerId\":\"idn:certification-signed-off\",\"invocationId\":\"inv-004\"},\"certification\":{\"name\":\"Revue Q1 2026 - Equipe Finance\"}}" | jq .
echo.

echo [9.8] Webhook SailPoint - Test dedoublonnage (meme uid-789)
curl -s -X POST %BASE%/api/webhooks/sailpoint %JSON% -d "{\"_metadata\":{\"triggerId\":\"idn:policy-violation\",\"invocationId\":\"inv-005\"},\"identity\":{\"id\":\"uid-789\",\"name\":\"Marie Martin\",\"type\":\"IDENTITY\"},\"policyName\":\"SoD Comptabilite-Tresorerie UPDATED\",\"violatingAccessItems\":[{\"name\":\"Comptabilite Admin\"},{\"name\":\"Tresorerie Approbateur\"},{\"name\":\"Audit Viewer\"}]}" | jq .
echo.
pause

REM ============================================================
REM  10. CONSULTATION ET FILTRAGE DES TICKETS
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   10. CONSULTATION ET FILTRAGE DES TICKETS
echo ------------------------------------------------------------
echo.

echo [10.1] GET /api/tickets - Tous les tickets (pagine)
curl -s "%BASE%/api/tickets?page=0&size=10" | jq .
echo.

echo [10.2] GET /api/tickets/open - Tickets ouverts
curl -s "%BASE%/api/tickets/open?page=0&size=10" | jq .
echo.

echo [10.3] Filtrer par source SAILPOINT
curl -s "%BASE%/api/tickets?source=SAILPOINT&page=0&size=10" | jq .
echo.

echo [10.4] Filtrer par source MANUAL
curl -s "%BASE%/api/tickets?source=MANUAL&page=0&size=10" | jq .
echo.

echo [10.5] Filtrer par severite CRITICAL
curl -s "%BASE%/api/tickets?severity=CRITICAL&page=0&size=10" | jq .
echo.

echo [10.6] Filtrer par severite HIGH
curl -s "%BASE%/api/tickets?severity=HIGH&page=0&size=10" | jq .
echo.

echo [10.7] Recherche "Martin"
curl -s "%BASE%/api/tickets/search?q=Martin&page=0&size=10" | jq .
echo.

echo [10.8] Recherche "SoD"
curl -s "%BASE%/api/tickets/search?q=SoD&page=0&size=10" | jq .
echo.

echo [10.9] Detail du ticket 1
curl -s %BASE%/api/tickets/1 | jq .
echo.
pause

REM ============================================================
REM  11. ACTIONS SUR LES TICKETS
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   11. ACTIONS SUR LES TICKETS
echo ------------------------------------------------------------
echo.

echo [11.1] Resoudre le ticket 3
curl -s -X POST %BASE%/api/tickets/3/resolve %JSON% -d "{\"closeNotes\":\"Certification validee - aucune action requise\"}" | jq .
echo.

echo [11.2] Resoudre le ticket 1
curl -s -X POST %BASE%/api/tickets/1/resolve %JSON% -d "{\"closeNotes\":\"Acces incompatibles revoques dans SailPoint\"}" | jq .
echo.

echo [11.3] Statistiques apres resolutions
curl -s %BASE%/api/tickets/stats | jq .
echo.

echo [11.4] Tickets encore ouverts
curl -s "%BASE%/api/tickets/open?page=0&size=10" | jq .
echo.

echo [11.5] Sync manuelle vers ServiceNow
curl -s -X POST %BASE%/api/tickets/sync | jq .
echo.
pause

REM ============================================================
REM  12. WEBHOOKS SERVICENOW (callback)
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   12. WEBHOOKS SERVICENOW (callbacks)
echo ------------------------------------------------------------
echo.

echo [12.1] Test connectivite ServiceNow
curl -s %BASE%/api/webhooks/servicenow/test | jq .
echo.

echo [12.2] Test connectivite SailPoint
curl -s %BASE%/api/webhooks/sailpoint/test | jq .
echo.

echo [12.3] Callback SNOW simule (ticket inconnu)
curl -s -X POST %BASE%/api/webhooks/servicenow %JSON% -d "{\"sys_id\":\"unknown-123\",\"number\":\"INC9999999\",\"state\":3,\"assigned_to\":\"Equipe Securite\"}" | jq .
echo.

echo [12.4] Statut des integrations
curl -s %BASE%/api/tickets/integrations | jq .
echo.
pause

REM ============================================================
REM  13. STATISTIQUES FINALES
REM ============================================================

echo.
echo ------------------------------------------------------------
echo   13. STATISTIQUES FINALES
echo ------------------------------------------------------------
echo.

echo [13.1] Health final
curl -s %BASE%/api/health | jq "{status,version,profile,rulesLoaded,sseClients}"
echo.

echo [13.2] Stats audits
curl -s %BASE%/api/audits/stats | jq "{totalSites,totalAudits,averageScore,trends}"
echo.

echo [13.3] Stats tickets
curl -s %BASE%/api/tickets/stats | jq .
echo.

echo [13.4] Poids finaux
curl -s %BASE%/api/config/weights | jq "{RGAA:.RGAA.weight,WCAG:.WCAG.weight,DSFR:.DSFR.weight,totalWeight,normalized}"
echo.

echo [13.5] Tous les sites avec scores
curl -s "%BASE%/api/sites?page=0&size=20" | jq ".content[] | {name,currentPhase,auditsCount,lastScore}"
echo.

echo.
echo ============================================================
echo   TESTS TERMINES
echo.
echo   Dashboards :
echo     http://localhost:8080/
echo     http://localhost:8080/admin.html
echo     http://localhost:8080/exploitation.html
echo.
echo   Documentation :
echo     http://localhost:8080/swagger-ui.html
echo.
echo   Console H2 :
echo     http://localhost:8080/h2-console
echo     JDBC URL: jdbc:h2:mem:cyberaudit7e
echo     User: sa / Password: (vide)
echo ============================================================
echo.

endlocal
pause