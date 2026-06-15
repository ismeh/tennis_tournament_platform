package com.tfm.tennis_platform.application.commands;

public record AgeCategoryOutput(
        Integer id,
        String category,
        boolean custom
) {
    public AgeCategoryOutput(Integer id, String category) {
        this(id, category, false);
    }
}
