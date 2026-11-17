package com.healthcare.api.exercise;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运动记录接口：查询、新增、编辑和删除。
 */
@RestController
@RequestMapping("/api/exercise-logs")
public class ExerciseLogController {

    private final ExerciseLogService exerciseLogService;

    public ExerciseLogController(ExerciseLogService exerciseLogService) {
        this.exerciseLogService = exerciseLogService;
    }

    @GetMapping
    public ApiResponse<List<ExerciseLogDtos.ExerciseLogDto>> list(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(exerciseLogService.list(userId));
    }

    @PostMapping
    public ApiResponse<ExerciseLogDtos.ExerciseLogDto> create(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody ExerciseLogDtos.CreateExerciseLogRequest request) {
        return ApiResponse.ok(exerciseLogService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExerciseLogDtos.ExerciseLogDto> update(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ExerciseLogDtos.CreateExerciseLogRequest request) {
        return ApiResponse.ok(exerciseLogService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id) {
        exerciseLogService.delete(userId, id);
        return ApiResponse.ok();
    }
}
