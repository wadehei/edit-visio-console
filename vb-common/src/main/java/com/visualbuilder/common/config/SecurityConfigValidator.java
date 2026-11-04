package com.visualbuilder.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * 生产环境启动时校验关键安全配置，防止使用开发默认值上线。
 */
@Slf4j
@Configuration
public class SecurityConfigValidator {

    /** 已知的开发/示例弱密钥片段 —— 生产环境命中即拒绝启动 */
    private static final List<String> KNOWN_INSECURE_SECRETS = List.of(
            "visual-builder-dev-secret",
            "dev-secret",
            "default-secret",
            "changeme",
            "your-secret-key",
            "please-change");

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    public SecurityConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.contains("prod");

        if (!isProd) {
            return;
        }

        // 生产环境强制校验
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 未配置或长度不足！请设置环境变量 JWT_SECRET（至少 32 字符）");
        }
        if (KNOWN_INSECURE_SECRETS.stream().anyMatch(jwtSecret::contains)) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 使用了已知的开发默认值！请立即更换为随机强密钥");
        }

        if (redisPassword == null || redisPassword.isBlank()) {
            log.warn("生产环境 Redis 密码未配置，建议使用 AUTH 认证");
        }

        log.info("✅ 生产环境安全配置校验通过");
    }
}
