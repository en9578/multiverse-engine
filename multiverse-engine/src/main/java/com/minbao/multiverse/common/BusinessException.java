package com.minbao.multiverse.common;

import com.minbao.multiverse.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCodeEnum errorCode;

    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCodeEnum errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCodeEnum errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }
}
