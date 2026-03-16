package com.univscheduler.model;
public abstract class Utilisateur {
    private int id; private String nom; private String prenom;
    private String email; private String motDePasse; private String role;
    public Utilisateur() {}
    public Utilisateur(int id,String nom,String prenom,String email,String motDePasse,String role){
        this.id=id;this.nom=nom;this.prenom=prenom;this.email=email;this.motDePasse=motDePasse;this.role=role;
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getNom(){return nom;} public void setNom(String n){this.nom=n;}
    public String getPrenom(){return prenom;} public void setPrenom(String p){this.prenom=p;}
    public String getEmail(){return email;} public void setEmail(String e){this.email=e;}
    public String getMotDePasse(){return motDePasse;} public void setMotDePasse(String m){this.motDePasse=m;}
    public String getRole(){return role;} public void setRole(String r){this.role=r;}
    public String getNomComplet(){return prenom+" "+nom;}
    @Override public String toString(){return getNomComplet()+" ("+role+")";}
}
