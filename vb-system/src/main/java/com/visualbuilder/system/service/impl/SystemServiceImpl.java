package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.system.entity.LoginLog;
import com.visualbuilder.system.entity.SysConfig;
import com.visualbuilder.system.entity.SysLog;
import com.visualbuilder.system.mapper.LoginLogMapper;
import com.visualbuilder.system.mapper.SysConfigMapper;
import com.visualbuilder.system.mapper.SysLogMapper;
import com.visualbuilder.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl extends BaseServiceImpl<SysConfigMapper, SysConfig>
        implements SystemService {

    private final SysLogMapper sysLogMapper;
    private final LoginLogMapper loginLogMapper;

    @Override
    public List<SysConfig> getConfigs() {
        return this.list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String configKey, String configValue) {
        SysConfig config = this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey));
        if (config == null) throw new BusinessException(40005, "配置项不存在");

        config.setConfigValue(configValue);
        this.updateById(config);
        log.info("更新系统配置: key={}", configKey);
    }

    @Override
    public List<SysLog> getLogs(String module, Integer pageNum, Integer pageSize) {
        Page<SysLog> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .like(StringUtils.hasText(module), SysLog::getModule, module)
                .orderByDesc(SysLog::getCreatedTime);
        Page<SysLog> result = sysLogMapper.selectPage(page, wrapper);
        return result.getRecords();
    }

    @Override
    public List<LoginLog> getLoginLogs(String username, Integer pageNum, Integer pageSize) {
        Page<LoginLog> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .orderByDesc(LoginLog::getCreatedTime);
        Page<LoginLog> result = loginLogMapper.selectPage(page, wrapper);
        return result.getRecords();
    }

    @Override
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "vb-system");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
