package com.healthcare.api.importdata;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 每日健康指标查询：供历史页/步骤页展示快捷指令导入的多日数据。
 */
@RestController
@RequestMapping("/api/health-metrics")
public class HealthMetricController {

    private final HealthImportService importService;

    public HealthMetricController(HealthImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public ApiResponse<List<HealthImportDtos.MetricDto>> list(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.ok(importService.listMetrics(userId, from, to));
    }
}
