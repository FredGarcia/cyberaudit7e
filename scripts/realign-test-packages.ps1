# realign-test-packages.ps1
# Aligne les packages des tests sur le package de l'application principale
# Usage : .\realign-test-packages.ps1

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Realignement packages tests" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Detecte le package reel de l'application
$appFile = Get-ChildItem -Path src\main\java -Recurse -Filter "*.java" |
    Where-Object { (Get-Content $_.FullName -Raw) -match '@SpringBootApplication' } |
    Select-Object -First 1

if (-not $appFile) {
    Write-Host "ERREUR : aucune classe @SpringBootApplication trouvee" -ForegroundColor Red
    exit 1
}

$firstLine = (Get-Content $appFile.FullName)[0]
$realPackage = $firstLine -replace 'package\s+', '' -replace ';', '' -replace '\s+', ''
Write-Host "Package application principale : $realPackage" -ForegroundColor Green

# 2. Determine l'ancien package (celui qu'on doit remplacer)
# Par defaut : com.cyberaudit7e (minuscules, celui de mes fichiers de test)
$oldPackage = "com.cyberaudit7e"

Write-Host ""
Write-Host "Replacement : $oldPackage -> $realPackage" -ForegroundColor Yellow
Write-Host ""

# 3. Demande confirmation
$confirm = Read-Host "Confirmer le replacement dans tous les .java de src/test ? [O/N]"
if ($confirm -ne "O" -and $confirm -ne "o") {
    Write-Host "Annule." -ForegroundColor Yellow
    exit 0
}

# 4. Supprime le test stub auto-genere par Spring Initializr
$stubTest = Get-ChildItem -Path src\test\java -Recurse -Filter "*ApplicationTests.java" -ErrorAction SilentlyContinue
if ($stubTest) {
    Write-Host ""
    Write-Host "Suppression du test stub auto-genere :" -ForegroundColor Yellow
    $stubTest | ForEach-Object {
        Write-Host "  - $($_.FullName)" -ForegroundColor Gray
        Remove-Item $_.FullName
    }
}

# 5. Remplace les packages dans tous les .java de test
Write-Host ""
Write-Host "Remplacement des packages et imports :" -ForegroundColor Yellow

$modified = 0
Get-ChildItem -Path src\test\java -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $newContent = $content -replace [regex]::Escape($oldPackage), $realPackage

    if ($content -ne $newContent) {
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "  - $($_.Name)" -ForegroundColor Green
        $modified++
    }
}

Write-Host ""
Write-Host "$modified fichier(s) modifie(s)." -ForegroundColor Green

# 6. Deplacement physique des fichiers
# Les declarations package ont change mais les dossiers physiques aussi doivent suivre
Write-Host ""
Write-Host "Verification de l'arborescence physique..." -ForegroundColor Yellow

# Chemin cible attendu, base sur le realPackage
$expectedPath = "src\test\java\" + ($realPackage -replace '\.', '\')
$oldPath = "src\test\java\" + ($oldPackage -replace '\.', '\')

if (Test-Path $oldPath) {
    Write-Host "Deplacement : $oldPath -> $expectedPath" -ForegroundColor Yellow

    # Cree les repertoires parents
    $parent = Split-Path $expectedPath -Parent
    if (-not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    # Deplace recursivement
    if (Test-Path $expectedPath) {
        Get-ChildItem -Path $oldPath -Recurse | Move-Item -Destination $expectedPath -Force
        Remove-Item -Path $oldPath -Recurse -Force -ErrorAction SilentlyContinue
    } else {
        Move-Item -Path $oldPath -Destination $expectedPath -Force
    }

    # Nettoie les dossiers vides restants
    Get-ChildItem -Path src\test\java -Directory -Recurse |
        Where-Object { -not (Get-ChildItem -Path $_.FullName -Recurse -File) } |
        Sort-Object -Property FullName -Descending |
        Remove-Item -Force -ErrorAction SilentlyContinue

    Write-Host "Arborescence alignee." -ForegroundColor Green
} else {
    Write-Host "Pas de dossier $oldPath a deplacer (deja aligne ?)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Termine !" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Prochaine etape : .\mvnw.cmd test" -ForegroundColor White
Write-Host ""
