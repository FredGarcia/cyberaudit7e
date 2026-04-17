package com.cyberaudit7e.service;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HtmlFetcherService — Crawl et cache")
class HtmlFetcherServiceTest {

    private HtmlFetcherService fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new HtmlFetcherService();
    }

    @Test
    @DisplayName("Retourne empty pour une URL invalide")
    void shouldReturnEmptyForInvalidUrl() {
        Optional<Document> result = fetcher.fetch("https://site-inexistant-xyz.invalid");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("clearCache vide le cache")
    void shouldClearCache() {
        // Le cache est vide au départ
        assertEquals(0, fetcher.getCacheSize());
        fetcher.clearCache();
        assertEquals(0, fetcher.getCacheSize());
    }

    @Test
    @DisplayName("Retourne empty pour une URL vide")
    void shouldHandleEmptyUrl() {
        Optional<Document> result = fetcher.fetch("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Retourne empty pour une URL mal formée")
    void shouldHandleMalformedUrl() {
        Optional<Document> result = fetcher.fetch("not-a-url");
        assertTrue(result.isEmpty());
    }

    // Note : les tests de crawl réel (example.com, etc.) sont des tests
    // d'intégration qui nécessitent un accès réseau. Ils sont volontairement
    // exclus des tests unitaires pour la rapidité du build.
    // Pour les activer : @Tag("integration") + profil Maven dédié.
}
