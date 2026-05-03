-- ============================================================
-- V4__postgresql_compat.sql
-- CyberAudit7E M7 — Compatibilité PostgreSQL
--
-- H2 et PostgreSQL ont des syntaxes différentes pour :
-- - AUTO_INCREMENT (H2) vs SERIAL/IDENTITY (PostgreSQL)
-- - CLOB (H2) vs TEXT (PostgreSQL)
--
-- Flyway gère cela via des migrations conditionnelles
-- ou des fichiers par vendeur (V4__xxx.sql + V4__xxx__postgresql.sql)
--
-- Cette migration ajoute des index supplémentaires
-- utiles pour les requêtes paginées de M6.
-- ============================================================

-- Index composites pour les requêtes paginées fréquentes
CREATE INDEX idx_reports_site_audited
    ON audit_reports(site_id, audited_at DESC);

CREATE INDEX idx_reports_score_audited
    ON audit_reports(score_global, audited_at DESC);

CREATE INDEX idx_reports_trend
    ON audit_reports(trend);

-- Index pour la recherche full-text M6
CREATE INDEX idx_sites_name
    ON sites(name);
