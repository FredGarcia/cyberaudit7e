package com.cyberaudit7e.service.cycle;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.event.AuditCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Phase 7E : ÉMETTRE
 * Publie un AuditCompletedEvent via le bus événementiel Spring.
 * Les listeners enregistrés réagiront (phase ÉQUILIBRER, notifications, etc.)
 */
@Service
public class EmitService {

    private static final Logger log = LoggerFactory.getLogger(EmitService.class);

    private final ApplicationEventPublisher publisher;

    public EmitService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Émet l'événement de fin d'audit.
     *
     * @param report Le rapport d'audit finalisé
     * @param trend  La tendance détectée par EvolveService
     */
    public void emit(AuditReport report, String trend) {
        log.info("[7E-ÉMETTRE] Publication AuditCompletedEvent — rapport #{}, tendance: {}",
                report.getId(), trend);
        publisher.publishEvent(new AuditCompletedEvent(this, report, trend));
    }
}
