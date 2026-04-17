package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.domain.enums.Phase7E;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA Site — un site web à auditer.
 *
 * M3 : POJO M2 transformé en entité JPA.
 * Changements vs M2 :
 * - @Entity, @Table, @Id, @GeneratedValue
 * - @Enumerated(STRING) pour Phase7E
 * - @OneToMany bidirectionnel avec AuditReport
 * - @PrePersist / @PreUpdate pour les timestamps
 * - @Column constraints alignées sur V1__create_schema.sql
 */
@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500, unique = true)
    private String url;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", length = 20)
    private Phase7E currentPhase = Phase7E.EVALUER;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("auditedAt DESC")
    private List<AuditReport> reports = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Lifecycle callbacks ──

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Constructeurs ──

    public Site() {
    }

    public Site(String url, String name) {
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Avance le site à la phase suivante du cycle 7E.
     */
    public void advancePhase() {
        this.currentPhase = this.currentPhase.next();
    }

    /**
     * Helper bidirectionnel : ajoute un rapport et maintient la relation.
     */
    public void addReport(AuditReport report) {
        reports.add(report);
        report.setSite(this);
    }

    @Override
    public String toString() {
        return String.format("Site{id=%d, url='%s', name='%s', phase=%s}",
                id, url, name, currentPhase);
    }
}
