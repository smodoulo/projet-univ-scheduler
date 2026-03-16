package com.univscheduler.dao;
import com.univscheduler.model.Notification;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class NotificationDAO {
    private DatabaseManager db=DatabaseManager.getInstance();

    public List<Notification> findByUtilisateur(int userId) {
        List<Notification> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "SELECT * FROM notifications WHERE utilisateur_id=? ORDER BY date_envoi DESC")){
            ps.setInt(1,userId); ResultSet rs=ps.executeQuery();
            while(rs.next()) list.add(map(rs));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public int countUnread(int userId) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "SELECT COUNT(*) FROM notifications WHERE utilisateur_id=? AND lu=0")){
            ps.setInt(1,userId); ResultSet rs=ps.executeQuery(); return rs.getInt(1);
        } catch(SQLException e){ return 0; }
    }

    public void save(Notification n) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "INSERT INTO notifications(message,date_envoi,lu,type,utilisateur_id) VALUES(?,?,?,?,?)")){
            ps.setString(1,n.getMessage());ps.setString(2,LocalDateTime.now().toString());
            ps.setInt(3,0);ps.setString(4,n.getType());ps.setInt(5,n.getUtilisateurId());
            ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }

    public void markAllRead(int userId) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
            "UPDATE notifications SET lu=1 WHERE utilisateur_id=?")){
            ps.setInt(1,userId); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }

    private Notification map(ResultSet rs) throws SQLException {
        LocalDateTime dt=null;
        String s=rs.getString("date_envoi"); if(s!=null) try{ dt=LocalDateTime.parse(s); }catch(Exception e){}
        return new Notification(rs.getInt("id"),rs.getString("message"),dt,rs.getInt("lu")==1,rs.getString("type"),rs.getInt("utilisateur_id"));
    }
}
