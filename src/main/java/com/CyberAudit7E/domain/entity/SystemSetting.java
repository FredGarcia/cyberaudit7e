package com.cyberaudit7e.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Parametre systeme cle/valeur.
 * Stocke les configurations globales modifiables a chaud
 * (seuil de ticket, limites, flags, etc.)
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 50)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String value;

    @Column(length = 500)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }

    // Constructeurs
    public SystemSetting() {
    }

    public SystemSetting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    // Getters & Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Retourne la valeur comme double (avec fallback).
     */
    public double asDouble(double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}