package com.univscheduler.dao;

import com.univscheduler.model.DemandeDisponibilite;
import com.univscheduler.model.StatutDisponibilite;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table demandes_disponibilite.
 * La table est créée automatiquement si elle n'existe pas.
 */
public class DemandeDisponibiliteDAO {

    private Connection getConn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // ════════════════════════════════════════════════════════════════
    //  CREATE TABLE
    // ════════════════════════════════════════════════════════════════
    public void createTableIfNeeded() {
        String sql = "CREATE TABLE IF NOT EXISTS demandes_disponibilite ("
                + "id              INT PRIMARY KEY AUTO_INCREMENT,"
                + "cours_id        INT  NOT NULL,"
                + "enseignant_id   INT  NOT NULL,"
                + "creneau_propose INT  NOT NULL,"
                + "commentaire     TEXT,"
                + "statut          VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',"
                + "date_demande    TEXT,"
                + "FOREIGN KEY(cours_id)        REFERENCES cours(id),"
                + "FOREIGN KEY(enseignant_id)   REFERENCES utilisateurs(id),"
                + "FOREIGN KEY(creneau_propose) REFERENCES creneaux(id))";
        try (Connection conn = getConn(); Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("[DemandeDisponibiliteDAO] createTable : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  INSERT
    // ════════════════════════════════════════════════════════════════
    public int save(DemandeDisponibilite d) {
        createTableIfNeeded();
        String sql = "INSERT INTO demandes_disponibilite"
                + "(cours_id, enseignant_id, creneau_propose, commentaire, statut, date_demande)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getCoursId());
            ps.setInt(2, d.getEnseignantId());
            ps.setInt(3, d.getCreneauPropose());
            ps.setString(4, d.getCommentaire());
            // getStatut() retourne désormais un String ("EN_ATTENTE", "ACCEPTE"…)
            ps.setString(5, d.getStatut() != null ? d.getStatut() : "EN_ATTENTE");
            ps.setString(6, d.getDateDemande() != null
                    ? d.getDateDemande().toString() : LocalDateTime.now().toString());
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) {
                int id = gk.getInt(1);
                d.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("[DemandeDisponibiliteDAO] save : " + e.getMessage());
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════════
    //  UPDATE
    // ════════════════════════════════════════════════════════════════
    public void updateStatut(int id, StatutDisponibilite statut) {
        String sql = "UPDATE demandes_disponibilite SET statut = ? WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DemandeDisponibiliteDAO] updateStatut : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SELECT
    // ════════════════════════════════════════════════════════════════
    public List<DemandeDisponibilite> findByEnseignant(int enseignantId) {
        return query("WHERE dd.enseignant_id = ?", enseignantId);
    }

    public List<DemandeDisponibilite> findEnAttente() {
        return query("WHERE dd.statut = 'EN_ATTENTE'", null);
    }

    public List<DemandeDisponibilite> findAll() {
        return query("", null);
    }

    public DemandeDisponibilite findById(int id) {
        List<DemandeDisponibilite> list = query("WHERE dd.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    // ════════════════════════════════════════════════════════════════
    //  COUNT
    // ════════════════════════════════════════════════════════════════
    public long countEnAttente() {
        createTableIfNeeded();
        String sql = "SELECT COUNT(*) FROM demandes_disponibilite WHERE statut = 'EN_ATTENTE'";
        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("[DemandeDisponibiliteDAO] countEnAttente : " + e.getMessage());
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════════════
    //  QUERY INTERNE avec jointures
    // ════════════════════════════════════════════════════════════════
    private List<DemandeDisponibilite> query(String whereClause, Integer param) {
        createTableIfNeeded();
        List<DemandeDisponibilite> result = new ArrayList<>();
        String sql = "SELECT dd.id, dd.cours_id, dd.enseignant_id, dd.creneau_propose,"
                + "  dd.commentaire, dd.statut, dd.date_demande,"
                + "  m.nom  AS matiere_nom,"
                + "  cp.nom AS classe_nom,"
                + "  CONCAT(u.prenom,' ',u.nom) AS enseignant_nom,"
                + "  CONCAT(cr.jour,' ',cr.heure_debut,'h (',cr.duree,'h)') AS creneau_info"
                + " FROM  demandes_disponibilite dd"
                + " JOIN  cours          c  ON dd.cours_id        = c.id"
                + " JOIN  matieres       m  ON c.matiere_id       = m.id"
                + " JOIN  classes_pedago cp ON c.classe_id        = cp.id"
                + " JOIN  utilisateurs   u  ON dd.enseignant_id   = u.id"
                + " JOIN  creneaux       cr ON dd.creneau_propose = cr.id"
                + " " + whereClause
                + " ORDER BY dd.date_demande DESC";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[DemandeDisponibiliteDAO] query : " + e.getMessage());
        }
        return result;
    }

    private DemandeDisponibilite map(ResultSet rs) throws SQLException {
        DemandeDisponibilite d = new DemandeDisponibilite();
        d.setId(rs.getInt("id"));
        d.setCoursId(rs.getInt("cours_id"));
        d.setEnseignantId(rs.getInt("enseignant_id"));
        d.setCreneauPropose(rs.getInt("creneau_propose"));
        d.setCommentaire(rs.getString("commentaire"));
        d.setMatiereNom(rs.getString("matiere_nom"));
        d.setClasseNom(rs.getString("classe_nom"));
        d.setEnseignantNom(rs.getString("enseignant_nom"));
        d.setCreneauInfo(rs.getString("creneau_info"));
        // setStatutString() accepte le String brut de la BDD
        d.setStatutString(rs.getString("statut"));
        String ds = rs.getString("date_demande");
        if (ds != null && !ds.isBlank()) {
            try { d.setDateDemande(LocalDateTime.parse(ds.substring(0, 19))); }
            catch (Exception ignored) {}
        }
        return d;
    }
}