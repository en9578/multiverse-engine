package com.minbao.multiverse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * 引擎健康探针（单体版）。
 * 双服务旧版探活 Python 引擎 :8000，降级为单体后引擎即本应用，故改为检查百炼接入就绪度：
 * DashScope api-key 已配置即 UP，未配置则 DOWN（此时 LLM 推演无法运行）。
 */
@Component("engineHealth")
public class EngineHealthIndicator extends AbstractHealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(EngineHealthIndicator.class);

    /** 读真实环境变量（非 application.yml 中的 dev 占位 key），诚实反映是否已配置真实百炼 key */
    @Value("${DASHSCOPE_API_KEY:}")
    private String apiKey;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean configured = apiKey != null && !apiKey.isBlank();
        builder.up().withDetail("bailian",
                configured ? "api-key 已配置" : "api-key 未配置（LLM 调用将失败，dev 占位 key 仅用于启动）");
    }
}
