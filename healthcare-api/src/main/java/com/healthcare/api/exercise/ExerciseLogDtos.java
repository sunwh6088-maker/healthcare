package com.healthcare.api.exercise;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ExerciseLogDtos {

    private ExerciseLogDtos() {
    }

    public record ExerciseLogDto(Long id, String date, String type, int minutes, int calories) {
    }

    public record CreateExerciseLogRequest(
            @NotBlank(message = "日期不能为空") String date,
            @NotBlank(message = "运动类型不能为空") @Size(max = 16, message = "运动类型过长") String type,
            @Min(value = 1, message = "运动时长至少 1 分钟") int minutes,
            @Min(value = 0, message = "热量不能为负数") int calories) {
    }
}
