package com.cyberaudit7e;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CyberAudit7E — Moteur d'audit cybernétique.
 *
 * M3 : plus d'exclusion DataSourceAutoConfiguration.
 * Spring Data JPA + H2 (dev) / PostgreSQL (prod) activés.
 */
@SpringBootApplication
public class CyberAudit7eApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyberAudit7eApplication.class, args);
    }
}
