package com.visualbuilder.publish.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 发布模块配置属性。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "business.publish")
public class PublishProperties {

    /** 发布文件在存储中的路径前缀 */
    private String storagePrefix = "published/";

    /** 渲染最大递归深度 */
    private int maxRenderDepth = 50;

    /** 单页面最大组件数 */
    private int maxComponentCount = 1000;

    /**
     * 每个页面在存储中保留的历史产物版本数（含最新版本）。
     * 产物路径为 {@code 前缀/项目/页面/v版本号/index.html}，不含日期段，因此旧版本可被精确清理。
     */
    private int retainArtifactVersions = 5;

    /** 发布失败后的最大重试次数（不含首次执行） */
    private int maxRetry = 2;

    /** 每次重试的间隔毫秒数（阻塞当前消费线程，发布为低频操作） */
    private long retryIntervalMs = 2000;
}
