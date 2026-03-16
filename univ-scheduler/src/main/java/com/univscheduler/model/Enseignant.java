package com.univscheduler.model;
public class Enseignant extends Utilisateur {
    public Enseignant(){super();setRole("ENSEIGNANT");}
    public Enseignant(int id,String nom,String prenom,String email,String mdp){super(id,nom,prenom,email,mdp,"ENSEIGNANT");}
}
