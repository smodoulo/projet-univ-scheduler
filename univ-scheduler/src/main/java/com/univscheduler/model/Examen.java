package com.univscheduler.model;

import java.time.LocalDateTime;

/**
 * Représente un examen, devoir ou contrôle programmé par un enseignant.
 *
 * Cycle de vie :
 *   EN_ATTENTE → VALIDE   (gestionnaire valide)
 *   EN_ATTENTE → REFUSE   (gestionnaire refuse)
 *   VALIDE     → ANNULE   (enseignant ou gestionnaire annule)
 */
public class Examen {

    // ── Types possibles ──────────────────────────────────────────
    public static final String TYPE_EXAMEN   = "EXAMEN";
    public static final String TYPE_DEVOIR   = "DEVOIR";
    public static final String TYPE_CONTROLE = "CONTROLE";

    // ── Statuts ──────────────────────────────────────────────────
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_VALIDE     = "VALIDE";
    public static final String STATUT_REFUSE     = "REFUSE";
    public static final String STATUT_ANNULE     = "ANNULE";

    // ── Champs BDD ───────────────────────────────────────────────
    private int    id;
    private String type;             // EXAMEN | DEVOIR | CONTROLE
    private String titre;            // "Examen final Algo"
    private String description;      // instructions, barème...
    private int    enseignantId;
    private String enseignantNom;    // cache
    private int    classeId;
    private String classeNom;        // cache
    private int    matiereId;
    private String matiereNom;       // cache
    private Integer salleId;         // nullable (devoir à la maison)
    private String  salleNumero;     // cache
    private String  dateExamen;      // "2026-05-12T09:00"
    private int     dureeMinutes;    // durée en minutes
    private String  statut;
    private String  commentaireGestionnaire;
    private LocalDateTime dateCreation;
    private LocalDateTime dateTraitement;

    // ════════════════════════════════════════════════════════════
    //  Constructeurs
    // ════════════════════════════════════════════════════════════
    public Examen() {
        this.statut      = STATUT_EN_ATTENTE;
        this.dateCreation = LocalDateTime.now();
    }

    // ════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ════════════════════════════════════════════════════════════
    public int     getId()                         { return id; }
    public void    setId(int id)                   { this.id = id; }

    public String  getType()                       { return type; }
    public void    setType(String type)            { this.type = type; }

    public String  getTitre()                      { return titre; }
    public void    setTitre(String titre)          { this.titre = titre; }

    public String  getDescription()                { return description; }
    public void    setDescription(String d)        { this.description = d; }

    public int     getEnseignantId()               { return enseignantId; }
    public void    setEnseignantId(int id)         { this.enseignantId = id; }

    public String  getEnseignantNom()              { return enseignantNom; }
    public void    setEnseignantNom(String n)      { this.enseignantNom = n; }

    public int     getClasseId()                   { return classeId; }
    public void    setClasseId(int id)             { this.classeId = id; }

    public String  getClasseNom()                  { return classeNom; }
    public void    setClasseNom(String n)          { this.classeNom = n; }

    public int     getMatiereId()                  { return matiereId; }
    public void    setMatiereId(int id)            { this.matiereId = id; }

    public String  getMatiereNom()                 { return matiereNom; }
    public void    setMatiereNom(String n)         { this.matiereNom = n; }

    public Integer getSalleId()                    { return salleId; }
    public void    setSalleId(Integer id)          { this.salleId = id; }

    public String  getSalleNumero()                { return salleNumero; }
    public void    setSalleNumero(String n)        { this.salleNumero = n; }

    public String  getDateExamen()                 { return dateExamen; }
    public void    setDateExamen(String d)         { this.dateExamen = d; }

    public int     getDureeMinutes()               { return dureeMinutes; }
    public void    setDureeMinutes(int d)          { this.dureeMinutes = d; }

    public String  getStatut()                     { return statut; }
    public void    setStatut(String s)             { this.statut = s; }

    public String  getCommentaireGestionnaire()    { return commentaireGestionnaire; }
    public void    setCommentaireGestionnaire(String c) { this.commentaireGestionnaire = c; }

    public LocalDateTime getDateCreation()         { return dateCreation; }
    public void          setDateCreation(LocalDateTime d) { this.dateCreation = d; }

    public LocalDateTime getDateTraitement()       { return dateTraitement; }
    public void          setDateTraitement(LocalDateTime d) { this.dateTraitement = d; }

    // ════════════════════════════════════════════════════════════
    //  Helpers affichage
    // ════════════════════════════════════════════════════════════

    /** Icône selon le type */
    public String getTypeIcon() {
        if (type == null) return "📝";
        return switch (type) {
            case TYPE_EXAMEN   -> "📋";
            case TYPE_DEVOIR   -> "📝";
            case TYPE_CONTROLE -> "✍️";
            default            -> "📝";
        };
    }

    /** Label lisible du statut avec icône */
    public String getStatutAffichage() {
        if (statut == null) return "—";
        return switch (statut) {
            case STATUT_EN_ATTENTE -> "⏳ En attente";
            case STATUT_VALIDE     -> "✅ Validé";
            case STATUT_REFUSE     -> "❌ Refusé";
            case STATUT_ANNULE     -> "🚫 Annulé";
            default                -> statut;
        };
    }

    /** Résumé court pour les notifications */
    public String getResume() {
        return getTypeIcon() + " " + (titre != null ? titre : type)
                + " | " + (classeNom != null ? classeNom : "")
                + " | " + (matiereNom != null ? matiereNom : "")
                + (dateExamen != null ? " | " + dateExamen.replace("T", " ") : "");
    }

    @Override
    public String toString() {
        return "Examen{id=" + id + ", type=" + type + ", titre=" + titre
                + ", classe=" + classeNom + ", statut=" + statut + "}";
    }
}
