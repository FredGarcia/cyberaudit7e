package com.cyberaudit7e.domain.rule;

import org.jsoup.nodes.Document;

import java.util.Optional;

/**
 * Contexte d'audit transmis à chaque règle.
 *
 * M4 : remplace la simple String url de M2/M3.
 * Encapsule l'URL + le Document Jsoup parsé (optionnel si le fetch échoue).
 *
 * Les règles qui ont besoin du DOM utilisent getDocument().
 * Les règles qui n'ont besoin que de l'URL utilisent getUrl().
 * Si le DOM est absent, les règles retournent un score partiel ou un échec.
 *
 * @param url      URL du site audité (toujours présente)
 * @param document Document Jsoup parsé (absent si erreur réseau)
 */
public record AuditContext(
        String url,
        Optional<Document> document
) {

    /**
     * Factory pour un contexte avec un document parsé.
     */
    public static AuditContext withDocument(String url, Document doc) {
        return new AuditContext(url, Optional.ofNullable(doc));
    }

    /**
     * Factory pour un contexte sans document (erreur réseau).
     */
    public static AuditContext withoutDocument(String url) {
        return new AuditContext(url, Optional.empty());
    }

    /**
     * @return true si le DOM est disponible pour l'analyse
     */
    public boolean hasDocument() {
        return document.isPresent();
    }
}
