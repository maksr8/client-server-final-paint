package org.example.service;

import org.example.model.Drawing;
import org.example.model.User;
import org.example.repository.DrawingRepository;
import org.example.repository.UserRepository;

import java.util.List;

public class DrawingService {
    private final DrawingRepository drawingRepository;
    private final UserRepository userRepository;

    public DrawingService(DrawingRepository drawingRepository, UserRepository userRepository) {
        this.drawingRepository = drawingRepository;
        this.userRepository = userRepository;
    }

    public Drawing createDrawing(String username, String drawingName) {
        if (drawingName == null || drawingName.isBlank()) {
            throw new IllegalArgumentException("Drawing name cannot be empty");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Drawing newDrawing = new Drawing(null, drawingName, user.id(), null);
        return drawingRepository.create(newDrawing);
    }

    public List<Drawing> getAllDrawings() {
        return drawingRepository.findAll();
    }

    public Drawing getDrawing(int id) {
        Drawing drawing = drawingRepository.findById(id);
        if (drawing == null) {
            throw new RuntimeException("Drawing not found");
        }
        return drawing;
    }

    public void saveDrawingData(int id, String data) {
        drawingRepository.updateData(id, data);
    }

    public void deleteDrawing(String username, int drawingId) {
        Drawing drawing = getDrawing(drawingId);
        User user = userRepository.findByUsername(username);

        if (!drawing.ownerId().equals(user.id())) {
            throw new SecurityException("You are not the owner of this drawing");
        }

        drawingRepository.delete(drawingId);
    }
}