package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.AuditReport;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository in-memory pour les rapports d'audit.
 *
 * M2 : ConcurrentHashMap.
 * M3 : JpaRepository<AuditReport, Long>.
 */
@Repository
public class AuditReportRepository {

    private final Map<Long, AuditReport> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public AuditReport save(AuditReport report) {
        if (report.getId() == null) {
            report.setId(sequence.getAndIncrement());
        }
        store.put(report.getId(), report);
        return report;
    }

    public Optional<AuditReport> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<AuditReport> findBySiteIdOrderByAuditedAtDesc(Long siteId) {
        return store.values().stream()
                .filter(r -> r.getSite() != null && siteId.equals(r.getSite().getId()))
                .sorted(Comparator.comparing(AuditReport::getAuditedAt).reversed())
                .toList();
    }

    public Optional<AuditReport> findFirstBySiteIdOrderByAuditedAtDesc(Long siteId) {
        return findBySiteIdOrderByAuditedAtDesc(siteId).stream().findFirst();
    }

    public List<AuditReport> findAll() {
        return new ArrayList<>(store.values());
    }

    public long count() {
        return store.size();
    }
}
