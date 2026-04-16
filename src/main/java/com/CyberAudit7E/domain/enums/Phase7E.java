package com.CyberAudit7E.domain.enums;

/**
 * Axiome 7E — Les 7 phases du cycle cybernétique.
 * Chaque audit traverse ce cycle complet :
 * Évaluer → Élaborer → Exécuter → Examiner → Évoluer → Émettre → Équilibrer
 */
public enum Phase7E {

    EVALUER("Évaluer", "Collecte des métriques du site via les règles d'audit"),
    ELABORER("Élaborer", "Génération du plan de remédiation"),
    EXECUTER("Exécuter", "Application des règles du moteur"),
    EXAMINER("Examiner", "Calcul du score pondéré composite"),
    EVOLUER("Évoluer", "Comparaison avec les audits précédents"),
    EMETTRE("Émettre", "Publication du résultat via événement"),
    EQUILIBRER("Équilibrer", "Ajustement des poids selon le feedback");

    private final String label;
    private final String description;

    Phase7E(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Retourne la phase suivante dans le cycle.
     * Après EQUILIBRER, le cycle recommence à EVALUER (boucle cybernétique).
     */
    public Phase7E next() {
        Phase7E[] phases = values();
        return phases[(this.ordinal() + 1) % phases.length];
    }
}
