package com.univscheduler.dao;

import com.univscheduler.model.Examen;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des examens / devoirs / contrôles.
 */
public class ExamenDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // ════════════════════════════════════════════════════════════
    //  Création de la table (appelée au démarrage)
    // ════════════════════════════════════════════════════════════
    public void createTableIfNeeded() {
        String sql = "CREATE TABLE IF NOT EXISTS examens ("
                + "id                       INT PRIMARY KEY AUTO_INCREMENT,"
                + "type                     VARCHAR(20)  NOT NULL DEFAULT 'EXAMEN',"
                + "titre                    VARCHAR(200) NOT NULL,"
                + "description              TEXT,"
                + "enseignant_id            INT          NOT NULL,"
                + "enseignant_nom           TEXT,"
                + "classe_id                INT          NOT NULL,"
                + "classe_nom               TEXT,"
                + "matiere_id               INT,"
                + "matiere_nom              TEXT,"
                + "salle_id                 INT,"
                + "salle_numero             TEXT,"
                + "date_examen              TEXT         NOT NULL,"
                + "duree_minutes            INT          DEFAULT 120,"
                + "statut                   VARCHAR(20)  DEFAULT 'EN_ATTENTE',"
                + "commentaire_gestionnaire TEXT,"
                + "date_creation            TEXT         NOT NULL,"
                + "date_traitement          TEXT,"
                + "FOREIGN KEY(enseignant_id) REFERENCES utilisateurs(id),"
                + "FOREIGN KEY(classe_id)     REFERENCES classes_pedago(id),"
                + "FOREIGN KEY(matiere_id)    REFERENCES matieres(id),"
                + "FOREIGN KEY(salle_id)      REFERENCES salles(id)"
                + ")";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[ExamenDAO] createTableIfNeeded : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  INSERT — Enseignant crée un examen
    // ════════════════════════════════════════════════════════════
    public void save(Examen e) {
        String sql = "INSERT INTO examens "
                + "(type, titre, description, enseignant_id, enseignant_nom, "
                + " classe_id, classe_nom, matiere_id, matiere_nom, "
                + " salle_id, salle_numero, date_examen, duree_minutes, "
                + " statut, date_creation) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'EN_ATTENTE',?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  e.getType());
            ps.setString(2,  e.getTitre());
            ps.setString(3,  e.getDescription());
            ps.setInt(4,     e.getEnseignantId());
            ps.setString(5,  nvl(e.getEnseignantNom()));
            ps.setInt(6,     e.getClasseId());
            ps.setString(7,  nvl(e.getClasseNom()));
            if (e.getMatiereId() > 0) ps.setInt(8, e.getMatiereId());
            else                      ps.setNull(8, Types.INTEGER);
            ps.setString(9,  nvl(e.getMatiereNom()));
            if (e.getSalleId() != null && e.getSalleId() > 0) ps.setInt(10, e.getSalleId());
            else                                               ps.setNull(10, Types.INTEGER);
            ps.setString(11, nvl(e.getSalleNumero()));
            ps.setString(12, e.getDateExamen());
            ps.setInt(13,    e.getDureeMinutes() > 0 ? e.getDureeMinutes() : 120);
            ps.setString(14, LocalDateTime.now().toString());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) e.setId(keys.getInt(1));
        } catch (SQLException ex) {
            System.err.println("[ExamenDAO] save : " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE statut (gestionnaire valide / refuse)
    // ════════════════════════════════════════════════════════════
    public void updateStatut(int id, String statut, String commentaire) {
        String sql = "UPDATE examens SET statut=?, commentaire_gestionnaire=?, "
                + "date_traitement=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setString(2, commentaire != null ? commentaire : "");
            ps.setString(3, LocalDateTime.now().toString());
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ExamenDAO] updateStatut : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════
    public void delete(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM examens WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ExamenDAO] delete : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LECTURES
    // ════════════════════════════════════════════════════════════

    /** Tous les examens (gestionnaire) */
    public List<Examen> findAll() {
        return query("SELECT * FROM examens ORDER BY date_creation DESC", null);
    }

    /** Examens EN_ATTENTE (gestionnaire) */
    public List<Examen> findEnAttente() {
        return query("SELECT * FROM examens WHERE statut='EN_ATTENTE' ORDER BY date_creation DESC", null);
    }

    /** Examens d'un enseignant précis */
    public List<Examen> findByEnseignant(int enseignantId) {
        return query("SELECT * FROM examens WHERE enseignant_id=? ORDER BY date_creation DESC",
                enseignantId);
    }

    /** Examens d'une classe précise (statut VALIDE) — pour les étudiants */
    public List<Examen> findByClasseValide(int classeId) {
        String sql = "SELECT * FROM examens WHERE classe_id=? AND statut='VALIDE' "
                + "ORDER BY date_examen ASC";
        return query(sql, classeId);
    }

    /** Tous les examens d'une classe (tous statuts) */
    public List<Examen> findByClasse(int classeId) {
        return query("SELECT * FROM examens WHERE classe_id=? ORDER BY date_examen ASC", classeId);
    }

    /** findById */
    public Examen findById(int id) {
        List<Examen> list = query("SELECT * FROM examens WHERE id=?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Nombre d'examens EN_ATTENTE */
    public long countEnAttente() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM examens WHERE statut='EN_ATTENTE'")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("[ExamenDAO] countEnAttente : " + e.getMessage());
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════════
    //  Vérification de conflit salle
    // ════════════════════════════════════════════════════════════
    /**
     * Vérifie si la salle est déjà réservée (cours ou autre examen)
     * sur le créneau [dateExamen, dateExamen + dureeMinutes].
     *
     * On simplifie : on compare uniquement la date (YYYY-MM-DD)
     * et les heures. Pour MySQL, on peut adapter avec DATETIME.
     */
    public boolean hasConflitSalle(int salleId, String dateExamen,
                                   int dureeMinutes, int excludeId) {
        if (salleId <= 0 || dateExamen == null) return false;
        // Conflit avec d'autres examens sur la même salle + même date
        String sql = "SELECT COUNT(*) FROM examens "
                + "WHERE salle_id=? AND date_examen LIKE ? "
                + "AND id != ? AND statut NOT IN ('REFUSE','ANNULE')";
        String datePart = dateExamen.substring(0, 10) + "%"; // YYYY-MM-DD%
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salleId);
            ps.setString(2, datePart);
            ps.setInt(3, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        } catch (SQLException e) {
            System.err.println("[ExamenDAO] hasConflitSalle : " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers privés
    // ════════════════════════════════════════════════════════════
    private List<Examen> query(String sql, Integer param) {
        List<Examen> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[ExamenDAO] query : " + e.getMessage());
        }
        return list;
    }

    private Examen map(ResultSet rs) throws SQLException {
        Examen e = new Examen();
        e.setId(rs.getInt("id"));
        e.setType(str(rs, "type", Examen.TYPE_EXAMEN));
        e.setTitre(str(rs, "titre", "—"));
        e.setDescription(str(rs, "description", ""));
        e.setEnseignantId(rs.getInt("enseignant_id"));
        e.setEnseignantNom(str(rs, "enseignant_nom", "—"));
        e.setClasseId(rs.getInt("classe_id"));
        e.setClasseNom(str(rs, "classe_nom", "—"));

        Object mid = rs.getObject("matiere_id");
        if (mid != null) e.setMatiereId(((Number) mid).intValue());
        e.setMatiereNom(str(rs, "matiere_nom", "—"));

        Object sid = rs.getObject("salle_id");
        if (sid != null) e.setSalleId(((Number) sid).intValue());
        e.setSalleNumero(str(rs, "salle_numero", "—"));

        e.setDateExamen(str(rs, "date_examen", ""));
        e.setDureeMinutes(rs.getInt("duree_minutes"));
        e.setStatut(str(rs, "statut", Examen.STATUT_EN_ATTENTE));
        e.setCommentaireGestionnaire(str(rs, "commentaire_gestionnaire", ""));

        String dc = rs.getString("date_creation");
        if (dc != null && !dc.isEmpty()) {
            try { e.setDateCreation(LocalDateTime.parse(dc.replace(" ", "T"))); }
            catch (Exception ignored) {}
        }
        String dt = rs.getString("date_traitement");
        if (dt != null && !dt.isEmpty()) {
            try { e.setDateTraitement(LocalDateTime.parse(dt.replace(" ", "T"))); }
            catch (Exception ignored) {}
        }
        return e;
    }

    private String str(ResultSet rs, String col, String def) {
        try { String v = rs.getString(col); return (v != null && !v.isBlank()) ? v : def; }
        catch (SQLException e) { return def; }
    }

    private String nvl(String s) { return s != null ? s : ""; }
}