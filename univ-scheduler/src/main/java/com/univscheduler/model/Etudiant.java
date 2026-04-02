package com.univscheduler.model;

public class Etudiant extends Utilisateur {
    private String INE;
    private String niveau;
    private int    classeId;
    private String classeNom;

    public Etudiant() { super(); setRole("ETUDIANT"); }

    public Etudiant(int id, String nom, String prenom, String email, String mdp, String INE, String niveau) {
        super(id, nom, prenom, email, mdp, "ETUDIANT");
        this.INE    = INE;
        this.niveau = niveau;
    }

    public String getINE()             { return INE; }
    public void   setINE(String s)     { this.INE = s; }
    public String getNiveau()          { return niveau; }
    public void   setNiveau(String n)  { this.niveau = n; }
    public int    getClasseId()        { return classeId; }
    public void   setClasseId(int c)   { this.classeId = c; }
    public String getClasseNom()       { return classeNom; }
    public void   setClasseNom(String s){ this.classeNom = s; }
}