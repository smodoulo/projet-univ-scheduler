package com.univscheduler.dao;

import com.univscheduler.model.Etudiant;
import com.univscheduler.model.Utilisateur;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtudiantDAO {

    private DatabaseManager db = DatabaseManager.getInstance();

    public List<Utilisateur> findByClasseId(int classeId) {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = """
            SELECT u.id, u.nom, u.prenom, u.email,
                   e.INE, e.niveau, e.classe_id, cp.nom AS classe_nom
            FROM utilisateurs u
            JOIN etudiants e       ON e.id  = u.id
            JOIN classes_pedago cp ON cp.id = e.classe_id
            WHERE e.classe_id = ?
              AND u.role = 'ETUDIANT'
            ORDER BY u.nom, u.prenom
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Etudiant etu = new Etudiant();
                etu.setId(rs.getInt("id"));
                etu.setNom(rs.getString("nom"));
                etu.setPrenom(rs.getString("prenom"));
                etu.setEmail(rs.getString("email"));
                etu.setINE(rs.getString("INE"));
                etu.setNiveau(rs.getString("niveau"));
                etu.setClasseId(rs.getInt("classe_id"));
                etu.setClasseNom(rs.getString("classe_nom"));
                liste.add(etu);
            }
        } catch (Exception e) {
            System.err.println("EtudiantDAO.findByClasseId : " + e.getMessage());
        }
        return liste;
    }

    public Etudiant findById(int id) {
        String sql = """
            SELECT u.id, u.nom, u.prenom, u.email, u.mot_de_passe,
                   e.INE, e.niveau, e.classe_id, cp.nom AS classe_nom
            FROM utilisateurs u
            JOIN etudiants e       ON e.id  = u.id
            JOIN classes_pedago cp ON cp.id = e.classe_id
            WHERE u.id = ?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Etudiant etu = new Etudiant();
                etu.setId(rs.getInt("id"));
                etu.setNom(rs.getString("nom"));
                etu.setPrenom(rs.getString("prenom"));
                etu.setEmail(rs.getString("email"));
                etu.setMotDePasse(rs.getString("mot_de_passe"));
                etu.setINE(rs.getString("INE"));
                etu.setNiveau(rs.getString("niveau"));
                etu.setClasseId(rs.getInt("classe_id"));
                etu.setClasseNom(rs.getString("classe_nom"));
                return etu;
            }
        } catch (Exception e) {
            System.err.println("EtudiantDAO.findById : " + e.getMessage());
        }
        return null;
    }
}