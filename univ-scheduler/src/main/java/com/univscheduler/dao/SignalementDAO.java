package com.univscheduler.dao;

import com.univscheduler.model.Signalement;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SignalementDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    // ========================= INIT TABLE =========================

    /**
     * À appeler une seule fois au démarrage (depuis DatabaseManager.initDatabase).
     * Crée la table signalements si elle n'existe pas.
     */
    public void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS signalements ("
                + "id              INT PRIMARY KEY AUTO_INCREMENT,"
                + "titre           VARCHAR(200) NOT NULL,"
                + "description     TEXT,"
                + "categorie       VARCHAR(30)  DEFAULT 'AUTRE',"   // EQUIPEMENT | SALLE | AUTRE
                + "priorite        VARCHAR(20)  DEFAULT 'NORMALE'," // BASSE | NORMALE | HAUTE | URGENTE
                + "statut          VARCHAR(20)  DEFAULT 'EN_ATTENTE'," // EN_ATTENTE | EN_COURS | RESOLU | FERME
                + "date_signalement TEXT,"
                + "date_resolution  TEXT,"
                + "commentaire_admin TEXT,"
                + "enseignant_id   INT,"
                + "salle_id        INT,"
                + "FOREIGN KEY(enseignant_id) REFERENCES utilisateurs(id),"
                + "FOREIGN KEY(salle_id)      REFERENCES salles(id))";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erreur création table signalements : " + e.getMessage());
        }
    }

    // ========================= SAVE =========================

    public void save(Signalement s) {
        if (s.getId() == 0) {
            insert(s);
        } else {
            update(s);
        }
    }

    private void insert(Signalement s) {
        String sql = "INSERT INTO signalements"
                + "(titre,description,categorie,priorite,statut,date_signalement,enseignant_id,salle_id)"
                + " VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getTitre());
            ps.setString(2, s.getDescription());
            ps.setString(3, s.getCategorie());
            ps.setString(4, s.getPriorite());
            ps.setString(5, s.getStatut());
            ps.setString(6, s.getDateSignalement() != null
                    ? s.getDateSignalement().toString() : LocalDateTime.now().toString());
            ps.setInt(7, s.getEnseignantId());
            if (s.getSalleId() > 0) ps.setInt(8, s.getSalleId());
            else ps.setNull(8, Types.INTEGER);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) s.setId(k.getInt(1));
        } catch (SQLException e) {
            System.err.println("Erreur insertion signalement : " + e.getMessage());
        }
    }

    private void update(Signalement s) {
        String sql = "UPDATE signalements SET titre=?,description=?,categorie=?,priorite=?,"
                + "statut=?,date_resolution=?,commentaire_admin=?,salle_id=? WHERE id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getTitre());
            ps.setString(2, s.getDescription());
            ps.setString(3, s.getCategorie());
            ps.setString(4, s.getPriorite());
            ps.setString(5, s.getStatut());
            ps.setString(6, s.getDateResolution() != null ? s.getDateResolution().toString() : null);
            ps.setString(7, s.getCommentaireAdmin());
            if (s.getSalleId() > 0) ps.setInt(8, s.getSalleId());
            else ps.setNull(8, Types.INTEGER);
            ps.setInt(9, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour signalement : " + e.getMessage());
        }
    }

    // ========================= UPDATE STATUT =========================

    public void updateStatut(int id, String statut, String commentaire) {
        String sql = "UPDATE signalements SET statut=?, commentaire_admin=?, date_resolution=? WHERE id=?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setString(2, commentaire);
            // Si résolu ou fermé, on enregistre la date
            if ("RESOLU".equals(statut) || "FERME".equals(statut)) {
                ps.setString(3, LocalDateTime.now().toString());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur updateStatut signalement : " + e.getMessage());
        }
    }

    // ========================= FIND =========================

    public List<Signalement> findAll() {
        return query("SELECT sg.*, CONCAT(u.prenom,' ',u.nom) AS ens_nom, s.numero AS salle_num "
                        + "FROM signalements sg "
                        + "LEFT JOIN utilisateurs u ON sg.enseignant_id = u.id "
                        + "LEFT JOIN salles s ON sg.salle_id = s.id "
                        + "ORDER BY "
                        + "  CASE sg.priorite WHEN 'URGENTE' THEN 1 WHEN 'HAUTE' THEN 2 "
                        + "                   WHEN 'NORMALE' THEN 3 ELSE 4 END, "
                        + "  sg.date_signalement DESC",
                null);
    }

    public List<Signalement> findByEnseignant(int enseignantId) {
        return query("SELECT sg.*, CONCAT(u.prenom,' ',u.nom) AS ens_nom, s.numero AS salle_num "
                        + "FROM signalements sg "
                        + "LEFT JOIN utilisateurs u ON sg.enseignant_id = u.id "
                        + "LEFT JOIN salles s ON sg.salle_id = s.id "
                        + "WHERE sg.enseignant_id = ? "
                        + "ORDER BY sg.date_signalement DESC",
                enseignantId);
    }

    public List<Signalement> findEnAttente() {
        return query("SELECT sg.*, CONCAT(u.prenom,' ',u.nom) AS ens_nom, s.numero AS salle_num "
                        + "FROM signalements sg "
                        + "LEFT JOIN utilisateurs u ON sg.enseignant_id = u.id "
                        + "LEFT JOIN salles s ON sg.salle_id = s.id "
                        + "WHERE sg.statut = 'EN_ATTENTE' OR sg.statut = 'EN_COURS' "
                        + "ORDER BY "
                        + "  CASE sg.priorite WHEN 'URGENTE' THEN 1 WHEN 'HAUTE' THEN 2 "
                        + "                   WHEN 'NORMALE' THEN 3 ELSE 4 END, "
                        + "  sg.date_signalement DESC",
                null);
    }

    public Map<String, Long> countByStatut() {
        Map<String, Long> result = new LinkedHashMap<>();
        String sql = "SELECT statut, COUNT(*) AS cnt FROM signalements GROUP BY statut";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.put(rs.getString("statut"), rs.getLong("cnt"));
        } catch (SQLException e) {
            System.err.println("Erreur countByStatut : " + e.getMessage());
        }
        return result;
    }

    public long countEnAttente() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM signalements WHERE statut='EN_ATTENTE'")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("Erreur countEnAttente : " + e.getMessage());
        }
        return 0;
    }

    // ========================= PRIVATE HELPERS =========================

    private List<Signalement> query(String sql, Integer param) {
        List<Signalement> list = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps;
            if (param != null) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, param);
            } else {
                ps = conn.prepareStatement(sql);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Erreur query signalement : " + e.getMessage());
        }
        return list;
    }

    private Signalement map(ResultSet rs) throws SQLException {
        Signalement s = new Signalement();
        s.setId(rs.getInt("id"));
        s.setTitre(rs.getString("titre"));
        s.setDescription(rs.getString("description"));
        s.setCategorie(rs.getString("categorie"));
        s.setPriorite(rs.getString("priorite"));
        s.setStatut(rs.getString("statut"));
        s.setCommentaireAdmin(rs.getString("commentaire_admin"));
        s.setEnseignantId(rs.getInt("enseignant_id"));
        s.setEnseignantNom(rs.getString("ens_nom"));
        s.setSalleId(rs.getInt("salle_id"));
        s.setSalleNumero(rs.getString("salle_num"));
        String d1 = rs.getString("date_signalement");
        if (d1 != null) try { s.setDateSignalement(LocalDateTime.parse(d1.replace(" ", "T"))); } catch (Exception ignored) {}
        String d2 = rs.getString("date_resolution");
        if (d2 != null) try { s.setDateResolution(LocalDateTime.parse(d2.replace(" ", "T"))); } catch (Exception ignored) {}
        return s;
    }
}
