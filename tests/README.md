# Tests CyberAudit7E — Stratégie complète

## Pyramide de tests Spring

```
           ╱╲
          ╱  ╲       Niveau 4 : Integration (lent, complet)
         ╱ E2E╲      @SpringBootTest — contexte complet
        ╱──────╲
       ╱        ╲    Niveau 3 : Web slice (moyen)
      ╱   Web    ╲   @WebMvcTest + MockMvc + @MockitoBean
     ╱────────────╲
    ╱              ╲ Niveau 2 : JPA slice (moyen)
   ╱   Repository   ╲ @DataJpaTest — couche JPA isolée
  ╱──────────────────╲
 ╱                    ╲ Niveau 1 : Unit (rapide, pur Java)
╱        Unit          ╲ Pas de Spring — JUnit + AssertJ + Mockito
─────────────────────────
```

**Règle d'or** : beaucoup de tests unitaires rapides à la base, peu de tests
d'intégration lents au sommet. Une exécution complète doit rester < 30 secondes
pour garder un feedback loop rapide.

## Dépendances (déjà dans spring-boot-starter-test)

Le starter de test Spring Boot amène tout ce qu'il faut :

| Librairie | Rôle |
|-----------|------|
| JUnit Jupiter | Framework de test (`@Test`, `@DisplayName`, `@ParameterizedTest`) |
| AssertJ | Assertions fluides (`assertThat(x).isEqualTo(y)`) |
| Mockito | Mocking des dépendances |
| Spring Test | Intégration Spring (`@SpringBootTest`, `@MockitoBean`) |
| JsonPath | Parsing des réponses JSON dans les tests MockMvc |

**À ajouter si besoin** (Awaitility pour tester les comportements async) :

```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.2</version>
    <scope>test</scope>
</dependency>
```

## Installation des tests

Copier l'arborescence `src/test/` dans le projet :

```bash
# Depuis le dossier racine du projet cyberaudit7e
cp -r tests/src/test src/
```

## Exécution

### Tous les tests
```powershell
.\mvnw.cmd test
```

### Un seul niveau
```powershell
# Unit tests uniquement (rapides)
.\mvnw.cmd test "-Dtest=*Test"

# Integration tests uniquement
.\mvnw.cmd test "-Dtest=*IntegrationTest"

# Un test spécifique
.\mvnw.cmd test "-Dtest=ScoringServiceTest"
```

### Avec rapport de couverture (JaCoCo)

Ajouter dans `pom.xml` :
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

Puis :
```powershell
.\mvnw.cmd test
# Rapport HTML → target/site/jacoco/index.html
```

## Contenu du pack

### Niveau 1 — Tests unitaires purs (3 fichiers)

| Fichier | Cible | Intérêt pédagogique |
|---------|-------|---------------------|
| `ScoringServiceTest.java` | ScoringService | `@Test`, AssertJ, `within()` pour tolérance |
| `Phase7ETest.java` | Enum Phase7E | `@ParameterizedTest` + `@CsvSource` |
| `DsfrHeaderRuleTest.java` | Règle DSFR | `@Nested` pour scénarios, test de Strategy |

### Niveau 2 — JPA slice (1 fichier)

| Fichier | Cible | Intérêt pédagogique |
|---------|-------|---------------------|
| `SiteRepositoryTest.java` | SiteRepository | `@DataJpaTest`, Flyway + H2 en test, rollback auto |

### Niveau 3 — Web slice (1 fichier)

| Fichier | Cible | Intérêt pédagogique |
|---------|-------|---------------------|
| `HealthControllerTest.java` | HealthController | `@WebMvcTest`, `@MockitoBean`, MockMvc, jsonPath |

### Niveau 4 — Intégration (1 fichier)

| Fichier | Cible | Intérêt pédagogique |
|---------|-------|---------------------|
| `AuditCycleIntegrationTest.java` | Cycle 7E complet | `@SpringBootTest`, Awaitility, persistance JPA |

### Smoke test E2E

| Fichier | Rôle |
|---------|------|
| `scripts/smoke-test.ps1` | Script PowerShell qui teste les 11 endpoints HTTP |

## Stratégie recommandée pour Fred

Vu ta méthodologie "POC/MVP validés à 18/18 et 47/47 tests", voici comment
construire une suite exhaustive similaire pour CyberAudit7E :

### Phase A — Validation M3 (tout de suite)
1. Lance `.\mvnw.cmd test` → doit passer les 5 tests actuels
2. Lance `.\mvnw.cmd spring-boot:run` dans un terminal
3. Dans un autre terminal : `.\scripts\smoke-test.ps1`
4. Ouvre `http://localhost:8080/h2-console` pour inspecter la BDD

### Phase B — Extension (au fil des modules M4 à M7)
- **M4 Moteur avancé** : 1 test unitaire par nouvelle règle (Strategy Pattern)
- **M5 Async/Events** : test Awaitility sur `FeedbackLoopListener`
- **M6 API validation** : tests MockMvc pour chaque endpoint avec cas d'erreur
- **M7 Docker** : Testcontainers pour remplacer H2 par un vrai PostgreSQL en test

### Phase C — Durcissement
- **Performance** : `@SpringBootTest(webEnvironment = RANDOM_PORT)` + JMH
- **Contract testing** : Spring Cloud Contract pour l'API
- **Mutation testing** : PIT (`pitest-maven-plugin`) pour mesurer la qualité des tests

## Outils complémentaires pour tester à la main

### Bruno (recommandé — local, git-friendly, open source)
Alternative à Postman, stocke les collections en fichiers texte versionnables.
Installation : `winget install Bruno.Bruno`

### HTTPie (si tu préfères la ligne de commande)
Plus lisible que curl pour les requêtes JSON :
```powershell
winget install HTTPie.HTTPie
http POST :8080/api/audits url=https://www.service-public.fr name="Service Public"
```

### Spring Boot DevTools (déjà activé en M1)
Le rechargement à chaud relance l'app à chaque modification de code, pratique
pour boucler rapidement avec le smoke-test.
