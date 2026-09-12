package com.minbao.multiverse.engine.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务级 token 预算守卫（设计 §14 Token 成本控制的落地件）。
 * 每个 task 一个内存累计器：BailianManager 每次 LLM 调用前 tryAcquire 放行、成功后 record 归账；
 * 累计达到 budget-per-task-tokens 后拒绝后续调用，调用方沿既有 LLM_DEGRADED 降级路径走
 * （模板宇宙 / 规则融合推演 / 规则兜底结算），任务不会失败，只是后续阶段不再花钱。
 * 内存态即可（进程内生命周期 = 任务生命周期），不落库；bailian_call_log 仍是逐调用明细账。
 */
@Component
public class TokenBudgetManager {
    private static final Logger log = LoggerFactory.getLogger(TokenBudgetManager.class);

    /** 单任务 token 预算（输入+输出合计）；默认按「瘦身后全链路 ~2 万」上浮留余量 */
    @Value("${minbao.llm.budget-per-task-tokens:30000}")
    private int budgetPerTask;

    /** 预算开关（评审/本地可关闭观察真实消耗） */
    @Value("${minbao.llm.budget-enabled:true}")
    private boolean enabled;

    private final Map<Long, AtomicInteger> usageByTask = new ConcurrentHashMap<>();

    /** 预算内放行返回 true；已达上限返回 false（调用方抛 LLM_DEGRADED 走降级） */
    public boolean tryAcquire(Long taskId) {
        if (!enabled || taskId == null || taskId <= 0) {
            return true;
        }
        int used = usageByTask.computeIfAbsent(taskId, k -> new AtomicInteger()).get();
        if (used >= budgetPerTask) {
            log.warn("任务 token 预算已耗尽，后续 LLM 调用将被拒绝 taskId={} used={}/budget={}",
                    taskId, used, budgetPerTask);
            return false;
        }
        return true;
    }

    /** 调用成功后归账（tokens 为该次调用 total tokens，缺省忽略） */
    public void record(Long taskId, Integer tokens) {
        if (!enabled || taskId == null || taskId <= 0 || tokens == null || tokens <= 0) {
            return;
        }
        int total = usageByTask.computeIfAbsent(taskId, k -> new AtomicInteger()).addAndGet(tokens);
        log.info("任务 token 累计 taskId={} +{} => {}/budget={}", taskId, tokens, total, budgetPerTask);
    }

    /** 任务结束时的累计消耗（观测用） */
    public int used(Long taskId) {
        if (taskId == null || taskId <= 0) return 0;
        AtomicInteger counter = usageByTask.get(taskId);
        return counter == null ? 0 : counter.get();
    }
}
