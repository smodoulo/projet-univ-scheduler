package com.univscheduler.dao;
import com.univscheduler.model.Matiere;
import java.sql.*;
import java.util.*;

public class MatiereDAO {
    private DatabaseManager db=DatabaseManager.getInstance();
    public List<Matiere> findAll() {
        List<Matiere> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT * FROM matieres ORDER BY nom");
            while(rs.next()) list.add(new Matiere(rs.getInt("id"),rs.getString("nom"),rs.getString("description"),rs.getInt("volume_cm"),rs.getInt("volume_td")));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
    public void save(Matiere m) {
        if(m.getId()==0){
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO matieres(nom,description,volume_cm,volume_td) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,m.getNom());ps.setString(2,m.getDescription());ps.setInt(3,m.getVolumeCm());ps.setInt(4,m.getVolumeTd());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) m.setId(k.getInt(1));
            } catch(SQLException e){ e.printStackTrace(); }
        } else {
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "UPDATE matieres SET nom=?,description=?,volume_cm=?,volume_td=? WHERE id=?")){
                ps.setString(1,m.getNom());ps.setString(2,m.getDescription());ps.setInt(3,m.getVolumeCm());ps.setInt(4,m.getVolumeTd());ps.setInt(5,m.getId());
                ps.executeUpdate();
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }
    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM matieres WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }
}
