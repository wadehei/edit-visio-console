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
}
