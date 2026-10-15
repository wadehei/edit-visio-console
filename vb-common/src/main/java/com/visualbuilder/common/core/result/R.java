package com.visualbuilder.common.core.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
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
        return fail(50000, message);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    // 常用错误码快捷方法
    public static <T> R<T> unauthorized(String message) {
        return fail(40001, message);
    }

    public static <T> R<T> forbidden(String message) {
        return fail(40004, message);
    }

    public static <T> R<T> notFound(String message) {
        return fail(40005, message);
    }

    public static <T> R<T> badRequest(String message) {
        return fail(41001, message);
    }
}
