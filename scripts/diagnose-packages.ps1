# diagnose-packages.ps1
# Identifie les vrais packages Java dans le projet pour fixer l'alignement

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DIAGNOSTIC DES PACKAGES CyberAudit7E" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Trouve la classe @SpringBootApplication (app principale)
Write-Host "[1/3] Recherche de @SpringBootApplication..." -ForegroundColor Yellow
$appFile = Get-ChildItem -Path src\main\java -Recurse -Filter "*.java" |
    Where-Object { (Get-Content $_.FullName -Raw) -match '@SpringBootApplication' } |
    Select-Object -First 1

if ($appFile) {
    $firstLine = (Get-Content $appFile.FullName)[0]
    Write-Host "   Fichier : $($appFile.FullName)" -ForegroundColor White
    Write-Host "   Package : $firstLine" -ForegroundColor Green
    $appPackage = $firstLine -replace 'package\s+', '' -replace ';', '' -replace '\s+', ''
} else {
    Write-Host "   ERREUR : aucune classe @SpringBootApplication trouvée !" -ForegroundColor Red
    exit 1
}

# 2. Liste tous les packages distincts dans src/main/java
Write-Host ""
Write-Host "[2/3] Packages dans src/main/java :" -ForegroundColor Yellow
$mainPackages = Get-ChildItem -Path src\main\java -Recurse -Filter "*.java" |
    ForEach-Object {
        (Get-Content $_.FullName)[0] -replace 'package\s+', '' -replace ';', '' -replace '\s+', ''
    } | Sort-Object -Unique

foreach ($pkg in $mainPackages) {
    $prefix = if ($pkg -like "$appPackage*") { "OK  " } else { "KO  " }
    $color = if ($pkg -like "$appPackage*") { "Green" } else { "Red" }
    Write-Host "   [$prefix] $pkg" -ForegroundColor $color
}

# 3. Liste tous les packages dans src/test/java
Write-Host ""
Write-Host "[3/3] Packages dans src/test/java :" -ForegroundColor Yellow
if (Test-Path src\test\java) {
    $testPackages = Get-ChildItem -Path src\test\java -Recurse -Filter "*.java" |
        ForEach-Object {
            (Get-Content $_.FullName)[0] -replace 'package\s+', '' -replace ';', '' -replace '\s+', ''
        } | Sort-Object -Unique

    foreach ($pkg in $testPackages) {
        $prefix = if ($pkg -like "$appPackage*") { "OK  " } else { "KO  " }
        $color = if ($pkg -like "$appPackage*") { "Green" } else { "Red" }
        Write-Host "   [$prefix] $pkg" -ForegroundColor $color
    }
} else {
    Write-Host "   (aucun répertoire src/test/java)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RÉSUMÉ" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Package principal : $appPackage" -ForegroundColor White
Write-Host ""
Write-Host "  Règle Spring Test :" -ForegroundColor Yellow
Write-Host "  Les tests DOIVENT être dans un sous-package de '$appPackage'" -ForegroundColor White
Write-Host "  sinon Spring ne trouve pas @SpringBootApplication." -ForegroundColor White
Write-Host ""
