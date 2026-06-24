package org.example.repository;

import org.example.model.Drawing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DrawingRepository {
    private final ConnectionProvider connectionProvider;

    public DrawingRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Drawing create(Drawing drawing) {
        String sql = "INSERT INTO drawings (name, owner_id, data) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, drawing.name());
            stmt.setInt(2, drawing.ownerId());
            stmt.setString(3, drawing.data());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Drawing(rs.getInt("id"), drawing.name(), drawing.ownerId(), drawing.data());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating drawing: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Drawing> findAll() {
        String sql = "SELECT id, name, owner_id, data FROM drawings ORDER BY id DESC";
        List<Drawing> list = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Drawing(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("owner_id"),
                        rs.getString("data")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching drawings: " + e.getMessage(), e);
        }
        return list;
    }

    public Drawing findById(int id) {
        String sql = "SELECT id, name, owner_id, data FROM drawings WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Drawing(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("owner_id"),
                            rs.getString("data")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching drawing by id: " + e.getMessage(), e);
        }
        return null;
    }

    public void updateData(int id, String data) {
        String sql = "UPDATE drawings SET data = ? WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating drawing data: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM drawings WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting drawing: " + e.getMessage(), e);
        }
    }
}