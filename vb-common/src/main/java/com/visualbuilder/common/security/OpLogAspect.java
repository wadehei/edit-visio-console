package com.visualbuilder.common.security;

import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.OpLogEvent;
import com.visualbuilder.common.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作日志 AOP 切面 — 拦截 @OpLog 注解方法，发布 OpLogEvent。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OpLog opLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        int status = 1; // 1=成功
        String errorMsg = null;

        try {
            Object result = point.proceed();
            return result;
        } catch (Throwable e) {
            status = 0;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            try {
                publishLogEvent(point, opLog, costTime, status, errorMsg);
            } catch (Exception e) {
                log.warn("发布操作日志事件失败: {}", e.getMessage());
            }
        }
    }

    private void publishLogEvent(ProceedingJoinPoint point, OpLog opLog,
                                 long costTime, int status, String errorMsg) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();

        // 获取请求信息
        String requestUrl = "";
        String ipAddress = "";
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            requestUrl = request.getRequestURI();
            ipAddress = getClientIp(request);
        }

        // 请求参数（截断避免过长）
        String params = "";
        try {
            Object[] args = point.getArgs();
            params = Arrays.toString(args);
            if (params.length() > 500) {
                params = params.substring(0, 500) + "...";
            }
        } catch (Exception e) {
            params = "参数序列化失败";
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();

        OpLogEvent event = new OpLogEvent(
                this, userId, username,
                opLog.module(), opLog.action(),
                className + "." + methodName,
                requestUrl, params,
                ipAddress, costTime,
                status, errorMsg
        );

        eventPublisher.publishEvent(event);
        log.debug("操作日志: module={}, action={}, userId={}, cost={}ms",
                opLog.module(), opLog.action(), userId, costTime);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
