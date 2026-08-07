package com.minbao.multiverse.constant;

public final class Constants {
    private Constants() {}

    /** 百炼 API 版本 */
    public static final String BAILIAN_API_VERSION = "2.0";

    /** 重试最大次数 */
    public static final int MAX_RETRY = 3;

    /** 初始退避毫秒 */
    public static final long RETRY_BACKOFF_BASE_MS = 1000L;

    /** 任务默认超时（分钟） */
    public static final long TASK_TIMEOUT_MINUTES = 30;

    /** 宇宙数量上限 */
    public static final int MAX_UNIVERSES = 10;
}
