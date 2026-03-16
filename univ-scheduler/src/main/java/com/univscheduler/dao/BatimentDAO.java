package com.univscheduler.dao;
import com.univscheduler.model.Batiment;
import java.sql.*;
import java.util.*;

public class BatimentDAO {
    private DatabaseManager db=DatabaseManager.getInstance();

    public List<Batiment> findAll() {
        List<Batiment> list=new ArrayList<>();
        try(Connection conn=db.getConnection(); Statement stmt=conn.createStatement()){
            ResultSet rs=stmt.executeQuery("SELECT * FROM batiments ORDER BY nom");
            while(rs.next()) list.add(new Batiment(rs.getInt("id"),rs.getString("nom"),rs.getString("localisation"),rs.getInt("nombre_etages")));
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public void save(Batiment b) {
        if(b.getId()==0){
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO batiments(nom,localisation,nombre_etages) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,b.getNom());ps.setString(2,b.getLocalisation());ps.setInt(3,b.getNombreEtages());
                ps.executeUpdate(); ResultSet k=ps.getGeneratedKeys(); if(k.next()) b.setId(k.getInt(1));
            } catch(SQLException e){ e.printStackTrace(); }
        } else {
            try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement(
                "UPDATE batiments SET nom=?,localisation=?,nombre_etages=? WHERE id=?")){
                ps.setString(1,b.getNom());ps.setString(2,b.getLocalisation());ps.setInt(3,b.getNombreEtages());ps.setInt(4,b.getId());
                ps.executeUpdate();
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }

    public void delete(int id) {
        try(Connection conn=db.getConnection(); PreparedStatement ps=conn.prepareStatement("DELETE FROM batiments WHERE id=?")){
            ps.setInt(1,id); ps.executeUpdate();
        } catch(SQLException e){ e.printStackTrace(); }
    }
}
