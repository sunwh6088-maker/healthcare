package com.healthcare.api.state;

import com.healthcare.api.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 健康状态业务：默认值、校验、DTO 转换和闹钟替换。
 */
@Service
public class StateService {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Set<String> GENDERS = Set.of("female", "male");

    private final HealthStateRepository states;
    private final AlarmRepository alarms;

    public StateService(HealthStateRepository states, AlarmRepository alarms) {
        this.states = states;
        this.alarms = alarms;
    }

    @Transactional
    public StateDto get(Long userId) {
        HealthStateEntity state = states.findById(userId)
                .orElseGet(() -> states.save(HealthStateEntity.defaults(userId)));
        return toDto(state, alarms.findByUserIdOrderByTimeAsc(userId));
    }

    @Transactional
    public StateDto save(Long userId, StateDto dto) {
        if (dto == null || dto.health() == null) {
            throw new ApiException(400, "健康数据不能为空");
        }
        validateHealth(dto.health());

        HealthStateEntity state = states.findById(userId)
                .orElseGet(() -> HealthStateEntity.defaults(userId));
        applyHealth(state, dto.health());
        states.save(state);

        alarms.deleteByUserId(userId);
        List<AlarmEntity> saved = new ArrayList<>();
        if (dto.alarms() != null) {
            for (StateDto.AlarmDto alarm : dto.alarms()) {
                if (alarm == null) {
                    continue;
                }
                AlarmEntity entity = toEntity(userId, alarm);
                saved.add(alarms.save(entity));
            }
        }
        return toDto(state, saved);
    }

    private StateDto toDto(HealthStateEntity state, List<AlarmEntity> alarmEntities) {
        StateDto.HealthDto health = new StateDto.HealthDto(
                state.getSteps(),
                state.getStepsGoal(),
                state.getWaterMl(),
                state.getWaterGoalMl(),
                state.getActiveMinutes(),
                state.getActiveGoalMinutes(),
                state.getSleepMinutes(),
                state.getSleepStartMinutes(),
                state.getFoodKcal(),
                state.getExerciseCount(),
                state.isNotifyEnabled(),
                state.isPrivacyEnabled(),
                state.isDeviceConnected(),
                state.isDeviceAdded(),
                state.getDeviceBattery(),
                state.getDeviceLastSyncAt(),
                state.getGender(),
                state.getAge(),
                state.getHeightCm(),
                state.getWeightKg());
        List<StateDto.AlarmDto> alarmDtos = alarmEntities.stream()
                .map(a -> new StateDto.AlarmDto(
                        a.getId(),
                        a.getTime(),
                        a.getLabel(),
                        parseRepeat(a.getRepeatDays()),
                        a.isEnabled()))
                .toList();
        return new StateDto(health, alarmDtos);
    }

    private void applyHealth(HealthStateEntity state, StateDto.HealthDto health) {
        state.setSteps(health.steps());
        state.setStepsGoal(health.stepsGoal());
        state.setWaterMl(health.waterMl());
        state.setWaterGoalMl(health.waterGoalMl());
        state.setActiveMinutes(health.activeMinutes());
        state.setActiveGoalMinutes(health.activeGoalMinutes());
        state.setSleepMinutes(health.sleepMinutes());
        state.setSleepStartMinutes(health.sleepStartMinutes());
        state.setFoodKcal(health.foodKcal());
        state.setExerciseCount(health.exerciseCount());
        state.setNotifyEnabled(health.notifyEnabled());
        state.setPrivacyEnabled(health.privacyEnabled());
        state.setDeviceConnected(health.deviceConnected());
        state.setDeviceAdded(health.deviceAdded());
        state.setDeviceBattery(health.deviceBattery());
        if (health.deviceLastSyncAt() != null) {
            state.setDeviceLastSyncAt(health.deviceLastSyncAt());
        }
        state.setGender(health.gender());
        state.setAge(health.age());
        state.setHeightCm(health.heightCm());
        state.setWeightKg(health.weightKg());
    }

    private void validateHealth(StateDto.HealthDto health) {
        requireNonNegative("steps", health.steps());
        requireNonNegative("stepsGoal", health.stepsGoal());
        requireNonNegative("waterMl", health.waterMl());
        requireNonNegative("waterGoalMl", health.waterGoalMl());
        requireNonNegative("activeMinutes", health.activeMinutes());
        requireNonNegative("activeGoalMinutes", health.activeGoalMinutes());
        requireNonNegative("sleepMinutes", health.sleepMinutes());
        requireNonNegative("sleepStartMinutes", health.sleepStartMinutes());
        requireNonNegative("foodKcal", health.foodKcal());
        requireNonNegative("exerciseCount", health.exerciseCount());
        if (health.age() < 0 || health.age() > 150) {
            throw new ApiException(400, "年龄超出范围");
        }
        if (health.heightCm() < 0 || health.heightCm() > 300) {
            throw new ApiException(400, "身高超出范围");
        }
        if (health.weightKg() < 0 || health.weightKg() > 500) {
            throw new ApiException(400, "体重超出范围");
        }
        if (health.deviceBattery() < 0 || health.deviceBattery() > 100) {
            throw new ApiException(400, "设备电量超出范围");
        }
        if (health.gender() != null && !GENDERS.contains(health.gender())) {
            throw new ApiException(400, "性别参数不正确");
        }
    }

    private void requireNonNegative(String field, int value) {
        if (value < 0) {
            throw new ApiException(400, field + " 不能为负数");
        }
    }

    private AlarmEntity toEntity(Long userId, StateDto.AlarmDto alarm) {
        if (alarm.time() == null || !TIME_PATTERN.matcher(alarm.time()).matches()) {
            throw new ApiException(400, "闹钟时间格式应为 HH:mm");
        }
        AlarmEntity entity = new AlarmEntity();
        entity.setUserId(userId);
        entity.setTime(alarm.time());
        String label = alarm.label() == null ? "" : alarm.label().trim();
        entity.setLabel(label.isEmpty() ? "闹钟" : label.substring(0, Math.min(label.length(), 64)));
        entity.setRepeatDays(toRepeatCsv(alarm.repeat()));
        entity.setEnabled(alarm.on());
        return entity;
    }

    private String toRepeatCsv(List<Integer> repeat) {
        if (repeat == null || repeat.isEmpty()) {
            return "";
        }
        return repeat.stream()
                .filter(day -> day != null && day >= 0 && day <= 6)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Integer> parseRepeat(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .filter(part -> !part.isBlank())
                .map(Integer::parseInt)
                .toList();
    }
}
