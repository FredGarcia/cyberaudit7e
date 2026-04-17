package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.rule.AuditContext;
import com.cyberaudit7e.domain.rule.AuditRule;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Moteur d'audit — orchestre l'exécution des règles.
 *
 * M4 : évolutions majeures vs M2/M3 :
 * - Intègre HtmlFetcherService pour le crawl réel HTTP (Jsoup)
 * - Construit un AuditContext (URL + Document) passé aux règles
 * - Trie les règles par priorité (structurelles d'abord)
 * - Gère les erreurs de crawl (toutes les règles reçoivent un context vide)
 * - Vide le cache du fetcher après chaque exécution
 */
@Service
public class AuditEngine {

    private static final Logger log = LoggerFactory.getLogger(AuditEngine.class);

    private final List<AuditRule> rules;
    private final ScoringService scoringService;
    private final HtmlFetcherService htmlFetcher;

    public AuditEngine(List<AuditRule> rules,
                       ScoringService scoringService,
                       HtmlFetcherService htmlFetcher) {
        // Trier les règles par priorité (plus bas = premier)
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(AuditRule::priority))
                .toList();
        this.scoringService = scoringService;
        this.htmlFetcher = htmlFetcher;

        log.info("══════════════════════════════════════════════════════");
        log.info("  AuditEngine M4 initialisé — {} règles (Jsoup actif)", this.rules.size());
        this.rules.forEach(r ->
                log.info("  ├─ [{}] p{} {} — {}",
                        r.category(), r.priority(), r.id(), r.description()));
        log.info("══════════════════════════════════════════════════════");
    }

    /**
     * Exécute toutes les règles sur l'URL donnée.
     *
     * M4 : crawl HTTP réel via Jsoup, puis injection du Document
     * dans l'AuditContext transmis à chaque règle.
     *
     * @param url URL du site à auditer
     * @return Liste des résultats, triés par priorité d'exécution
     */
    public List<RuleResultDto> runAllRules(String url) {
        log.info("[7E-EXECUTER] Crawl + exécution de {} règles sur {}", rules.size(), url);

        // Étape 1 : Crawler la page
        Optional<Document> docOpt = htmlFetcher.fetch(url);

        AuditContext context = docOpt
                .map(doc -> AuditContext.withDocument(url, doc))
                .orElseGet(() -> {
                    log.warn("[7E-EXECUTER] Crawl échoué pour {} — exécution en mode dégradé", url);
                    return AuditContext.withoutDocument(url);
                });

        // Étape 2 : Exécuter chaque règle
        List<RuleResultDto> results = rules.stream()
                .map(rule -> {
                    try {
                        RuleResultDto result = rule.evaluate(context);
                        String status = result.passed() ? "PASS" : (result.score() > 0 ? "PARTIAL" : "FAIL");
                        log.info("  ├─ [{}] {} → {} (score: {})",
                                rule.category(), rule.id(), status, result.score());
                        return result;
                    } catch (Exception e) {
                        log.error("  ├─ [{}] {} → ERREUR : {}", rule.category(), rule.id(), e.getMessage());
                        return RuleResultDto.failure(rule.id(), rule.category(),
                                "Erreur d'exécution : " + e.getMessage());
                    }
                })
                .toList();

        // Étape 3 : Nettoyer le cache pour la prochaine URL
        htmlFetcher.clearCache();

        long passed = results.stream().filter(RuleResultDto::passed).count();
        log.info("[7E-EXECUTER] Terminé — {}/{} règle(s) réussies", passed, results.size());

        return results;
    }

    public int getRulesCount() {
        return rules.size();
    }

    public ScoringService getScoringService() {
        return scoringService;
    }
}
