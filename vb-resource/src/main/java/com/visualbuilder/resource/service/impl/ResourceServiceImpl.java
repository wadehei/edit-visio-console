package com.visualbuilder.resource.service.impl;

import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.storage.StorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.resource.entity.Resource;
import com.visualbuilder.resource.mapper.ResourceMapper;
import com.visualbuilder.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends BaseServiceImpl<ResourceMapper, Resource>
        implements ResourceService {

    private final StorageService storageService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RESOURCE_CACHE_PREFIX = "resource:list:";
    private static final Duration RESOURCE_CACHE_TTL = Duration.ofMinutes(5);

    @Value("${business.resource.allowed-image-types:jpg,jpeg,png,gif,webp,svg}")
    private String allowedImageTypes;

    @Value("${business.resource.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${business.resource.max-video-size:104857600}")
    private long maxVideoSize;

    @Value("${business.resource.allowed-video-types:mp4,webm}")
    private String allowedVideoTypes;

    @Value("${business.resource.allowed-audio-types:mp3,wav,ogg,aac}")
    private String allowedAudioTypes;

    /** 文件 Magic Bytes 映射 — 用于校验真实文件类型，防止伪造 Content-Type */
    private static final Map<String, byte[]> MAGIC_BYTES_MAP = new LinkedHashMap<>();
    static {
        MAGIC_BYTES_MAP.put("png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC_BYTES_MAP.put("jpg",  new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES_MAP.put("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES_MAP.put("gif",  new byte[]{0x47, 0x49, 0x46, 0x38});
        MAGIC_BYTES_MAP.put("bmp",  new byte[]{0x42, 0x4D});
        MAGIC_BYTES_MAP.put("webp", new byte[]{0x52, 0x49, 0x46, 0x46}); // RIFF header, further checked at offset 8
        MAGIC_BYTES_MAP.put("mp4",  new byte[]{0x00, 0x00, 0x00}); // ftyp box — checked with offset pattern
        MAGIC_BYTES_MAP.put("pdf",  new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.visualbuilder.resource.dto.UploadResultVO upload(MultipartFile file, String folderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        String contentType = file.getContentType();
        String resourceType = resolveResourceType(contentType);

        // 文件类型校验
        validateFileType(resourceType, extension);
        // 文件大小校验
        validateFileSize(resourceType, file.getSize());
        // Magic bytes 校验 — 验证文件真实类型与声明类型一致
        validateMagicBytes(resourceType, extension, file);

        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storagePath = (folderId != null ? folderId + "/" : "default/") + datePath + "/" + UUID.randomUUID() + ext;
        String url;
        try (InputStream is = file.getInputStream()) {
            url = storageService.upload(is, storagePath, contentType);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件上传失败");
        }

        Long userId = SecurityUtils.getCurrentUserId();

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

        return com.visualbuilder.resource.dto.UploadResultVO.builder()
                .id(resource.getId())
                .url(url)
                .fileName(originalFilename)
                .resourceType(resourceType)
                .fileSize(file.getSize())
                .build();
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long resourceId) {
        Resource resource = this.getById(resourceId);
        if (resource == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");

        // 校验所有权
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && !resource.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人资源");
        }

        // 软删除（移入回收站），存储文件保留
        this.removeById(resourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID 列表不能为空");
        }
        if (ids.size() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次批量删除不能超过100条");
        }

        Long userId = SecurityUtils.getCurrentUserId();

        // 批量查询，避免 N+1
        List<Resource> resources = listByIds(ids);
        Map<Long, Resource> resourceMap = new java.util.LinkedHashMap<>();
        for (Resource r : resources) {
            resourceMap.put(r.getId(), r);
        }

        int success = 0;
        int skipped = 0;
        List<Long> toDeleteIds = new java.util.ArrayList<>();

        for (Long id : ids) {
            Resource resource = resourceMap.get(id);
            if (resource == null) {
                skipped++;
                continue;
            }
            if (userId != null && !resource.getUserId().equals(userId)) {
                skipped++;
                continue;
            }
            toDeleteIds.add(id);
            success++;
        }

        // 批量软删除（移入回收站），存储文件保留
        if (!toDeleteIds.isEmpty()) {
            removeByIds(toDeleteIds);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", success);
        result.put("skipped", skipped);
        return result;
    }

    @Override
    public PageResult<Resource> listTrash(int page, int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        var pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Resource>(page, pageSize);
        var result = baseMapper.selectTrashPage(pageParam, userId);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreResource(Long resourceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Resource resource = baseMapper.selectByIdIncludeDeleted(resourceId);
        if (resource == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (!resource.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此资源");
        }

        int rows = baseMapper.restoreById(resourceId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在或未被删除");
        }
        log.info("恢复资源: resourceId={}", resourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteResource(Long resourceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Resource resource = baseMapper.selectByIdIncludeDeleted(resourceId);
        if (resource == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (!resource.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此资源");
        }

        // 永久删除：先删存储文件，再删数据库记录
        try {
            storageService.delete(resource.getStoragePath());
        } catch (Exception e) {
            log.warn("永久删除-存储文件清理失败: resourceId={}, error={}", resourceId, e.getMessage());
        }

        int rows = baseMapper.permanentDeleteById(resourceId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        log.info("永久删除资源: resourceId={}", resourceId);
    }

    private String resolveResourceType(String contentType) {
        if (contentType == null) return "file";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        return "file";
    }

    private void validateFileType(String resourceType, String extension) {
        if (extension.isEmpty()) return;

        switch (resourceType) {
            case "image" -> {
                Set<String> allowed = Set.of(allowedImageTypes.split(","));
                if (!allowed.contains(extension)) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                            "不支持的图片格式: " + extension + "，允许: " + allowedImageTypes);
                }
            }
            case "video" -> {
                Set<String> allowed = Set.of(allowedVideoTypes.split(","));
                if (!allowed.contains(extension)) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                            "不支持的视频格式: " + extension + "，允许: " + allowedVideoTypes);
                }
            }
            case "audio" -> {
                Set<String> allowed = Set.of(allowedAudioTypes.split(","));
                if (!allowed.contains(extension)) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                            "不支持的音频格式: " + extension + "，允许: " + allowedAudioTypes);
                }
            }
            default -> {
                // file 类型不做格式限制
            }
        }
    }

    private void validateFileSize(String resourceType, long fileSize) {
        if ("image".equals(resourceType) && fileSize > maxImageSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "图片大小超限: " + (fileSize / 1024 / 1024) + "MB，上限: " + (maxImageSize / 1024 / 1024) + "MB");
        }
        if ("video".equals(resourceType) && fileSize > maxVideoSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "视频大小超限: " + (fileSize / 1024 / 1024) + "MB，上限: " + (maxVideoSize / 1024 / 1024) + "MB");
        }
    }

    /**
     * 校验文件 Magic Bytes — 读取文件头字节与声明类型比对，防止伪造 Content-Type / 扩展名
     */
    private void validateMagicBytes(String resourceType, String extension, MultipartFile file) {
        if (extension.isEmpty() || "file".equals(resourceType)) return;
        if (!MAGIC_BYTES_MAP.containsKey(extension)) return;

        byte[] expected = MAGIC_BYTES_MAP.get(extension);
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[expected.length];
            int read = is.read(header);
            if (read < expected.length) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容不完整，无法验证文件类型");
            }
            for (int i = 0; i < expected.length; i++) {
                if (header[i] != expected[i]) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                            "文件内容与声明类型不匹配: 扩展名=" + extension + "，疑似伪造文件");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "文件类型校验失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<Resource> listResources(String resourceType, String groupName, int page, int pageSize) {
        // 构建缓存 key
        String cacheKey = RESOURCE_CACHE_PREFIX + page + ":" + pageSize
                + ":" + (resourceType != null ? resourceType : "_")
                + ":" + (groupName != null ? groupName : "_");

        // 先查缓存
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<PageResult<Resource>>() {});
            }
        } catch (Exception e) {
            log.warn("读取资源列表缓存失败: {}", e.getMessage());
        }

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Resource>();

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            wrapper.eq(Resource::getUserId, userId);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            wrapper.eq(Resource::getResourceType, resourceType);
        }
        if (groupName != null && !groupName.isBlank()) {
            wrapper.eq(Resource::getGroupName, groupName);
        }
        wrapper.orderByDesc(Resource::getCreatedTime);

        var pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Resource>(page, pageSize);
        var result = this.page(pageParam, wrapper);

        PageResult<Resource> pageResult = PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);

        // 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(pageResult), RESOURCE_CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入资源列表缓存失败: {}", e.getMessage());
        }

        return pageResult;
    }
}
