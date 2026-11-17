package com.healthcare.api.food;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/food-logs")
public class FoodLogController {

    private final FoodLogService foodLogService;

    public FoodLogController(FoodLogService foodLogService) {
        this.foodLogService = foodLogService;
    }

    @GetMapping
    public ApiResponse<FoodLogDtos.FoodLogSummary> list(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String date) {
        return ApiResponse.ok(foodLogService.list(userId, date));
    }

    @PostMapping
    public ApiResponse<FoodLogDtos.CreateFoodLogResponse> create(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody FoodLogDtos.CreateFoodLogRequest request) {
        return ApiResponse.ok(foodLogService.create(userId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id) {
        foodLogService.delete(userId, id);
        return ApiResponse.ok();
    }
}
