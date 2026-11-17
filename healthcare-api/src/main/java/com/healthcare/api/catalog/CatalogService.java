package com.healthcare.api.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务基础数据统一从数据库读取，前端不再内置食物/运动/设备等清单。
 */
@Service
public class CatalogService {

    private final JdbcTemplate jdbc;

    public CatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CatalogDtos.CatalogResponse load(Long userId) {
        List<CatalogDtos.Food> foods = jdbc.query(
                "SELECT id, name, portion, calories, emoji, category, is_custom "
                        + "FROM foods ORDER BY sort_order, id",
                (rs, rowNum) -> new CatalogDtos.Food(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("portion"),
                        rs.getInt("calories"),
                        rs.getString("emoji"),
                        rs.getString("category"),
                        rs.getBoolean("is_custom")));

        List<CatalogDtos.ExerciseType> exerciseTypes = jdbc.query(
                "SELECT type_key, name, emoji, bg_color, bar_color, kcal "
                        + "FROM exercise_types ORDER BY sort_order, type_key",
                (rs, rowNum) -> new CatalogDtos.ExerciseType(
                        rs.getString("type_key"),
                        rs.getString("name"),
                        rs.getString("emoji"),
                        rs.getString("bg_color"),
                        rs.getString("bar_color"),
                        rs.getInt("kcal")));

        List<CatalogDtos.ExerciseLesson> exerciseLessons = jdbc.query(
                "SELECT id, type_key, title, level, minutes, calories, emoji "
                        + "FROM exercise_lessons ORDER BY sort_order, id",
                (rs, rowNum) -> new CatalogDtos.ExerciseLesson(
                        rs.getLong("id"),
                        rs.getString("type_key"),
                        rs.getString("title"),
                        rs.getString("level"),
                        rs.getInt("minutes"),
                        rs.getInt("calories"),
                        rs.getString("emoji")));

        List<CatalogDtos.SleepSound> sleepSounds = jdbc.query(
                "SELECT id, name, emoji, audio_url FROM sleep_sounds ORDER BY sort_order, id",
                (rs, rowNum) -> new CatalogDtos.SleepSound(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("emoji"),
                        rs.getString("audio_url")));

        List<CatalogDtos.Device> devices = jdbc.query(
                "SELECT id, name, icon, capabilities, is_primary FROM devices "
                        + "ORDER BY sort_order, id",
                (rs, rowNum) -> new CatalogDtos.Device(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("icon"),
                        rs.getString("capabilities"),
                        rs.getBoolean("is_primary")));

        List<CatalogDtos.WaterPortion> waterPortions = jdbc.query(
                "SELECT id, name, size_label, emoji, ml FROM water_portions "
                        + "ORDER BY sort_order, id",
                (rs, rowNum) -> new CatalogDtos.WaterPortion(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("size_label"),
                        rs.getString("emoji"),
                        rs.getInt("ml")));

        List<CatalogDtos.Meal> meals = jdbc.query(
                "SELECT meal_key, label, emoji FROM meal_types ORDER BY sort_order, meal_key",
                (rs, rowNum) -> new CatalogDtos.Meal(
                        rs.getString("meal_key"),
                        rs.getString("label"),
                        rs.getString("emoji")));

        List<String> recentFoodNames = jdbc.queryForList(
                "SELECT name FROM food_logs WHERE user_id = ? "
                        + "GROUP BY name ORDER BY MAX(created_at) DESC LIMIT 12",
                String.class,
                userId);

        List<String> frequentFoodNames = jdbc.queryForList(
                "SELECT name FROM food_logs WHERE user_id = ? "
                        + "GROUP BY name ORDER BY COUNT(*) DESC, MAX(created_at) DESC LIMIT 12",
                String.class,
                userId);

        return new CatalogDtos.CatalogResponse(
                foods,
                exerciseTypes,
                exerciseLessons,
                sleepSounds,
                devices,
                waterPortions,
                meals,
                recentFoodNames,
                frequentFoodNames);
    }
}
