-- ============================================================
-- V2__seed_data.sql
-- CyberAudit7E — Données de test (POC)
-- Sites de référence pour valider le moteur d'audit
-- ============================================================

INSERT INTO sites (url, name, current_phase, created_at, updated_at) VALUES
    ('https://www.service-public.fr', 'Service Public', 'EVALUER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('https://www.gouvernement.gouv.fr', 'Gouvernement FR', 'EVALUER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('https://www.legifrance.gouv.fr', 'Légifrance', 'EVALUER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('https://www.example.com', 'Example.com', 'EVALUER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
