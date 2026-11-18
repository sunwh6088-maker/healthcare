package com.healthcare.api.config;

import com.healthcare.api.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web 配置：登录拦截器、CSRF 来源校验和 CORS 白名单。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final OriginCheckInterceptor originCheckInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     OriginCheckInterceptor originCheckInterceptor,
                     @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:5175}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.originCheckInterceptor = originCheckInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先校验来源，再校验登录
        registry.addInterceptor(originCheckInterceptor)
                .addPathPatterns("/api/**")
                .order(0);
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**")
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
