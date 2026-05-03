package com.cyberaudit7e.integration;

import com.cyberaudit7e.domain.entity.SecurityTicket;
import com.cyberaudit7e.domain.enums.TicketSeverity;
import com.cyberaudit7e.domain.enums.TicketSource;
import com.cyberaudit7e.domain.enums.TicketStatus;
import com.cyberaudit7e.dto.PagedResponse;
import com.cyberaudit7e.dto.integration.TicketDto;
import com.cyberaudit7e.integration.sailpoint.SailPointClient;
import com.cyberaudit7e.integration.servicenow.ServiceNowClient;
import com.cyberaudit7e.repository.SecurityTicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller pour les tickets de sécurité unifiés.
 * Pont central entre SailPoint, CyberAudit7E et ServiceNow.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Tickets de sécurité unifiés — SailPoint × CyberAudit7E × ServiceNow")
public class TicketController {

    private final SecurityTicketRepository ticketRepository;
    private final TicketOrchestrator orchestrator;
    private final ServiceNowClient serviceNowClient;
    private final SailPointClient sailPointClient;

    public TicketController(SecurityTicketRepository ticketRepository,
                            TicketOrchestrator orchestrator,
                            ServiceNowClient serviceNowClient,
                            SailPointClient sailPointClient) {
        this.ticketRepository = ticketRepository;
        this.orchestrator = orchestrator;
        this.serviceNowClient = serviceNowClient;
        this.sailPointClient = sailPointClient;
    }

    // ═══════════════════════════════════════════
    // CRUD TICKETS
    // ═══════════════════════════════════════════

    @Operation(summary = "Lister les tickets (paginé)")
    @GetMapping
    @Transactional(readOnly = true)
    public PagedResponse<TicketDto> listTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Filtrer par source : AUDIT, SAILPOINT, SERVICENOW, MANUAL")
            @RequestParam(required = false) String source,
            @Parameter(description = "Filtrer par statut : NEW, OPEN, IN_PROGRESS, RESOLVED, CLOSED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtrer par sévérité : CRITICAL, HIGH, MEDIUM, LOW, INFO")
            @RequestParam(required = false) String severity) {

        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        if (source != null) {
            return PagedResponse.from(ticketRepository.findBySource(TicketSource.valueOf(source.toUpperCase()), pageable).map(TicketDto::from));
        }
        if (status != null) {
            return PagedResponse.from(ticketRepository.findByStatus(TicketStatus.valueOf(status.toUpperCase()), pageable).map(TicketDto::from));
        }
        if (severity != null) {
            return PagedResponse.from(ticketRepository.findBySeverity(TicketSeverity.valueOf(severity.toUpperCase()), pageable).map(TicketDto::from));
        }

        return PagedResponse.from(ticketRepository.findAll(pageable).map(TicketDto::from));
    }

    @Operation(summary = "Tickets ouverts (non résolus)")
    @GetMapping("/open")
    @Transactional(readOnly = true)
    public PagedResponse<TicketDto> openTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return PagedResponse.from(ticketRepository.findOpenTickets(pageable).map(TicketDto::from));
    }

    @Operation(summary = "Détail d'un ticket")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<TicketDto> getTicket(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(TicketDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Rechercher des tickets")
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public PagedResponse<TicketDto> searchTickets(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PagedResponse.from(ticketRepository.search(q, pageable).map(TicketDto::from));
    }

    // ═══════════════════════════════════════════
    // ACTIONS SUR LES TICKETS
    // ═══════════════════════════════════════════

    @Operation(summary = "Créer un ticket manuellement")
    @PostMapping
    public ResponseEntity<TicketDto> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        SecurityTicket ticket = new SecurityTicket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setSource(TicketSource.MANUAL);
        ticket.setSeverity(TicketSeverity.valueOf(request.severity().toUpperCase()));
        ticket.setCategory(request.category() != null ? request.category() : "manual");
        ticket.setSiteUrl(request.siteUrl());

        SecurityTicket saved = orchestrator.processIncomingTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketDto.from(saved));
    }

    @Operation(summary = "Résoudre un ticket (+ sync ServiceNow)")
    @PostMapping("/{id}/resolve")
    public ResponseEntity<TicketDto> resolveTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("closeNotes", "Résolu via CyberAudit7E") : "Résolu";
        SecurityTicket ticket = orchestrator.resolveTicket(id, notes);
        return ResponseEntity.ok(TicketDto.from(ticket));
    }

    @Operation(summary = "Synchroniser les tickets non poussés vers ServiceNow")
    @PostMapping("/sync")
    public Map<String, Object> syncTickets() {
        int synced = orchestrator.syncUnsyncedTickets();
        return Map.of("synced", synced, "message", synced + " ticket(s) synchronisé(s) avec ServiceNow");
    }

    // ═══════════════════════════════════════════
    // STATISTIQUES & INTÉGRATIONS
    // ═══════════════════════════════════════════

    @Operation(summary = "Statistiques des tickets")
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> ticketStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", ticketRepository.count());

        Map<String, Long> bySource = new LinkedHashMap<>();
        ticketRepository.countBySourceGroup().forEach(r -> bySource.put(r[0].toString(), (Long) r[1]));
        stats.put("bySource", bySource);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        ticketRepository.countByStatusGroup().forEach(r -> byStatus.put(r[0].toString(), (Long) r[1]));
        stats.put("byStatus", byStatus);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        ticketRepository.countBySeverityGroup().forEach(r -> bySeverity.put(r[0].toString(), (Long) r[1]));
        stats.put("bySeverity", bySeverity);

        stats.put("unsynced", ticketRepository.findUnsyncedWithServiceNow().size());
        stats.put("open", ticketRepository.findOpenTickets().size());

        return stats;
    }

    @Operation(summary = "Statut des intégrations ServiceNow et SailPoint")
    @GetMapping("/integrations")
    public Map<String, Object> integrationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // ServiceNow
        Map<String, Object> snow = new LinkedHashMap<>();
        snow.put("connected", serviceNowClient.testConnection());
        status.put("servicenow", snow);

        // SailPoint
        status.put("sailpoint", sailPointClient.getConnectionInfo());

        return status;
    }

    // ── Request DTOs ──

    public record CreateTicketRequest(
            @NotBlank String title,
            String description,
            @NotBlank String severity,  // CRITICAL, HIGH, MEDIUM, LOW, INFO
            String category,
            String siteUrl
    ) {}
}
