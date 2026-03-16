package com.univscheduler.model;
public class Creneau {
    private int id; private String jour; private int heureDebut; private int duree;
    public Creneau(){}
    public Creneau(int id,String jour,int heureDebut,int duree){this.id=id;this.jour=jour;this.heureDebut=heureDebut;this.duree=duree;}
    public String getHeureFormatee(){return String.format("%02d:00 - %02d:00",heureDebut,heureDebut+duree);}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getJour(){return jour;} public void setJour(String j){this.jour=j;}
    public int getHeureDebut(){return heureDebut;} public void setHeureDebut(int h){this.heureDebut=h;}
    public int getDuree(){return duree;} public void setDuree(int d){this.duree=d;}
    @Override public String toString(){return jour+" "+getHeureFormatee();}
}
