package com.cyberaudit7e.service.cycle;

import com.cyberaudit7e.dto.RuleResultDto;
import com.cyberaudit7e.service.AuditEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 7E : EXÉCUTER
 * Délègue l'exécution des règles au moteur AuditEngine.
 * Sépare l'orchestration (cycle 7E) de l'exécution (Strategy Pattern).
 */
@Service
public class ExecuteService {

    private static final Logger log = LoggerFactory.getLogger(ExecuteService.class);

    private final AuditEngine auditEngine;

    public ExecuteService(AuditEngine auditEngine) {
        this.auditEngine = auditEngine;
    }

    /**
     * Exécute toutes les règles du moteur sur l'URL.
     *
     * @param url URL validée par la phase ÉVALUER
     * @return Résultats bruts de toutes les règles
     */
    public List<RuleResultDto> execute(String url) {
        log.info("[7E-EXÉCUTER] Lancement du moteur sur {}", url);
        List<RuleResultDto> results = auditEngine.runAllRules(url);
        log.info("[7E-EXÉCUTER] {} règle(s) exécutée(s) — {} réussie(s)",
                results.size(), results.stream().filter(RuleResultDto::passed).count());
        return results;
    }
}
