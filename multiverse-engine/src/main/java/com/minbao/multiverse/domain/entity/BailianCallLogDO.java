package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BailianCallLogDO extends BaseDO {
    private String requestId;
    private String callType;
    private String model;
    private String inputPrompt;
    private String outputText;
    private Boolean success;
    private Long costMs;
    private Integer tokenCount;

    public static BailianCallLogDO success(String requestId, String callType, String model,
                                           String inputPrompt, String output, Long costMs, Integer tokenCount) {
        BailianCallLogDO log = new BailianCallLogDO();
        log.requestId = requestId;
        log.callType = callType;
        log.model = model;
        log.inputPrompt = inputPrompt;
        log.success = true;
        log.outputText = output;
        log.costMs = costMs;
        log.tokenCount = tokenCount;
        log.setTraceId("");
        return log;
    }

    public static BailianCallLogDO fail(String requestId, String callType, String model,
                                        String inputPrompt, String errorMsg) {
        BailianCallLogDO log = new BailianCallLogDO();
        log.requestId = requestId;
        log.callType = callType;
        log.model = model;
        log.inputPrompt = inputPrompt;
        log.success = false;
        log.outputText = errorMsg;
        log.setTraceId("");
        return log;
    }

    /** 失败落库带实测耗时（重试退避后终局失败用） */
    public static BailianCallLogDO fail(String requestId, String callType, String model,
                                        String inputPrompt, String errorMsg, Long costMs) {
        BailianCallLogDO log = fail(requestId, callType, model, inputPrompt, errorMsg);
        log.costMs = costMs;
        return log;
    }
}