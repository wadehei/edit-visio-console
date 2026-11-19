package com.visualbuilder.resource.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.storage.StorageService;
import com.visualbuilder.common.util.PageUtils;
import com.visualbuilder.common.util.TransactionUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.resource.dto.UploadResultVO;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends BaseServiceImpl<ResourceMapper, Resource>
        implements ResourceService {

    private final StorageService storageService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RESOURCE_CACHE_PREFIX = "resource:list:";
    /** 列表缓存版本号 key 前缀：写操作 INCR 该值即可让旧缓存整体失效，免去按前缀 SCAN 遍历键空间。 */
    private static final String RESOURCE_CACHE_VERSION_PREFIX = "resource:list:ver:";
    private static final Duration RESOURCE_CACHE_TTL = Duration.ofMinutes(5);

    /** Magic bytes 比对窗口：需覆盖到偏移 12，以容纳 RIFF....WEBP 与 ....ftyp 的结构校验。 */
    private static final int MAGIC_HEADER_SIZE = 12;

    /** 目录标识白名单：仅允许字母/数字/下划线/中划线，防止路径穿越污染存储 key。 */
    private static final java.util.regex.Pattern FOLDER_ID_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Value("${business.resource.allowed-image-types:jpg,jpeg,png,gif,webp}")
    private String allowedImageTypes;

    @Value("${business.resource.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${business.resource.max-video-size:104857600}")
    private long maxVideoSize;

    @Value("${business.resource.allowed-video-types:mp4,webm}")
    private String allowedVideoTypes;

    @Value("${business.resource.allowed-audio-types:mp3,wav,ogg,aac}")
    private String allowedAudioTypes;

    /**
     * 文件 Magic Bytes 映射 — 用于校验真实文件类型，防止伪造 Content-Type。
     *
     * <p>这里只登记「前缀即全部签名」的格式；需要按偏移比对的结构化格式（{@code webp}、{@code mp4}）
     * 在 {@link #validateMagicBytes} 内单独校验，以免用弱前缀冒充强签名。</p>
     */
    private static final Map<String, byte[]> MAGIC_BYTES_MAP = new LinkedHashMap<>();
    static {
        MAGIC_BYTES_MAP.put("png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC_BYTES_MAP.put("jpg",  new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES_MAP.put("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES_MAP.put("gif",  new byte[]{0x47, 0x49, 0x46, 0x38});
        MAGIC_BYTES_MAP.put("bmp",  new byte[]{0x42, 0x4D});
        MAGIC_BYTES_MAP.put("webp", new byte[]{0x52, 0x49, 0x46, 0x46}); // RIFF 前缀，完整校验见偏移 8 的 WEBP
        MAGIC_BYTES_MAP.put("pdf",  new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF
    }

    /** webp 容器类型标识：RIFF 之后偏移 8 起的 4 字节，用于与其它 RIFF 容器（wav/avi）区分。 */
    private static final byte[] WEBP_FORM_TYPE = "WEBP".getBytes(StandardCharsets.US_ASCII);
    /** mp4 的 ISO Base Media File Format box 标识：位于偏移 4（前 4 字节为 box 长度）。 */
    private static final byte[] MP4_BOX_TYPE = "ftyp".getBytes(StandardCharsets.US_ASCII);

    /**
     * 扩展名 → 权威 Content-Type 映射：contentType 一律以扩展名推导的服务端权威值落库与上传，
     * 不信任客户端请求头（伪造 {@code text/html} 可被对象存储直接渲染为页面）。
     */
    private static final Map<String, String> EXT_CONTENT_TYPE = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("aac", "audio/aac"));

    @Override
    public UploadResultVO upload(MultipartFile file, String folderId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        // 无扩展名的文件直接拒绝：扩展名是类型校验与权威 Content-Type 推导的锚点，
        // 缺失时所有校验都会被短路，等于放行任意内容
        if (extension.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件缺少扩展名，无法验证文件类型");
        }
        // 扩展名必须已登记权威 Content-Type —— 未登记的类型（如 svg，可承载脚本）不允许上传
        String authoritativeContentType = EXT_CONTENT_TYPE.get(extension);
        if (authoritativeContentType == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "不支持的文件类型: " + extension + "。请上传常见图片/音视频格式");
        }

        String contentType = authoritativeContentType;
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
        // 目录标识来自请求参数，必须白名单校验后再拼接存储 key，防止路径穿越污染存储路径
        String folder = "default";
        if (folderId != null && !folderId.isBlank()) {
            if (!FOLDER_ID_PATTERN.matcher(folderId).matches()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "目录标识非法，仅允许字母、数字、下划线、中划线");
            }
            folder = folderId;
        }
        String storagePath = folder + "/" + datePath + "/" + UUID.randomUUID() + ext;

        // 存储 IO 放在事务之外：远程对象存储上传可能耗时数百毫秒到数秒，
        // 若包在 @Transactional 内会长时间占用数据库连接，放大连接池压力。
        String url;
        try (InputStream is = file.getInputStream()) {
            url = storageService.upload(is, storagePath, contentType);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件上传失败");
        }

        // 落库为单条 INSERT，本身即原子操作，无需事务包覆。
        // 失败时手动清理已写入的存储文件，避免留下「有文件、无记录」的孤儿文件。
        Resource resource = new Resource();
        resource.setUserId(userId);
        resource.setResourceType(resourceType);
        resource.setFileName(originalFilename);
        resource.setStoragePath(storagePath);
        resource.setUrl(url);
        resource.setFileSize(file.getSize());
        resource.setMimeType(contentType);
        resource.setGroupName(folderId);
        try {
            this.save(resource);
        } catch (RuntimeException e) {
            deleteStorageQuietly(storagePath, "上传落库失败");
            throw e;
        }

        // 列表缓存失效，避免新上传资源在缓存中不可见
        evictResourceListCache(userId);

        return UploadResultVO.builder()
                .id(resource.getId())
                .url(url)
                .fileName(originalFilename)
                .resourceType(resourceType)
                .fileSize(file.getSize())
                .build();
    }

    /** 尽力清理存储文件，失败仅告警——用于上传落库失败后的补偿。 */
    private void deleteStorageQuietly(String storagePath, String reason) {
        try {
            storageService.delete(storagePath);
        } catch (Exception ex) {
            log.warn("存储文件清理失败[{}]: path={}, error={}", reason, storagePath, ex.getMessage());
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long resourceId) {
        Long userId = SecurityUtils.requireCurrentUserId();

        Resource resource = this.getById(resourceId);
        if (resource == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");

        // 校验所有权
        if (!resource.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人资源");
        }

        // 软删除（移入回收站），存储文件保留
        this.removeById(resourceId);
        // 列表缓存失效
        evictResourceListCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID 列表不能为空");
        }
        if (ids.size() > LimitsConst.MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次批量删除不能超过" + LimitsConst.MAX_BATCH_SIZE + "条");
        }

        Long userId = SecurityUtils.requireCurrentUserId();

        // 批量查询，避免 N+1
        List<Resource> resources = listByIds(ids);
        Map<Long, Resource> resourceMap = new LinkedHashMap<>();
        for (Resource r : resources) {
            resourceMap.put(r.getId(), r);
        }

        int success = 0;
        int skipped = 0;
        List<Long> toDeleteIds = new ArrayList<>();

        for (Long id : ids) {
            Resource resource = resourceMap.get(id);
            if (resource == null) {
                skipped++;
                continue;
            }
            if (!resource.getUserId().equals(userId)) {
                skipped++;
                continue;
            }
            toDeleteIds.add(id);
            success++;
        }

        // 批量软删除（移入回收站），存储文件保留
        if (!toDeleteIds.isEmpty()) {
            removeByIds(toDeleteIds);
            // 列表缓存失效
            evictResourceListCache(userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", success);
        result.put("skipped", skipped);
        return result;
    }

    @Override
    public PageResult<Resource> listTrash(int page, int pageSize) {
        Long userId = SecurityUtils.requireCurrentUserId();

        int pn = PageUtils.normalizePage(page);
        int ps = PageUtils.normalizePageSize(pageSize);
        var pageParam = new Page<Resource>(pn, ps);
        var result = baseMapper.selectTrashPage(pageParam, userId);
        return PageResult.of(result.getRecords(), result.getTotal(), pn, ps);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreResource(Long resourceId) {
        Long userId = SecurityUtils.requireCurrentUserId();

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
        // 资源回到列表，缓存失效
        evictResourceListCache(userId);
        log.info("恢复资源: resourceId={}", resourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteResource(Long resourceId) {
        Long userId = SecurityUtils.requireCurrentUserId();

        Resource resource = baseMapper.selectByIdIncludeDeleted(resourceId);
        if (resource == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (!resource.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此资源");
        }

        // 永久删除：先删数据库记录（事务内），提交成功后再清理存储文件，
        // 避免「文件已删、记录仍在」产生坏引用；文件清理失败仅告警，不影响记录删除结果。
        int rows = baseMapper.permanentDeleteById(resourceId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String storagePath = resource.getStoragePath();
        TransactionUtils.executeAfterCommit(() -> {
            try {
                storageService.delete(storagePath);
            } catch (Exception e) {
                log.warn("永久删除-存储文件清理失败(需人工补偿): resourceId={}, path={}, error={}",
                        resourceId, storagePath, e.getMessage());
            }
        });
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
     * 校验文件 Magic Bytes — 读取文件头字节与声明类型比对，防止伪造 Content-Type / 扩展名。
     *
     * <p>除前缀签名外，对容器型格式做结构校验：</p>
     * <ul>
     *   <li>{@code webp}：RIFF 前缀之外，还要求偏移 8 起的 4 字节为 {@code WEBP}，
     *       以与 {@code wav}/{@code avi} 等其它 RIFF 容器区分。</li>
     *   <li>{@code mp4}：要求偏移 4 起的 4 字节为 {@code ftyp}（ISO BMFF 的 box 标识）。
     *       旧实现只看前 3 字节为 {@code 0x00}，几乎所有二进制文件都能满足，等同于没有校验。</li>
     * </ul>
     */
    private void validateMagicBytes(String resourceType, String extension, MultipartFile file) {
        if (extension.isEmpty() || "file".equals(resourceType)) return;

        byte[] expected = MAGIC_BYTES_MAP.get(extension);
        boolean webp = "webp".equals(extension);
        boolean mp4 = "mp4".equals(extension);
        if (expected == null && !webp && !mp4) return;

        // 需读取的字节数：webp/mp4 要看到偏移 12 的结构标识，其余只需前缀长度
        int required = webp || mp4 ? MAGIC_HEADER_SIZE : expected.length;
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[MAGIC_HEADER_SIZE];
            int read = readUpTo(is, header);
            if (read < required) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容不足 " + required + " 字节，无法验证文件类型");
            }
            if (expected != null && !matchesAt(header, 0, expected)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容与声明类型不匹配: 扩展名=" + extension + "，疑似伪造文件");
            }
            if (webp && !matchesAt(header, 8, WEBP_FORM_TYPE)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容与声明类型不匹配: 扩展名=webp，偏移 8 处缺少 WEBP 标识");
            }
            if (mp4 && !matchesAt(header, 4, MP4_BOX_TYPE)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                        "文件内容与声明类型不匹配: 扩展名=mp4，偏移 4 处缺少 ftyp box 标识");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "文件类型校验失败: " + e.getMessage());
        }
    }

    /** 比对 {@code header} 自 {@code offset} 起是否与 {@code expected} 逐字节相等。 */
    private static boolean matchesAt(byte[] header, int offset, byte[] expected) {
        if (offset + expected.length > header.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /** {@link InputStream#read} 不保证一次填满缓冲区，循环读取直到填满或流结束，返回实际字节数。 */
    private static int readUpTo(InputStream is, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int n = is.read(buffer, total, buffer.length - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }

    @Override
    public PageResult<Resource> listResources(String resourceType, String groupName, int page, int pageSize) {
        // 未登录一律拒绝
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        // 归一化分页参数，限制 pageSize 取值范围以避免缓存 key 基数膨胀
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), LimitsConst.MAX_PAGE_SIZE);

        // 缓存 key 包含 userId（防止跨用户数据泄露）与当前版本号（写操作后整体失效）
        String cacheKey = RESOURCE_CACHE_PREFIX + currentUserId
                + ":v" + currentCacheVersion(currentUserId)
                + ":" + safePage + ":" + safePageSize
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

        wrapper.eq(Resource::getUserId, currentUserId);
        if (resourceType != null && !resourceType.isBlank()) {
            wrapper.eq(Resource::getResourceType, resourceType);
        }
        if (groupName != null && !groupName.isBlank()) {
            wrapper.eq(Resource::getGroupName, groupName);
        }
        wrapper.orderByDesc(Resource::getCreatedTime);

        var pageParam = new Page<Resource>(safePage, safePageSize);
        var result = this.page(pageParam, wrapper);

        PageResult<Resource> pageResult = PageResult.of(result.getRecords(), result.getTotal(), safePage, safePageSize);

        // 写入缓存（TTL 加随机抖动，避免大量 key 在同一时刻集中过期）
        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(pageResult), jitteredCacheTtl());
        } catch (Exception e) {
            log.warn("写入资源列表缓存失败: {}", e.getMessage());
        }

        return pageResult;
    }

    /**
     * 资源列表缓存 TTL = 基础 5 分钟 + 0~60 秒随机抖动。
     *
     * <p>资源列表缓存 key 维度多（用户/页码/页大小/类型/分组），若不抖动，
     * 批量写入的 key 可能在同一时刻集中过期，形成缓存雪崩。</p>
     */
    private static Duration jitteredCacheTtl() {
        return RESOURCE_CACHE_TTL.plusSeconds(ThreadLocalRandom.current().nextLong(60));
    }

    /**
     * 读取当前用户的列表缓存版本号，缺省（无键或读取失败）视为 0。
     * 读取失败时降级为 0 仍可命中缓存，避免缓存故障放大为接口故障。
     */
    private long currentCacheVersion(Long userId) {
        try {
            String version = redisTemplate.opsForValue().get(versionKey(userId));
            return version == null ? 0L : Long.parseLong(version);
        } catch (Exception e) {
            log.warn("读取资源列表缓存版本失败，降级为 0: userId={}, error={}", userId, e.getMessage());
            return 0L;
        }
    }

    /**
     * 让指定用户的资源列表缓存整体失效：事务提交后把版本号 +1，
     * 旧版本缓存不再被命中并随 TTL 自然过期。
     *
     * <p>相比按前缀 SCAN 后逐键删除，这里只做一次 O(1) 的 INCR，成本与缓存键数量无关，
     * 也不会因键空间增大而线性变慢。写操作（上传/删除/批量删除/恢复）后调用。</p>
     */
    private void evictResourceListCache(Long userId) {
        if (userId == null) {
            return;
        }
        // 事务提交后再失效缓存，避免回滚后旧缓存已被作废
        TransactionUtils.executeAfterCommit(() -> {
            try {
                redisTemplate.opsForValue().increment(versionKey(userId));
            } catch (Exception e) {
                log.warn("失效资源列表缓存失败: userId={}, error={}", userId, e.getMessage());
            }
        });
    }

    private static String versionKey(Long userId) {
        return RESOURCE_CACHE_VERSION_PREFIX + userId;
    }
}
