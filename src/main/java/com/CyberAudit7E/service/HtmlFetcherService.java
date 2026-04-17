package com.cyberaudit7e.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de crawl HTTP — récupère et parse le DOM des sites à auditer.
 *
 * M4 : remplace les simulations M2/M3 par un vrai fetch HTTP via Jsoup.
 * Les règles d'audit reçoivent un Document Jsoup au lieu d'une simple URL.
 *
 * Fonctionnalités :
 * - Cache par URL pour éviter de refetcher pendant un même cycle 7E
 * - Timeout configurable
 * - User-Agent conforme (identification du bot)
 * - Gestion propre des erreurs réseau
 *
 * Analogie AuditAccess : équivalent du crawler Playwright (Django/Celery),
 * mais synchrone et léger pour le POC Spring.
 */
@Service
public class HtmlFetcherService {

    private static final Logger log = LoggerFactory.getLogger(HtmlFetcherService.class);

    private static final String USER_AGENT =
            "CyberAudit7E/1.0 (Accessibility Audit Bot; +https://cyberaudit7e.local)";
    private static final int TIMEOUT_MS = 10_000; // 10 secondes
    private static final int MAX_BODY_SIZE = 5 * 1024 * 1024; // 5 MB

    /**
     * Cache des documents par URL (vidé entre chaque cycle via clearCache()).
     * Évite de fetcher 7 fois la même page pour 7 règles.
     */
    private final Map<String, Document> cache = new ConcurrentHashMap<>();

    /**
     * Récupère et parse le HTML d'une URL.
     * Retourne un Optional.empty() en cas d'erreur réseau.
     *
     * @param url URL à crawler
     * @return Document Jsoup parsé, ou empty si erreur
     */
    public Optional<Document> fetch(String url) {
        // Vérifier le cache
        if (cache.containsKey(url)) {
            log.debug("[FETCHER] Cache hit pour {}", url);
            return Optional.of(cache.get(url));
        }

        log.info("[FETCHER] Crawl HTTP → {}", url);
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_SIZE)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();

            cache.put(url, doc);
            log.info("[FETCHER] OK — {} ({} octets, titre: '{}')",
                    url, doc.html().length(), doc.title());
            return Optional.of(doc);
        } catch (Exception e) {
            log.warn("[FETCHER] Erreur crawl {} : {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Vide le cache. Appelé par l'orchestrateur entre chaque cycle 7E.
     */
    public void clearCache() {
        int size = cache.size();
        cache.clear();
        if (size > 0) {
            log.debug("[FETCHER] Cache vidé ({} entrées)", size);
        }
    }

    /**
     * @return Nombre d'URLs en cache
     */
    public int getCacheSize() {
        return cache.size();
    }
}
