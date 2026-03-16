package com.univscheduler.model;
import java.util.*;
public class Salle {
    private int id; private String numero; private int capacite;
    private String typeSalle; private boolean disponible;
    private int batimentId; private String batimentNom;
    private List<Equipement> equipements = new ArrayList<>();
    public Salle(){}
    public Salle(int id,String numero,int capacite,String typeSalle,boolean disponible,int batimentId){
        this.id=id;this.numero=numero;this.capacite=capacite;
        this.typeSalle=typeSalle;this.disponible=disponible;this.batimentId=batimentId;
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getNumero(){return numero;} public void setNumero(String n){this.numero=n;}
    public int getCapacite(){return capacite;} public void setCapacite(int c){this.capacite=c;}
    public String getTypeSalle(){return typeSalle;} public void setTypeSalle(String t){this.typeSalle=t;}
    public boolean isDisponible(){return disponible;} public void setDisponible(boolean d){this.disponible=d;}
    public int getBatimentId(){return batimentId;} public void setBatimentId(int b){this.batimentId=b;}
    public String getBatimentNom(){return batimentNom;} public void setBatimentNom(String b){this.batimentNom=b;}
    public List<Equipement> getEquipements(){return equipements;}
    @Override public String toString(){return numero+" - "+typeSalle+" (cap:"+capacite+")";}
}
