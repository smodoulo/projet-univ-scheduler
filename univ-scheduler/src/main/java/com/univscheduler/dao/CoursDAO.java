package com.univscheduler.dao;

import com.univscheduler.model.Cours;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class CoursDAO {

    private Connection getConn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // ════════════════════════════════════════════════════════════════
    //  REQUÊTES PRINCIPALES
    // ════════════════════════════════════════════════════════════════

    public List<Cours> findAll() {
        return query("", null, null);
    }

    public List<Cours> findByEnseignant(int enseignantId) {
        return query("WHERE c.enseignant_id = ?", enseignantId, null);
    }

    public List<Cours> findByClasse(int classeId) {
        return query("WHERE c.classe_id = ?", classeId, null);
    }

    public List<Cours> findBySalle(int salleId) {
        return query("WHERE c.salle_id = ?", salleId, null);
    }

    public Cours findById(int id) {
        List<Cours> list = query("WHERE c.id = ?", id, null);
        return list.isEmpty() ? null : list.get(0);
    }

    // ════════════════════════════════════════════════════════════════
    //  COMPTAGES
    // ════════════════════════════════════════════════════════════════

    public int count() {
        String sql = "SELECT COUNT(*) FROM cours";
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[CoursDAO] count : " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> countByJour() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String[] jours = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};
        for (String j : jours) result.put(j, 0);
        String sql = "SELECT cr.jour, COUNT(*) AS nb"
                + " FROM cours c JOIN creneaux cr ON c.creneau_id = cr.id GROUP BY cr.jour";
        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.put(rs.getString("jour"), rs.getInt("nb"));
        } catch (SQLException e) {
            System.err.println("[CoursDAO] countByJour : " + e.getMessage());
        }
        return result;
    }

    public Map<String, Integer> countByStatut() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT statut, COUNT(*) AS nb FROM cours GROUP BY statut";
        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.put(rs.getString("statut"), rs.getInt("nb"));
        } catch (SQLException e) {
            System.err.println("[CoursDAO] countByStatut : " + e.getMessage());
        }
        return result;
    }

    public Map<String, Integer> countByStatutForEnseignant(int enseignantId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT statut, COUNT(*) as nb FROM cours WHERE enseignant_id = ? GROUP BY statut";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enseignantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.put(rs.getString("statut"), rs.getInt("nb"));
        } catch (SQLException e) {
            System.err.println("[CoursDAO] countByStatutForEnseignant: " + e.getMessage());
        }
        return result;
    }

    public Map<Integer, Integer> countBySalle() {
        Map<Integer, Integer> result = new HashMap<>();
        String sql = "SELECT salle_id, COUNT(*) as nb FROM cours WHERE salle_id IS NOT NULL GROUP BY salle_id";
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.put(rs.getInt("salle_id"), rs.getInt("nb"));
        } catch (SQLException e) {
            System.err.println("[CoursDAO] countBySalle: " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  VÉRIFICATIONS DE CONFLIT
    // ════════════════════════════════════════════════════════════════

    public boolean hasConflitSalle(int salleId, int creneauId, String date, int exclureId) {
        String sql = "SELECT COUNT(*) FROM cours WHERE salle_id=? AND creneau_id=? AND date=? AND id<>?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salleId); ps.setInt(2, creneauId);
            ps.setString(3, date); ps.setInt(4, exclureId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[CoursDAO] hasConflitSalle : " + e.getMessage());
        }
        return false;
    }

    public boolean hasConflitEnseignant(int enseignantId, int creneauId, String date, int exclureId) {
        String sql = "SELECT COUNT(*) FROM cours WHERE enseignant_id=? AND creneau_id=? AND date=? AND id<>?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enseignantId); ps.setInt(2, creneauId);
            ps.setString(3, date); ps.setInt(4, exclureId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[CoursDAO] hasConflitEnseignant : " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════
    //  OPÉRATIONS D'ÉCRITURE
    // ════════════════════════════════════════════════════════════════

    public void save(Cours c) {
        if (c.getId() == 0) {
            String sql = "INSERT INTO cours(statut, date, matiere_id, enseignant_id, classe_id, creneau_id, salle_id)"
                    + " VALUES(?,?,?,?,?,?,?)";
            try (Connection conn = getConn();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getStatut() != null ? c.getStatut() : "PLANIFIE");
                ps.setString(2, c.getDate() != null ? c.getDate().toString() : null);
                ps.setInt(3, c.getMatiereId()); ps.setInt(4, c.getEnseignantId());
                ps.setInt(5, c.getClasseId());  ps.setInt(6, c.getCreneauId());
                ps.setInt(7, c.getSalleId());
                ps.executeUpdate();
                ResultSet gk = ps.getGeneratedKeys();
                if (gk.next()) c.setId(gk.getInt(1));
            } catch (SQLException e) {
                System.err.println("[CoursDAO] save INSERT : " + e.getMessage());
            }
        } else {
            String sql = "UPDATE cours SET statut=?, date=?, matiere_id=?, enseignant_id=?,"
                    + " classe_id=?, creneau_id=?, salle_id=? WHERE id=?";
            try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getStatut());
                ps.setString(2, c.getDate() != null ? c.getDate().toString() : null);
                ps.setInt(3, c.getMatiereId()); ps.setInt(4, c.getEnseignantId());
                ps.setInt(5, c.getClasseId());  ps.setInt(6, c.getCreneauId());
                ps.setInt(7, c.getSalleId());   ps.setInt(8, c.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[CoursDAO] save UPDATE : " + e.getMessage());
            }
        }
    }

    public void updateStatut(int id, String statut) {
        String sql = "UPDATE cours SET statut = ? WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut); ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CoursDAO] updateStatut : " + e.getMessage());
        }
    }

    /**
     * ✅ Méthode requise par DisponibiliteService.accepter()
     * Met à jour le créneau d'un cours quand une demande de disponibilité est acceptée.
     */
    public void updateCreneau(int coursId, int creneauId) {
        String sql = "UPDATE cours SET creneau_id = ? WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, creneauId); ps.setInt(2, coursId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CoursDAO] updateCreneau : " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM cours WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CoursDAO] delete : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MÉTHODES INTERNES
    // ════════════════════════════════════════════════════════════════

    private List<Cours> query(String whereClause, Integer param1, Integer param2) {
        List<Cours> result = new ArrayList<>();
        String sql = "SELECT c.id, c.statut, c.date,"
                + " m.id AS mat_id,  m.nom  AS mat_nom,"
                + " u.id AS ens_id,  CONCAT(u.prenom,' ',u.nom) AS ens_nom,"
                + " cp.id AS cls_id, cp.nom AS cls_nom,"
                + " cr.id AS cren_id,"
                + " CONCAT(cr.jour,' ',cr.heure_debut,'h (',cr.duree,'h)') AS cren_info,"
                + " s.id  AS sal_id, s.numero AS sal_num"
                + " FROM cours c"
                + " LEFT JOIN matieres       m  ON c.matiere_id    = m.id"
                + " LEFT JOIN utilisateurs   u  ON c.enseignant_id = u.id"
                + " LEFT JOIN classes_pedago cp ON c.classe_id     = cp.id"
                + " LEFT JOIN creneaux       cr ON c.creneau_id    = cr.id"
                + " LEFT JOIN salles         s  ON c.salle_id      = s.id"
                + " " + whereClause
                + " ORDER BY c.date DESC, cr.heure_debut ASC";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param1 != null) ps.setInt(1, param1);
            if (param2 != null) ps.setInt(2, param2);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CoursDAO] query : " + e.getMessage());
        }
        return result;
    }

    private Cours mapRow(ResultSet rs) throws SQLException {
        Cours c = new Cours();
        c.setId(rs.getInt("id"));
        c.setStatut(rs.getString("statut"));
        String d = rs.getString("date");
        if (d != null && !d.isBlank()) {
            try { c.setDate(LocalDate.parse(d.substring(0, 10))); }
            catch (Exception ignored) {}
        }
        c.setMatiereId(rs.getInt("mat_id"));
        c.setMatiereNom(rs.getString("mat_nom"));
        c.setEnseignantId(rs.getInt("ens_id"));
        c.setEnseignantNom(rs.getString("ens_nom"));
        c.setClasseId(rs.getInt("cls_id"));
        c.setClasseNom(rs.getString("cls_nom"));
        c.setCreneauId(rs.getInt("cren_id"));
        c.setCreneauInfo(rs.getString("cren_info"));
        c.setSalleId(rs.getInt("sal_id"));
        c.setSalleNumero(rs.getString("sal_num"));
        return c;
    }
}