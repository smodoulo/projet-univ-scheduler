package com.univscheduler.dao;
import com.univscheduler.model.Salle;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class SalleDAO {
    private DatabaseManager db = DatabaseManager.getInstance();

    public List<Salle> findAll() {
        List<Salle> list = new ArrayList<>();
        String sql = "SELECT s.*, b.nom as bat_nom FROM salles s " +
                "LEFT JOIN batiments b ON s.batiment_id = b.id ORDER BY s.numero";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Salle s = map(rs);
                s.setBatimentNom(rs.getString("bat_nom"));
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Salle> findDisponibles(int capMin, String type) {
        return findDisponiblesAvancee(capMin, type, null, null, null, null);
    }

    public List<Salle> findDisponiblesAvancee(int capMin, String type,
                                              LocalDate date, Integer heureDebut, Integer heureFin,
                                              List<String> equipements) {
        List<Salle> all = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT s.*, b.nom as bat_nom FROM salles s " +
                        "LEFT JOIN batiments b ON s.batiment_id = b.id " +
                        "WHERE s.disponible = 1 AND s.capacite >= ?");
        if (type != null && !type.isBlank() && !type.equals("TOUS"))
            sql.append(" AND s.type_salle = ?");
        sql.append(" ORDER BY s.numero");

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, capMin);
            if (type != null && !type.isBlank() && !type.equals("TOUS"))
                ps.setString(idx++, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Salle s = map(rs);
                s.setBatimentNom(rs.getString("bat_nom"));
                all.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (all.isEmpty()) return all;

        if (date != null && heureDebut != null) {
            int fin = (heureFin != null) ? heureFin : heureDebut + 2;
            Set<Integer> occupiesCours = getSallesOccupeesCours(date, heureDebut, fin);
            all.removeIf(s -> occupiesCours.contains(s.getId()));
        }

        if (date != null && heureDebut != null) {
            int fin = (heureFin != null) ? heureFin : heureDebut + 2;
            Set<Integer> occupiesReserv = getSallesOccupeesReservations(date, heureDebut, fin);
            all.removeIf(s -> occupiesReserv.contains(s.getId()));
        }

        if (equipements != null && !equipements.isEmpty()) {
            all.removeIf(s -> !salleHasAllEquipements(s.getId(), equipements));
        }

        return all;
    }

    private Set<Integer> getSallesOccupeesCours(LocalDate date, int heureDebut, int heureFin) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT DISTINCT c.salle_id FROM cours c " +
                "JOIN creneaux cr ON c.creneau_id = cr.id " +
                "WHERE c.date = ? " +
                "AND cr.heure_debut < ? " +
                "AND (cr.heure_debut + cr.duree) > ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, heureFin);
            ps.setInt(3, heureDebut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("salle_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    private Set<Integer> getSallesOccupeesReservations(LocalDate date, int heureDebut, int heureFin) {
        Set<Integer> ids = new HashSet<>();
        // ← CORRECTION : SUBSTRING() au lieu de substr() (syntaxe MySQL)
        String sql = "SELECT DISTINCT salle_id, date_reservation FROM reservations " +
                "WHERE statut = 'VALIDEE' " +
                "AND SUBSTRING(date_reservation, 1, 10) = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dt = rs.getString("date_reservation");
                if (dt != null && dt.length() >= 16) {
                    try {
                        String timePart = dt.substring(11, 13);
                        int resHeure = Integer.parseInt(timePart);
                        int resFin = resHeure + 2;
                        if (resHeure < heureFin && resFin > heureDebut) {
                            ids.add(rs.getInt("salle_id"));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    public boolean salleHasAllEquipements(int salleId, List<String> reqTypes) {
        if (reqTypes == null || reqTypes.isEmpty()) return true;
        for (String t : reqTypes) {
            String sql = "SELECT COUNT(*) FROM equipements " +
                    "WHERE salle_id = ? AND type_equipement = ? " +
                    "AND disponible = 1 AND etat != 'EN_PANNE'";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, salleId);
                ps.setString(2, t);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) return false; // ← rs.next() ajouté
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }
        return true;
    }

    public boolean isSalleLibre(int salleId, LocalDate date, int heureDebut, int heureFin) {
        Set<Integer> cours = getSallesOccupeesCours(date, heureDebut, heureFin);
        Set<Integer> reserv = getSallesOccupeesReservations(date, heureDebut, heureFin);
        return !cours.contains(salleId) && !reserv.contains(salleId);
    }

    public List<String> getEquipementsDisponibles(int salleId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT type_equipement FROM equipements " +
                "WHERE salle_id = ? AND disponible = 1 AND etat != 'EN_PANNE'";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("type_equipement"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean isSalleLibreMaintenantById(int salleId) {
        LocalDate today = LocalDate.now();
        int hour = LocalTime.now().getHour();
        return isSalleLibre(salleId, today, hour, hour + 1);
    }

    public Map<String, Integer> countByType() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT type_salle, COUNT(*) as cnt FROM salles GROUP BY type_salle");
            while (rs.next()) m.put(rs.getString("type_salle"), rs.getInt("cnt"));
        } catch (SQLException e) { e.printStackTrace(); }
        return m;
    }

    public int countDisponibles() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM salles WHERE disponible = 1");
            if (rs.next()) return rs.getInt(1); // ← rs.next() ajouté
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void save(Salle s) {
        if (s.getId() == 0) {
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO salles(numero,capacite,type_salle,disponible,batiment_id) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getNumero()); ps.setInt(2, s.getCapacite());
                ps.setString(3, s.getTypeSalle()); ps.setInt(4, s.isDisponible() ? 1 : 0);
                ps.setInt(5, s.getBatimentId());
                ps.executeUpdate();
                ResultSet k = ps.getGeneratedKeys();
                if (k.next()) s.setId(k.getInt(1));
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                    "UPDATE salles SET numero=?,capacite=?,type_salle=?,disponible=?,batiment_id=? WHERE id=?")) {
                ps.setString(1, s.getNumero()); ps.setInt(2, s.getCapacite());
                ps.setString(3, s.getTypeSalle()); ps.setInt(4, s.isDisponible() ? 1 : 0);
                ps.setInt(5, s.getBatimentId()); ps.setInt(6, s.getId());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void delete(int id) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM salles WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int count() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM salles");
            if (rs.next()) return rs.getInt(1); // ← rs.next() ajouté
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private Salle map(ResultSet rs) throws SQLException {
        return new Salle(rs.getInt("id"), rs.getString("numero"), rs.getInt("capacite"),
                rs.getString("type_salle"), rs.getInt("disponible") == 1, rs.getInt("batiment_id"));
    }
}