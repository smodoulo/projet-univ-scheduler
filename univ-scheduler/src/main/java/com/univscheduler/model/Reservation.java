package com.univscheduler.model;
import java.time.LocalDateTime;
public class Reservation {
    private int id; private String motif; private String statut;
    private LocalDateTime dateReservation;
    private LocalDateTime dateFin;          // ✅ AJOUT : date de fin de réservation
    private int salleId; private String salleNumero;
    private int utilisateurId; private String utilisateurNom;
    public Reservation(){}
    public void valider(){this.statut="VALIDEE";}
    public void annuler(){this.statut="ANNULEE";}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getMotif(){return motif;} public void setMotif(String m){this.motif=m;}
    public String getStatut(){return statut;} public void setStatut(String s){this.statut=s;}
    public LocalDateTime getDateReservation(){return dateReservation;} public void setDateReservation(LocalDateTime d){this.dateReservation=d;}
    public LocalDateTime getDateFin(){return dateFin;} public void setDateFin(LocalDateTime d){this.dateFin=d;}  // ✅ AJOUT
    public int getSalleId(){return salleId;} public void setSalleId(int s){this.salleId=s;}
    public String getSalleNumero(){return salleNumero;} public void setSalleNumero(String s){this.salleNumero=s;}
    public int getUtilisateurId(){return utilisateurId;} public void setUtilisateurId(int u){this.utilisateurId=u;}
    public String getUtilisateurNom(){return utilisateurNom;} public void setUtilisateurNom(String u){this.utilisateurNom=u;}
    @Override public String toString(){return "Réservation "+salleNumero+" - "+motif+" ("+statut+")";}
}