package com.minbao.multiverse.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    SUCCESS(0, "成功"),
    INVALID_PARAM(400, "请求参数校验失败"),
    UNAUTHORIZED(401, "未授权"),
    RATE_LIMITED(429, "系统繁忙，请求过多"),
    SYSTEM_ERROR(500, "系统内部错误"),
    BAILIAN_CALL_TIMEOUT(1001, "百炼调用超时"),
    BAILIAN_RATE_LIMITED(1002, "百炼调用频率限制"),
    CIRCUIT_OPEN(1003, "熔断器已开启，快速失败"),
    BAILIAN_API_ERROR(1004, "百炼API返回错误"),
    LLM_DEGRADED(1005, "已降级为快速推理"),
    RULE_ONLY_FALLBACK(1006, "已降级为规则兜底"),
    TASK_NOT_FOUND(2001, "任务不存在"),
    TASK_STATUS_INVALID(2002, "任务状态不合法"),
    TASK_ALREADY_EXISTS(2003, "任务已存在(幂等命中)"),
    TASK_QUEUE_FULL(2004, "任务队列已满"),
    CHECKPOINT_CORRUPTED(2005, "checkpoint损坏"),
    TASK_TIMEOUT(2006, "任务执行超时(>2h)"),
    FILE_TOO_LARGE(3001, "文件超限"),
    FILE_TYPE_UNSUPPORTED(3002, "文件类型不支持"),
    ENGINE_UNAVAILABLE(4001, "Python引擎不可用"),
    KB_DATA_STALE(4002, "知识库数据过期(降权)");

    private final int code;
    private final String message;

    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}