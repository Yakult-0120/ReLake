package com.relake.common.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("Validation failed [{}] {}: {}", request.getMethod(), request.getRequestURI(), msg);
        return R.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error [{}] {}: [{}] {}", request.getMethod(), request.getRequestURI(),
                ex.getResultCode().getCode(), ex.getMessage());
        return R.fail(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error [{}] {}", request.getMethod(), request.getRequestURI(), ex);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
