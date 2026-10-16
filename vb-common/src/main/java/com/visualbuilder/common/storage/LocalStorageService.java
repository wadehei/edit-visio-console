package com.visualbuilder.common.storage;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
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
            Path fullPath = resolveSafePath(path);
            Files.createDirectories(fullPath.getParent());
            Files.copy(stream, fullPath, StandardCopyOption.REPLACE_EXISTING);
            String url = baseUrl + "/" + path;
            log.info("文件上传成功: {} -> {}", path, url);
            return url;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("本地文件上传失败: path={}", path, e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件上传失败");
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path fullPath = resolveSafePath(path);
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

    /**
     * 安全解析路径 — 防止路径穿越攻击
     */
    private Path resolveSafePath(String path) {
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path full = base.resolve(path).normalize();

        if (!full.startsWith(base)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法文件路径");
        }
        return full;
    }
}
