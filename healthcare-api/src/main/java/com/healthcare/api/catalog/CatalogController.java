package com.healthcare.api.catalog;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse<CatalogDtos.CatalogResponse> get(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(catalogService.load(userId));
    }
}
