package com.minbao.multiverse.common;

import com.minbao.multiverse.enums.ErrorCodeEnum;
import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCodeEnum.SUCCESS.getCode();
        r.message = ErrorCodeEnum.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCodeEnum error) {
        Result<T> r = new Result<>();
        r.code = error.getCode();
        r.message = error.getMessage();
        return r;
    }

    public static <T> Result<T> fail(ErrorCodeEnum error, String detail) {
        Result<T> r = new Result<>();
        r.code = error.getCode();
        r.message = error.getMessage() + ": " + detail;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
