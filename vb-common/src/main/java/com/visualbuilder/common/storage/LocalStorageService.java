package com.visualbuilder.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现 — 开发环境使用
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public String upload(InputStream stream, String path, String contentType) {
        try {
            Path fullPath = Paths.get(basePath, path);
            Files.createDirectories(fullPath.getParent());
            Files.copy(stream, fullPath, StandardCopyOption.REPLACE_EXISTING);
            String url = baseUrl + "/" + path;
            log.info("文件上传成功: {} -> {}", path, url);
            return url;
        } catch (IOException e) {
            log.error("本地文件上传失败: path={}", path, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path fullPath = Paths.get(basePath, path);
            Files.deleteIfExists(fullPath);
            log.info("文件删除成功: {}", path);
        } catch (IOException e) {
            log.error("本地文件删除失败: path={}", path, e);
        }
    }

    @Override
    public String getPresignedUrl(String path, int expireMinutes) {
        // 本地存储不需要预签名，直接返回 URL
        return baseUrl + "/" + path;
    }
}
