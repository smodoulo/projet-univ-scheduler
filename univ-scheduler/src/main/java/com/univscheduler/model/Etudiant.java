package com.univscheduler.model;
public class Etudiant extends Utilisateur {
    private String INE; private String niveau;
    public Etudiant(){super();setRole("ETUDIANT");}
    public Etudiant(int id,String nom,String prenom,String email,String mdp,String INE,String niveau){
        super(id,nom,prenom,email,mdp,"ETUDIANT"); this.INE=INE; this.niveau=niveau;
    }
    public String getINE(){return INE;} public void setINE(String s){this.INE=s;}
    public String getNiveau(){return niveau;} public void setNiveau(String n){this.niveau=n;}
}
