package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BailianCallLogDO extends BaseDO {
    private String requestId;
    private String callType;
    private String inputText;
    private String outputText;
    private Boolean success;
    private Long costMs;
    private Integer tokenCount;

    public static BailianCallLogDO success(String requestId, String callType, String output) {
        BailianCallLogDO log = new BailianCallLogDO();
        log.requestId = requestId;
        log.callType = callType;
        log.success = true;
        log.outputText = output;
        return log;
    }

    public static BailianCallLogDO fail(String requestId, String callType, Throwable e) {
        BailianCallLogDO log = new BailianCallLogDO();
        log.requestId = requestId;
        log.callType = callType;
        log.success = false;
        log.outputText = e.getMessage();
        return log;
    }
}