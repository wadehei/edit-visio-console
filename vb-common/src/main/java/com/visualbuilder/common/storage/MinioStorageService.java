package com.visualbuilder.common.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    public MinioStorageService(StorageProperties properties) {
        this.storageProperties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getMinio().getEndpoint())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
        // 初始化时确保 Bucket 存在
        ensureBucketExists(properties.getMinio().getBucket());
    }

    @Override
    public String upload(InputStream stream, String path, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(path)
                    .stream(stream, -1, 10485760)
                    .contentType(contentType)
                    .build());
            return storageProperties.getMinio().getEndpoint() + "/" +
                    storageProperties.getMinio().getBucket() + "/" + path;
        } catch (Exception e) {
            log.error("MinIO上传失败: path={}", path, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.error("MinIO删除失败: path={}", path, e);
        }
    }

    @Override
    public String getPresignedUrl(String path, int expireMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(path)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("MinIO预签名URL生成失败: path={}", path, e);
            throw new RuntimeException("获取文件访问链接失败", e);
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO Bucket [{}] 创建成功", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO Bucket检查失败", e);
        }
    }
}
