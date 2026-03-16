package com.univscheduler.dao;
import com.univscheduler.model.*;
import java.sql.*;
import java.util.*;

public class UtilisateurDAO {
    private DatabaseManager db = DatabaseManager.getInstance();

    public Utilisateur authentifier(String email, String motDePasse) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "SELECT * FROM utilisateurs WHERE email=? AND mot_de_passe=?")){
            ps.setString(1,email); ps.setString(2,motDePasse);
            ResultSet rs=ps.executeQuery();
            if(rs.next()) return buildUser(rs,conn);
        } catch(Exception e){ e.printStackTrace(); }
        return null;
    }

    public List<Utilisateur> findAll() {
        List<Utilisateur> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT * FROM utilisateurs ORDER BY role,nom");
            while(rs.next()) list.add(buildUser(rs,conn));
        } catch(Exception e){ e.printStackTrace(); }
        return list;
    }

    public List<Enseignant> findAllEnseignants() {
        List<Enseignant> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "SELECT * FROM utilisateurs WHERE role='ENSEIGNANT' ORDER BY nom")){
            ResultSet rs=ps.executeQuery();
            while(rs.next()) list.add(new Enseignant(rs.getInt("id"),rs.getString("nom"),rs.getString("prenom"),rs.getString("email"),rs.getString("mot_de_passe")));
        } catch(Exception e){ e.printStackTrace(); }
        return list;
    }

    public void save(Utilisateur u) { if(u.getId()==0) insert(u); else update(u); }

    private void insert(Utilisateur u) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "INSERT INTO utilisateurs(nom,prenom,email,mot_de_passe,role) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,u.getNom());ps.setString(2,u.getPrenom());ps.setString(3,u.getEmail());
            ps.setString(4,u.getMotDePasse());ps.setString(5,u.getRole());
            ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) u.setId(k.getInt(1));
        } catch(Exception e){ e.printStackTrace(); }
    }

    private void update(Utilisateur u) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "UPDATE utilisateurs SET nom=?,prenom=?,email=?,mot_de_passe=?,role=? WHERE id=?")){
            ps.setString(1,u.getNom());ps.setString(2,u.getPrenom());ps.setString(3,u.getEmail());
            ps.setString(4,u.getMotDePasse());ps.setString(5,u.getRole());ps.setInt(6,u.getId());
            ps.executeUpdate();
        } catch(Exception e){ e.printStackTrace(); }
    }

    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM utilisateurs WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(Exception e){ e.printStackTrace(); }
    }

    public int countByRole(String role) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "SELECT COUNT(*) FROM utilisateurs WHERE role=?")){
            ps.setString(1,role); ResultSet rs=ps.executeQuery(); return rs.getInt(1);
        } catch(Exception e){ return 0; }
    }

    public Map<String,Integer> countByAllRoles() {
        Map<String,Integer> map=new LinkedHashMap<>();
        map.put("ADMIN",countByRole("ADMIN"));
        map.put("GESTIONNAIRE",countByRole("GESTIONNAIRE"));
        map.put("ENSEIGNANT",countByRole("ENSEIGNANT"));
        map.put("ETUDIANT",countByRole("ETUDIANT"));
        return map;
    }

    private Utilisateur buildUser(ResultSet rs, Connection conn) throws SQLException {
        int id=rs.getInt("id"); String nom=rs.getString("nom"); String prenom=rs.getString("prenom");
        String email=rs.getString("email"); String mdp=rs.getString("mot_de_passe"); String role=rs.getString("role");
        switch(role){
            case "ADMIN": return new Administrateur(id,nom,prenom,email,mdp);
            case "GESTIONNAIRE": return new Gestionnaire(id,nom,prenom,email,mdp);
            case "ENSEIGNANT": return new Enseignant(id,nom,prenom,email,mdp);
            case "ETUDIANT":
                String ine=""; String niveau="L1";
                try(PreparedStatement ps2=conn.prepareStatement("SELECT * FROM etudiants WHERE id=?")){
                    ps2.setInt(1,id); ResultSet rs2=ps2.executeQuery();
                    if(rs2.next()){ ine=rs2.getString("INE")!=null?rs2.getString("INE"):""; niveau=rs2.getString("niveau")!=null?rs2.getString("niveau"):"L1"; }
                } catch(Exception e){}
                return new Etudiant(id,nom,prenom,email,mdp,ine,niveau);
            default: return new Administrateur(id,nom,prenom,email,mdp);
        }
    }
}
