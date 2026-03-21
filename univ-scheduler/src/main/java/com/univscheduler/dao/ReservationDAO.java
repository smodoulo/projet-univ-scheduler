package com.univscheduler.dao;
import com.univscheduler.model.Reservation;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReservationDAO {
    private DatabaseManager db=DatabaseManager.getInstance();

    public List<Reservation> findAll() {
        List<Reservation> list=new ArrayList<>();
        String sql="SELECT r.*,s.numero as salle_num,CONCAT(u.prenom,' ',u.nom) as user_nom FROM reservations r LEFT JOIN salles s ON r.salle_id=s.id LEFT JOIN utilisateurs u ON r.utilisateur_id=u.id ORDER BY r.date_reservation DESC";
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery(sql); while(rs.next()) list.add(map(rs));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public List<Reservation> findByUtilisateur(int userId) {
        List<Reservation> list=new ArrayList<>();
        String sql="SELECT r.*,s.numero as salle_num,CONCAT(u.prenom,' ',u.nom) as user_nom FROM reservations r LEFT JOIN salles s ON r.salle_id=s.id LEFT JOIN utilisateurs u ON r.utilisateur_id=u.id WHERE r.utilisateur_id=? ORDER BY r.date_reservation DESC";
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setInt(1,userId); ResultSet rs=ps.executeQuery(); while(rs.next()) list.add(map(rs));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    // ✅ AJOUT : Rappel de fin de réservation
    // Retourne les réservations VALIDÉES dont la date_fin arrive dans <= minutesAvant minutes
    public List<Reservation> findReservationsTerminantBientot(int minutesAvant) {
        List<Reservation> result=new ArrayList<>();
        String sql="SELECT r.*,s.numero as salle_num,CONCAT(u.prenom,' ',u.nom) as user_nom FROM reservations r LEFT JOIN salles s ON r.salle_id=s.id LEFT JOIN utilisateurs u ON r.utilisateur_id=u.id WHERE r.statut='VALIDEE' AND r.date_fin IS NOT NULL";
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement(); ResultSet rs=stmt.executeQuery(sql)){
            LocalDateTime maintenant=LocalDateTime.now();
            while(rs.next()){
                Reservation r=map(rs);
                if(r.getDateFin()==null) continue;
                long minutesRestantes=ChronoUnit.MINUTES.between(maintenant, r.getDateFin());
                // Déclencher le rappel si la fin est dans la fenêtre [0 , minutesAvant]
                if(minutesRestantes>=0 && minutesRestantes<=minutesAvant) result.add(r);
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return result;
    }

    public void updateStatut(int id, String statut) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("UPDATE reservations SET statut=? WHERE id=?")){
            ps.setString(1,statut);ps.setInt(2,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }

    public Map<String,Integer> countByStatut() {
        Map<String,Integer> m=new LinkedHashMap<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT statut,COUNT(*) as cnt FROM reservations GROUP BY statut");
            while(rs.next()) m.put(rs.getString("statut"),rs.getInt("cnt"));
        } catch(SQLException e){ e.printStackTrace(); }
        return m;
    }

    public void save(Reservation r) {
        if(r.getId()==0){
            // ✅ MODIFIÉ : ajout de date_fin dans l'INSERT
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO reservations(motif,statut,date_reservation,date_fin,salle_id,utilisateur_id) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,r.getMotif()); ps.setString(2,r.getStatut());
                ps.setString(3,r.getDateReservation().toString());
                // Si date_fin non fournie → début + 2h par défaut
                LocalDateTime fin=r.getDateFin()!=null ? r.getDateFin() : r.getDateReservation().plusHours(2);
                ps.setString(4,fin.toString());
                ps.setInt(5,r.getSalleId()); ps.setInt(6,r.getUtilisateurId());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) r.setId(k.getInt(1));
            } catch(SQLException e){ e.printStackTrace(); }
        } else {
            // ✅ MODIFIÉ : ajout de date_fin dans l'UPDATE
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                    "UPDATE reservations SET motif=?,statut=?,date_reservation=?,date_fin=?,salle_id=?,utilisateur_id=? WHERE id=?")){
                ps.setString(1,r.getMotif()); ps.setString(2,r.getStatut());
                ps.setString(3,r.getDateReservation().toString());
                LocalDateTime fin=r.getDateFin()!=null ? r.getDateFin() : r.getDateReservation().plusHours(2);
                ps.setString(4,fin.toString());
                ps.setInt(5,r.getSalleId()); ps.setInt(6,r.getUtilisateurId()); ps.setInt(7,r.getId());
                ps.executeUpdate();
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }

    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM reservations WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r=new Reservation();
        r.setId(rs.getInt("id")); r.setMotif(rs.getString("motif")); r.setStatut(rs.getString("statut"));
        String d=rs.getString("date_reservation"); if(d!=null) try{ r.setDateReservation(LocalDateTime.parse(d.replace(" ","T"))); }catch(Exception e){}
        // ✅ AJOUT : lecture de date_fin depuis la base
        String f=rs.getString("date_fin"); if(f!=null) try{ r.setDateFin(LocalDateTime.parse(f.replace(" ","T"))); }catch(Exception e){}
        r.setSalleId(rs.getInt("salle_id")); r.setSalleNumero(rs.getString("salle_num"));
        r.setUtilisateurId(rs.getInt("utilisateur_id")); r.setUtilisateurNom(rs.getString("user_nom"));
        return r;
    }
}