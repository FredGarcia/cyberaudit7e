# Fix Maven Tests — CyberAudit7E

## Le problème

Spring Test cherche `@SpringBootApplication` en remontant l'arborescence des
packages **à partir du package du test**. Si le test est dans un package qui
n'est **pas un sous-package** de ton application principale, Spring ne trouve
rien et tu obtiens :

```
Unable to find a @SpringBootConfiguration by searching packages upwards
from the test.
```

Dans ton cas, il y a eu un mix de packages dans le projet :
- Application principale (auto-générée par Spring Initializr) : probablement `com.CyberAudit7E`
- Mes fichiers de test : `com.cyberaudit7e` (minuscules)
- Test stub `Cyberaudit7eApplicationTests` : dans un package incomplet (`CyberAudit7E`)

Windows est case-insensitive sur les dossiers mais Java est case-sensitive sur
les packages — d'où le mismatch.

## Solution en 2 commandes

### 1. Diagnostic (vérifier la situation)

Place `diagnose-packages.ps1` dans la racine de ton projet et lance :

```powershell
powershell -ExecutionPolicy Bypass -File .\diagnose-packages.ps1
```

Le script te montrera :
- Le vrai package de ton `@SpringBootApplication`
- Tous les packages dans `src/main/java`
- Tous les packages dans `src/test/java` avec `[OK]` ou `[KO]` selon
  l'alignement

### 2. Correction automatique

Place `realign-test-packages.ps1` dans la racine et lance :

```powershell
powershell -ExecutionPolicy Bypass -File .\realign-test-packages.ps1
```

Le script va :
1. Détecter le vrai package de ton application principale
2. Supprimer le test stub `Cyberaudit7eApplicationTests.java`
   (il était inutile de toute façon)
3. Remplacer `com.cyberaudit7e` par le vrai package dans tous les
   fichiers `.java` de `src/test`
4. Déplacer physiquement les fichiers dans la bonne arborescence
5. Nettoyer les dossiers vides

### 3. Relance des tests

```powershell
.\mvnw.cmd test
```

Résultat attendu : les 6 classes de test (ScoringServiceTest,
Phase7ETest, DsfrHeaderRuleTest, SiteRepositoryTest, HealthControllerTest,
AuditCycleIntegrationTest) passent en vert.

## Si le package n'est pas `com.cyberaudit7e`

Par défaut, le script suppose que mes fichiers de test utilisent
`com.cyberaudit7e` (c'était le cas dans le zip initial).

Si tu as déjà modifié les fichiers de test avec un autre package,
ouvre `realign-test-packages.ps1` et change la ligne :

```powershell
$oldPackage = "com.cyberaudit7e"
```

Remplace par la valeur actuelle de tes tests.

## Alternative manuelle (si tu préfères voir ce qui se passe)

```powershell
# 1. Trouver le vrai package de l'app principale
Get-ChildItem -Path src\main\java -Recurse -Filter "*Application.java" |
    ForEach-Object { Get-Content $_.FullName | Select-Object -First 1 }

# Exemple de sortie : package com.CyberAudit7E;

# 2. Supprimer le test stub
Get-ChildItem -Recurse -Filter "Cyberaudit7eApplicationTests.java" | Remove-Item

# 3. Remplacer le package dans tous les fichiers de test
# (adapter com.CyberAudit7E selon ta sortie de l'étape 1)
Get-ChildItem -Path src\test -Recurse -Filter "*.java" |
    ForEach-Object {
        (Get-Content $_.FullName) -replace 'com\.cyberaudit7e', 'com.CyberAudit7E' |
        Set-Content $_.FullName
    }

# 4. Déplacer physiquement les dossiers
# src\test\java\com\cyberaudit7e\* -> src\test\java\com\CyberAudit7E\*
# (ceci est case-insensitive sur Windows mais Maven ne s'en plaint pas)

# 5. Tester
.\mvnw.cmd test
```
