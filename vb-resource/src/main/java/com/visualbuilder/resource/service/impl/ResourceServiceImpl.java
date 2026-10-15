package com.visualbuilder.resource.service.impl;

import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.storage.StorageService;
import com.visualbuilder.resource.entity.Resource;
import com.visualbuilder.resource.mapper.ResourceMapper;
import com.visualbuilder.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends BaseServiceImpl<ResourceMapper, Resource>
        implements ResourceService {

    private final StorageService storageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(MultipartFile file, String folderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(41001, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storagePath = (folderId != null ? folderId + "/" : "default/") + datePath + "/" + UUID.randomUUID() + extension;

        String contentType = file.getContentType();
        String url;
        try (InputStream is = file.getInputStream()) {
            url = storageService.upload(is, storagePath, contentType);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(50000, "文件上传失败");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String resourceType = resolveResourceType(contentType);

        Resource resource = new Resource();
        resource.setUserId(userId);
        resource.setResourceType(resourceType);
        resource.setFileName(originalFilename);
        resource.setStoragePath(storagePath);
        resource.setUrl(url);
        resource.setFileSize(file.getSize());
        resource.setMimeType(contentType);
        resource.setGroupName(folderId);
        this.save(resource);

        Map<String, Object> result = new HashMap<>();
        result.put("id", resource.getId());
        result.put("url", url);
        result.put("fileName", originalFilename);
        result.put("resourceType", resourceType);
        result.put("fileSize", file.getSize());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long resourceId) {
        Resource resource = this.getById(resourceId);
        if (resource == null) throw new BusinessException(40005, "资源不存在");

        try {
            storageService.delete(resource.getStoragePath());
        } catch (Exception e) {
            log.warn("删除存储文件失败: {}", e.getMessage());
        }
        this.removeById(resourceId);
    }

    private String resolveResourceType(String contentType) {
        if (contentType == null) return "file";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        return "file";
    }
}
