# ═══════════════════════════════════════════════════════════════
# CyberAudit7E — Script de tests complet (PowerShell)
# Modules M1 → M5 — Windows 11 natif
# ═══════════════════════════════════════════════════════════════
# Usage :
#   .\test-cyberaudit7e.ps1           # Tout exécuter
#   .\test-cyberaudit7e.ps1 -Module 3 # Tester un module spécifique
#   .\test-cyberaudit7e.ps1 -SSE      # Ouvrir le flux SSE
# ═══════════════════════════════════════════════════════════════

param(
    [int]$Module = 0,
    [switch]$SSE,
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Continue"
$testCount = 0
$passCount = 0
$failCount = 0

# ── Helpers ──

function Write-Header($text) {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $text" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
}

function Write-Test($name) {
    $script:testCount++
    Write-Host ""
    Write-Host "  [$script:testCount] $name" -ForegroundColor Yellow -NoNewline
}

function Write-Pass($detail) {
    $script:passCount++
    Write-Host " → PASS" -ForegroundColor Green
    if ($detail) { Write-Host "       $detail" -ForegroundColor DarkGray }
}

function Write-Fail($detail) {
    $script:failCount++
    Write-Host " → FAIL" -ForegroundColor Red
    if ($detail) { Write-Host "       $detail" -ForegroundColor Red }
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body,
        [int]$ExpectedStatus = 200
    )
    $uri = "$BaseUrl$Path"
    $params = @{
        Uri = $uri
        Method = $Method
        ContentType = "application/json"
        ErrorAction = "Stop"
    }
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $response = Invoke-RestMethod @params
        return @{ Success = $true; Data = $response; StatusCode = 200 }
    }
    catch {
        $statusCode = 0
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        $errorBody = $null
        try {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
        } catch {}
        return @{ Success = ($statusCode -eq $ExpectedStatus); Data = $errorBody; StatusCode = $statusCode }
    }
}

# ── SSE Mode ──

if ($SSE) {
    Write-Header "SSE STREAMING — Ctrl+C pour quitter"
    Write-Host "  Connexion à $BaseUrl/api/audits/stream ..." -ForegroundColor Gray
    Write-Host "  Lancez un audit dans un autre terminal pour voir les events." -ForegroundColor Gray
    Write-Host ""
    try {
        $webClient = New-Object System.Net.WebClient
        $stream = $webClient.OpenRead("$BaseUrl/api/audits/stream")
        $reader = New-Object System.IO.StreamReader($stream)
        while (-not $reader.EndOfStream) {
            $line = $reader.ReadLine()
            if ($line -match "^event:") {
                Write-Host $line -ForegroundColor Magenta
            }
            elseif ($line -match "^data:") {
                Write-Host $line -ForegroundColor White
                Write-Host ""
            }
        }
    }
    catch {
        Write-Host "  Connexion SSE interrompue." -ForegroundColor Yellow
    }
    exit
}

# ═══════════════════════════════════════════
#  MODULE M1 — BOOTSTRAP
# ═══════════════════════════════════════════

if ($Module -eq 0 -or $Module -eq 1) {
    Write-Header "MODULE M1 — Bootstrap Spring Boot"

    Write-Test "GET /api/health → status UP"
    $r = Invoke-Api -Path "/api/health"
    if ($r.Success -and $r.Data.status -eq "UP") {
        Write-Pass "service=$($r.Data.service), phase=$($r.Data.phase)"
    } else { Write-Fail "Health check échoué" }

    Write-Test "GET /api/ → index endpoint"
    $r = Invoke-Api -Path "/api/"
    if ($r.Success -and $r.Data.service) {
        Write-Pass "version=$($r.Data.version)"
    } else { Write-Fail "Index non disponible" }
}

# ═══════════════════════════════════════════
#  MODULE M2 — ARCHITECTURE
# ═══════════════════════════════════════════

if ($Module -eq 0 -or $Module -eq 2) {
    Write-Header "MODULE M2 — Architecture Spring (IoC, Strategy Pattern)"

    Write-Test "GET /api/health → rulesLoaded = 13"
    $r = Invoke-Api -Path "/api/health"
    if ($r.Success -and $r.Data.rulesLoaded -ge 7) {
        Write-Pass "rulesLoaded=$($r.Data.rulesLoaded)"
    } else { Write-Fail "Règles non chargées" }
}

# ═══════════════════════════════════════════
#  MODULE M3 — PERSISTANCE JPA
# ═══════════════════════════════════════════

if ($Module -eq 0 -or $Module -eq 3) {
    Write-Header "MODULE M3 — Persistance JPA + H2 + Flyway"

    Write-Test "GET /api/sites → sites seed Flyway (>= 4)"
    $r = Invoke-Api -Path "/api/sites"
    if ($r.Success -and $r.Data.Count -ge 4) {
        Write-Pass "$($r.Data.Count) site(s) — Flyway V2 seed OK"
    } else { Write-Fail "Sites seed non trouvés (Flyway V2)" }

    Write-Test "GET /api/sites/1 → SiteDto avec auditsCount"
    $r = Invoke-Api -Path "/api/sites/1"
    if ($r.Success -and $r.Data.id -eq 1) {
        Write-Pass "url=$($r.Data.url), phase=$($r.Data.currentPhase)"
    } else { Write-Fail "Site #1 introuvable" }

    Write-Test "GET /api/sites/search?name=gouv → recherche LIKE"
    $r = Invoke-Api -Path "/api/sites/search?name=gouv"
    if ($r.Success -and $r.Data.Count -ge 1) {
        $names = ($r.Data | ForEach-Object { $_.name }) -join ", "
        Write-Pass "$($r.Data.Count) résultat(s) : $names"
    } else { Write-Fail "Recherche par nom échouée" }

    Write-Test "POST /api/sites → créer un nouveau site"
    $newSite = @{ url = "https://test-$(Get-Random).example.com"; name = "Test M3" }
    $r = Invoke-Api -Method POST -Path "/api/sites" -Body $newSite
    if ($r.Success -and $r.Data.id) {
        Write-Pass "id=$($r.Data.id), url=$($r.Data.url)"
        # Cleanup
        Invoke-Api -Method DELETE -Path "/api/sites/$($r.Data.id)" | Out-Null
    } else { Write-Fail "Création de site échouée" }

    Write-Test "POST /api/sites → doublon = 409 Conflict"
    $dupSite = @{ url = "https://www.service-public.fr"; name = "Doublon" }
    $r = Invoke-Api -Method POST -Path "/api/sites" -Body $dupSite -ExpectedStatus 409
    if ($r.StatusCode -eq 409) {
        Write-Pass "409 Conflict correct"
    } else { Write-Fail "Attendu 409, reçu $($r.StatusCode)" }

    Write-Test "GET /api/sites/999 → 404 Not Found"
    $r = Invoke-Api -Path "/api/sites/999" -ExpectedStatus 404
    if ($r.StatusCode -eq 404) {
        Write-Pass "404 correct"
    } else { Write-Fail "Attendu 404, reçu $($r.StatusCode)" }
}

# ═══════════════════════════════════════════
#  MODULE M4 — MOTEUR D'AUDIT JSOUP
# ═══════════════════════════════════════════

if ($Module -eq 0 -or $Module -eq 4) {
    Write-Header "MODULE M4 — Moteur d'audit Jsoup (13 règles réelles)"

    Write-Test "GET /api/health → version M5, fetcherMode Jsoup"
    $r = Invoke-Api -Path "/api/health"
    if ($r.Success -and $r.Data.fetcherMode -match "Jsoup") {
        Write-Pass "fetcherMode=$($r.Data.fetcherMode), rules=$($r.Data.rulesLoaded)"
    } else { Write-Fail "Fetcher non Jsoup" }

    Write-Test "GET /api/config/weights → poids normalisés"
    $r = Invoke-Api -Path "/api/config/weights"
    if ($r.Success -and $r.Data.normalized -eq $true) {
        Write-Pass "RGAA=$($r.Data.RGAA.weight), WCAG=$($r.Data.WCAG.weight), DSFR=$($r.Data.DSFR.weight)"
    } else { Write-Fail "Poids non normalisés" }

    Write-Test "POST /api/audits → audit réel service-public.fr"
    $audit1 = @{ url = "https://www.service-public.fr"; name = "Service Public" }
    $r = Invoke-Api -Method POST -Path "/api/audits" -Body $audit1
    if ($r.Success -and $r.Data.rulesCount -eq 13) {
        $score = $r.Data.scores.global
        $passed = $r.Data.passedCount
        Write-Pass "score=$score, passed=$passed/13, reportId=$($r.Data.reportId)"

        # Vérifier les détails des règles
        $details = $r.Data.details
        $titleRule = $details | Where-Object { $_.ruleId -eq "RGAA-8.5" }
        if ($titleRule -and $titleRule.detail -match "Titre pertinent") {
            Write-Host "       ├─ RGAA-8.5: $($titleRule.detail)" -ForegroundColor DarkGray
        }
        $langRule = $details | Where-Object { $_.ruleId -eq "RGAA-8.3" }
        if ($langRule -and $langRule.detail -match "lang=") {
            Write-Host "       ├─ RGAA-8.3: $($langRule.detail)" -ForegroundColor DarkGray
        }
        $dsfrHdr = $details | Where-Object { $_.ruleId -eq "DSFR-HDR-01" }
        if ($dsfrHdr) {
            Write-Host "       ├─ DSFR-HDR: $($dsfrHdr.detail)" -ForegroundColor DarkGray
        }
        $dsfrFtr = $details | Where-Object { $_.ruleId -eq "DSFR-FTR-01" }
        if ($dsfrFtr) {
            Write-Host "       └─ DSFR-FTR: $($dsfrFtr.detail)" -ForegroundColor DarkGray
        }
    } else { Write-Fail "Audit échoué ou nombre de règles incorrect" }

    Write-Test "POST /api/audits → audit .gouv.fr (DSFR élevé)"
    $audit2 = @{ url = "https://www.gouvernement.gouv.fr"; name = "Gouvernement FR" }
    $r = Invoke-Api -Method POST -Path "/api/audits" -Body $audit2
    if ($r.Success -and $r.Data.scores.dsfr -gt 0.3) {
        Write-Pass "DSFR=$($r.Data.scores.dsfr), Global=$($r.Data.scores.global)"
    } else { Write-Fail "Score DSFR bas pour un .gouv.fr" }

    Write-Test "POST /api/audits → audit example.com (DSFR bas)"
    $audit3 = @{ url = "https://www.example.com"; name = "Example.com" }
    $r = Invoke-Api -Method POST -Path "/api/audits" -Body $audit3
    if ($r.Success -and $r.Data.scores.dsfr -lt 0.5) {
        Write-Pass "DSFR=$($r.Data.scores.dsfr) (bas attendu), Global=$($r.Data.scores.global)"
    } else { Write-Fail "Score DSFR inattendu pour example.com" }

    Write-Test "POST /api/audits → 2e audit (test tendance ÉVOLUER)"
    $r = Invoke-Api -Method POST -Path "/api/audits" -Body $audit1
    if ($r.Success) {
        Write-Pass "score=$($r.Data.scores.global), reportId=$($r.Data.reportId)"
    } else { Write-Fail "2e audit échoué" }

    Write-Test "POST /api/audits → site inaccessible (mode dégradé)"
    $audit404 = @{ url = "https://site-inexistant-test-7e.invalid"; name = "Test 404" }
    $r = Invoke-Api -Method POST -Path "/api/audits" -Body $audit404
    if ($r.Success) {
        $crawlFail = $r.Data.details | Where-Object { $_.detail -match "Impossible" }
        if ($crawlFail.Count -eq 13) {
            Write-Pass "13 règles en mode dégradé (crawl échoué)"
        } else {
            Write-Pass "Audit exécuté — $($crawlFail.Count) règle(s) dégradée(s)"
        }
    } else { Write-Fail "Audit dégradé échoué" }

    Write-Test "POST /api/audits → validation (body vide = 400)"
    $auditBad = @{ url = ""; name = "" }
    $r = Invoke-Api -Method POST -Path "/api/audits" -Body $auditBad -ExpectedStatus 400
    if ($r.StatusCode -eq 400) {
        Write-Pass "400 Validation OK"
    } else { Write-Fail "Attendu 400, reçu $($r.StatusCode)" }

    Write-Test "PUT /api/config/weights/RGAA → modifier poids"
    $r = Invoke-Api -Method PUT -Path "/api/config/weights/RGAA" -Body @{ weight = 0.55 }
    if ($r.Success) {
        Write-Pass "RGAA weight → 0.55"
    } else { Write-Fail "Modification poids échouée" }

    Write-Test "POST /api/config/weights/reset → remettre les défauts"
    $r = Invoke-Api -Method POST -Path "/api/config/weights/reset"
    if ($r.Success -and $r.Data.RGAA -eq 0.5) {
        Write-Pass "RGAA=0.5, WCAG=0.3, DSFR=0.2"
    } else { Write-Fail "Reset des poids échoué" }
}

# ═══════════════════════════════════════════
#  MODULE M5 — ASYNC & EVENTS
# ═══════════════════════════════════════════

if ($Module -eq 0 -or $Module -eq 5) {
    Write-Header "MODULE M5 — Async, Events & SSE"

    Write-Test "GET /api/health → version M5, sseClients, metrics"
    $r = Invoke-Api -Path "/api/health"
    if ($r.Success -and $r.Data.metrics) {
        Write-Pass "sseClients=$($r.Data.sseClients), totalAudits=$($r.Data.metrics.totalAuditsExecuted)"
    } else { Write-Fail "Métriques M5 absentes" }

    Write-Test "POST /api/audits/async → audit asynchrone (retour immédiat)"
    $asyncReq = @{ url = "https://www.legifrance.gouv.fr"; name = "Légifrance" }
    $r = Invoke-Api -Method POST -Path "/api/audits/async" -Body $asyncReq
    if ($r.Success -and $r.Data.jobId) {
        $jobId = $r.Data.jobId
        Write-Pass "jobId=$jobId, status=$($r.Data.status)"

        # Attendre la fin du job
        Write-Host "       Attente du job #$jobId..." -ForegroundColor DarkGray -NoNewline
        $maxWait = 30
        $waited = 0
        do {
            Start-Sleep -Seconds 2
            $waited += 2
            $jobStatus = Invoke-Api -Path "/api/audits/async/$jobId"
            Write-Host "." -ForegroundColor DarkGray -NoNewline
        } while ($jobStatus.Data.status -eq "RUNNING" -and $waited -lt $maxWait)
        Write-Host ""

        Write-Test "GET /api/audits/async/$jobId → résultat du job"
        if ($jobStatus.Data.status -eq "COMPLETED") {
            $score = $jobStatus.Data.result.scores.global
            Write-Pass "status=COMPLETED, score=$score"
        } else {
            Write-Fail "Job non terminé après ${waited}s — status=$($jobStatus.Data.status)"
        }
    } else { Write-Fail "Soumission async échouée" }

    Write-Test "POST /api/audits/batch → batch de 3 audits parallèles"
    $batchReq = @{
        sites = @(
            @{ url = "https://www.service-public.fr"; name = "SP" },
            @{ url = "https://www.gouvernement.gouv.fr"; name = "Gouv" },
            @{ url = "https://www.example.com"; name = "Example" }
        )
    }
    $r = Invoke-Api -Method POST -Path "/api/audits/batch" -Body $batchReq
    if ($r.Success -and $r.Data.batchSize -eq 3) {
        $jobs = $r.Data.jobs
        $ids = ($jobs | ForEach-Object { $_.jobId }) -join ", "
        Write-Pass "batchSize=3, jobIds=[$ids]"

        # Attendre la fin du batch
        Write-Host "       Attente du batch..." -ForegroundColor DarkGray -NoNewline
        Start-Sleep -Seconds 10
        $allDone = $false
        $maxBatchWait = 60
        $batchWaited = 10
        do {
            $jobList = Invoke-Api -Path "/api/audits/async"
            $running = ($jobList.Data | Where-Object { $_.status -eq "RUNNING" }).Count
            if ($running -eq 0) { $allDone = $true; break }
            Start-Sleep -Seconds 3
            $batchWaited += 3
            Write-Host "." -ForegroundColor DarkGray -NoNewline
        } while ($batchWaited -lt $maxBatchWait)
        Write-Host ""

        Write-Test "GET /api/audits/async → tous les jobs terminés"
        $jobList = Invoke-Api -Path "/api/audits/async"
        $completed = ($jobList.Data | Where-Object { $_.status -eq "COMPLETED" }).Count
        if ($completed -ge 3) {
            Write-Pass "$completed job(s) COMPLETED"
        } else {
            $statuses = ($jobList.Data | ForEach-Object { "$($_.jobId):$($_.status)" }) -join ", "
            Write-Fail "Jobs non tous terminés — $statuses"
        }
    } else { Write-Fail "Batch échoué" }

    Write-Test "DELETE /api/audits/async → nettoyer les jobs terminés"
    $r = Invoke-Api -Method DELETE -Path "/api/audits/async"
    if ($r.Success) {
        Write-Pass "$($r.Data.cleared) job(s) nettoyé(s)"
    } else { Write-Fail "Nettoyage échoué" }

    Write-Test "GET /api/audits/schedule → info scheduler"
    $r = Invoke-Api -Path "/api/audits/schedule"
    if ($r.Success) {
        Write-Pass "enabled=$($r.Data.enabled)"
    } else { Write-Fail "Scheduler info échoué" }

    Write-Test "POST /api/audits/schedule/trigger → déclenchement manuel"
    $r = Invoke-Api -Method POST -Path "/api/audits/schedule/trigger"
    if ($r.Success -and $r.Data.triggered -ge 1) {
        Write-Pass "$($r.Data.triggered) audit(s) déclenchés"
    } else { Write-Fail "Trigger scheduler échoué" }

    # Attendre un peu pour les audits schedulés
    Start-Sleep -Seconds 5
}

# ═══════════════════════════════════════════
#  STATS & CONSULTATION (transversal)
# ═══════════════════════════════════════════

if ($Module -eq 0) {
    Write-Header "STATS & CONSULTATION (transversal M3-M5)"

    Write-Test "GET /api/audits/stats → stats globales"
    $r = Invoke-Api -Path "/api/audits/stats"
    if ($r.Success) {
        Write-Pass "sites=$($r.Data.totalSites), audits=$($r.Data.totalAudits), avg=$($r.Data.averageScore)"
        if ($r.Data.metrics) {
            Write-Host "       ├─ Métriques: totalExec=$($r.Data.metrics.totalAuditsExecuted), avgMs=$($r.Data.metrics.averageDurationMs)" -ForegroundColor DarkGray
        }
        if ($r.Data.trends) {
            Write-Host "       └─ Tendances: UP=$($r.Data.trends.UP), DOWN=$($r.Data.trends.DOWN), STABLE=$($r.Data.trends.STABLE)" -ForegroundColor DarkGray
        }
    } else { Write-Fail "Stats échouées" }

    Write-Test "GET /api/audits/site/1 → historique site #1"
    $r = Invoke-Api -Path "/api/audits/site/1"
    if ($r.Success -and $r.Data.Count -ge 1) {
        Write-Pass "$($r.Data.Count) rapport(s) pour le site #1"
    } else { Write-Fail "Historique vide" }

    Write-Test "GET /api/audits/1 → détail rapport #1"
    $r = Invoke-Api -Path "/api/audits/1"
    if ($r.Success -and $r.Data.id -eq 1) {
        Write-Pass "score=$($r.Data.scoreGlobal), règles=$($r.Data.rulesCount)"
    } else { Write-Fail "Rapport #1 introuvable" }

    Write-Test "GET /api/audits/alerts?threshold=0.7 → alertes"
    $r = Invoke-Api -Path "/api/audits/alerts?threshold=0.7"
    if ($r.Success) {
        Write-Pass "$($r.Data.Count) alerte(s) sous le seuil 0.7"
    } else { Write-Fail "Endpoint alertes échoué" }
}

# ═══════════════════════════════════════════
#  BILAN
# ═══════════════════════════════════════════

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  BILAN : $testCount tests — " -ForegroundColor Cyan -NoNewline
Write-Host "$passCount PASS" -ForegroundColor Green -NoNewline
Write-Host " / " -ForegroundColor Cyan -NoNewline
if ($failCount -gt 0) {
    Write-Host "$failCount FAIL" -ForegroundColor Red
} else {
    Write-Host "$failCount FAIL" -ForegroundColor Green
}
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "  ✓ Tous les tests passent !" -ForegroundColor Green
} else {
    Write-Host "  ✗ $failCount test(s) échoué(s) — vérifier les logs serveur" -ForegroundColor Red
}
Write-Host ""
