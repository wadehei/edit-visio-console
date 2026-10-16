package com.visualbuilder.system.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.system.dto.HealthCheckVO;
import com.visualbuilder.system.entity.LoginLog;
import com.visualbuilder.system.entity.SysConfig;
import com.visualbuilder.system.entity.SysLog;
import java.util.List;

public interface SystemService extends BaseService<SysConfig> {
    List<SysConfig> getConfigs();
    void updateConfig(String configKey, String configValue);
    PageResult<SysLog> getLogs(String module, Integer pageNum, Integer pageSize);
    PageResult<LoginLog> getLoginLogs(String username, Integer pageNum, Integer pageSize);
    HealthCheckVO healthCheck();
}
