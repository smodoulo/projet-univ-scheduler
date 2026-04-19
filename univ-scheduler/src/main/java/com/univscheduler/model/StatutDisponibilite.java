package com.univscheduler.model;

/**
 * Statuts possibles d'une demande de disponibilité.
 */
public enum StatutDisponibilite {
    /** Demande soumise par l'enseignant, en attente de traitement. */
    EN_ATTENTE,
    /** Gestionnaire a accepté → cours.creneau_id mis à jour en BDD. */
    ACCEPTE,
    /** Gestionnaire a refusé la demande. */
    REFUSE,
    /** Accepté mais conflit détecté → cours NON modifié. */
    CONFLIT
}