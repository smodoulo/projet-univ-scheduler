package com.univscheduler.dao;
import com.univscheduler.model.Creneau;
import java.sql.*;
import java.util.*;

public class CreneauDAO {
    private DatabaseManager db=DatabaseManager.getInstance();
    public List<Creneau> findAll() {
        List<Creneau> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT * FROM creneaux ORDER BY jour,heure_debut");
            while(rs.next()) list.add(new Creneau(rs.getInt("id"),rs.getString("jour"),rs.getInt("heure_debut"),rs.getInt("duree")));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
    public void save(Creneau c) {
        if(c.getId()==0){
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO creneaux(jour,heure_debut,duree) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,c.getJour());ps.setInt(2,c.getHeureDebut());ps.setInt(3,c.getDuree());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) c.setId(k.getInt(1));
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }
    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM creneaux WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }
}
