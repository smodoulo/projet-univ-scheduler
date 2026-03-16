package com.univscheduler.model;
public class Matiere {
    private int id; private String nom; private String description; private int volumeCm; private int volumeTd;
    public Matiere(){}
    public Matiere(int id,String nom,String description,int volumeCm,int volumeTd){this.id=id;this.nom=nom;this.description=description;this.volumeCm=volumeCm;this.volumeTd=volumeTd;}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getNom(){return nom;} public void setNom(String n){this.nom=n;}
    public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
    public int getVolumeCm(){return volumeCm;} public void setVolumeCm(int v){this.volumeCm=v;}
    public int getVolumeTd(){return volumeTd;} public void setVolumeTd(int v){this.volumeTd=v;}
    @Override public String toString(){return nom;}
}
