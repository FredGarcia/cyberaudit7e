package com.cyberaudit7e.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Événement émis au DÉMARRAGE d'un cycle 7E.
 * M5 NOUVEAU : permet aux listeners de réagir au lancement
 * (logging, notification SSE, métriques).
 */
public class AuditStartedEvent extends ApplicationEvent {

    private final String siteUrl;
    private final String siteName;
    private final Instant startedAt;

    public AuditStartedEvent(Object source, String siteUrl, String siteName) {
        super(source);
        this.siteUrl = siteUrl;
        this.siteName = siteName;
        this.startedAt = Instant.now();
    }

    public String getSiteUrl() { return siteUrl; }
    public String getSiteName() { return siteName; }
    public Instant getStartedAt() { return startedAt; }
}
