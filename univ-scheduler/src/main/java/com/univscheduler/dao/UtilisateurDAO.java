package com.univscheduler.dao;

import com.univscheduler.model.*;
import java.sql.*;
import java.util.*;

public class UtilisateurDAO {
    private DatabaseManager db = DatabaseManager.getInstance();

    public Utilisateur authentifier(String email, String motDePasse) {
        String sql = "SELECT * FROM utilisateurs WHERE email = ? AND mot_de_passe = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, motDePasse);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                int    id   = rs.getInt("id");

                if ("ETUDIANT".equals(role)) {
                    return chargerEtudiant(id, rs);
                }

                // ✅ Utiliser la bonne sous-classe selon le rôle
                Utilisateur u;
                switch (role) {
                    case "ADMIN":        u = new Administrateur(); break;
                    case "GESTIONNAIRE": u = new Gestionnaire();   break;
                    case "ENSEIGNANT":   u = new Enseignant();     break;
                    default:             u = new Administrateur(); break;
                }
                u.setId(id);
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setMotDePasse(rs.getString("mot_de_passe"));
                u.setRole(role);
                return u;
            }
        } catch (Exception e) {
            System.err.println("authentifier : " + e.getMessage());
        }
        return null;
    }

    // ✅ Charge un Etudiant complet avec niveau + classe correcte
    private Etudiant chargerEtudiant(int id, ResultSet rsUtil) throws SQLException {
        Etudiant etu = new Etudiant();
        etu.setId(id);
        etu.setNom(rsUtil.getString("nom"));
        etu.setPrenom(rsUtil.getString("prenom"));
        etu.setEmail(rsUtil.getString("email"));
        etu.setMotDePasse(rsUtil.getString("mot_de_passe"));
        etu.setRole("ETUDIANT");

        String sql2 = """
            SELECT e.INE, e.niveau, e.classe_id, cp.nom AS classe_nom
            FROM etudiants e
            JOIN classes_pedago cp ON cp.id = e.classe_id
            WHERE e.id = ?
        """;
        try (Connection conn2 = db.getConnection();
             PreparedStatement ps2 = conn2.prepareStatement(sql2)) {
            ps2.setInt(1, id);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                etu.setINE(rs2.getString("INE"));
                etu.setNiveau(rs2.getString("niveau"));        // "L3"
                etu.setClasseId(rs2.getInt("classe_id"));      // 12
                etu.setClasseNom(rs2.getString("classe_nom")); // "CHIMIE-L3"
            }
        }
        return etu;
    }

    public List<Utilisateur> findAll() {
        List<Utilisateur> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM utilisateurs ORDER BY role, nom");
            while (rs.next()) list.add(buildUser(rs, conn));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<Enseignant> findAllEnseignants() {
        List<Enseignant> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM utilisateurs WHERE role='ENSEIGNANT' ORDER BY nom")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Enseignant(
                    rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"),
                    rs.getString("email"), rs.getString("mot_de_passe")));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public void save(Utilisateur u) {
        if (u.getId() == 0) insert(u);
        else update(u);
    }

    private void insert(Utilisateur u) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO utilisateurs(nom,prenom,email,mot_de_passe,role) VALUES(?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getMotDePasse());
            ps.setString(5, u.getRole());
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) u.setId(k.getInt(1));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void update(Utilisateur u) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE utilisateurs SET nom=?,prenom=?,email=?,mot_de_passe=?,role=? WHERE id=?")) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getMotDePasse());
            ps.setString(5, u.getRole());
            ps.setInt(6, u.getId());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM utilisateurs WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int countByRole(String role) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM utilisateurs WHERE role=?")) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public Map<String, Integer> countByAllRoles() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("ADMIN",        countByRole("ADMIN"));
        map.put("GESTIONNAIRE", countByRole("GESTIONNAIRE"));
        map.put("ENSEIGNANT",   countByRole("ENSEIGNANT"));
        map.put("ETUDIANT",     countByRole("ETUDIANT"));
        return map;
    }

    private Utilisateur buildUser(ResultSet rs, Connection conn) throws SQLException {
        int    id     = rs.getInt("id");
        String nom    = rs.getString("nom");
        String prenom = rs.getString("prenom");
        String email  = rs.getString("email");
        String mdp    = rs.getString("mot_de_passe");
        String role   = rs.getString("role");

        switch (role) {
            case "ADMIN":        return new Administrateur(id, nom, prenom, email, mdp);
            case "GESTIONNAIRE": return new Gestionnaire(id, nom, prenom, email, mdp);
            case "ENSEIGNANT":   return new Enseignant(id, nom, prenom, email, mdp);
            case "ETUDIANT": {
                Etudiant etu = new Etudiant(id, nom, prenom, email, mdp, "", "");
                try (PreparedStatement ps2 = conn.prepareStatement("""
                        SELECT e.INE, e.niveau, e.classe_id, cp.nom AS classe_nom
                        FROM etudiants e
                        JOIN classes_pedago cp ON cp.id = e.classe_id
                        WHERE e.id = ?
                    """)) {
                    ps2.setInt(1, id);
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        etu.setINE(rs2.getString("INE")       != null ? rs2.getString("INE")       : "");
                        etu.setNiveau(rs2.getString("niveau") != null ? rs2.getString("niveau")     : "");
                        etu.setClasseId(rs2.getInt("classe_id"));
                        etu.setClasseNom(rs2.getString("classe_nom") != null ? rs2.getString("classe_nom") : "");
                    }
                } catch (Exception e) { e.printStackTrace(); }
                return etu;
            }
            default:
                System.err.println("Role inconnu : '" + role + "' pour id=" + id);
                return new Administrateur(id, nom, prenom, email, mdp);
        }
    }
}