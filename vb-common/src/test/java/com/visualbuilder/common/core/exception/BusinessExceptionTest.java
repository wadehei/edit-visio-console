package com.visualbuilder.common.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 测试。
 */
@DisplayName("BusinessException 业务异常")
class BusinessExceptionTest {

    @Test
    @DisplayName("无参构造默认 code=50000")
    void defaultCode() {
        BusinessException ex = new BusinessException("出错了");
        assertEquals(50000, ex.getCode());
        assertEquals("出错了", ex.getMessage());
    }

    @Test
    @DisplayName("带 code 构造")
    void withCode() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR, "参数错误");
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
        assertEquals("参数错误", ex.getMessage());
    }

    @Test
    @DisplayName("带 cause 构造")
    void withCause() {
        RuntimeException cause = new RuntimeException("原始异常");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "内部错误", cause);
        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("是 RuntimeException 子类")
    void isRuntimeException() {
        BusinessException ex = new BusinessException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
