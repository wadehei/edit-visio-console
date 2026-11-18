package com.visualbuilder.resource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.storage.StorageService;
import com.visualbuilder.resource.entity.Resource;
import com.visualbuilder.resource.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResourceServiceImpl 单元测试 — 聚焦文件校验与列表缓存逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceServiceImpl 文件上传")
class ResourceServiceImplTest {

    @Mock
    private StorageService storageService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceService, "allowedImageTypes", "jpg,jpeg,png,gif");
        ReflectionTestUtils.setField(resourceService, "allowedVideoTypes", "mp4,webm");
        ReflectionTestUtils.setField(resourceService, "allowedAudioTypes", "mp3,wav");
        ReflectionTestUtils.setField(resourceService, "maxImageSize", 10_485_760L);
        ReflectionTestUtils.setField(resourceService, "maxVideoSize", 104_857_600L);
        SecurityUtils.setContext(1L, "tester");
    }

    @AfterEach
    void tearDown() {
        SecurityUtils.clearContext();
    }

    @Test
    @DisplayName("未登录上传抛出 UNAUTHORIZED")
    void upload_unauthenticated() {
        SecurityUtils.clearContext();
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", new byte[10]);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.upload(file, null));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    @DisplayName("空文件抛出 FILE_EMPTY")
    void emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.upload(file, null));
        assertEquals(ErrorCode.FILE_EMPTY, ex.getCode());
    }

    @Test
    @DisplayName("null 文件抛出 FILE_EMPTY")
    void nullFile() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.upload(null, null));
        assertEquals(ErrorCode.FILE_EMPTY, ex.getCode());
    }

    @Test
    @DisplayName("不支持的图片格式抛出 FILE_TYPE_NOT_ALLOWED")
    void invalidImageType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.bmp", "image/bmp", new byte[100]);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.upload(file, null));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getCode());
    }

    @Test
    @DisplayName("图片超过大小限制抛出 FILE_SIZE_EXCEEDED")
    void imageTooLarge() {
        // 11MB > maxImageSize (10MB)
        byte[] bigData = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", bigData);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.upload(file, null));
        assertEquals(ErrorCode.FILE_SIZE_EXCEEDED, ex.getCode());
    }

    // ---------------- Magic Bytes 结构校验 ----------------

    @Test
    @DisplayName("webp 结构完整（RIFF 前缀 + 偏移 8 的 WEBP）校验通过")
    void magicBytes_webp_valid() {
        byte[] data = new byte[12];
        put(data, 0, "RIFF");
        put(data, 8, "WEBP");
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", data);
        assertDoesNotThrow(() -> validateMagicBytes("image", "webp", file));
    }

    @Test
    @DisplayName("webp 偏移 8 处缺少 WEBP 标识抛出 FILE_TYPE_NOT_ALLOWED")
    void magicBytes_webp_missingFormType() {
        byte[] data = new byte[12];
        put(data, 0, "RIFF");
        put(data, 8, "WAVE"); // 合法 RIFF，但不是 webp
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", data);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validateMagicBytes("image", "webp", file));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getCode());
    }

    @Test
    @DisplayName("mp4 结构完整（偏移 4 的 ftyp box）校验通过")
    void magicBytes_mp4_valid() {
        byte[] data = new byte[12];
        data[3] = 0x18; // box 长度
        put(data, 4, "ftyp");
        MockMultipartFile file = new MockMultipartFile("file", "a.mp4", "video/mp4", data);
        assertDoesNotThrow(() -> validateMagicBytes("video", "mp4", file));
    }

    @Test
    @DisplayName("mp4 偏移 4 处缺少 ftyp box 抛出 FILE_TYPE_NOT_ALLOWED")
    void magicBytes_mp4_missingFtyp() {
        byte[] data = new byte[12];
        MockMultipartFile file = new MockMultipartFile("file", "a.mp4", "video/mp4", data);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validateMagicBytes("video", "mp4", file));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getCode());
    }

    @Test
    @DisplayName("前缀签名与声明扩展名不匹配抛出 FILE_TYPE_NOT_ALLOWED")
    void magicBytes_prefixMismatch() {
        byte[] data = new byte[12]; // 声明 png 但内容全 0
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", data);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validateMagicBytes("image", "png", file));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getCode());
    }

    @Test
    @DisplayName("文件头字节不足抛出 FILE_TYPE_NOT_ALLOWED")
    void magicBytes_insufficientBytes() {
        byte[] data = new byte[2]; // png 需要 4 字节
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", data);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validateMagicBytes("image", "png", file));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getCode());
    }

    @Test
    @DisplayName("未登记的扩展名跳过 magic bytes 校验")
    void magicBytes_unknownExtensionSkipped() {
        MockMultipartFile file = new MockMultipartFile("file", "a.xyz", "image/xyz", new byte[12]);
        assertDoesNotThrow(() -> validateMagicBytes("image", "xyz", file));
    }

    @Test
    @DisplayName("扩展名为空或 file 类型跳过 magic bytes 校验")
    void magicBytes_emptyExtensionOrFileTypeSkipped() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a", "application/octet-stream", new byte[12]);
        assertDoesNotThrow(() -> validateMagicBytes("file", "bin", file));
        assertDoesNotThrow(() -> validateMagicBytes("image", "", file));
    }

    @Test
    @DisplayName("读取文件流异常时抛出 FILE_TYPE_NOT_ALLOWED")
    void magicBytes_streamError() throws Exception {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.getInputStream()).thenThrow(new IOException("boom"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validateMagicBytes("image", "png", broken));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, ex.getCode());
    }

    // ---------------- 列表缓存版本号 ----------------

    @Test
    @DisplayName("缓存版本号缺失时降级为 0")
    void currentCacheVersion_missing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("resource:list:ver:1")).thenReturn(null);
        assertEquals(0L, currentCacheVersion(1L));
    }

    @Test
    @DisplayName("缓存版本号可解析时返回实际值")
    void currentCacheVersion_present() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("resource:list:ver:1")).thenReturn("7");
        assertEquals(7L, currentCacheVersion(1L));
    }

    @Test
    @DisplayName("版本号非数字时降级为 0")
    void currentCacheVersion_unparsable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("not-a-number");
        assertEquals(0L, currentCacheVersion(1L));
    }

    @Test
    @DisplayName("Redis 读取异常时缓存版本号降级为 0")
    void currentCacheVersion_redisError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        assertEquals(0L, currentCacheVersion(1L));
    }

    @Test
    @DisplayName("userId 为空时跳过缓存失效，不触碰 Redis")
    void evictCache_nullUser() {
        invokePrivate("evictResourceListCache", new Class<?>[]{Long.class}, (Object) null);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("缓存失效对版本号执行 INCR")
    void evictCache_incrementsVersion() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        invokePrivate("evictResourceListCache", new Class<?>[]{Long.class}, 1L);
        verify(valueOps).increment("resource:list:ver:1");
    }

    @Test
    @DisplayName("INCR 异常时仅告警，不影响主流程")
    void evictCache_incrementError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));
        assertDoesNotThrow(() -> invokePrivate("evictResourceListCache", new Class<?>[]{Long.class}, 1L));
    }

    @Test
    @DisplayName("命中列表缓存时直接返回缓存内容")
    void listResources_cacheHit() throws Exception {
        PageResult<Resource> cached = PageResult.of(Collections.emptyList(), 3L, 1, 20);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("resource:list:ver:1")).thenReturn(null);
        when(valueOps.get("resource:list:1:v0:1:20:_:_")).thenReturn("{\"list\":[]}");
        when(objectMapper.readValue(anyString(), ArgumentMatchers.<TypeReference<PageResult<Resource>>>any()))
                .thenReturn(cached);

        PageResult<Resource> result = resourceService.listResources(null, null, 1, 20);

        assertSame(cached, result);
    }

    // ---------------- helpers ----------------

    /** 反射调用私有 validateMagicBytes。 */
    private void validateMagicBytes(String resourceType, String extension, MultipartFile file) {
        invokePrivate("validateMagicBytes",
                new Class<?>[]{String.class, String.class, MultipartFile.class},
                resourceType, extension, file);
    }

    /** 反射调用私有 currentCacheVersion。 */
    private long currentCacheVersion(long userId) {
        return (Long) invokePrivate("currentCacheVersion", new Class<?>[]{Long.class}, userId);
    }

    /** 反射调用 ResourceServiceImpl 的私有方法，返回调用结果；目标方法抛出的异常按原样重抛。 */
    private Object invokePrivate(String name, Class<?>[] types, Object... args) {
        try {
            Method method = ResourceServiceImpl.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(resourceService, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new IllegalStateException(cause);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void put(byte[] target, int offset, String ascii) {
        byte[] src = ascii.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(src, 0, target, offset, src.length);
    }
}
