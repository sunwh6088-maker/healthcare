package com.healthcare.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 登录拦截器：从 HttpOnly Cookie 或 Bearer 头解析 token。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTR = "userId";

    private final TokenService tokenService;
    private final AuthCookieService authCookies;

    public AuthInterceptor(TokenService tokenService, AuthCookieService authCookies) {
        this.tokenService = tokenService;
        this.authCookies = authCookies;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Optional<Long> userId = tokenService.resolve(authCookies.readToken(request));
        if (userId.isEmpty()) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                userId = tokenService.resolve(header.substring("Bearer ".length()));
            }
        }
        if (userId.isPresent()) {
            request.setAttribute(USER_ID_ATTR, userId.get());
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }
}
