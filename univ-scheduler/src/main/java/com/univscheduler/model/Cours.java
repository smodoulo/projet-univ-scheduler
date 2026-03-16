package com.univscheduler.model;
import java.time.LocalDate;
public class Cours {
    private int id; private String statut; private LocalDate date;
    private int matiereId; private String matiereNom;
    private int enseignantId; private String enseignantNom;
    private int classeId; private String classeNom;
    private int creneauId; private String creneauInfo;
    private int salleId; private String salleNumero;
    public Cours(){}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getStatut(){return statut;} public void setStatut(String s){this.statut=s;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate d){this.date=d;}
    public int getMatiereId(){return matiereId;} public void setMatiereId(int m){this.matiereId=m;}
    public String getMatiereNom(){return matiereNom;} public void setMatiereNom(String m){this.matiereNom=m;}
    public int getEnseignantId(){return enseignantId;} public void setEnseignantId(int e){this.enseignantId=e;}
    public String getEnseignantNom(){return enseignantNom;} public void setEnseignantNom(String e){this.enseignantNom=e;}
    public int getClasseId(){return classeId;} public void setClasseId(int c){this.classeId=c;}
    public String getClasseNom(){return classeNom;} public void setClasseNom(String c){this.classeNom=c;}
    public int getCreneauId(){return creneauId;} public void setCreneauId(int c){this.creneauId=c;}
    public String getCreneauInfo(){return creneauInfo;} public void setCreneauInfo(String c){this.creneauInfo=c;}
    public int getSalleId(){return salleId;} public void setSalleId(int s){this.salleId=s;}
    public String getSalleNumero(){return salleNumero;} public void setSalleNumero(String s){this.salleNumero=s;}
    @Override public String toString(){return matiereNom+" - "+classeNom;}
}
