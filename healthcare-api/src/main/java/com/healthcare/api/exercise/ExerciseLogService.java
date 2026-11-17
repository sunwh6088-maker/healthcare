package com.healthcare.api.exercise;

import com.healthcare.api.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 运动记录业务：类型/日期校验、增删改和活动量汇总。
 */
@Service
public class ExerciseLogService {

    private final ExerciseLogRepository logs;
    private final JdbcTemplate jdbc;

    public ExerciseLogService(ExerciseLogRepository logs, JdbcTemplate jdbc) {
        this.logs = logs;
        this.jdbc = jdbc;
    }

    // ===== 查询 =====
    @Transactional(readOnly = true)
    public List<ExerciseLogDtos.ExerciseLogDto> list(Long userId) {
        return logs.findByUserIdOrderByLogDateDescIdDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    // ===== 新增 / 编辑 / 删除 =====
    @Transactional
    public ExerciseLogDtos.ExerciseLogDto create(
            Long userId, ExerciseLogDtos.CreateExerciseLogRequest request) {
        ExerciseLogEntity entity = new ExerciseLogEntity();
        entity.setUserId(userId);
        apply(entity, request);
        ExerciseLogEntity saved = logs.save(entity);
        logs.flush();
        ExerciseLogDtos.ExerciseLogDto dto = toDto(saved);
        recalculateAggregates(userId);
        return dto;
    }

    @Transactional
    public ExerciseLogDtos.ExerciseLogDto update(
            Long userId, Long id, ExerciseLogDtos.CreateExerciseLogRequest request) {
        ExerciseLogEntity entity = logs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(404, "记录不存在"));
        apply(entity, request);
        ExerciseLogDtos.ExerciseLogDto dto = toDto(logs.save(entity));
        logs.flush();
        recalculateAggregates(userId);
        return dto;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ExerciseLogEntity entity = logs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(404, "记录不存在"));
        logs.delete(entity);
        logs.flush();
        recalculateAggregates(userId);
    }

    // ===== 请求校验：类型来自 exercise_types 表，日期必须为 yyyy-MM-dd =====
    private void apply(ExerciseLogEntity entity, ExerciseLogDtos.CreateExerciseLogRequest request) {
        if (!exerciseTypeExists(request.type())) {
            throw new ApiException(400, "运动类型参数不正确");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.date());
        } catch (DateTimeParseException ex) {
            throw new ApiException(400, "日期格式应为 yyyy-MM-dd");
        }
        entity.setLogDate(date);
        entity.setType(request.type());
        entity.setMinutes(request.minutes());
        entity.setCalories(request.calories());
    }

    private boolean exerciseTypeExists(String type) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercise_types WHERE type_key = ?",
                Integer.class,
                type);
        return count != null && count > 0;
    }

    // ===== 活动量汇总 =====
    /**
     * 活动时长和运动次数直接由运动记录汇总，避免前端累加与多设备不同步。
     */
    private void recalculateAggregates(Long userId) {
        jdbc.update("""
                UPDATE health_states
                SET active_minutes = (SELECT COALESCE(SUM(minutes), 0) FROM exercise_logs WHERE user_id = ?),
                    exercise_count = (SELECT COUNT(*) FROM exercise_logs WHERE user_id = ?),
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """,
                userId,
                userId,
                userId);
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
