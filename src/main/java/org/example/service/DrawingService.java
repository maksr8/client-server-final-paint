package org.example.service;

import org.example.dto.DrawingsListDto;
import org.example.model.Drawing;
import org.example.model.User;
import org.example.repository.DrawingRepository;
import org.example.repository.UserRepository;

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

    public DrawingsListDto getAllDrawings(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return new DrawingsListDto(
                drawingRepository.findAllOwnedByUser(user.id()),
                drawingRepository.findAllSharedWithUser(user.id())
        );
    }

    public Drawing getDrawing(String username, String id) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Drawing drawing = drawingRepository.findById(id);
        if (drawing == null) {
            throw new RuntimeException("Drawing not found");
        }

        if (!drawing.ownerId().equals(user.id())) {
            drawingRepository.addSharedDrawing(user.id(), drawing.id());
        }

        return drawing;
    }

    public void saveDrawingData(String id, String data) {
        Drawing drawing = drawingRepository.findById(id);
        if (drawing != null) {
            drawingRepository.updateData(drawing.id(), data);
        }
    }

    public void renameDrawing(String username, String id, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Drawing name cannot be empty");
        }
        User user = userRepository.findByUsername(username);
        Drawing drawing = drawingRepository.findById(id);

        if (drawing == null) {
            throw new RuntimeException("Drawing not found");
        }
        if (!drawing.ownerId().equals(user.id())) {
            throw new SecurityException("Only the owner can rename the drawing");
        }

        drawingRepository.renameDrawing(drawing.id(), newName);
    }

    public void deleteDrawing(String username, String id) {
        Drawing drawing = drawingRepository.findById(id);
        if (drawing == null) {
            throw new RuntimeException("Drawing not found");
        }
        User user = userRepository.findByUsername(username);

        if (!drawing.ownerId().equals(user.id())) {
            throw new SecurityException("You are not the owner of this drawing");
        }

        drawingRepository.delete(drawing.id());
    }
}