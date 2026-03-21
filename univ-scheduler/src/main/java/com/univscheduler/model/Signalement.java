package com.univscheduler.model;
import java.time.LocalDateTime;

public class Signalement {
    private int id;
    private String titre;
    private String description;
    private String categorie;   // EQUIPEMENT, SALLE, AUTRE
    private String priorite;    // BASSE, NORMALE, HAUTE, URGENTE
    private String statut;      // EN_ATTENTE, EN_COURS, RESOLU, FERME
    private LocalDateTime dateSignalement;
    private LocalDateTime dateResolution;
    private int enseignantId;
    private String enseignantNom;
    private int salleId;
    private String salleNumero;
    private String commentaireAdmin;

    public Signalement() {
        this.statut = "EN_ATTENTE";
        this.priorite = "NORMALE";
        this.dateSignalement = LocalDateTime.now();
    }

    // ---- Getters & Setters ----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(LocalDateTime d) { this.dateSignalement = d; }

    public LocalDateTime getDateResolution() { return dateResolution; }
    public void setDateResolution(LocalDateTime d) { this.dateResolution = d; }

    public int getEnseignantId() { return enseignantId; }
    public void setEnseignantId(int enseignantId) { this.enseignantId = enseignantId; }

    public String getEnseignantNom() { return enseignantNom; }
    public void setEnseignantNom(String enseignantNom) { this.enseignantNom = enseignantNom; }

    public int getSalleId() { return salleId; }
    public void setSalleId(int salleId) { this.salleId = salleId; }

    public String getSalleNumero() { return salleNumero; }
    public void setSalleNumero(String salleNumero) { this.salleNumero = salleNumero; }

    public String getCommentaireAdmin() { return commentaireAdmin; }
    public void setCommentaireAdmin(String commentaireAdmin) { this.commentaireAdmin = commentaireAdmin; }

    /** Icône selon la priorité */
    public String getPrioriteIcon() {
        switch (priorite != null ? priorite : "") {
            case "URGENTE": return "🔴";
            case "HAUTE":   return "🟠";
            case "NORMALE": return "🟡";
            default:        return "🟢";
        }
    }

    /** Icône selon la catégorie */
    public String getCategorieIcon() {
        switch (categorie != null ? categorie : "") {
            case "EQUIPEMENT": return "🔧";
            case "SALLE":      return "🏫";
            default:           return "📋";
        }
    }

    @Override
    public String toString() {
        return getPrioriteIcon() + " " + titre + " (" + statut + ")";
    }
}
