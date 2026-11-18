package com.healthcare.api.auth;

/**
 * 从 User-Agent 生成简短的设备描述，例如「Windows · Edge」。
 */
public final class DeviceNames {

    private DeviceNames() {
    }

    public static String describe(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "未知设备";
        }
        String system = systemOf(userAgent);
        String browser = browserOf(userAgent);
        return system + " · " + browser;
    }

    private static String systemOf(String ua) {
        if (ua.contains("Windows NT")) return "Windows";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "未知系统";
    }

    private static String browserOf(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        if (ua.contains("HealthcareApp")) return "客户端";
        return "浏览器";
    }
}
