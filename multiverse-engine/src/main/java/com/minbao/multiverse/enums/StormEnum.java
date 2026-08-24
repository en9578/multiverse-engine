package com.minbao.multiverse.enums;

import lombok.Getter;

/**
 * 5 种极端风暴（设计 §3.3.4）。
 * 每个策略宇宙被投入全部 5 种风暴压力测试，输出存活率 + 最弱环节。
 */
@Getter
public enum StormEnum {
    /** 头部竞品突然降价 30% → 定价策略抗压 */
    PRICE_TSUNAMI("价格海啸", "定价策略抗压"),
    /** 目标市场新法规（如欧盟塑料禁令）→ 合规策略抗压 */
    POLICY_EARTHQUAKE("政策地震", "合规策略抗压"),
    /** 一条病毒式差评扩散 → 评论/产品策略抗压 */
    REVIEW_TSUNAMI("差评海啸", "评论/产品策略抗压"),
    /** 头部大卖同款入局 → 策略差异化抗压 */
    GIANT_INVASION("巨头入侵", "策略差异化抗压"),
    /** 目标市场货币贬值 15% → 成本/定价抗压 */
    FX_STORM("汇率风暴", "成本/定价抗压");

    private final String label;
    private final String pressureDimension;

    StormEnum(String label, String pressureDimension) {
        this.label = label;
        this.pressureDimension = pressureDimension;
    }
}
