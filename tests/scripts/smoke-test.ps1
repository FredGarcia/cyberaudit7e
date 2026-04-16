#
# smoke-test.ps1 — Test end-to-end de l'API CyberAudit7E
# Usage : .\smoke-test.ps1
# Prérequis : l'application doit tourner sur http://localhost:8080
#

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080/api"
$Passed = 0
$Failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Body = $null,
        [int]$ExpectedStatus = 200
    )

    Write-Host ""
    Write-Host "────────────────────────────────────────" -ForegroundColor Cyan
    Write-Host "  $Name" -ForegroundColor White
    Write-Host "  $Method $Path" -ForegroundColor Gray

    try {
        $params = @{
            Uri = "$BaseUrl$Path"
            Method = $Method
            ContentType = "application/json"
            UseBasicParsing = $true
        }
        if ($Body) { $params.Body = $Body }

        $response = Invoke-WebRequest @params
        $status = $response.StatusCode

        if ($status -eq $ExpectedStatus) {
            Write-Host "  ✓ $status OK" -ForegroundColor Green
            $script:Passed++
            if ($response.Content) {
                $json = $response.Content | ConvertFrom-Json
                $json | ConvertTo-Json -Depth 5 | Write-Host -ForegroundColor DarkGray
            }
            return $json
        } else {
            Write-Host "  ✗ Attendu $ExpectedStatus, reçu $status" -ForegroundColor Red
            $script:Failed++
        }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -eq $ExpectedStatus) {
            Write-Host "  ✓ $status OK (erreur attendue)" -ForegroundColor Green
            $script:Passed++
        } else {
            Write-Host "  ✗ Erreur : $_" -ForegroundColor Red
            $script:Failed++
        }
    }
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║  CyberAudit7E — Smoke Test E2E              ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Magenta

# ═══ 1. Health ═══
Test-Endpoint -Name "Health check" -Method GET -Path "/health"

# ═══ 2. Liste des sites seed ═══
Test-Endpoint -Name "Liste des sites (4 seed attendus)" -Method GET -Path "/sites"

# ═══ 3. Recherche ═══
Test-Endpoint -Name "Recherche sites" -Method GET -Path "/sites/search?name=gouv"

# ═══ 4. Site par ID ═══
Test-Endpoint -Name "Détail site #1" -Method GET -Path "/sites/1"

# ═══ 5. Audit site .gouv.fr (score DSFR élevé) ═══
$bodyGouv = '{"url":"https://www.gouvernement.gouv.fr","name":"Gouvernement FR"}'
$audit1 = Test-Endpoint -Name "Audit site .gouv.fr" -Method POST -Path "/audits" -Body $bodyGouv -ExpectedStatus 201

# ═══ 6. Audit site non-.gouv.fr ═══
$bodyCom = '{"url":"https://www.example.com","name":"Example"}'
Test-Endpoint -Name "Audit site .com (score DSFR bas)" -Method POST -Path "/audits" -Body $bodyCom -ExpectedStatus 201

# ═══ 7. Re-audit pour tester la tendance ═══
Test-Endpoint -Name "Re-audit (tendance STABLE)" -Method POST -Path "/audits" -Body $bodyGouv -ExpectedStatus 201

# ═══ 8. Historique ═══
Test-Endpoint -Name "Historique audits site #2" -Method GET -Path "/audits/site/2"

# ═══ 9. Stats ═══
Test-Endpoint -Name "Stats globales" -Method GET -Path "/audits/stats"

# ═══ 10. Validation KO ═══
Test-Endpoint -Name "Validation échouée (400 attendu)" -Method POST -Path "/audits" -Body '{"url":"","name":""}' -ExpectedStatus 400

# ═══ 11. Not found ═══
Test-Endpoint -Name "Site inexistant (404 attendu)" -Method GET -Path "/sites/999" -ExpectedStatus 404

# ═══ Résumé ═══
Write-Host ""
Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║  Résultats : $Passed ✓  |  $Failed ✗                   ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Magenta

if ($Failed -eq 0) {
    Write-Host "  Tous les tests sont passés !" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  $Failed test(s) en échec" -ForegroundColor Red
    exit 1
}
