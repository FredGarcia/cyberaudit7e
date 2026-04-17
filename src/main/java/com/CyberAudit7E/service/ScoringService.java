package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.entity.RuleConfig;
import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import com.cyberaudit7e.repository.RuleConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de scoring pondéré.
 *
 * M4 : les poids sont chargés dynamiquement depuis la table rule_configs.
 * Fallback sur RuleCategory.getDefaultWeight() si la table est vide.
 *
 * C'est le cœur de la phase ÉQUILIBRER : le FeedbackLoopListener
 * peut modifier les poids en BDD, et le prochain audit utilisera
 * automatiquement les nouvelles valeurs.
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final RuleConfigRepository ruleConfigRepository;

    public ScoringService(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    /**
     * Charge les poids dynamiques depuis la BDD.
     * Fallback sur les valeurs par défaut de l'enum si absent.
     */
    public Map<RuleCategory, Double> loadWeights() {
        Map<RuleCategory, Double> weights = new LinkedHashMap<>();

        List<RuleConfig> configs = ruleConfigRepository.findByEnabledTrue();

        if (configs.isEmpty()) {
            // Fallback : utiliser les valeurs par défaut de l'enum
            log.debug("[SCORING] Aucune config en BDD — utilisation des poids par défaut");
            for (RuleCategory cat : RuleCategory.values()) {
                weights.put(cat, cat.getDefaultWeight());
            }
        } else {
            for (RuleConfig config : configs) {
                weights.put(config.getCategory(), config.getWeight());
                log.debug("[SCORING] Poids {} = {} (depuis BDD)", config.getCategory(), config.getWeight());
            }
            // S'assurer que toutes les catégories ont un poids
            for (RuleCategory cat : RuleCategory.values()) {
                weights.putIfAbsent(cat, cat.getDefaultWeight());
            }
        }

        return weights;
    }

    /**
     * Calcule les scores par catégorie et le score global composite.
     *
     * @param results Liste des résultats de règles
     * @return Map avec clés : "rgaa", "wcag", "dsfr", "global", + poids utilisés
     */
    public Map<String, Double> computeScores(List<RuleResultDto> results) {
        Map<RuleCategory, Double> weights = loadWeights();

        Map<RuleCategory, DoubleSummaryStatistics> stats = results.stream()
                .collect(Collectors.groupingBy(
                        RuleResultDto::category,
                        Collectors.summarizingDouble(RuleResultDto::score)
                ));

        double scoreRgaa = average(stats, RuleCategory.RGAA);
        double scoreWcag = average(stats, RuleCategory.WCAG);
        double scoreDsfr = average(stats, RuleCategory.DSFR);

        double global = scoreRgaa * weights.get(RuleCategory.RGAA)
                       + scoreWcag * weights.get(RuleCategory.WCAG)
                       + scoreDsfr * weights.get(RuleCategory.DSFR);

        log.info("[7E-EXAMINER] Scores — RGAA: {} (×{}), WCAG: {} (×{}), DSFR: {} (×{}), Global: {}",
                round(scoreRgaa), weights.get(RuleCategory.RGAA),
                round(scoreWcag), weights.get(RuleCategory.WCAG),
                round(scoreDsfr), weights.get(RuleCategory.DSFR),
                round(global));

        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("rgaa", round(scoreRgaa));
        scores.put("wcag", round(scoreWcag));
        scores.put("dsfr", round(scoreDsfr));
        scores.put("global", round(global));
        scores.put("weight_rgaa", weights.get(RuleCategory.RGAA));
        scores.put("weight_wcag", weights.get(RuleCategory.WCAG));
        scores.put("weight_dsfr", weights.get(RuleCategory.DSFR));

        return scores;
    }

    /**
     * Met à jour le poids d'une catégorie en BDD.
     * Utilisé par la phase ÉQUILIBRER du FeedbackLoopListener.
     *
     * @param category Catégorie à modifier
     * @param newWeight Nouveau poids (0.0 à 1.0)
     */
    public void updateWeight(RuleCategory category, double newWeight) {
        ruleConfigRepository.findByCategory(category).ifPresent(config -> {
            double oldWeight = config.getWeight();
            config.setWeight(newWeight);
            ruleConfigRepository.save(config);
            log.info("[7E-ÉQUILIBRER] Poids {} ajusté : {} → {}", category, oldWeight, newWeight);
        });
    }

    private double average(Map<RuleCategory, DoubleSummaryStatistics> stats, RuleCategory cat) {
        return stats.containsKey(cat) ? stats.get(cat).getAverage() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
