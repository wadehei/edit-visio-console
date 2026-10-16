package com.visualbuilder.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IP 地址工具类 — 从 HttpServletRequest 中提取客户端真实 IP。
 *
 * <p>支持常见反向代理头：X-Forwarded-For、X-Real-IP、Proxy-Client-IP 等。</p>
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtils() {}

    /**
     * 获取当前请求的客户端真实 IP。
     *
     * @return IP 地址，无法获取时返回空字符串
     */
    public static String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        return getClientIp(attrs.getRequest());
    }

    /**
     * 从 HttpServletRequest 中提取客户端真实 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        // 依次检查代理头
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }

        // 回退到 remoteAddr
        String ip = request.getRemoteAddr();
        return isValidIp(ip) ? ip : "";
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }
}
