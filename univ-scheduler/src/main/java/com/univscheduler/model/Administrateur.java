package com.univscheduler.model;
public class Administrateur extends Utilisateur {
    public Administrateur(){super();setRole("ADMIN");}
    public Administrateur(int id,String nom,String prenom,String email,String mdp){super(id,nom,prenom,email,mdp,"ADMIN");}
}
