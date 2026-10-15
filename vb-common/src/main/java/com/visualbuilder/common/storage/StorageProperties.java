package com.visualbuilder.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String type = "minio";
    private MinioProperties minio = new MinioProperties();

    @Data
    public static class MinioProperties {
        private String endpoint = "http://localhost:9000";
        private String accessKey;
        private String secretKey;
        private String bucket = "visual-builder";
    }
}
