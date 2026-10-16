package com.visualbuilder.common.core.result;

import com.visualbuilder.common.core.exception.ErrorCode;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class R<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;
    private String path;

    /** 字段级校验错误详情（仅校验失败时存在） */
    private List<FieldError> fieldErrors;

    @Data
    public static class FieldError implements Serializable {
        private String field;
        private String message;
        private Object rejectedValue;

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
    }

    public R() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = MDC.get("traceId");
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(ErrorCode.INTERNAL_ERROR, message);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /**
     * 带字段级校验错误的失败响应
     */
    public static <T> R<T> fieldErrors(List<FieldError> errors) {
        R<T> r = new R<>();
        r.setCode(ErrorCode.PARAM_ERROR);
        r.setMessage("参数校验失败");
        r.setFieldErrors(errors);
        return r;
    }

    // 常用错误码快捷方法（与 ErrorCode 常量保持一致）
    public static <T> R<T> unauthorized(String message) {
        return fail(ErrorCode.UNAUTHORIZED, message);
    }

    public static <T> R<T> forbidden(String message) {
        return fail(ErrorCode.FORBIDDEN, message);
    }

    public static <T> R<T> notFound(String message) {
        return fail(ErrorCode.DATA_NOT_FOUND, message);
    }

    public static <T> R<T> badRequest(String message) {
        return fail(ErrorCode.PARAM_ERROR, message);
    }
}
