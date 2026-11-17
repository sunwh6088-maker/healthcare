package com.healthcare.api.food;

import com.healthcare.api.common.ApiException;
import com.healthcare.api.state.HealthStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
public class FoodLogService {

    private static final Set<String> MEALS = Set.of("breakfast", "lunch", "dinner", "snack");

    private final FoodLogRepository foodLogs;
    private final HealthStateRepository healthStates;

    public FoodLogService(FoodLogRepository foodLogs, HealthStateRepository healthStates) {
        this.foodLogs = foodLogs;
        this.healthStates = healthStates;
    }

    @Transactional(readOnly = true)
    public FoodLogDtos.FoodLogSummary list(Long userId, String date) {
        LocalDate logDate = parseDate(date);
        List<FoodLogEntity> logs = foodLogs.findByUserIdAndLogDateOrderByIdAsc(userId, logDate);
        int total = logs.stream().mapToInt(FoodLogEntity::getCalories).sum();
        return new FoodLogDtos.FoodLogSummary(logDate.toString(), toDtos(logs), total);
    }

    @Transactional
    public FoodLogDtos.CreateFoodLogResponse create(Long userId, FoodLogDtos.CreateFoodLogRequest request) {
        LocalDate logDate = parseDate(request.date());
        if (!MEALS.contains(request.meal())) {
            throw new ApiException(400, "餐次参数不正确");
        }

        FoodLogEntity entity = new FoodLogEntity();
        entity.setUserId(userId);
        entity.setLogDate(logDate);
        entity.setMeal(request.meal());
        entity.setName(request.name().trim());
        entity.setPortion(request.portion() == null ? "" : request.portion().trim());
        entity.setCalories(request.calories());
        entity.setEmoji(request.emoji() == null ? "" : request.emoji());
        FoodLogEntity saved = foodLogs.save(entity);

        int todayTotal = recalcToday(userId);
        return new FoodLogDtos.CreateFoodLogResponse(toDto(saved), todayTotal);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        FoodLogEntity entity = foodLogs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(404, "记录不存在"));
        foodLogs.delete(entity);
        recalcToday(userId);
    }

    private int recalcToday(Long userId) {
        LocalDate today = LocalDate.now();
        int total = foodLogs.sumCalories(userId, today);
        healthStates.findById(userId).ifPresent(state -> {
            state.setFoodKcal(total);
            healthStates.save(state);
        });
        return total;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new ApiException(400, "日期格式应为 yyyy-MM-dd");
        }
    }

    private List<FoodLogDtos.FoodLogDto> toDtos(List<FoodLogEntity> logs) {
        return logs.stream().map(this::toDto).toList();
    }

    private FoodLogDtos.FoodLogDto toDto(FoodLogEntity entity) {
        return new FoodLogDtos.FoodLogDto(
                entity.getId(),
                entity.getLogDate().toString(),
                entity.getMeal(),
                entity.getName(),
                entity.getPortion(),
                entity.getCalories(),
                entity.getEmoji());
    }
}
