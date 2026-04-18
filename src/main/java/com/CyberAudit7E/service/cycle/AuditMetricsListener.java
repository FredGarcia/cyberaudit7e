package com.cyberaudit7e.service.cycle;

import com.cyberaudit7e.event.AuditCompletedEvent;
import com.cyberaudit7e.event.AuditStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listener de métriques — collecte les statistiques de performance.
 *
 * M5 NOUVEAU : écoute les événements pour calculer :
 * - Durée moyenne d'un audit
 * - Nombre total d'audits exécutés
 * - Nombre d'audits en cours
 * - Dernier audit exécuté
 *
 * En production : ces métriques alimenteraient Spring Actuator + Micrometer
 * pour un export vers Prometheus/Grafana (comme dans Labmanager).
 */
@Component
public class AuditMetricsListener {

    private static final Logger log = LoggerFactory.getLogger(AuditMetricsListener.class);

    private final AtomicLong totalAudits = new AtomicLong(0);
    private final AtomicLong activeAudits = new AtomicLong(0);
    private final Map<String, Instant> auditStartTimes = new ConcurrentHashMap<>();
    private volatile long totalDurationMs = 0;
    private volatile String lastAuditUrl = "";
    private volatile long lastAuditDurationMs = 0;

    @EventListener
    public void onStarted(AuditStartedEvent event) {
        activeAudits.incrementAndGet();
        auditStartTimes.put(event.getSiteUrl(), event.getStartedAt());
        log.debug("[METRICS] Audit démarré : {} — {} actif(s)",
                event.getSiteUrl(), activeAudits.get());
    }

    @EventListener
    public void onCompleted(AuditCompletedEvent event) {
        activeAudits.decrementAndGet();
        totalAudits.incrementAndGet();

        String url = event.getReport().getSite().getUrl();
        Instant startTime = auditStartTimes.remove(url);

        if (startTime != null) {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            totalDurationMs += durationMs;
            lastAuditDurationMs = durationMs;
            lastAuditUrl = url;
            log.info("[METRICS] Audit terminé : {} en {}ms — total: {}, actifs: {}",
                    url, durationMs, totalAudits.get(), activeAudits.get());
        }
    }

    /**
     * Retourne les métriques actuelles.
     */
    public Map<String, Object> getMetrics() {
        long total = totalAudits.get();
        long avgMs = total > 0 ? totalDurationMs / total : 0;

        return Map.of(
                "totalAuditsExecuted", total,
                "activeAudits", activeAudits.get(),
                "averageDurationMs", avgMs,
                "lastAuditUrl", lastAuditUrl,
                "lastAuditDurationMs", lastAuditDurationMs
        );
    }
}
