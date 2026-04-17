package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.domain.enums.Phase7E;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NIVEAU 2 — Test de tranche JPA (@DataJpaTest).
 *
 * Spring Boot 4 : import modularisé
 *   AVANT : org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
 *   APRÈS : org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 *
 * Nécessite spring-boot-starter-data-jpa-test dans le pom.xml (scope test).
 *
 * @DataJpaTest ne charge QUE la couche JPA :
 *   - Repositories, Entités, Hibernate + DataSource (H2)
 *   - Flyway exécuté → seed data V2 disponible
 *   - Transaction rollback automatique après chaque test
 */
@DataJpaTest
@ActiveProfiles("dev")
class SiteRepositoryTest {

    @Autowired
    private SiteRepository siteRepository;

    @Test
    @DisplayName("Les 4 sites seed de V2__seed_data.sql sont chargés")
    void seedDataIsLoaded() {
        List<Site> sites = siteRepository.findAll();

        assertThat(sites).hasSize(4);
        assertThat(sites).extracting(Site::getName)
                .containsExactlyInAnyOrder(
                        "Service Public",
                        "Gouvernement FR",
                        "Légifrance",
                        "Example.com"
                );
    }

    @Test
    @DisplayName("findByUrl retourne le site attendu")
    void findByUrlWorks() {
        Optional<Site> site = siteRepository
                .findByUrl("https://www.gouvernement.gouv.fr");

        assertThat(site).isPresent();
        assertThat(site.get().getName()).isEqualTo("Gouvernement FR");
    }

    @Test
    @DisplayName("findByUrl retourne Optional.empty() si absent")
    void findByUrlReturnsEmptyWhenNotFound() {
        Optional<Site> site = siteRepository.findByUrl("https://inexistant.fr");

        assertThat(site).isEmpty();
    }

    @Test
    @DisplayName("existsByUrl retourne true pour un site existant")
    void existsByUrlTrue() {
        assertThat(siteRepository.existsByUrl("https://www.service-public.fr")).isTrue();
    }

    @Test
    @DisplayName("save persiste un nouveau site avec ID auto-généré")
    void saveAndRetrieve() {
        Site newSite = new Site("https://new-site.gouv.fr", "Nouveau Site");

        Site saved = siteRepository.save(newSite);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCurrentPhase()).isEqualTo(Phase7E.EVALUER);
    }
}
