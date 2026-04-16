package com.CyberAudit7E;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CyberAudit7E — Moteur d'audit cybernétique.
 *
 * Fusion de trois projets :
 * - GitManager → Registre de services (organes)
 * - AuditAccess → Moteur de règles multi-référentiel
 * - Axiome 7E → Boucle cybernétique à 7 phases
 *
 * Uilisation des repositories in-memory. M3 supprimera cette exclusion
 * pour activer Spring Data JPA + H2/PostgreSQL.
 */
@SpringBootApplication 
public class CyberAudit7eApplication {
    public static void main(String[] args) {
        SpringApplication.run(CyberAudit7eApplication.class, args);
    }
}