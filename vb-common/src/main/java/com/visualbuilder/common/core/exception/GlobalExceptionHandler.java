package com.visualbuilder.common.core.exception;

import com.visualbuilder.common.core.result.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        R<Void> r = R.fail(e.getCode(), e.getMessage());
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 参数校验失败（@Valid） — 返回字段级错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<R.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new R.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()))
                .collect(Collectors.toList());

        String summary = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getMessage())
                .collect(Collectors.joining("; "));

        R<Void> r = R.fieldErrors(fieldErrors);
        r.setMessage(summary);
        r.setPath(request.getRequestURI());
        return r;
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        List<R.FieldError> fieldErrors = e.getFieldErrors().stream()
                .map(fe -> new R.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()))
                .collect(Collectors.toList());

        R<Void> r = R.fieldErrors(fieldErrors);
        r.setPath(request.getRequestURI());
        return r;
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        R<Void> r = R.unauthorized("用户名或密码错误");
        r.setPath(request.getRequestURI());
        return r;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        R<Void> r = R.forbidden("权限不足");
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 请求方法不支持（如 GET 请求了 POST 接口）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        R<Void> r = R.fail(ErrorCode.PARAM_ERROR,
                "不支持 " + e.getMethod() + " 请求，允许: " + e.getSupportedHttpMethods());
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 缺少必填参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        R<Void> r = R.badRequest("缺少必填参数: " + e.getParameterName() + " (" + e.getParameterType() + ")");
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 参数类型不匹配（如 String 参数传了数字）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String paramName = e.getName();
        String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        R<Void> r = R.badRequest("参数 " + paramName + " 类型错误，期望: " + expectedType);
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 请求体 JSON 解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        R<Void> r = R.badRequest("请求体格式错误，请检查 JSON 格式");
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<Void> handleMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        R<Void> r = R.fail(ErrorCode.PARAM_ERROR,
                "不支持的 Content-Type: " + e.getContentType());
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 文件上传超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        R<Void> r = R.fail(ErrorCode.FILE_SIZE_EXCEEDED, "上传文件超过大小限制（最大 50MB）");
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 404 兜底
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        R<Void> r = R.notFound("接口不存在: " + request.getRequestURI());
        r.setPath(request.getRequestURI());
        return r;
    }

    /**
     * 未知异常兜底 — 不泄露内部细节
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] [traceId={}]: {}", request.getRequestURI(),
                org.slf4j.MDC.get("traceId"), e.getMessage(), e);
        R<Void> r = R.fail("系统繁忙，请稍后重试");
        r.setPath(request.getRequestURI());
        return r;
    }
}
