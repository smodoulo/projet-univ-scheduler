package com.univscheduler.model;
import java.util.*;
public class Batiment {
    private int id; private String nom; private String localisation; private int nombreEtages;
    private List<Salle> salles = new ArrayList<>();
    public Batiment(){}
    public Batiment(int id,String nom,String localisation,int nombreEtages){
        this.id=id;this.nom=nom;this.localisation=localisation;this.nombreEtages=nombreEtages;
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getNom(){return nom;} public void setNom(String n){this.nom=n;}
    public String getLocalisation(){return localisation;} public void setLocalisation(String l){this.localisation=l;}
    public int getNombreEtages(){return nombreEtages;} public void setNombreEtages(int n){this.nombreEtages=n;}
    public List<Salle> getSalles(){return salles;}
    @Override public String toString(){return nom+" ("+localisation+")";}
}
