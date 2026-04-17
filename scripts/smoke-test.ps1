# ============================================================
# smoke-test.ps1 - Test end-to-end de l'API CyberAudit7E
# Usage  : powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
# Prereq : l'application doit tourner sur http://localhost:8080
# ============================================================
# IMPORTANT : powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
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
    Write-Host "----------------------------------------" -ForegroundColor Cyan
    Write-Host "  $Name" -ForegroundColor White
    Write-Host "  $Method $Path" -ForegroundColor Gray

    try {
        $params = @{
            Uri             = "$BaseUrl$Path"
            Method          = $Method
            ContentType     = "application/json"
            UseBasicParsing = $true
        }
        if ($Body) { $params.Body = $Body }

        $response = Invoke-WebRequest @params
        $status = $response.StatusCode

        if ($status -eq $ExpectedStatus) {
            Write-Host "  [OK] $status" -ForegroundColor Green
            $script:Passed++
            if ($response.Content) {
                $json = $response.Content | ConvertFrom-Json
                $json | ConvertTo-Json -Depth 5 | Write-Host -ForegroundColor DarkGray
            }
            return $json
        }
        else {
            Write-Host "  [KO] Attendu $ExpectedStatus, recu $status" -ForegroundColor Red
            $script:Failed++
        }
    }
    catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -eq $ExpectedStatus) {
            Write-Host "  [OK] $status (erreur attendue)" -ForegroundColor Green
            $script:Passed++
        }
        else {
            Write-Host "  [KO] Erreur : $_" -ForegroundColor Red
            $script:Failed++
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  CyberAudit7E - Smoke Test E2E        " -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

# === 1. Health ===
Test-Endpoint -Name "Health check" -Method GET -Path "/health"

# === 2. Liste des sites seed ===
Test-Endpoint -Name "Liste des sites - 4 seed attendus" -Method GET -Path "/sites"

# === 3. Recherche ===
Test-Endpoint -Name "Recherche sites par nom" -Method GET -Path "/sites/search?name=gouv"

# === 4. Site par ID ===
Test-Endpoint -Name "Detail site 1" -Method GET -Path "/sites/1"

# === 5. Audit site .gouv.fr ===
$bodyGouv = '{"url":"https://www.gouvernement.gouv.fr","name":"Gouvernement FR"}'
Test-Endpoint -Name "Audit site .gouv.fr - score DSFR eleve" -Method POST -Path "/audits" -Body $bodyGouv -ExpectedStatus 201

# === 6. Audit site non-.gouv.fr ===
$bodyCom = '{"url":"https://www.example.com","name":"Example"}'
Test-Endpoint -Name "Audit site .com - score DSFR bas" -Method POST -Path "/audits" -Body $bodyCom -ExpectedStatus 201

# === 7. Re-audit pour tester la tendance ===
Test-Endpoint -Name "Re-audit - tendance STABLE attendue" -Method POST -Path "/audits" -Body $bodyGouv -ExpectedStatus 201

# === 8. Historique ===
Test-Endpoint -Name "Historique audits du site 2" -Method GET -Path "/audits/site/2"

# === 9. Stats ===
Test-Endpoint -Name "Stats globales" -Method GET -Path "/audits/stats"

# === 10. Validation KO ===
Test-Endpoint -Name "Validation echouee - 400 attendu" -Method POST -Path "/audits" -Body '{"url":"","name":""}' -ExpectedStatus 400

# === 11. Not found ===
Test-Endpoint -Name "Site inexistant - 404 attendu" -Method GET -Path "/sites/999" -ExpectedStatus 404

# === Resume ===
Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  Resultats : $Passed OK  |  $Failed KO                " -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

if ($Failed -eq 0) {
    Write-Host "  Tous les tests sont passes !" -ForegroundColor Green
    exit 0
}
else {
    Write-Host "  $Failed test(s) en echec" -ForegroundColor Red
    exit 1
}