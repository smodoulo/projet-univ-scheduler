package com.univscheduler.dao;
import com.univscheduler.model.Cours;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class CoursDAO {
    private DatabaseManager db = DatabaseManager.getInstance();

    private static final String SELECT_SQL =
            "SELECT c.*, m.nom as mat_nom, " +
                    "CONCAT(u.prenom,' ',u.nom) as ens_nom, " +   // ← CONCAT au lieu de ||
                    "cp.nom as cls_nom, " +
                    "CONCAT(cr.jour,' ',cr.heure_debut,'h') as cren_info, " + // ← CONCAT
                    "s.numero as salle_num " +
                    "FROM cours c " +
                    "LEFT JOIN matieres m ON c.matiere_id = m.id " +
                    "LEFT JOIN utilisateurs u ON c.enseignant_id = u.id " +
                    "LEFT JOIN classes_pedago cp ON c.classe_id = cp.id " +
                    "LEFT JOIN creneaux cr ON c.creneau_id = cr.id " +
                    "LEFT JOIN salles s ON c.salle_id = s.id ";

    public List<Cours> findAll() {
        List<Cours> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(SELECT_SQL + "ORDER BY c.date, cr.heure_debut");
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Cours> findByEnseignant(int ensId) {
        List<Cours> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                SELECT_SQL + "WHERE c.enseignant_id=? ORDER BY c.date, cr.heure_debut")) {
            ps.setInt(1, ensId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Cours> findByClasse(int classeId) {
        List<Cours> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                SELECT_SQL + "WHERE c.classe_id=? ORDER BY c.date, cr.heure_debut")) {
            ps.setInt(1, classeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Cours> findBySalle(int salleId) {
        List<Cours> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                SELECT_SQL + "WHERE c.salle_id=? ORDER BY c.date, cr.heure_debut")) {
            ps.setInt(1, salleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Map<Integer, Integer> countBySalle() {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT salle_id, COUNT(*) as cnt FROM cours GROUP BY salle_id");
            while (rs.next()) m.put(rs.getInt("salle_id"), rs.getInt("cnt"));
        } catch (SQLException e) { e.printStackTrace(); }
        return m;
    }

    public Map<String, Integer> countByJour() {
        Map<String, Integer> m = new LinkedHashMap<>();
        String[] jours = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};
        for (String j : jours) m.put(j, 0);
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT cr.jour, COUNT(*) as cnt FROM cours c " +
                            "LEFT JOIN creneaux cr ON c.creneau_id = cr.id GROUP BY cr.jour");
            while (rs.next()) {
                String j = rs.getString("jour");
                if (j != null) m.put(j, rs.getInt("cnt"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return m;
    }

    public Map<String, Integer> countByStatut() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT statut, COUNT(*) as cnt FROM cours GROUP BY statut");
            while (rs.next()) m.put(rs.getString("statut"), rs.getInt("cnt"));
        } catch (SQLException e) { e.printStackTrace(); }
        return m;
    }

    public boolean hasConflitSalle(int salleId, int creneauId, String date, int excludeId) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM cours WHERE salle_id=? AND creneau_id=? AND date=? AND id!=?")) {
            ps.setInt(1, salleId); ps.setInt(2, creneauId);
            ps.setString(3, date); ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0; // ← rs.next() ajouté
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean hasConflitEnseignant(int ensId, int creneauId, String date, int excludeId) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM cours WHERE enseignant_id=? AND creneau_id=? AND date=? AND id!=?")) {
            ps.setInt(1, ensId); ps.setInt(2, creneauId);
            ps.setString(3, date); ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0; // ← rs.next() ajouté
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public void save(Cours c) {
        if (c.getId() == 0) {
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO cours(statut,date,matiere_id,enseignant_id,classe_id,creneau_id,salle_id) " +
                            "VALUES(?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getStatut());
                ps.setString(2, c.getDate().toString());
                ps.setInt(3, c.getMatiereId()); ps.setInt(4, c.getEnseignantId());
                ps.setInt(5, c.getClasseId());  ps.setInt(6, c.getCreneauId());
                ps.setInt(7, c.getSalleId());
                ps.executeUpdate();
                ResultSet k = ps.getGeneratedKeys();
                if (k.next()) c.setId(k.getInt(1));
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cours SET statut=?,date=?,matiere_id=?,enseignant_id=?," +
                            "classe_id=?,creneau_id=?,salle_id=? WHERE id=?")) {
                ps.setString(1, c.getStatut());
                ps.setString(2, c.getDate().toString());
                ps.setInt(3, c.getMatiereId()); ps.setInt(4, c.getEnseignantId());
                ps.setInt(5, c.getClasseId());  ps.setInt(6, c.getCreneauId());
                ps.setInt(7, c.getSalleId());   ps.setInt(8, c.getId());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void delete(int id) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM cours WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int count() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cours");
            if (rs.next()) return rs.getInt(1); // ← rs.next() ajouté
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private Cours map(ResultSet rs) throws SQLException {
        Cours c = new Cours();
        c.setId(rs.getInt("id"));
        c.setStatut(rs.getString("statut"));
        String d = rs.getString("date");
        if (d != null) c.setDate(LocalDate.parse(d));
        c.setMatiereId(rs.getInt("matiere_id"));
        c.setMatiereNom(rs.getString("mat_nom"));
        c.setEnseignantId(rs.getInt("enseignant_id"));
        c.setEnseignantNom(rs.getString("ens_nom"));
        c.setClasseId(rs.getInt("classe_id"));
        c.setClasseNom(rs.getString("cls_nom"));
        c.setCreneauId(rs.getInt("creneau_id"));
        c.setCreneauInfo(rs.getString("cren_info"));
        c.setSalleId(rs.getInt("salle_id"));
        c.setSalleNumero(rs.getString("salle_num"));
        return c;
    }
}