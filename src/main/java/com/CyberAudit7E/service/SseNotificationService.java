package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.event.AuditCompletedEvent;
import com.cyberaudit7e.event.AuditProgressEvent;
import com.cyberaudit7e.event.AuditStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service de notification SSE (Server-Sent Events).
 *
 * M5 NOUVEAU : diffuse les événements d'audit en temps réel
 * vers tous les clients connectés via GET /api/audits/stream.
 *
 * Architecture événementielle :
 *   AuditOrchestrator publie → Spring Events → SseNotificationService écoute → SSE push
 *
 * Équivalent du Redis Pub/Sub d'AuditAccess mais sans infrastructure externe.
 * Les clients SSE sont des navigateurs, des dashboards Vue.js, ou des outils CLI.
 *
 * SSE vs WebSocket :
 * - SSE est unidirectionnel (serveur → client) — parfait pour les notifications
 * - Reconnexion automatique native côté navigateur
 * - Pas besoin de bibliothèque JS (EventSource natif)
 * - Fonctionne avec les proxies HTTP classiques
 */
@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    /** Timeout SSE : 30 minutes (les audits longs sont possibles) */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * Liste thread-safe des emitters SSE actifs.
     * CopyOnWriteArrayList : optimisé pour beaucoup de lectures (broadcast)
     * et peu d'écritures (connect/disconnect).
     */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Crée un nouvel emitter SSE pour un client.
     * Appelé par le controller GET /api/audits/stream.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("[SSE] Client déconnecté — {} client(s) restant(s)", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("[SSE] Timeout client — {} client(s) restant(s)", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("[SSE] Erreur client — {} client(s) restant(s)", emitters.size());
        });

        emitters.add(emitter);
        log.info("[SSE] Nouveau client connecté — {} client(s) actif(s)", emitters.size());

        // Envoyer un heartbeat initial pour confirmer la connexion
        sendToEmitter(emitter, "connected",
                Map.of("message", "CyberAudit7E SSE connecté", "clients", emitters.size()));

        return emitter;
    }

    // ── Event Listeners ──

    @EventListener
    public void onAuditStarted(AuditStartedEvent event) {
        broadcast("audit-started", Map.of(
                "siteUrl", event.getSiteUrl(),
                "siteName", event.getSiteName(),
                "startedAt", event.getStartedAt().toString()
        ));
    }

    @EventListener
    public void onAuditProgress(AuditProgressEvent event) {
        broadcast("audit-progress", Map.of(
                "siteUrl", event.getSiteUrl(),
                "phase", event.getPhase().name(),
                "phaseLabel", event.getPhase().getLabel(),
                "message", event.getMessage(),
                "progress", event.getProgressPercent(),
                "step", event.getPhaseIndex() + "/" + event.getTotalPhases(),
                "timestamp", event.getInstant()
        ));
    }

    @EventListener
    public void onAuditCompleted(AuditCompletedEvent event) {
        AuditReport report = event.getReport();
        broadcast("audit-completed", Map.of(
                "reportId", report.getId(),
                "siteUrl", report.getSite().getUrl(),
                "siteName", report.getSite().getName(),
                "scoreGlobal", report.getScoreGlobal(),
                "trend", event.getTrend(),
                "auditedAt", report.getAuditedAt().toString()
        ));
    }

    // ── Broadcast ──

    /**
     * Diffuse un événement à tous les clients SSE connectés.
     * Les emitters morts sont automatiquement retirés.
     */
    private void broadcast(String eventName, Map<String, Object> data) {
        if (emitters.isEmpty()) return;

        log.debug("[SSE] Broadcast '{}' → {} client(s)", eventName, emitters.size());

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("[SSE] {} emitter(s) mort(s) retirés", deadEmitters.size());
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    /**
     * @return Nombre de clients SSE connectés
     */
    public int getActiveClients() {
        return emitters.size();
    }
}
