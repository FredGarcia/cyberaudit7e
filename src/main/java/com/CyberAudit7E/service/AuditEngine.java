package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.rule.AuditRule;
import com.cyberaudit7e.dto.RuleResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Moteur d'audit — orchestre l'exécution des règles.
 *
 * Démonstration clé de l'IoC Spring :
 * Le constructeur reçoit List<AuditRule>, et Spring y injecte
 * TOUTES les implémentations trouvées via @ComponentScan.
 * Ajouter une règle = créer un @Component, zéro modification ici.
 *
 * C'est le pattern Strategy appliqué avec l'injection de dépendances.
 * Équivalent du moteur à 17 règles d'AuditAccess, en Java idiomatique.
 */
@Service
public class AuditEngine {

    private static final Logger log = LoggerFactory.getLogger(AuditEngine.class);

    private final List<AuditRule> rules;
    private final ScoringService scoringService;

    /**
     * Injection par constructeur — la méthode recommandée en Spring.
     * Pas besoin de @Autowired quand il n'y a qu'un constructeur.
     *
     * @param rules          Toutes les implémentations d'AuditRule (injectées par Spring)
     * @param scoringService Service de calcul des scores pondérés
     */
    public AuditEngine(List<AuditRule> rules, ScoringService scoringService) {
        this.rules = rules;
        this.scoringService = scoringService;
        log.info("══════════════════════════════════════════════════════");
        log.info("  AuditEngine initialisé — {} règles chargées", rules.size());
        rules.forEach(r -> log.info("  ├─ [{}] {} ({})", r.category(), r.id(), r.description()));
        log.info("══════════════════════════════════════════════════════");
    }

    /**
     * Exécute toutes les règles sur l'URL donnée.
     * Phase 7E : EXECUTER
     *
     * @param url URL du site à auditer
     * @return Liste des résultats de chaque règle
     */
    public List<RuleResultDto> runAllRules(String url) {
        log.info("[7E-EXECUTER] Lancement de {} règles sur {}", rules.size(), url);
        return rules.stream()
                .map(rule -> {
                    RuleResultDto result = rule.evaluate(url);
                    log.debug("  ├─ {} → {} (score: {})",
                            rule.id(), result.passed() ? "PASS" : "FAIL", result.score());
                    return result;
                })
                .toList();
    }

    /**
     * @return Nombre de règles enregistrées dans le moteur
     */
    public int getRulesCount() {
        return rules.size();
    }

    /**
     * @return Le service de scoring (pour l'orchestrateur)
     */
    public ScoringService getScoringService() {
        return scoringService;
    }
}
