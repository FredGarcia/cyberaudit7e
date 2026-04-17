-- ============================================================
-- V1__create_schema.sql
-- CyberAudit7E — Schema initial (H2 2.4+ compatible)
-- ============================================================

CREATE TABLE sites (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    url             VARCHAR       NOT NULL UNIQUE,
    name            VARCHAR       NOT NULL,
    current_phase   VARCHAR       DEFAULT 'EVALUER',
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_reports (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    site_id         BIGINT        NOT NULL,
    score_rgaa      DOUBLE PRECISION,
    score_wcag      DOUBLE PRECISION,
    score_dsfr      DOUBLE PRECISION,
    score_global    DOUBLE PRECISION,
    completed_phase VARCHAR,
    trend           VARCHAR,
    results_json    CLOB,
    audited_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_site
        FOREIGN KEY (site_id) REFERENCES sites(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_sites_url          ON sites(url);
CREATE INDEX idx_sites_phase        ON sites(current_phase);
CREATE INDEX idx_reports_site_id    ON audit_reports(site_id);
CREATE INDEX idx_reports_audited_at ON audit_reports(audited_at);
CREATE INDEX idx_reports_score      ON audit_reports(score_global);