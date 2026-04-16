package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.domain.enums.Phase7E;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Site — un site web à auditer.
 * Inspiré du modèle AuditAccess (Django) transposé en Java.
 *
 * M2 : POJO simple avec getters/setters.
 * M3 : ajout des annotations JPA (@Entity, @Table, etc.)
 *
 * Note : en production, utiliser Lombok (@Data, @Builder)
 * pour éliminer le boilerplate. Ici on est explicite pour la pédagogie.
 */
public class Site {

    private Long id;
    private String url;
    private String name;
    private Phase7E currentPhase = Phase7E.EVALUER;
    private List<AuditReport> reports = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructeurs ──

    public Site() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Site(String url, String name) {
        this();
        this.url = url;
        this.name = name;
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Phase7E getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(Phase7E currentPhase) { this.currentPhase = currentPhase; }

    public List<AuditReport> getReports() { return reports; }
    public void setReports(List<AuditReport> reports) { this.reports = reports; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Avance le site à la phase suivante du cycle 7E.
     */
    public void advancePhase() {
        this.currentPhase = this.currentPhase.next();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Site{id=%d, url='%s', name='%s', phase=%s}",
                id, url, name, currentPhase);
    }
}
