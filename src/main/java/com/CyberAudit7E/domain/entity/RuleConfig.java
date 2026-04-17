package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.domain.enums.RuleCategory;
import jakarta.persistence.*;

/**
 * Configuration dynamique des poids de scoring par catégorie.
 *
 * M4 NOUVEAU : permet d'ajuster les poids RGAA/WCAG/DSFR en BDD
 * au lieu de les coder en dur dans RuleCategory.
 *
 * C'est la base de la phase ÉQUILIBRER en cybernétique :
 * le feedback loop peut modifier ces poids pour s'adapter.
 */
@Entity
@Table(name = "rule_configs")
public class RuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RuleCategory category;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 500)
    private String description;

    // ── Constructeurs ──

    public RuleConfig() {}

    public RuleConfig(RuleCategory category, Double weight, String description) {
        this.category = category;
        this.weight = weight;
        this.description = description;
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RuleCategory getCategory() { return category; }
    public void setCategory(RuleCategory category) { this.category = category; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
