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
            SELECT u.id, u.nom, u.prenom, u.email
            FROM utilisateurs u
            JOIN etudiants e ON e.id = u.id
            JOIN classes_pedago cp ON cp.nom = e.niveau
            WHERE cp.id = ?
            AND u.role = 'ETUDIANT'
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Etudiant u = new Etudiant();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                liste.add(u);
            }
        } catch (Exception e) {
            System.err.println("EtudiantDAO.findByClasseId : " + e.getMessage());
        }
        return liste;
    }
}