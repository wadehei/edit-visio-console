package com.visualbuilder.system.listener;

import com.visualbuilder.common.security.annotation.OpLogEvent;
import com.visualbuilder.system.entity.SysLog;
import com.visualbuilder.system.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志事件监听器 — 异步写入 sys_log 表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpLogEventListener {

    private final SysLogMapper sysLogMapper;

    @Async
    @EventListener
    public void onOpLog(OpLogEvent event) {
        try {
            SysLog sysLog = new SysLog();
            sysLog.setUserId(event.getUserId());
            sysLog.setUsername(event.getUsername());
            sysLog.setModule(event.getModule());
            sysLog.setOperation(event.getAction());
            sysLog.setMethod(event.getMethod());
            sysLog.setRequestUrl(event.getRequestUrl());
            sysLog.setRequestParams(event.getRequestParams());
            sysLog.setStatus(event.getStatus());
            sysLog.setErrorMsg(event.getErrorMsg());
            sysLog.setIpAddress(event.getIpAddress());
            sysLog.setCostTime(event.getCostTime());
            sysLog.setCreatedTime(LocalDateTime.now());

            sysLogMapper.insert(sysLog);
        } catch (Exception e) {
            log.error("写入操作日志失败: {}", e.getMessage());
        }
    }
}
