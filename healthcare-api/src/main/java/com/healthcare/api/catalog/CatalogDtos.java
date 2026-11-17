package com.healthcare.api.catalog;

import java.util.List;

/**
 * 基础数据 DTO：食物、运动、课程、设备、份量和餐次。
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record Food(
            Long id,
            String name,
            String portion,
            int calories,
            String emoji,
            String category,
            boolean custom) {
    }

    public record ExerciseType(
            String key,
            String name,
            String emoji,
            String bg,
            String bar,
            int kcal) {
    }

    public record ExerciseLesson(
            Long id,
            String typeKey,
            String title,
            String level,
            int minutes,
            int calories,
            String emoji) {
    }

    public record SleepSound(Long id, String name, String emoji, String audioUrl) {
    }

    public record Device(Long id, String name, String icon, String capabilities, boolean primary) {
    }

    public record WaterPortion(Long id, String name, String sizeLabel, String emoji, int ml) {
    }

    public record Meal(String key, String label, String emoji) {
    }

    public record CatalogResponse(
            List<Food> foods,
            List<ExerciseType> exerciseTypes,
            List<ExerciseLesson> exerciseLessons,
            List<SleepSound> sleepSounds,
            List<Device> devices,
            List<WaterPortion> waterPortions,
            List<Meal> meals,
            List<String> recentFoodNames,
            List<String> frequentFoodNames) {
    }
}
