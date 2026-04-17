-- ============================================================
-- V3__rule_configs.sql
-- CyberAudit7E M4 — Configuration dynamique des poids
-- Phase ÉQUILIBRER : les poids sont ajustables en BDD
-- ============================================================

CREATE TABLE rule_configs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category    VARCHAR(20)   NOT NULL UNIQUE,
    weight      DOUBLE        NOT NULL,
    enabled     BOOLEAN       DEFAULT TRUE,
    description VARCHAR(500)
);

-- Poids par défaut (identiques à RuleCategory.getDefaultWeight())
-- Modifiables via API ou par le FeedbackLoopListener
INSERT INTO rule_configs (category, weight, enabled, description) VALUES
    ('RGAA', 0.50, TRUE, 'Référentiel Général d''Amélioration de l''Accessibilité — poids dominant'),
    ('WCAG', 0.30, TRUE, 'Web Content Accessibility Guidelines — poids secondaire'),
    ('DSFR', 0.20, TRUE, 'Design System de l''État Français — poids complémentaire');
