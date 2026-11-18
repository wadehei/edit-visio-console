package com.visualbuilder.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    /** 存储类型：local / minio；缺省与 LocalStorageService 的 matchIfMissing 保持一致 */
    private String type = "local";
    private MinioProperties minio = new MinioProperties();

    @Data
    public static class MinioProperties {
        private String endpoint = "http://localhost:9000";
        private String accessKey;
        private String secretKey;
        private String bucket = "visual-builder";
    }
}
