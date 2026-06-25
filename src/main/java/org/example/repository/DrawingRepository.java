package org.example.repository;

import org.example.model.Drawing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                    return new Drawing(rs.getString("id"), drawing.name(), drawing.ownerId(), drawing.data());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating drawing: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Drawing> findAllOwnedByUser(int userId) {
        String sql = "SELECT id, name, owner_id, data FROM drawings WHERE owner_id = ? ORDER BY created_at DESC";
        List<Drawing> list = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Drawing(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getInt("owner_id"),
                            rs.getString("data")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching drawings: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Drawing> findAllSharedWithUser(int userId) {
        String sql = "SELECT d.id, d.name, d.owner_id, d.data " +
                     "FROM drawings d JOIN shared_drawings sd ON d.id = sd.drawing_id " +
                     "WHERE sd.user_id = ? ORDER BY d.created_at DESC";
        List<Drawing> list = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Drawing(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getInt("owner_id"),
                            rs.getString("data")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching shared drawings: " + e.getMessage(), e);
        }
        return list;
    }

    public Drawing findById(String id) {
        String sql = "SELECT id, name, owner_id, data FROM drawings WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, UUID.fromString(id));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Drawing(
                            rs.getString("id"),
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

    public void updateData(String id, String data) {
        String sql = "UPDATE drawings SET data = ? WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            stmt.setObject(2, UUID.fromString(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating drawing data: " + e.getMessage(), e);
        }
    }

    public void renameDrawing(String id, String newName) {
        String sql = "UPDATE drawings SET name = ? WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setObject(2, UUID.fromString(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error renaming drawing: " + e.getMessage(), e);
        }
    }

    public void addSharedDrawing(int userId, String drawingId) {
        String sql = "INSERT INTO shared_drawings (user_id, drawing_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setObject(2, UUID.fromString(drawingId));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error adding shared drawing: " + e.getMessage(), e);
        }
    }

    public void delete(String id) {
        String sql = "DELETE FROM drawings WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting drawing: " + e.getMessage(), e);
        }
    }
}