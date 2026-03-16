package com.univscheduler.dao;
import com.univscheduler.model.Equipement;
import java.sql.*;
import java.util.*;

public class EquipementDAO {
    private DatabaseManager db=DatabaseManager.getInstance();

    public List<Equipement> findAll() {
        List<Equipement> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT * FROM equipements ORDER BY nom");
            while(rs.next()) list.add(map(rs));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public List<Equipement> findBySalle(int salleId) {
        List<Equipement> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("SELECT * FROM equipements WHERE salle_id=?")){
            ps.setInt(1,salleId); ResultSet rs=ps.executeQuery();
            while(rs.next()) list.add(map(rs));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public Map<String,Integer> countByEtat() {
        Map<String,Integer> m=new LinkedHashMap<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT etat,COUNT(*) as cnt FROM equipements GROUP BY etat");
            while(rs.next()) m.put(rs.getString("etat"),rs.getInt("cnt"));
        } catch(SQLException e){ e.printStackTrace(); }
        return m;
    }

    public void save(Equipement e) {
        if(e.getId()==0){
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO equipements(nom,description,etat,type_equipement,disponible,salle_id) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,e.getNom());ps.setString(2,e.getDescription());ps.setString(3,e.getEtat());
                ps.setString(4,e.getTypeEquipement());ps.setInt(5,e.isDisponible()?1:0);ps.setInt(6,e.getSalleId());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) e.setId(k.getInt(1));
            } catch(SQLException ex){ ex.printStackTrace(); }
        } else {
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "UPDATE equipements SET nom=?,description=?,etat=?,type_equipement=?,disponible=?,salle_id=? WHERE id=?")){
                ps.setString(1,e.getNom());ps.setString(2,e.getDescription());ps.setString(3,e.getEtat());
                ps.setString(4,e.getTypeEquipement());ps.setInt(5,e.isDisponible()?1:0);ps.setInt(6,e.getSalleId());ps.setInt(7,e.getId());
                ps.executeUpdate();
            } catch(SQLException ex){ ex.printStackTrace(); }
        }
    }

    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM equipements WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }

    private Equipement map(ResultSet rs) throws SQLException {
        return new Equipement(rs.getInt("id"),rs.getString("nom"),rs.getString("description"),
            rs.getString("etat"),rs.getString("type_equipement"),rs.getInt("disponible")==1,rs.getInt("salle_id"));
    }
}
