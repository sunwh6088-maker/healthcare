package com.healthcare.api.user;

import com.healthcare.api.exercise.ExerciseLogEntity;
import com.healthcare.api.exercise.ExerciseLogRepository;
import com.healthcare.api.food.FoodLogEntity;
import com.healthcare.api.food.FoodLogRepository;
import com.healthcare.api.state.StateService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账号数据导出：汇总账号资料、健康状态、食物和运动记录，满足个人信息可携带权。
 */
@Service
public class AccountExportService {

    private final UserService users;
    private final StateService states;
    private final FoodLogRepository foods;
    private final ExerciseLogRepository exercises;

    public AccountExportService(UserService users,
                                StateService states,
                                FoodLogRepository foods,
                                ExerciseLogRepository exercises) {
        this.users = users;
        this.states = states;
        this.foods = foods;
        this.exercises = exercises;
    }

    public Map<String, Object> export(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exportedAt", Instant.now().toString());
        result.put("account", UserDto.from(users.get(userId)));
        result.put("health", states.get(userId));
        result.put("foodLogs", foods.findByUserIdOrderByLogDateDescIdDesc(userId).stream()
                .map(ExportFood::from)
                .toList());
        result.put("exerciseLogs", exercises.findByUserIdOrderByLogDateDescIdDesc(userId).stream()
                .map(ExportExercise::from)
                .toList());
        return result;
    }

    record ExportFood(
            Long id,
            LocalDate date,
            String meal,
            String name,
            String portion,
            int calories,
            String emoji,
            Instant createdAt) {

        static ExportFood from(FoodLogEntity entity) {
            return new ExportFood(
                    entity.getId(),
                    entity.getLogDate(),
                    entity.getMeal(),
                    entity.getName(),
                    entity.getPortion(),
                    entity.getCalories(),
                    entity.getEmoji(),
                    entity.getCreatedAt());
        }
    }

    record ExportExercise(
            Long id,
            LocalDate date,
            String type,
            int minutes,
            int calories,
            Instant createdAt) {

        static ExportExercise from(ExerciseLogEntity entity) {
            return new ExportExercise(
                    entity.getId(),
                    entity.getLogDate(),
                    entity.getType(),
                    entity.getMinutes(),
                    entity.getCalories(),
                    entity.getCreatedAt());
        }
    }
}
