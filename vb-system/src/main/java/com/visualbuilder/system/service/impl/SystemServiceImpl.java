package com.visualbuilder.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.system.dto.HealthCheckVO;
import com.visualbuilder.system.entity.LoginLog;
import com.visualbuilder.system.entity.SysConfig;
import com.visualbuilder.system.entity.SysLog;
import com.visualbuilder.system.mapper.LoginLogMapper;
import com.visualbuilder.system.mapper.SysConfigMapper;
import com.visualbuilder.system.mapper.SysLogMapper;
import com.visualbuilder.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl extends BaseServiceImpl<SysConfigMapper, SysConfig>
        implements SystemService {

    private final SysLogMapper sysLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CONFIG_CACHE_KEY = "system:configs:all";
    private static final Duration CONFIG_CACHE_TTL = Duration.ofHours(1);

    @Override
    public List<SysConfig> getConfigs() {
        // 先查缓存
        try {
            String cached = redisTemplate.opsForValue().get(CONFIG_CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<SysConfig>>() {});
            }
        } catch (Exception e) {
            log.warn("读取系统配置缓存失败: {}", e.getMessage());
        }

        List<SysConfig> configs = this.list();

        // 写入缓存
        try {
            redisTemplate.opsForValue().set(CONFIG_CACHE_KEY,
                    objectMapper.writeValueAsString(configs), CONFIG_CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入系统配置缓存失败: {}", e.getMessage());
        }

        return configs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String configKey, String configValue) {
        SysConfig config = this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey));
        if (config == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置项不存在");

        config.setConfigValue(configValue);
        this.updateById(config);

        // 清除缓存
        redisTemplate.delete(CONFIG_CACHE_KEY);
        log.info("更新系统配置: key={}", configKey);
    }

    @Override
    public PageResult<SysLog> getLogs(String module, Integer page, Integer pageSize) {
        int pn = page == null ? 1 : page;
        int ps = pageSize == null ? 20 : pageSize;
        Page<SysLog> pageParam = new Page<>(pn, ps);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .like(StringUtils.hasText(module), SysLog::getModule, module)
                .orderByDesc(SysLog::getCreatedTime);
        Page<SysLog> result = sysLogMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pn, ps);
    }

    @Override
    public PageResult<LoginLog> getLoginLogs(String username, Integer page, Integer pageSize) {
        int pn = page == null ? 1 : page;
        int ps = pageSize == null ? 20 : pageSize;
        Page<LoginLog> pageParam = new Page<>(pn, ps);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .orderByDesc(LoginLog::getLoginTime);
        Page<LoginLog> result = loginLogMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pn, ps);
    }

    @Override
    public HealthCheckVO healthCheck() {
        return HealthCheckVO.builder()
                .status("UP")
                .service("vb-system")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
