package com.visualbuilder.publish.controller;

import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.publish.service.PublishService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * PublishController 单元测试 — 聚焦发布备注的长度校验边界。
 *
 * <p>控制器上的 {@code @Validated} + {@code @Size} 依赖方法级校验代理生效，
 * 因此这里通过 {@link MethodValidationPostProcessor} 显式获取代理对象后再调用。</p>
 */
@DisplayName("PublishController 发布接口")
class PublishControllerTest {

    private PublishService publishService;
    private PublishController controller;

    @BeforeEach
    void setUp() {
        publishService = mock(PublishService.class);
        PublishController target = new PublishController(publishService);

        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setProxyTargetClass(true);
        processor.afterPropertiesSet();

        controller = (PublishController) processor.postProcessAfterInitialization(target, "publishController");
    }

    @Test
    @DisplayName("备注长度恰好等于上限时放行")
    void acceptsChangelogAtLimit() {
        String changelog = "x".repeat(LimitsConst.MAX_TEXT_LENGTH);

        controller.publish(1L, 2L, changelog);

        verify(publishService).publish(1L, 2L, changelog);
    }

    @Test
    @DisplayName("备注长度超过上限时抛出校验异常，且不触达服务层")
    void rejectsChangelogOverLimit() {
        String changelog = "x".repeat(LimitsConst.MAX_TEXT_LENGTH + 1);

        assertThrows(ConstraintViolationException.class, () -> controller.publish(1L, 2L, changelog));

        verifyNoInteractions(publishService);
    }

    @Test
    @DisplayName("备注为空时放行")
    void acceptsNullChangelog() {
        controller.publish(1L, 2L, null);

        verify(publishService).publish(1L, 2L, null);
    }
}
