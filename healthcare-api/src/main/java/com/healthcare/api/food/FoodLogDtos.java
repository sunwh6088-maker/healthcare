package com.healthcare.api.food;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class FoodLogDtos {

    private FoodLogDtos() {
    }

    public record FoodLogDto(
            Long id,
            String date,
            String meal,
            String name,
            String portion,
            int calories,
            String emoji) {
    }

    public record CreateFoodLogRequest(
            @NotBlank(message = "日期不能为空") String date,
            @NotBlank(message = "餐次不能为空") String meal,
            @NotBlank(message = "食物名称不能为空") @Size(max = 64, message = "食物名称过长") String name,
            @Size(max = 64, message = "份量描述过长") String portion,
            @Min(value = 0, message = "热量不能为负数") int calories,
            @Size(max = 16, message = "emoji 过长") String emoji) {
    }

    public record FoodLogSummary(String date, List<FoodLogDto> logs, int totalCalories) {
    }

    public record CreateFoodLogResponse(FoodLogDto log, int todayTotalCalories) {
    }
}
