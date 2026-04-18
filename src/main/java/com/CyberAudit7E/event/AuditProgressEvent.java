package com.cyberaudit7e.event;

import com.cyberaudit7e.domain.enums.Phase7E;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Événement émis à chaque transition de phase du cycle 7E.
 * M5 NOUVEAU : granularité fine pour le streaming SSE.
 *
 * Le dashboard ou un client SSE reçoit en temps réel :
 * EVALUER → ELABORER → EXECUTER → EXAMINER → EVOLUER → EMETTRE → EQUILIBRER
 */
public class AuditProgressEvent extends ApplicationEvent {

    private final String siteUrl;
    private final Phase7E phase;
    private final String message;
    private final int phaseIndex; // 1 à 7
    private final Instant timestamp;

    public AuditProgressEvent(Object source, String siteUrl,
            Phase7E phase, String message) {
        super(source);
        this.siteUrl = siteUrl;
        this.phase = phase;
        this.message = message;
        this.phaseIndex = phase.ordinal() + 1;
        this.timestamp = Instant.now();
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public Phase7E getPhase() {
        return phase;
    }

    public String getMessage() {
        return message;
    }

    public int getPhaseIndex() {
        return phaseIndex;
    }

    public int getTotalPhases() {
        return 7;
    }

    /**
     * Retourne l'instant de l'événement (type moderne {@link Instant}).
     * Le nom a été changé pour éviter le conflit avec
     * {@link org.springframework.context.ApplicationEvent#getTimestamp()}
     * qui retourne un {@code long}.
     */
    public Instant getInstant() {
        return timestamp;
    }

    /**
     * Progression en pourcentage (0-100).
     */
    public int getProgressPercent() {
        return Math.round((float) phaseIndex / 7 * 100);
    }
}