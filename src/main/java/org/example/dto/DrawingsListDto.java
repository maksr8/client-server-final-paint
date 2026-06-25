package org.example.dto;

import org.example.model.Drawing;

import java.util.List;

public record DrawingsListDto(List<Drawing> owned, List<Drawing> shared) {
}
