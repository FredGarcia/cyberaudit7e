-- ============================================================
-- V6__ticket_threshold.sql
-- CyberAudit7E — Seuil configurable de creation auto de tickets
-- Stocke dans rule_configs comme parametre systeme
-- ============================================================

-- Utilise la table rule_configs existante avec une categorie speciale
-- category = 'SYSTEM' pour les parametres globaux (pas une RuleCategory enum)
-- On cree une table dediee pour eviter de polluer rule_configs

CREATE TABLE system_settings (
    setting_key   VARCHAR(50)  PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description   VARCHAR(500),
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('ticket.auto.threshold', '0.50', 'Seuil de score en dessous duquel un ticket est cree automatiquement (0.0 a 1.0)');
