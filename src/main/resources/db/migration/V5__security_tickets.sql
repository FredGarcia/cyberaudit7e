-- ============================================================
-- V5__security_tickets.sql
-- CyberAudit7E — Tickets de sécurité unifiés
-- Pont entre SailPoint, CyberAudit7E et ServiceNow
-- ============================================================

CREATE TABLE security_tickets (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Identité du ticket
    title                   VARCHAR(500)   NOT NULL,
    description             CLOB,
    source                  VARCHAR(20)    NOT NULL,    -- AUDIT, SAILPOINT, SERVICENOW, MANUAL
    status                  VARCHAR(20)    NOT NULL DEFAULT 'NEW',
    severity                VARCHAR(20)    NOT NULL,    -- CRITICAL, HIGH, MEDIUM, LOW, INFO
    category                VARCHAR(100),

    -- Références croisées SailPoint
    sailpoint_violation_id  VARCHAR(100),
    sailpoint_event_type    VARCHAR(100),
    sailpoint_identity_id   VARCHAR(100),
    sailpoint_identity_name VARCHAR(255),

    -- Références croisées ServiceNow
    servicenow_sys_id       VARCHAR(50),
    servicenow_number       VARCHAR(20),
    servicenow_url          VARCHAR(500),

    -- Référence CyberAudit7E
    audit_report_id         BIGINT,
    site_url                VARCHAR(500),

    -- Assignation
    assigned_to             VARCHAR(255),
    assignment_group        VARCHAR(255),

    -- Payload brut (debug)
    raw_payload             CLOB,

    -- Timestamps
    created_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    resolved_at             TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_ticket_audit_report
        FOREIGN KEY (audit_report_id) REFERENCES audit_reports(id)
        ON DELETE SET NULL
);

-- Index pour les requêtes courantes
CREATE INDEX idx_tickets_source    ON security_tickets(source);
CREATE INDEX idx_tickets_status    ON security_tickets(status);
CREATE INDEX idx_tickets_severity  ON security_tickets(severity);
CREATE INDEX idx_tickets_sp_id     ON security_tickets(sailpoint_violation_id);
CREATE INDEX idx_tickets_snow_id   ON security_tickets(servicenow_sys_id);
CREATE INDEX idx_tickets_snow_num  ON security_tickets(servicenow_number);
CREATE INDEX idx_tickets_audit_id  ON security_tickets(audit_report_id);
CREATE INDEX idx_tickets_created   ON security_tickets(created_at);
