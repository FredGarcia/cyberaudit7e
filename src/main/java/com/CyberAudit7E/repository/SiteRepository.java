package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.domain.enums.Phase7E;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository in-memory pour les Sites.
 *
 * M2 : implémentation ConcurrentHashMap (pas de BDD).
 * M3 : sera remplacé par une interface JpaRepository<Site, Long>.
 *
 * Le pattern est identique : on code contre une interface stable,
 * l'implémentation change sans toucher aux services.
 * C'est l'IoC en action.
 */
@Repository
public class SiteRepository {

    private final Map<Long, Site> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public Site save(Site site) {
        if (site.getId() == null) {
            site.setId(sequence.getAndIncrement());
        }
        store.put(site.getId(), site);
        return site;
    }

    public Optional<Site> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<Site> findByUrl(String url) {
        return store.values().stream()
                .filter(s -> s.getUrl().equals(url))
                .findFirst();
    }

    public List<Site> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<Site> findByCurrentPhase(Phase7E phase) {
        return store.values().stream()
                .filter(s -> s.getCurrentPhase() == phase)
                .toList();
    }

    public boolean existsByUrl(String url) {
        return store.values().stream().anyMatch(s -> s.getUrl().equals(url));
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public long count() {
        return store.size();
    }
}
