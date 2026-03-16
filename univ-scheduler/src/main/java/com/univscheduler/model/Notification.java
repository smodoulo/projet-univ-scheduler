package com.univscheduler.model;
import java.time.LocalDateTime;
public class Notification {
    private int id; private String message; private LocalDateTime dateEnvoi;
    private boolean lu; private String type; private int utilisateurId;
    public Notification(){}
    public Notification(int id,String message,LocalDateTime dateEnvoi,boolean lu,String type,int utilisateurId){
        this.id=id;this.message=message;this.dateEnvoi=dateEnvoi;this.lu=lu;this.type=type;this.utilisateurId=utilisateurId;
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getMessage(){return message;} public void setMessage(String m){this.message=m;}
    public LocalDateTime getDateEnvoi(){return dateEnvoi;} public void setDateEnvoi(LocalDateTime d){this.dateEnvoi=d;}
    public boolean isLu(){return lu;} public void setLu(boolean l){this.lu=l;}
    public String getType(){return type;} public void setType(String t){this.type=t;}
    public int getUtilisateurId(){return utilisateurId;} public void setUtilisateurId(int u){this.utilisateurId=u;}
}
