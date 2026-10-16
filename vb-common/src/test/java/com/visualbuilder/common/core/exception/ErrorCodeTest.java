package com.visualbuilder.common.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 常量测试 — 确保错误码不重复、分区合理。
 */
@DisplayName("ErrorCode 错误码常量")
class ErrorCodeTest {

    @Test
    @DisplayName("错误码值不应重复")
    void noDuplicateErrorCodes() {
        int[] codes = {
            ErrorCode.PARAM_ERROR, ErrorCode.DATA_NOT_FOUND, ErrorCode.DATA_ALREADY_EXISTS,
            ErrorCode.FILE_EMPTY, ErrorCode.FILE_TYPE_NOT_ALLOWED, ErrorCode.FILE_SIZE_EXCEEDED,
            ErrorCode.USER_NOT_FOUND, ErrorCode.USERNAME_EXISTS, ErrorCode.PASSWORD_WRONG,
            ErrorCode.TOKEN_EXPIRED, ErrorCode.TOKEN_INVALID, ErrorCode.UNAUTHORIZED,
            ErrorCode.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED,
            ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_MEMBER_EXISTS,
            ErrorCode.PAGE_NOT_FOUND, ErrorCode.COMPONENT_NOT_FOUND,
            ErrorCode.TEMPLATE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.PUBLISH_FAILED,
            ErrorCode.INTERNAL_ERROR, ErrorCode.STORAGE_ERROR, ErrorCode.THIRD_PARTY_ERROR,
        };

        // 所有码值应唯一
        long distinctCount = java.util.Arrays.stream(codes).distinct().count();
        assertEquals(codes.length, distinctCount, "错误码存在重复值");
    }

    @Test
    @DisplayName("通用错误码在 1xxxx 范围")
    void commonErrorsIn10000Range() {
        assertTrue(ErrorCode.PARAM_ERROR >= 10000 && ErrorCode.PARAM_ERROR < 20000);
        assertTrue(ErrorCode.DATA_NOT_FOUND >= 10000 && ErrorCode.DATA_NOT_FOUND < 20000);
        assertTrue(ErrorCode.FILE_EMPTY >= 10000 && ErrorCode.FILE_EMPTY < 20000);
    }

    @Test
    @DisplayName("用户错误码在 2xxxx 范围")
    void userErrorsIn20000Range() {
        assertTrue(ErrorCode.USER_NOT_FOUND >= 20000 && ErrorCode.USER_NOT_FOUND < 30000);
        assertTrue(ErrorCode.UNAUTHORIZED >= 20000 && ErrorCode.UNAUTHORIZED < 30000);
    }

    @Test
    @DisplayName("系统错误码在 5xxxx 范围")
    void systemErrorsIn50000Range() {
        assertTrue(ErrorCode.INTERNAL_ERROR >= 50000 && ErrorCode.INTERNAL_ERROR < 60000);
        assertTrue(ErrorCode.STORAGE_ERROR >= 50000 && ErrorCode.STORAGE_ERROR < 60000);
    }
}
