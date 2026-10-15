package com.visualbuilder.system.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.system.entity.LoginLog;
import com.visualbuilder.system.entity.SysConfig;
import com.visualbuilder.system.entity.SysLog;
import java.util.List;
import java.util.Map;

public interface SystemService extends BaseService<SysConfig> {
    List<SysConfig> getConfigs();
    void updateConfig(String configKey, String configValue);
    List<SysLog> getLogs(String module, Integer pageNum, Integer pageSize);
    List<LoginLog> getLoginLogs(String username, Integer pageNum, Integer pageSize);
    Map<String, Object> healthCheck();
}
