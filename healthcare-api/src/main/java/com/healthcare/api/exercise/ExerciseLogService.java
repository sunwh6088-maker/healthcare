package com.healthcare.api.exercise;

import com.healthcare.api.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
public class ExerciseLogService {

    private static final Set<String> TYPES = Set.of("yoga", "walk", "run", "ride");

    private final ExerciseLogRepository logs;

    public ExerciseLogService(ExerciseLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public List<ExerciseLogDtos.ExerciseLogDto> list(Long userId) {
        return logs.findByUserIdOrderByLogDateDescIdDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ExerciseLogDtos.ExerciseLogDto create(
            Long userId, ExerciseLogDtos.CreateExerciseLogRequest request) {
        if (!TYPES.contains(request.type())) {
            throw new ApiException(400, "运动类型参数不正确");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.date());
        } catch (DateTimeParseException ex) {
            throw new ApiException(400, "日期格式应为 yyyy-MM-dd");
        }

        ExerciseLogEntity entity = new ExerciseLogEntity();
        entity.setUserId(userId);
        entity.setLogDate(date);
        entity.setType(request.type());
        entity.setMinutes(request.minutes());
        entity.setCalories(request.calories());
        return toDto(logs.save(entity));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ExerciseLogEntity entity = logs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(404, "记录不存在"));
        logs.delete(entity);
    }

    private ExerciseLogDtos.ExerciseLogDto toDto(ExerciseLogEntity entity) {
        return new ExerciseLogDtos.ExerciseLogDto(
                entity.getId(),
                entity.getLogDate().toString(),
                entity.getType(),
                entity.getMinutes(),
                entity.getCalories());
    }
}
