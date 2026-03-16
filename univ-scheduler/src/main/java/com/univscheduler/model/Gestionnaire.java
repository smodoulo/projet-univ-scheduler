package com.univscheduler.model;
public class Gestionnaire extends Utilisateur {
    public Gestionnaire(){super();setRole("GESTIONNAIRE");}
    public Gestionnaire(int id,String nom,String prenom,String email,String mdp){super(id,nom,prenom,email,mdp,"GESTIONNAIRE");}
}
