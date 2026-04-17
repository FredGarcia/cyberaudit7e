# Fix Tests Spring Boot 4 — Imports modularisés

## Le problème

Spring Boot 4 a découpé le monolithe `spring-boot-test-autoconfigure` en modules :

| Annotation | Package Spring Boot 3 (ancien) | Package Spring Boot 4 (nouveau) |
|------------|-------------------------------|--------------------------------|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |

Et chaque module a son propre **starter test** Maven.

## Étape 1 — Ajouter les dépendances test dans pom.xml

Ouvrir `pom.xml` et ajouter ces 3 dépendances dans le bloc `<dependencies>` :

```xml
<!-- Test starters modulaires Spring Boot 4 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Awaitility pour tester les comportements @Async -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

Note : `awaitility` est déjà managé par Spring Boot (pas besoin de <version>).

## Étape 2 — Remplacer les 2 fichiers de test corrigés

Copier les fichiers de ce zip :

- `HealthControllerTest.java`  →  `src/test/java/com/cyberaudit7e/controller/`
- `SiteRepositoryTest.java`    →  `src/test/java/com/cyberaudit7e/repository/`

## Étape 3 — Lancer les tests

```powershell
.\mvnw.cmd clean test
```

Résultat attendu : 6 classes de test, ~15 tests au total, tout en vert.
