package com.visualbuilder.common.core.result;

import com.visualbuilder.common.core.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("R 统一响应体")
class RTest {

    @Test
    @DisplayName("ok() → code=0, message=success")
    void ok() {
        R<String> result = R.ok("data");
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("data", result.getData());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    @DisplayName("ok(null) → data=null")
    void okNull() {
        R<Void> result = R.ok();
        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail(message) → 使用 INTERNAL_ERROR 错误码")
    void failMessage() {
        R<Void> result = R.fail("出错了");
        assertEquals(ErrorCode.INTERNAL_ERROR, result.getCode());
        assertEquals("出错了", result.getMessage());
    }

    @Test
    @DisplayName("unauthorized → 使用 ErrorCode.UNAUTHORIZED")
    void unauthorized() {
        R<Void> result = R.unauthorized("未登录");
        assertEquals(ErrorCode.UNAUTHORIZED, result.getCode());
    }

    @Test
    @DisplayName("forbidden → 使用 ErrorCode.FORBIDDEN")
    void forbidden() {
        R<Void> result = R.forbidden("无权限");
        assertEquals(ErrorCode.FORBIDDEN, result.getCode());
    }

    @Test
    @DisplayName("notFound → 使用 ErrorCode.DATA_NOT_FOUND")
    void notFound() {
        R<Void> result = R.notFound("未找到");
        assertEquals(ErrorCode.DATA_NOT_FOUND, result.getCode());
    }

    @Test
    @DisplayName("badRequest → 使用 ErrorCode.PARAM_ERROR")
    void badRequest() {
        R<Void> result = R.badRequest("参数错误");
        assertEquals(ErrorCode.PARAM_ERROR, result.getCode());
    }

    @Test
    @DisplayName("R 错误码与 ErrorCode 常量完全一致")
    void errorCodesConsistency() {
        // 确保 R 快捷方法使用的错误码与 ErrorCode 定义一致，不会出现两套体系
        assertEquals(ErrorCode.UNAUTHORIZED, R.unauthorized("").getCode());
        assertEquals(ErrorCode.FORBIDDEN, R.forbidden("").getCode());
        assertEquals(ErrorCode.DATA_NOT_FOUND, R.notFound("").getCode());
        assertEquals(ErrorCode.PARAM_ERROR, R.badRequest("").getCode());
        assertEquals(ErrorCode.INTERNAL_ERROR, R.fail("").getCode());
    }
}
