package com.visualbuilder.common.security.annotation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 操作日志事件 — OpLogAspect 发布，由 vb-system 模块监听并持久化。
 */
@Getter
public class OpLogEvent extends ApplicationEvent {

    private final Long userId;
    private final String username;
    private final String module;
    private final String action;
    private final String method;
    private final String requestUrl;
    private final String requestParams;
    private final String ipAddress;
    private final long costTime;
    private final int status;
    private final String errorMsg;

    public OpLogEvent(Object source, Long userId, String username,
                      String module, String action, String method,
                      String requestUrl, String requestParams,
                      String ipAddress, long costTime,
                      int status, String errorMsg) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.module = module;
        this.action = action;
        this.method = method;
        this.requestUrl = requestUrl;
        this.requestParams = requestParams;
        this.ipAddress = ipAddress;
        this.costTime = costTime;
        this.status = status;
        this.errorMsg = errorMsg;
    }
}
