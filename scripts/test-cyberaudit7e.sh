#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# CyberAudit7E — Script de tests complet (Git Bash / WSL / Linux)
# Modules M1 → M5
# ═══════════════════════════════════════════════════════════════
# Usage :
#   ./test-cyberaudit7e.sh           # Tout exécuter
#   ./test-cyberaudit7e.sh 3         # Module spécifique
#   ./test-cyberaudit7e.sh sse       # Ouvrir le flux SSE
# ═══════════════════════════════════════════════════════════════

BASE="http://localhost:8080"
MODULE="${1:-all}"
PASS=0
FAIL=0
TOTAL=0

# ── Couleurs ──
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

header() { echo -e "\n${CYAN}═══════════════════════════════════════════════════════${NC}"; echo -e "${CYAN}  $1${NC}"; echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"; }
test_name() { TOTAL=$((TOTAL+1)); echo -ne "\n  ${YELLOW}[$TOTAL] $1${NC}"; }
pass() { PASS=$((PASS+1)); echo -e " → ${GREEN}PASS${NC}"; [ -n "$1" ] && echo -e "       ${GRAY}$1${NC}"; }
fail() { FAIL=$((FAIL+1)); echo -e " → ${RED}FAIL${NC}"; [ -n "$1" ] && echo -e "       ${RED}$1${NC}"; }

# Helper : curl JSON + extract field
api() {
    curl -s -w "\n%{http_code}" "$@"
}
api_post() {
    curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" "$@"
}
api_put() {
    curl -s -w "\n%{http_code}" -X PUT -H "Content-Type: application/json" "$@"
}
api_delete() {
    curl -s -w "\n%{http_code}" -X DELETE "$@"
}

# Parse response : body on all lines except last, status on last line
parse_response() {
    local response="$1"
    BODY=$(echo "$response" | sed '$d')
    HTTP_CODE=$(echo "$response" | tail -1)
}

# Extract JSON field (simple grep, no jq dependency)
json_field() {
    echo "$BODY" | grep -o "\"$1\"[[:space:]]*:[[:space:]]*[^,}]*" | head -1 | sed 's/.*:[[:space:]]*//' | tr -d '"' | tr -d ' '
}

# ── SSE Mode ──
if [ "$MODULE" = "sse" ]; then
    header "SSE STREAMING — Ctrl+C pour quitter"
    echo -e "  ${GRAY}Lancez un audit dans un autre terminal pour voir les events.${NC}\n"
    curl -N "$BASE/api/audits/stream"
    exit
fi

# ═══════════════════════════════════════════
#  MODULE M1 — BOOTSTRAP
# ═══════════════════════════════════════════

if [ "$MODULE" = "all" ] || [ "$MODULE" = "1" ]; then
    header "MODULE M1 — Bootstrap Spring Boot"

    test_name "GET /api/health → status UP"
    parse_response "$(api "$BASE/api/health")"
    STATUS=$(json_field "status")
    if [ "$STATUS" = "UP" ]; then
        SERVICE=$(json_field "service")
        pass "service=$SERVICE"
    else fail "Health check échoué (HTTP $HTTP_CODE)"; fi

    test_name "GET /api/ → index endpoint"
    parse_response "$(api "$BASE/api/")"
    if [ "$HTTP_CODE" = "200" ]; then
        VER=$(json_field "version")
        pass "version=$VER"
    else fail "Index non disponible"; fi
fi

# ═══════════════════════════════════════════
#  MODULE M2 — ARCHITECTURE
# ═══════════════════════════════════════════

if [ "$MODULE" = "all" ] || [ "$MODULE" = "2" ]; then
    header "MODULE M2 — Architecture Spring"

    test_name "GET /api/health → rulesLoaded >= 7"
    parse_response "$(api "$BASE/api/health")"
    RULES=$(json_field "rulesLoaded")
    if [ "$RULES" -ge 7 ] 2>/dev/null; then
        pass "rulesLoaded=$RULES"
    else fail "Règles non chargées"; fi
fi

# ═══════════════════════════════════════════
#  MODULE M3 — PERSISTANCE JPA
# ═══════════════════════════════════════════

if [ "$MODULE" = "all" ] || [ "$MODULE" = "3" ]; then
    header "MODULE M3 — Persistance JPA + H2 + Flyway"

    test_name "GET /api/sites → sites seed Flyway (>= 4)"
    parse_response "$(api "$BASE/api/sites")"
    COUNT=$(echo "$BODY" | grep -o '"id"' | wc -l)
    if [ "$COUNT" -ge 4 ]; then
        pass "$COUNT site(s) — Flyway V2 seed OK"
    else fail "Sites seed non trouvés"; fi

    test_name "GET /api/sites/1 → SiteDto"
    parse_response "$(api "$BASE/api/sites/1")"
    if [ "$HTTP_CODE" = "200" ]; then
        URL=$(json_field "url")
        pass "url=$URL"
    else fail "Site #1 introuvable"; fi

    test_name "GET /api/sites/search?name=gouv → recherche"
    parse_response "$(api "$BASE/api/sites/search?name=gouv")"
    FOUND=$(echo "$BODY" | grep -o '"id"' | wc -l)
    if [ "$FOUND" -ge 1 ]; then
        pass "$FOUND résultat(s)"
    else fail "Recherche échouée"; fi

    test_name "POST /api/sites → créer un site"
    RAND=$RANDOM
    parse_response "$(api_post "$BASE/api/sites" -d "{\"url\":\"https://test-$RAND.example.com\",\"name\":\"Test M3\"}")"
    NEW_ID=$(json_field "id")
    if [ "$HTTP_CODE" = "201" ] && [ -n "$NEW_ID" ]; then
        pass "id=$NEW_ID"
        api_delete "$BASE/api/sites/$NEW_ID" > /dev/null 2>&1
    else fail "Création échouée (HTTP $HTTP_CODE)"; fi

    test_name "POST /api/sites → doublon = 409"
    parse_response "$(api_post "$BASE/api/sites" -d '{"url":"https://www.service-public.fr","name":"Doublon"}')"
    if [ "$HTTP_CODE" = "409" ]; then
        pass "409 Conflict correct"
    else fail "Attendu 409, reçu $HTTP_CODE"; fi

    test_name "GET /api/sites/999 → 404"
    parse_response "$(api "$BASE/api/sites/999")"
    if [ "$HTTP_CODE" = "404" ]; then
        pass "404 correct"
    else fail "Attendu 404, reçu $HTTP_CODE"; fi
fi

# ═══════════════════════════════════════════
#  MODULE M4 — MOTEUR JSOUP
# ═══════════════════════════════════════════

if [ "$MODULE" = "all" ] || [ "$MODULE" = "4" ]; then
    header "MODULE M4 — Moteur d'audit Jsoup (13 règles)"

    test_name "GET /api/config/weights → poids normalisés"
    parse_response "$(api "$BASE/api/config/weights")"
    NORM=$(json_field "normalized")
    if [ "$NORM" = "true" ]; then
        pass "normalized=true"
    else fail "Poids non normalisés"; fi

    test_name "POST /api/audits → audit réel service-public.fr"
    parse_response "$(api_post "$BASE/api/audits" -d '{"url":"https://www.service-public.fr","name":"Service Public"}')"
    RULES_COUNT=$(json_field "rulesCount")
    SCORE=$(json_field "global")
    if [ "$HTTP_CODE" = "201" ] && [ "$RULES_COUNT" = "13" ]; then
        PASSED_C=$(json_field "passedCount")
        pass "score=$SCORE, passed=$PASSED_C/13"
    else fail "Audit échoué (HTTP $HTTP_CODE, rules=$RULES_COUNT)"; fi

    test_name "POST /api/audits → audit .gouv.fr (DSFR élevé)"
    parse_response "$(api_post "$BASE/api/audits" -d '{"url":"https://www.gouvernement.gouv.fr","name":"Gouvernement FR"}')"
    if [ "$HTTP_CODE" = "201" ]; then
        DSFR=$(json_field "dsfr")
        GLOBAL=$(json_field "global")
        pass "DSFR=$DSFR, Global=$GLOBAL"
    else fail "Audit .gouv.fr échoué"; fi

    test_name "POST /api/audits → audit example.com (DSFR bas)"
    parse_response "$(api_post "$BASE/api/audits" -d '{"url":"https://www.example.com","name":"Example.com"}')"
    if [ "$HTTP_CODE" = "201" ]; then
        DSFR=$(json_field "dsfr")
        pass "DSFR=$DSFR (bas attendu)"
    else fail "Audit example.com échoué"; fi

    test_name "POST /api/audits → validation body vide = 400"
    parse_response "$(api_post "$BASE/api/audits" -d '{"url":"","name":""}')"
    if [ "$HTTP_CODE" = "400" ]; then
        pass "400 Validation OK"
    else fail "Attendu 400, reçu $HTTP_CODE"; fi

    test_name "PUT /api/config/weights/RGAA → modifier poids"
    parse_response "$(api_put "$BASE/api/config/weights/RGAA" -d '{"weight":0.55}')"
    if [ "$HTTP_CODE" = "200" ]; then
        pass "RGAA → 0.55"
    else fail "Modification échouée"; fi

    test_name "POST /api/config/weights/reset → défauts"
    parse_response "$(api_post "$BASE/api/config/weights/reset")"
    if [ "$HTTP_CODE" = "200" ]; then
        pass "Poids réinitialisés"
    else fail "Reset échoué"; fi
fi

# ═══════════════════════════════════════════
#  MODULE M5 — ASYNC & EVENTS
# ═══════════════════════════════════════════

if [ "$MODULE" = "all" ] || [ "$MODULE" = "5" ]; then
    header "MODULE M5 — Async, Events & SSE"

    test_name "GET /api/health → métriques M5"
    parse_response "$(api "$BASE/api/health")"
    if echo "$BODY" | grep -q "metrics"; then
        pass "Métriques présentes"
    else fail "Métriques absentes"; fi

    test_name "POST /api/audits/async → audit asynchrone"
    parse_response "$(api_post "$BASE/api/audits/async" -d '{"url":"https://www.legifrance.gouv.fr","name":"Légifrance"}')"
    JOB_ID=$(json_field "jobId")
    if [ "$HTTP_CODE" = "202" ] && [ -n "$JOB_ID" ]; then
        pass "jobId=$JOB_ID, status=PENDING"

        echo -ne "       Attente du job #$JOB_ID"
        for i in $(seq 1 15); do
            sleep 2
            echo -n "."
            parse_response "$(api "$BASE/api/audits/async/$JOB_ID")"
            JOB_STATUS=$(json_field "status")
            if [ "$JOB_STATUS" = "COMPLETED" ] || [ "$JOB_STATUS" = "FAILED" ]; then
                break
            fi
        done
        echo ""

        test_name "GET /api/audits/async/$JOB_ID → résultat"
        if [ "$JOB_STATUS" = "COMPLETED" ]; then
            pass "status=COMPLETED"
        else fail "status=$JOB_STATUS"; fi
    else fail "Soumission async échouée (HTTP $HTTP_CODE)"; fi

    test_name "POST /api/audits/batch → 3 audits parallèles"
    parse_response "$(api_post "$BASE/api/audits/batch" -d '{"sites":[{"url":"https://www.service-public.fr","name":"SP"},{"url":"https://www.gouvernement.gouv.fr","name":"Gouv"},{"url":"https://www.example.com","name":"Ex"}]}')"
    BATCH_SIZE=$(json_field "batchSize")
    if [ "$HTTP_CODE" = "202" ] && [ "$BATCH_SIZE" = "3" ]; then
        pass "batchSize=3 soumis"
        echo -ne "       Attente du batch"
        for i in $(seq 1 20); do
            sleep 3
            echo -n "."
            parse_response "$(api "$BASE/api/audits/async")"
            RUNNING=$(echo "$BODY" | grep -o '"RUNNING"' | wc -l)
            if [ "$RUNNING" -eq 0 ]; then break; fi
        done
        echo ""
        COMPLETED=$(echo "$BODY" | grep -o '"COMPLETED"' | wc -l)
        test_name "Batch terminé → jobs COMPLETED"
        if [ "$COMPLETED" -ge 3 ]; then
            pass "$COMPLETED job(s) COMPLETED"
        else fail "$COMPLETED/$BATCH_SIZE terminé(s)"; fi
    else fail "Batch échoué (HTTP $HTTP_CODE)"; fi

    test_name "DELETE /api/audits/async → nettoyer"
    parse_response "$(api_delete "$BASE/api/audits/async")"
    if [ "$HTTP_CODE" = "200" ]; then
        CLEARED=$(json_field "cleared")
        pass "$CLEARED job(s) nettoyé(s)"
    else fail "Nettoyage échoué"; fi

    test_name "GET /api/audits/schedule → info scheduler"
    parse_response "$(api "$BASE/api/audits/schedule")"
    if [ "$HTTP_CODE" = "200" ]; then
        ENABLED=$(json_field "enabled")
        pass "enabled=$ENABLED"
    else fail "Scheduler échoué"; fi

    test_name "POST /api/audits/schedule/trigger → déclenchement"
    parse_response "$(api_post "$BASE/api/audits/schedule/trigger")"
    TRIGGERED=$(json_field "triggered")
    if [ "$HTTP_CODE" = "200" ] && [ "$TRIGGERED" -ge 1 ] 2>/dev/null; then
        pass "$TRIGGERED audit(s) déclenchés"
    else fail "Trigger échoué"; fi

    sleep 3
fi

# ═══════════════════════════════════════════
#  STATS & CONSULTATION
# ═══════════════════════════════════════════

if [ "$MODULE" = "all" ]; then
    header "STATS & CONSULTATION (transversal)"

    test_name "GET /api/audits/stats → stats globales"
    parse_response "$(api "$BASE/api/audits/stats")"
    if [ "$HTTP_CODE" = "200" ]; then
        TOTAL_S=$(json_field "totalSites")
        TOTAL_A=$(json_field "totalAudits")
        pass "sites=$TOTAL_S, audits=$TOTAL_A"
    else fail "Stats échouées"; fi

    test_name "GET /api/audits/site/1 → historique"
    parse_response "$(api "$BASE/api/audits/site/1")"
    REPORTS=$(echo "$BODY" | grep -o '"id"' | wc -l)
    if [ "$HTTP_CODE" = "200" ] && [ "$REPORTS" -ge 1 ]; then
        pass "$REPORTS rapport(s)"
    else fail "Historique vide"; fi

    test_name "GET /api/audits/1 → détail rapport"
    parse_response "$(api "$BASE/api/audits/1")"
    if [ "$HTTP_CODE" = "200" ]; then
        SCORE=$(json_field "scoreGlobal")
        pass "score=$SCORE"
    else fail "Rapport introuvable"; fi

    test_name "GET /api/audits/alerts?threshold=0.7 → alertes"
    parse_response "$(api "$BASE/api/audits/alerts?threshold=0.7")"
    if [ "$HTTP_CODE" = "200" ]; then
        ALERTS=$(echo "$BODY" | grep -o '"id"' | wc -l)
        pass "$ALERTS alerte(s)"
    else fail "Alertes échouées"; fi
fi

# ═══════════════════════════════════════════
#  BILAN
# ═══════════════════════════════════════════

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
if [ "$FAIL" -eq 0 ]; then
    echo -e "${CYAN}  BILAN : $TOTAL tests — ${GREEN}$PASS PASS${CYAN} / ${GREEN}$FAIL FAIL${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
    echo -e "\n  ${GREEN}✓ Tous les tests passent !${NC}\n"
else
    echo -e "${CYAN}  BILAN : $TOTAL tests — ${GREEN}$PASS PASS${CYAN} / ${RED}$FAIL FAIL${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
    echo -e "\n  ${RED}✗ $FAIL test(s) échoué(s) — vérifier les logs serveur${NC}\n"
fi
