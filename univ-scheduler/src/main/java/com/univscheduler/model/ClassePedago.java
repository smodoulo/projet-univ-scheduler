package com.univscheduler.model;
public class ClassePedago {
    private int id; private String nom; private String niveau; private int effectif;
    public ClassePedago(){}
    public ClassePedago(int id,String nom,String niveau,int effectif){this.id=id;this.nom=nom;this.niveau=niveau;this.effectif=effectif;}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getNom(){return nom;} public void setNom(String n){this.nom=n;}
    public String getNiveau(){return niveau;} public void setNiveau(String n){this.niveau=n;}
    public int getEffectif(){return effectif;} public void setEffectif(int e){this.effectif=e;}
    @Override public String toString(){return nom+" ("+niveau+")";}
}
