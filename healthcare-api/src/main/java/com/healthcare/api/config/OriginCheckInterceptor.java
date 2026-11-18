package com.healthcare.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CSRF 防护：对 Cookie 认证下的写操作校验 Origin/Referer。
 *
 * <p>浏览器发起的跨站 POST/PUT/PATCH/DELETE 会携带 Origin，若来源既不是同源站点、
 * 也不在白名单中则拒绝。非浏览器客户端（无 Origin/Referer，如脚本、App）不受影响；
 * 真正的 CSRF 攻击来自浏览器，无法省略这两个头。</p>
 */
@Component
public class OriginCheckInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final Set<String> allowedOrigins;

    public OriginCheckInterceptor(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:5175}")
            String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(OriginCheckInterceptor::normalizeOrigin)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }

        String origin = normalizeOrigin(request.getHeader("Origin"));
        String referer = normalizeOrigin(request.getHeader("Referer"));
        String source = !origin.isEmpty() ? origin : referer;
        // 无 Origin/Referer 的请求来自脚本或 App；"null" 是不透明来源（如沙箱 iframe），一律拒绝
        if (source.isEmpty()) {
            return true;
        }
        if (source.equals("null")) {
            return reject(response);
        }

        String sameOrigin = normalizeOrigin(request.getScheme() + "://" + request.getServerName()
                + portSuffix(request));
        if (source.equals(sameOrigin) || allowedOrigins.contains(source)) {
            return true;
        }

        return reject(response);
    }

    private boolean reject(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"请求来源不被允许\",\"data\":null}");
        return false;
    }

    private String portSuffix(HttpServletRequest request) {
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
        return defaultPort ? "" : ":" + port;
    }

    /** 统一成 scheme://host[:port] 的小写形式，忽略末尾斜杠。 */
    private static String normalizeOrigin(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        int pathIndex = trimmed.indexOf('/', trimmed.indexOf("//") + 2);
        if (pathIndex > 0) {
            trimmed = trimmed.substring(0, pathIndex);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.toLowerCase();
    }
}
