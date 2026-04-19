package com.univscheduler.model;

import java.time.LocalDateTime;

/**
 * Représente une demande de changement de créneau soumise par un enseignant.
 *
 * Table SQL : demandes_disponibilite
 *  id              INT PK AUTO_INCREMENT
 *  cours_id        INT FK → cours(id)
 *  enseignant_id   INT FK → utilisateurs(id)
 *  creneau_propose INT FK → creneaux(id)
 *  commentaire     TEXT
 *  statut          VARCHAR(20) DEFAULT 'EN_ATTENTE'
 *  date_demande    TEXT
 */
public class DemandeDisponibilite {

    private int           id;
    private int           coursId;
    private int           enseignantId;
    private int           creneauPropose;   // FK creneaux.id (aussi accessible via getCreneauId())
    private String        commentaire;
    private String        statut;           // Stocké en String : "EN_ATTENTE", "ACCEPTE", "REFUSE", "CONFLIT"
    private LocalDateTime dateDemande;

    // ── Champs enrichis par jointure SQL ─────────────────────────
    private String matiereNom;
    private String classeNom;
    private String enseignantNom;
    private String creneauInfo;   // ex: "Lundi 10h (2h)"

    // ── Constructeur ─────────────────────────────────────────────
    public DemandeDisponibilite() {
        this.statut = "EN_ATTENTE";
    }

    // ════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ════════════════════════════════════════════════════════════════

    public int getId()                       { return id; }
    public void setId(int id)               { this.id = id; }

    public int getCoursId()                  { return coursId; }
    public void setCoursId(int coursId)     { this.coursId = coursId; }

    public int getEnseignantId()             { return enseignantId; }
    public void setEnseignantId(int id)     { this.enseignantId = id; }

    /** Identifiant du créneau proposé (FK creneaux.id). */
    public int getCreneauPropose()           { return creneauPropose; }
    public void setCreneauPropose(int id)   { this.creneauPropose = id; }

    /** Alias de getCreneauPropose() pour compatibilité avec l'ancien code. */
    public int getCreneauId()               { return creneauPropose; }
    public void setCreneauId(int id)        { this.creneauPropose = id; }

    public String getCommentaire()           { return commentaire; }
    public void setCommentaire(String c)    { this.commentaire = c; }

    /**
     * Retourne le statut sous forme de String.
     * Valeurs possibles : "EN_ATTENTE", "ACCEPTE", "REFUSE", "CONFLIT"
     */
    public String getStatut()               { return statut; }
    public void setStatut(String s)         { this.statut = s; }

    /**
     * Setter acceptant un enum {@link StatutDisponibilite} pour compatibilité
     * avec DisponibiliteService et DemandeDisponibiliteDAO.
     */
    public void setStatut(StatutDisponibilite s) {
        this.statut = (s != null) ? s.name() : "EN_ATTENTE";
    }

    /**
     * Setter acceptant une String brute (appelé depuis le DAO au mapping SQL).
     */
    public void setStatutString(String s) {
        this.statut = (s != null) ? s : "EN_ATTENTE";
    }

    public LocalDateTime getDateDemande()    { return dateDemande; }
    public void setDateDemande(LocalDateTime d) { this.dateDemande = d; }

    // ── Champs enrichis ──────────────────────────────────────────
    public String getMatiereNom()            { return matiereNom; }
    public void setMatiereNom(String s)     { this.matiereNom = s; }

    public String getClasseNom()            { return classeNom; }
    public void setClasseNom(String s)      { this.classeNom = s; }

    public String getEnseignantNom()        { return enseignantNom; }
    public void setEnseignantNom(String s)  { this.enseignantNom = s; }

    public String getCreneauInfo()          { return creneauInfo; }
    public void setCreneauInfo(String s)    { this.creneauInfo = s; }

    // ════════════════════════════════════════════════════════════════
    //  Helpers UI
    // ════════════════════════════════════════════════════════════════

    /** Icône + libellé pour affichage dans les TableView. */
    public String getStatutAffichage() {
        if (statut == null) return "—";
        switch (statut) {
            case "EN_ATTENTE": return "⏳ En attente";
            case "ACCEPTE":    return "✅ Acceptée";
            case "REFUSE":     return "❌ Refusée";
            case "CONFLIT":    return "⚠️ Conflit";
            default:           return statut;
        }
    }

    @Override
    public String toString() {
        return (matiereNom != null ? matiereNom : "Cours#" + coursId)
                + " → " + (creneauInfo != null ? creneauInfo : "Créneau#" + creneauPropose);
    }
}