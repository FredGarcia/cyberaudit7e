-- ============================================================
-- V1__create_schema.sql
-- CyberAudit7E — Schéma initial
-- Flyway migration : exécutée automatiquement au démarrage
-- ============================================================

-- Table des sites à auditer
-- Inspirée du registre d'organes GitManager
CREATE TABLE sites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    url             VARCHAR(500)  NOT NULL UNIQUE,
    name            VARCHAR(255)  NOT NULL,
    current_phase   VARCHAR(20)   DEFAULT 'EVALUER',
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Table des rapports d'audit
-- Un rapport = un cycle 7E complet
CREATE TABLE audit_reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_id         BIGINT        NOT NULL,
    score_rgaa      DOUBLE,
    score_wcag      DOUBLE,
    score_dsfr      DOUBLE,
    score_global    DOUBLE,
    completed_phase VARCHAR(20),
    trend           VARCHAR(10),
    results_json    CLOB,
    audited_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_site
        FOREIGN KEY (site_id) REFERENCES sites(id)
        ON DELETE CASCADE
);

-- Index pour les requêtes fréquentes
CREATE INDEX idx_sites_url          ON sites(url);
CREATE INDEX idx_sites_phase        ON sites(current_phase);
CREATE INDEX idx_reports_site_id    ON audit_reports(site_id);
CREATE INDEX idx_reports_audited_at ON audit_reports(audited_at);
CREATE INDEX idx_reports_score      ON audit_reports(score_global);
