package com.univscheduler.dao;
import com.univscheduler.model.ClassePedago;
import java.sql.*;
import java.util.*;

public class ClassePedagoDAO {
    private DatabaseManager db=DatabaseManager.getInstance();
    public List<ClassePedago> findAll() {
        List<ClassePedago> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT * FROM classes_pedago ORDER BY nom");
            while(rs.next()) list.add(new ClassePedago(rs.getInt("id"),rs.getString("nom"),rs.getString("niveau"),rs.getInt("effectif")));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
    public void save(ClassePedago c) {
        if(c.getId()==0){
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO classes_pedago(nom,niveau,effectif) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,c.getNom());ps.setString(2,c.getNiveau());ps.setInt(3,c.getEffectif());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) c.setId(k.getInt(1));
            } catch(SQLException e){ e.printStackTrace(); }
        } else {
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "UPDATE classes_pedago SET nom=?,niveau=?,effectif=? WHERE id=?")){
                ps.setString(1,c.getNom());ps.setString(2,c.getNiveau());ps.setInt(3,c.getEffectif());ps.setInt(4,c.getId());
                ps.executeUpdate();
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }
    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM classes_pedago WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }
}
