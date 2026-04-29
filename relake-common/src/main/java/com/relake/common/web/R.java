package com.relake.common.web;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    // ---- 工厂方法 ----

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(T data, String message) {
        R<T> r = ok(data);
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.message = resultCode.getMessage();
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        R<T> r = fail(resultCode);
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    // ---- 分页快捷方法 ----

    public static <T> R<PageData<T>> page(java.util.List<T> records, long total, long page, long size) {
        R<PageData<T>> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = new PageData<>(records, total, page, size);
        return r;
    }

    @Data
    public static class PageData<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private java.util.List<T> records;
        private long total;
        private long page;
        private long size;

        public PageData(java.util.List<T> records, long total, long page, long size) {
            this.records = records;
            this.total = total;
            this.page = page;
            this.size = size;
        }
    }
}
