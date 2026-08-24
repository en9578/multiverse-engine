package com.minbao.multiverse.engine.evolution;

import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.enums.StormEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 压力测试引擎（设计 §3.3.4 / §5.3）。
 * 对每个策略宇宙施加 5 种风暴，输出存活率 + 最弱环节 + 修复建议。
 * 规则化可解释推演：存活率由策略画像（定价/卖点/定位）与市场事实（合规/评论/竞争密度）逐条规则推导。
 */
@Component
public class StressTestEngine {
    private static final Logger log = LoggerFactory.getLogger(StressTestEngine.class);

    /** 单风暴压力测试结果 */
    public record StormResult(StormEnum storm, double survivalRate, String weakestLink, String fix) {}

    public StormResult stressTest(UniverseBO universe, CollectedDataBO data, StormEnum storm) {
        Map<String, Object> pkg = JsonUtil.parseObject(universe.getStrategyPackage());
        String pricing = str(pkg == null ? null : pkg.get("pricingStrategy"));
        String sellingPoint = str(pkg == null ? null : pkg.get("sellingPointStrategy"));

        double rate = switch (storm) {
            case PRICE_TSUNAMI -> priceTsunami(pricing);
            case POLICY_EARTHQUAKE -> policyEarthquake(data);
            case REVIEW_TSUNAMI -> reviewTsunami(data);
            case GIANT_INVASION -> giantInvasion(sellingPoint, data);
            case FX_STORM -> fxStorm(pricing);
        };
        rate = clamp(rate);
        String weakest = weakestLink(storm, pricing, data);
        String fix = fixSuggestion(storm);
        return new StormResult(storm, rate, weakest, fix);
    }

    /** 价格海啸：头部竞品降价 30%，低价线抗压最强，高端溢价被侵蚀 */
    private double priceTsunami(String pricing) {
        return switch (pricing) {
            case "低价引流" -> 0.88;
            case "性价比" -> 0.72;
            case "高端" -> 0.58;
            default -> 0.70;
        };
    }

    /** 政策地震：合规高风险越多存活率越低（high 每条 -0.22，medium -0.08） */
    private double policyEarthquake(CollectedDataBO data) {
        int high = 0, medium = 0;
        for (Map<String, Object> item : listOf(data.getComplianceData(), "compliance")) {
            String level = str(item.get("level"));
            if ("high".equalsIgnoreCase(level)) high++;
            else if ("medium".equalsIgnoreCase(level)) medium++;
        }
        return 1.0 - high * 0.22 - medium * 0.08;
    }

    /** 差评海啸：评论情绪越正面存活率越高（病毒式差评扩散对低情绪市场杀伤更大） */
    private double reviewTsunami(CollectedDataBO data) {
        double sentiment = sentiment(data);
        return 0.35 + sentiment * 0.55;
    }

    /** 巨头入侵：差异化卖点存活更久，竞争密度越高越难守 */
    private double giantInvasion(String sellingPoint, CollectedDataBO data) {
        double base = "差异化型".equals(sellingPoint) ? 0.85 : 0.65;
        int count = listOf(data.getCompetitorData(), "competitors").size();
        double penalty = count > 8 ? 0.15 : (count >= 4 ? 0.08 : 0);
        return base - penalty;
    }

    /** 汇率风暴：高毛利（高端）可吸收 15% 贬值，低价薄毛利线受损最大 */
    private double fxStorm(String pricing) {
        return switch (pricing) {
            case "高端" -> 0.85;
            case "性价比" -> 0.70;
            case "低价引流" -> 0.62;
            default -> 0.72;
        };
    }

    private String weakestLink(StormEnum storm, String pricing, CollectedDataBO data) {
        return switch (storm) {
            case PRICE_TSUNAMI -> "高端".equals(pricing)
                    ? "定价高于竞品均价时溢价空间被竞品降价压缩" : "低价线利润空间已薄，再跟价将触及成本线";
            case POLICY_EARTHQUAKE -> topComplianceRisk(data);
            case REVIEW_TSUNAMI -> topDefect(data);
            case GIANT_INVASION -> "同质化卖点缺乏护城河，巨头同款入局即被分流";
            case FX_STORM -> "成本以人民币计价，目标市场货币贬值 15% 侵蚀利润";
        };
    }

    private String fixSuggestion(StormEnum storm) {
        return switch (storm) {
            case PRICE_TSUNAMI -> "加入高端线对冲 + 压缩供应链成本";
            case POLICY_EARTHQUAKE -> "切换合规材质 + Listing 标注认证";
            case REVIEW_TSUNAMI -> "前置改进高频缺陷 + 好评回流机制";
            case GIANT_INVASION -> "强化差异化卖点 + 深耕细分垂直人群";
            case FX_STORM -> "本地化定价 + 锁汇对冲";
        };
    }

    private String topComplianceRisk(CollectedDataBO data) {
        for (Map<String, Object> item : listOf(data.getComplianceData(), "compliance")) {
            if ("high".equalsIgnoreCase(str(item.get("level")))) {
                return str(item.get("risk")) + "：" + str(item.get("detail"));
            }
        }
        return "无 high 级合规风险，但政策突变仍可能引入新规";
    }

    private String topDefect(CollectedDataBO data) {
        for (Map<String, Object> item : listOf(data.getReviewData(), "defects")) {
            return str(item.get("name")) + "（frequency=" + str(item.get("frequency")) + "）";
        }
        return "评论情绪一般，差评海啸将放大既有短板";
    }

    private double sentiment(CollectedDataBO data) {
        Object s = data.getReviewData() == null ? null : data.getReviewData().get("sentiment");
        return s instanceof Number ? ((Number) s).doubleValue() : 0.7;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Map<String, Object> data, String key) {
        if (data == null) return List.of();
        Object v = data.get(key);
        return v instanceof List ? (List<Map<String, Object>>) v : List.of();
    }

    private String str(Object v) { return v == null ? "" : v.toString(); }

    private double clamp(double v) { return Math.max(0.05, Math.min(0.99, v)); }
}
