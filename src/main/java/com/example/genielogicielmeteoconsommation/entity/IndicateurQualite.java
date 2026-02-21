package com.example.genielogicielmeteoconsommation.entity;

/**
 * Indicateur de qualité d'un agrégat journalier.
 */
public enum IndicateurQualite {

    /**
     * Données complètes et fiables.
     */
    OK,

    /**
     * Données partielles (nombre insuffisant de mesures).
     */
    INCOMPLET,

    /**
     * Données suspectes ou inexistantes.
     */
    SUSPECT
}