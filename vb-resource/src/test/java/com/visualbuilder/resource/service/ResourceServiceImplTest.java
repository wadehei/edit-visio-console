package com.visualbuilder.resource.service;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.storage.StorageService;
import com.visualbuilder.resource.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResourceServiceImpl 单元测试 — 聚焦文件校验逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceServiceImpl 文件上传")
class ResourceServiceImplTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceService, "allowedImageTypes", "jpg,jpeg,png,gif");
        ReflectionTestUtils.setField(resourceService, "allowedVideoTypes", "mp4,webm");
        ReflectionTestUtils.setField(resourceService, "allowedAudioTypes", "mp3,wav");
        ReflectionTestUtils.setField(resourceService, "maxImageSize", 10_485_760L);
        ReflectionTestUtils.setField(resourceService, "maxVideoSize", 104_857_600L);
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
}
