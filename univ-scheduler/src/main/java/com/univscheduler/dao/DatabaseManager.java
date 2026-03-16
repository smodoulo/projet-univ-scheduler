package com.univscheduler.dao;
import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:sqlite:univ_scheduler.db";
    public DatabaseManager(){}
    public static DatabaseManager getInstance(){
        if(instance==null) instance=new DatabaseManager();
        return instance;
    }
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    public void initDatabase(){
        try(Connection conn=getConnection(); Statement stmt=conn.createStatement()){
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("CREATE TABLE IF NOT EXISTS utilisateurs (id INTEGER PRIMARY KEY AUTOINCREMENT,nom TEXT NOT NULL,prenom TEXT NOT NULL,email TEXT UNIQUE NOT NULL,mot_de_passe TEXT NOT NULL,role TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS etudiants (id INTEGER PRIMARY KEY,INE TEXT,niveau TEXT,FOREIGN KEY(id) REFERENCES utilisateurs(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS batiments (id INTEGER PRIMARY KEY AUTOINCREMENT,nom TEXT NOT NULL,localisation TEXT,nombre_etages INTEGER DEFAULT 1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS salles (id INTEGER PRIMARY KEY AUTOINCREMENT,numero TEXT NOT NULL,capacite INTEGER DEFAULT 30,type_salle TEXT DEFAULT 'TD',disponible INTEGER DEFAULT 1,batiment_id INTEGER,FOREIGN KEY(batiment_id) REFERENCES batiments(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS equipements (id INTEGER PRIMARY KEY AUTOINCREMENT,nom TEXT NOT NULL,description TEXT,etat TEXT DEFAULT 'BON',type_equipement TEXT,disponible INTEGER DEFAULT 1,salle_id INTEGER,FOREIGN KEY(salle_id) REFERENCES salles(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS matieres (id INTEGER PRIMARY KEY AUTOINCREMENT,nom TEXT NOT NULL,description TEXT,volume_cm INTEGER DEFAULT 0,volume_td INTEGER DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS classes_pedago (id INTEGER PRIMARY KEY AUTOINCREMENT,nom TEXT NOT NULL,niveau TEXT,effectif INTEGER DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS creneaux (id INTEGER PRIMARY KEY AUTOINCREMENT,jour TEXT NOT NULL,heure_debut INTEGER NOT NULL,duree INTEGER DEFAULT 2)");
            stmt.execute("CREATE TABLE IF NOT EXISTS cours (id INTEGER PRIMARY KEY AUTOINCREMENT,statut TEXT DEFAULT 'PLANIFIE',date TEXT,matiere_id INTEGER,enseignant_id INTEGER,classe_id INTEGER,creneau_id INTEGER,salle_id INTEGER,FOREIGN KEY(matiere_id) REFERENCES matieres(id),FOREIGN KEY(enseignant_id) REFERENCES utilisateurs(id),FOREIGN KEY(classe_id) REFERENCES classes_pedago(id),FOREIGN KEY(creneau_id) REFERENCES creneaux(id),FOREIGN KEY(salle_id) REFERENCES salles(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (id INTEGER PRIMARY KEY AUTOINCREMENT,motif TEXT,statut TEXT DEFAULT 'EN_ATTENTE',date_reservation TEXT,salle_id INTEGER,utilisateur_id INTEGER,FOREIGN KEY(salle_id) REFERENCES salles(id),FOREIGN KEY(utilisateur_id) REFERENCES utilisateurs(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT,message TEXT NOT NULL,date_envoi TEXT,lu INTEGER DEFAULT 0,type TEXT DEFAULT 'INFO',utilisateur_id INTEGER,FOREIGN KEY(utilisateur_id) REFERENCES utilisateurs(id))");
            insertDemoData(conn);
            System.out.println("DB initialisée avec succès.");
        } catch(SQLException e){ System.err.println("Erreur DB: "+e.getMessage()); e.printStackTrace(); }
    }

    private void insertDemoData(Connection conn) throws SQLException {
        String[][] users = {
            {"Admin","System","admin@univ.fr","admin123","ADMIN"},
            {"Dupont","Marie","marie.dupont@univ.fr","gest123","GESTIONNAIRE"},
            {"Martin","Jean","jean.martin@univ.fr","ens123","ENSEIGNANT"},
            {"Nguyen","Sophie","sophie.nguyen@univ.fr","ens456","ENSEIGNANT"},
            {"Leroy","Paul","paul.leroy@univ.fr","etu123","ETUDIANT"}
        };
        int[] uids = new int[users.length];
        PreparedStatement chk = conn.prepareStatement("SELECT id FROM utilisateurs WHERE email=?");
        PreparedStatement ins = conn.prepareStatement("INSERT INTO utilisateurs (nom,prenom,email,mot_de_passe,role) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        for(int i=0;i<users.length;i++){
            chk.setString(1,users[i][2]); ResultSet rs=chk.executeQuery();
            if(rs.next()){ uids[i]=rs.getInt("id"); }
            else{
                ins.setString(1,users[i][0]);ins.setString(2,users[i][1]);ins.setString(3,users[i][2]);
                ins.setString(4,users[i][3]);ins.setString(5,users[i][4]);
                ins.executeUpdate(); uids[i]=ins.getGeneratedKeys().getInt(1);
            }
        }
        PreparedStatement ce=conn.prepareStatement("SELECT id FROM etudiants WHERE id=?");
        ce.setInt(1,uids[4]); if(!ce.executeQuery().next()){
            PreparedStatement pe=conn.prepareStatement("INSERT INTO etudiants(id,INE,niveau) VALUES(?,?,?)");
            pe.setInt(1,uids[4]);pe.setString(2,"ETU2024001");pe.setString(3,"L2");pe.executeUpdate();
        }
        int b1=getOrIns(conn,"batiments","nom","Bâtiment A","INSERT INTO batiments(nom,localisation,nombre_etages) VALUES('Bâtiment A','Campus Nord',3)");
        int b2=getOrIns(conn,"batiments","nom","Bâtiment B","INSERT INTO batiments(nom,localisation,nombre_etages) VALUES('Bâtiment B','Campus Centre',4)");
        int s1=getOrInsSalle(conn,"A101",30,"TD",b1);
        int s2=getOrInsSalle(conn,"A102",30,"TD",b1);
        int s3=getOrInsSalle(conn,"A201",60,"TP",b1);
        int s4=getOrInsSalle(conn,"B001",200,"AMPHI",b2);
        int s5=getOrInsSalle(conn,"B101",25,"TD",b2);
        getOrInsEquip(conn,"Vidéoprojecteur","Epson EB-X41","PROJECTEUR",s1);
        getOrInsEquip(conn,"Tableau interactif","SMART Board","TABLEAU",s2);
        getOrInsEquip(conn,"Climatisation","Daikin","CLIM",s4);
        int m1=getOrInsMat(conn,"Programmation Java","POO en Java",30,20);
        int m2=getOrInsMat(conn,"Base de Données","SQL et conception",20,15);
        int m3=getOrInsMat(conn,"Algorithmique","Structures de données",25,20);
        int m4=getOrInsMat(conn,"Mathématiques","Analyse et algèbre",30,15);
        int c1=getOrInsCls(conn,"L2-INFO-A","L2",35);
        int c2=getOrInsCls(conn,"L2-INFO-B","L2",30);
        int cr1=getOrInsCren(conn,"Lundi",8,2);
        int cr2=getOrInsCren(conn,"Lundi",10,2);
        int cr3=getOrInsCren(conn,"Mardi",8,2);
        int cr4=getOrInsCren(conn,"Mardi",10,2);
        int cr5=getOrInsCren(conn,"Mercredi",8,3);
        int cr6=getOrInsCren(conn,"Jeudi",14,2);
        int cr7=getOrInsCren(conn,"Vendredi",8,2);
        int cr8=getOrInsCren(conn,"Vendredi",10,2);
        ResultSet rc=conn.createStatement().executeQuery("SELECT COUNT(*) FROM cours");
        if(rc.getInt(1)==0){
            insCours(conn,"PLANIFIE","2026-03-10",m1,uids[2],c1,cr1,s1);
            insCours(conn,"PLANIFIE","2026-03-10",m2,uids[3],c1,cr2,s2);
            insCours(conn,"PLANIFIE","2026-03-11",m3,uids[2],c2,cr3,s3);
            insCours(conn,"PLANIFIE","2026-03-11",m4,uids[3],c2,cr4,s4);
            insCours(conn,"PLANIFIE","2026-03-12",m1,uids[2],c2,cr5,s5);
            insCours(conn,"TERMINE","2026-03-05",m2,uids[3],c1,cr6,s1);
            insCours(conn,"PLANIFIE","2026-03-13",m3,uids[3],c1,cr7,s2);
            insCours(conn,"PLANIFIE","2026-03-14",m4,uids[2],c2,cr8,s3);
            insCours(conn,"PLANIFIE","2026-03-10",m3,uids[3],c2,cr1,s4);
            insCours(conn,"EN_COURS","2026-03-16",m1,uids[3],c1,cr3,s5);
        }
        // Demo reservations
        ResultSet rr=conn.createStatement().executeQuery("SELECT COUNT(*) FROM reservations");
        if(rr.getInt(1)==0){
            PreparedStatement ir=conn.prepareStatement("INSERT INTO reservations(motif,statut,date_reservation,salle_id,utilisateur_id) VALUES(?,?,?,?,?)");
            ir.setString(1,"Soutenance de projet");ir.setString(2,"EN_ATTENTE");ir.setString(3,"2026-03-20T10:00:00");ir.setInt(4,s1);ir.setInt(5,uids[2]);ir.executeUpdate();
            ir.setString(1,"Réunion pédagogique");ir.setString(2,"VALIDEE");ir.setString(3,"2026-03-18T14:00:00");ir.setInt(4,s2);ir.setInt(5,uids[3]);ir.executeUpdate();
            ir.setString(1,"Examen de rattrapage");ir.setString(2,"EN_ATTENTE");ir.setString(3,"2026-03-22T08:00:00");ir.setInt(4,s4);ir.setInt(5,uids[2]);ir.executeUpdate();
            ir.setString(1,"Conférence étudiants");ir.setString(2,"REFUSEE");ir.setString(3,"2026-03-15T09:00:00");ir.setInt(4,s3);ir.setInt(5,uids[3]);ir.executeUpdate();
        }
        System.out.println("Données démo OK.");
    }

    private int getOrIns(Connection conn,String table,String col,String val,String insertSql) throws SQLException {
        PreparedStatement ps=conn.prepareStatement("SELECT id FROM "+table+" WHERE "+col+"=?");
        ps.setString(1,val); ResultSet rs=ps.executeQuery();
        if(rs.next()) return rs.getInt("id");
        PreparedStatement ins=conn.prepareStatement(insertSql,Statement.RETURN_GENERATED_KEYS);
        ins.executeUpdate(); return ins.getGeneratedKeys().getInt(1);
    }
    private int getOrInsSalle(Connection conn,String num,int cap,String type,int batId) throws SQLException {
        PreparedStatement ps=conn.prepareStatement("SELECT id FROM salles WHERE numero=?");
        ps.setString(1,num); ResultSet rs=ps.executeQuery();
        if(rs.next()) return rs.getInt("id");
        PreparedStatement ins=conn.prepareStatement("INSERT INTO salles(numero,capacite,type_salle,disponible,batiment_id) VALUES(?,?,?,1,?)",Statement.RETURN_GENERATED_KEYS);
        ins.setString(1,num);ins.setInt(2,cap);ins.setString(3,type);ins.setInt(4,batId);
        ins.executeUpdate(); return ins.getGeneratedKeys().getInt(1);
    }
    private void getOrInsEquip(Connection conn,String nom,String desc,String type,int salleId) throws SQLException {
        PreparedStatement ps=conn.prepareStatement("SELECT id FROM equipements WHERE nom=? AND salle_id=?");
        ps.setString(1,nom);ps.setInt(2,salleId); if(ps.executeQuery().next()) return;
        PreparedStatement ins=conn.prepareStatement("INSERT INTO equipements(nom,description,etat,type_equipement,disponible,salle_id) VALUES(?,?,'BON',?,1,?)");
        ins.setString(1,nom);ins.setString(2,desc);ins.setString(3,type);ins.setInt(4,salleId);ins.executeUpdate();
    }
    private int getOrInsMat(Connection conn,String nom,String desc,int cm,int td) throws SQLException {
        PreparedStatement ps=conn.prepareStatement("SELECT id FROM matieres WHERE nom=?");
        ps.setString(1,nom); ResultSet rs=ps.executeQuery();
        if(rs.next()) return rs.getInt("id");
        PreparedStatement ins=conn.prepareStatement("INSERT INTO matieres(nom,description,volume_cm,volume_td) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
        ins.setString(1,nom);ins.setString(2,desc);ins.setInt(3,cm);ins.setInt(4,td);
        ins.executeUpdate(); return ins.getGeneratedKeys().getInt(1);
    }
    private int getOrInsCls(Connection conn,String nom,String niv,int eff) throws SQLException {
        PreparedStatement ps=conn.prepareStatement("SELECT id FROM classes_pedago WHERE nom=?");
        ps.setString(1,nom); ResultSet rs=ps.executeQuery();
        if(rs.next()) return rs.getInt("id");
        PreparedStatement ins=conn.prepareStatement("INSERT INTO classes_pedago(nom,niveau,effectif) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS);
        ins.setString(1,nom);ins.setString(2,niv);ins.setInt(3,eff);
        ins.executeUpdate(); return ins.getGeneratedKeys().getInt(1);
    }
    private int getOrInsCren(Connection conn,String jour,int heure,int duree) throws SQLException {
        PreparedStatement ps=conn.prepareStatement("SELECT id FROM creneaux WHERE jour=? AND heure_debut=? AND duree=?");
        ps.setString(1,jour);ps.setInt(2,heure);ps.setInt(3,duree); ResultSet rs=ps.executeQuery();
        if(rs.next()) return rs.getInt("id");
        PreparedStatement ins=conn.prepareStatement("INSERT INTO creneaux(jour,heure_debut,duree) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS);
        ins.setString(1,jour);ins.setInt(2,heure);ins.setInt(3,duree);
        ins.executeUpdate(); return ins.getGeneratedKeys().getInt(1);
    }
    private void insCours(Connection conn,String statut,String date,int matId,int ensId,int clsId,int crenId,int salleId) throws SQLException {
        PreparedStatement ins=conn.prepareStatement("INSERT INTO cours(statut,date,matiere_id,enseignant_id,classe_id,creneau_id,salle_id) VALUES(?,?,?,?,?,?,?)");
        ins.setString(1,statut);ins.setString(2,date);ins.setInt(3,matId);ins.setInt(4,ensId);
        ins.setInt(5,clsId);ins.setInt(6,crenId);ins.setInt(7,salleId);ins.executeUpdate();
    }
}
