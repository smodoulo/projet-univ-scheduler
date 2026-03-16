package com.univscheduler.model;
public class Equipement {
    private int id; private String nom; private String description;
    private String etat; private String typeEquipement; private boolean disponible; private int salleId;
    public Equipement(){}
    public Equipement(int id,String nom,String description,String etat,String typeEquipement,boolean disponible,int salleId){
        this.id=id;this.nom=nom;this.description=description;this.etat=etat;
        this.typeEquipement=typeEquipement;this.disponible=disponible;this.salleId=salleId;
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getNom(){return nom;} public void setNom(String n){this.nom=n;}
    public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
    public String getEtat(){return etat;} public void setEtat(String e){this.etat=e;}
    public String getTypeEquipement(){return typeEquipement;} public void setTypeEquipement(String t){this.typeEquipement=t;}
    public boolean isDisponible(){return disponible;} public void setDisponible(boolean d){this.disponible=d;}
    public int getSalleId(){return salleId;} public void setSalleId(int s){this.salleId=s;}
    @Override public String toString(){return nom+" ("+typeEquipement+")";}
}
