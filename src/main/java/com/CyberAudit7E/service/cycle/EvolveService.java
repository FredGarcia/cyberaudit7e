package com.CyberAudit7E.service.cycle;

import com.CyberAudit7E.domain.entity.AuditReport;
import com.CyberAudit7E.domain.entity.Site;
import com.CyberAudit7E.repository.AuditReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Phase 7E : ÉVOLUER
 * Compare le score actuel avec l'audit précédent pour détecter la tendance.
 *
 * M3 : aucun changement de code ! La signature du repository est identique
 * entre le ConcurrentHashMap M2 et le JpaRepository M3.
 * C'est la puissance de l'IoC — le service ne sait pas comment les données
 * sont stockées, il travaille avec une abstraction.
 */
@Service
public class EvolveService {

    private static final Logger log = LoggerFactory.getLogger(EvolveService.class);

    private final AuditReportRepository reportRepository;

    public EvolveService(AuditReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Analyse la tendance du score par rapport au dernier audit.
     *
     * @param site   Le site audité
     * @param scores Les scores du cycle en cours
     * @return Tendance : "UP", "DOWN", "STABLE", ou "FIRST"
     */
    public String evolve(Site site, Map<String, Double> scores) {
        double currentScore = scores.getOrDefault("global", 0.0);

        Optional<AuditReport> previousOpt = reportRepository
                .findFirstBySiteIdOrderByAuditedAtDesc(site.getId());

        if (previousOpt.isEmpty()) {
            log.info("[7E-ÉVOLUER] Premier audit pour {} — pas de référence", site.getName());
            return "FIRST";
        }

        double previousScore = previousOpt.get().getScoreGlobal();
        double delta = currentScore - previousScore;
        String trend;

        if (delta > 0.05) {
            trend = "UP";
        } else if (delta < -0.05) {
            trend = "DOWN";
        } else {
            trend = "STABLE";
        }

        log.info("[7E-ÉVOLUER] {} — précédent: {}, actuel: {}, delta: {}, tendance: {}",
                site.getName(), previousScore, currentScore,
                String.format("%+.2f", delta), trend);

        return trend;
    }
}
