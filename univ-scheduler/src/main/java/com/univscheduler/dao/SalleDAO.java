package com.univscheduler.dao;
import com.univscheduler.model.Salle;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class SalleDAO {
    private DatabaseManager db=DatabaseManager.getInstance();

    public List<Salle> findAll() {
        List<Salle> list=new ArrayList<>();
        String sql="SELECT s.*,b.nom as bat_nom FROM salles s LEFT JOIN batiments b ON s.batiment_id=b.id ORDER BY s.numero";
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery(sql);
            while(rs.next()){ Salle s=map(rs); s.setBatimentNom(rs.getString("bat_nom")); list.add(s); }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    /**
     * Simple availability search (no time check) — legacy/basic use.
     */
    public List<Salle> findDisponibles(int capMin, String type) {
        return findDisponiblesAvancee(capMin, type, null, null, null, null);
    }

    /**
     * Advanced real-time availability search.
     *
     * @param capMin       minimum capacity (0 = no constraint)
     * @param type         room type: TD, TP, AMPHI, or null/TOUS
     * @param date         the date to check (null = ignore scheduling conflicts)
     * @param heureDebut   start hour to check (e.g. 8 for 8h00). null = ignore
     * @param heureFin     end hour to check (e.g. 10). null = same as heureDebut+2
     * @param equipements  list of required equipment types (null or empty = no constraint)
     * @return rooms that are: marked disponible=1, meet capacity/type filters,
     *         have no cours scheduled at that date+hour,
     *         have no validated reservation at that date+hour,
     *         and have all required equipment types.
     */
    public List<Salle> findDisponiblesAvancee(int capMin, String type,
                                               LocalDate date, Integer heureDebut, Integer heureFin,
                                               List<String> equipements) {
        List<Salle> all = new ArrayList<>();

        // Step 1: Base query — disponible + capacity + type
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

        // Step 2: If date+hour given, filter out rooms with conflicting cours
        if (date != null && heureDebut != null) {
            int fin = (heureFin != null) ? heureFin : heureDebut + 2;
            Set<Integer> occupiesCours = getSallesOccupeesCours(date, heureDebut, fin);
            all.removeIf(s -> occupiesCours.contains(s.getId()));
        }

        // Step 3: If date+hour given, filter out rooms with conflicting validated reservations
        if (date != null && heureDebut != null) {
            int fin = (heureFin != null) ? heureFin : heureDebut + 2;
            Set<Integer> occupiesReserv = getSallesOccupeesReservations(date, heureDebut, fin);
            all.removeIf(s -> occupiesReserv.contains(s.getId()));
        }

        // Step 4: If equipment types required, filter rooms that have all of them
        if (equipements != null && !equipements.isEmpty()) {
            all.removeIf(s -> !salleHasAllEquipements(s.getId(), equipements));
        }

        return all;
    }

    /**
     * Returns set of salle_ids that have a cours on the given date overlapping [heureDebut, heureFin).
     */
    private Set<Integer> getSallesOccupeesCours(LocalDate date, int heureDebut, int heureFin) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT DISTINCT c.salle_id FROM cours c " +
                     "JOIN creneaux cr ON c.creneau_id = cr.id " +
                     "WHERE c.date = ? " +
                     "AND cr.heure_debut < ? " +         // creneau starts before our end
                     "AND (cr.heure_debut + cr.duree) > ?"; // creneau ends after our start
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

    /**
     * Returns set of salle_ids with a VALIDEE reservation on the given date overlapping [heureDebut, heureFin).
     * Reservations are stored as datetime; we treat them as 2-hour blocks from the stored hour.
     */
    private Set<Integer> getSallesOccupeesReservations(LocalDate date, int heureDebut, int heureFin) {
        Set<Integer> ids = new HashSet<>();
        // date_reservation is stored as ISO datetime string: "2026-03-20T10:00:00"
        // We match the date part and treat the hour as a 2-hour block
        String sql = "SELECT DISTINCT salle_id, date_reservation FROM reservations " +
                     "WHERE statut = 'VALIDEE' " +
                     "AND substr(date_reservation, 1, 10) = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dt = rs.getString("date_reservation");
                if (dt != null && dt.length() >= 16) {
                    try {
                        // parse hour from "2026-03-20T10:00:00" or "2026-03-20 10:00:00"
                        String timePart = dt.substring(11, 13); // "10"
                        int resHeure = Integer.parseInt(timePart);
                        int resFin = resHeure + 2; // assume 2-hour block
                        // overlap check: [heureDebut, heureFin) overlaps [resHeure, resFin)?
                        if (resHeure < heureFin && resFin > heureDebut) {
                            ids.add(rs.getInt("salle_id"));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    /**
     * Returns true if the room has ALL the requested equipment types in working/available state.
     */
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
                if (rs.getInt(1) == 0) return false;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }
        return true;
    }

    /**
     * Check if a specific room is free at a given date+hour.
     */
    public boolean isSalleLibre(int salleId, LocalDate date, int heureDebut, int heureFin) {
        Set<Integer> cours = getSallesOccupeesCours(date, heureDebut, heureFin);
        Set<Integer> reserv = getSallesOccupeesReservations(date, heureDebut, heureFin);
        return !cours.contains(salleId) && !reserv.contains(salleId);
    }

    /**
     * Returns the list of equipment types available in a room.
     */
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

    /**
     * Check if room is free right now (current time).
     */
    public boolean isSalleLibreMaintenantById(int salleId) {
        LocalDate today = LocalDate.now();
        int hour = LocalTime.now().getHour();
        return isSalleLibre(salleId, today, hour, hour + 1);
    }

    public Map<String,Integer> countByType() {
        Map<String,Integer> m=new LinkedHashMap<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT type_salle,COUNT(*) as cnt FROM salles GROUP BY type_salle");
            while(rs.next()) m.put(rs.getString("type_salle"),rs.getInt("cnt"));
        } catch(SQLException e){ e.printStackTrace(); }
        return m;
    }

    public int countDisponibles() {
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT COUNT(*) FROM salles WHERE disponible=1");
            return rs.getInt(1);
        } catch(SQLException e){ return 0; }
    }

    public void save(Salle s) {
        if(s.getId()==0){
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO salles(numero,capacite,type_salle,disponible,batiment_id) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,s.getNumero());ps.setInt(2,s.getCapacite());ps.setString(3,s.getTypeSalle());
                ps.setInt(4,s.isDisponible()?1:0);ps.setInt(5,s.getBatimentId());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) s.setId(k.getInt(1));
            } catch(SQLException e){ e.printStackTrace(); }
        } else {
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "UPDATE salles SET numero=?,capacite=?,type_salle=?,disponible=?,batiment_id=? WHERE id=?")){
                ps.setString(1,s.getNumero());ps.setInt(2,s.getCapacite());ps.setString(3,s.getTypeSalle());
                ps.setInt(4,s.isDisponible()?1:0);ps.setInt(5,s.getBatimentId());ps.setInt(6,s.getId());
                ps.executeUpdate();
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }

    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM salles WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }

    public int count() {
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT COUNT(*) FROM salles");
            return rs.getInt(1);
        } catch(SQLException e){ return 0; }
    }

    private Salle map(ResultSet rs) throws SQLException {
        return new Salle(rs.getInt("id"),rs.getString("numero"),rs.getInt("capacite"),
            rs.getString("type_salle"),rs.getInt("disponible")==1,rs.getInt("batiment_id"));
    }
}
