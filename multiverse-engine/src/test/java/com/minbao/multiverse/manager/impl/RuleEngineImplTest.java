package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.collector.DataFreshnessService;
import com.minbao.multiverse.collector.kb.KnowledgeBaseRegistry;
import com.minbao.multiverse.config.DataSourceProperties;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.engine.rating.UniverseRater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2 证据真话化：启发式规则 source=heuristic；RULE_COMPLIANCE_KB 真读 policy_kb、引用 KB 条目 id、
 * source 为 kb|kb_stale、扣分为负。
 */
class RuleEngineImplTest {

    private RuleEngineImpl engine;

    @BeforeEach
    void setUp() {
        KnowledgeBaseRegistry kbRegistry = new KnowledgeBaseRegistry(new DataSourceProperties());
        kbRegistry.load();
        engine = new RuleEngineImpl();
        ReflectionTestUtils.setField(engine, "universeRater", new UniverseRater());
        ReflectionTestUtils.setField(engine, "kbRegistry", kbRegistry);
        ReflectionTestUtils.setField(engine, "freshnessService", new DataFreshnessService());
    }

    private UniverseBO deUniverse() {
        UniverseBO u = new UniverseBO();
        u.setUniverseId(1L);
        u.setUniverseIndex(1);
        u.setProductName("测试产品");
        u.setTargetMarket("DE");
        u.setStrategyPackage("{}");
        return u;
    }

    @Test
    void heuristicRulesAreTaggedHeuristic() {
        EvolutionResultBO result = engine.evolve(deUniverse(), new CollectedDataBO());
        List<EvolutionResultBO.RuleEvidence> ev = result.getEvidences();
        assertNotNull(ev);
        for (String ruleId : List.of("RULE_COMPLIANCE_RISK", "RULE_COMPETITION",
                "RULE_REVIEW_SENTIMENT", "RULE_PRICE_POSITION")) {
            EvolutionResultBO.RuleEvidence e = ev.stream()
                    .filter(x -> ruleId.equals(x.getRuleId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("缺少启发式规则证据: " + ruleId));
            assertEquals("heuristic", e.getSource(),
                    ruleId + " 是无 KB 依据的确定性规则，应标 heuristic 而非 kb");
        }
    }

    @Test
    void kbPolicyRuleCitesRealEntriesWithFreshness() {
        EvolutionResultBO result = engine.evolve(deUniverse(), new CollectedDataBO());
        List<EvolutionResultBO.RuleEvidence> kbRows = result.getEvidences().stream()
                .filter(e -> "RULE_COMPLIANCE_KB".equals(e.getRuleId()))
                .toList();
        assertFalse(kbRows.isEmpty(), "DE 任务应产生 KB 政策合规证据");
        assertTrue(kbRows.stream().anyMatch(e -> e.getInput().contains("PLCY-003")),
                "KB 行 input 应引用 EU GPSR PLCY-003（区域匹配生效）");
        assertTrue(kbRows.stream().allMatch(e ->
                        "kb".equals(e.getSource()) || "kb_stale".equals(e.getSource())),
                "KB 行 source 应为 kb|kb_stale（真 KB 依据），而非 heuristic");
        assertTrue(kbRows.stream().allMatch(e -> Double.parseDouble(e.getOutput()) < 0),
                "KB 扣分应为负值");
    }
}
