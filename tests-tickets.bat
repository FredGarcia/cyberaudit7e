@echo off
REM ============================================================
REM  CyberAudit7E - Tests seuil de ticket auto
REM  A executer apres les modifications du seuil
REM ============================================================

set BASE=http://localhost:8080
set JSON=-H "Content-Type: application/json"

echo.
echo ============================================================
echo   Tests du seuil de creation auto de tickets
echo ============================================================
echo.

echo [1] GET /api/config/settings - Tous les parametres systeme
curl -s %BASE%/api/config/settings | jq .
echo.

echo [2] GET /api/config/settings/ticket-threshold - Seuil actuel
curl -s %BASE%/api/config/settings/ticket-threshold | jq .
echo.

echo [3] PUT - Changer le seuil a 70%% (0.7)
curl -s -X PUT %BASE%/api/config/settings/ticket-threshold %JSON% -d "{\"threshold\":0.7}" | jq .
echo.

echo [4] GET - Verifier le changement
curl -s %BASE%/api/config/settings/ticket-threshold | jq .
echo.

echo [5] PUT - Changer le seuil a 30%% (0.3)
curl -s -X PUT %BASE%/api/config/settings/ticket-threshold %JSON% -d "{\"threshold\":0.3}" | jq .
echo.

echo [6] GET - Verifier
curl -s %BASE%/api/config/settings/ticket-threshold | jq .
echo.

echo [7] PUT - Remettre a 50%% (defaut)
curl -s -X PUT %BASE%/api/config/settings/ticket-threshold %JSON% -d "{\"threshold\":0.5}" | jq .
echo.

echo [8] PUT - Test validation (valeur invalide, 400 attendu)
curl -s -X PUT %BASE%/api/config/settings/ticket-threshold %JSON% -d "{\"threshold\":1.5}" | jq .
echo.

echo [9] Lancer un audit pour tester la creation auto
echo     (le seuil actuel determinera si un ticket est cree)
curl -s -X POST %BASE%/api/audits %JSON% -d "{\"url\":\"https://www.example.com\",\"name\":\"Test seuil\"}" | jq "{scores:.scores,reportId}"
echo.

echo [10] Verifier les tickets crees
curl -s %BASE%/api/tickets/stats | jq .
echo.

echo ============================================================
echo   Tests termines
echo ============================================================
pause
