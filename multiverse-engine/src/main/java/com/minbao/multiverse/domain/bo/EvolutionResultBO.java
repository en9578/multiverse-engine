package com.minbao.multiverse.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class EvolutionResultBO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rating;
    private Double survivalRate;
    private List<RuleEvidence> evidences;
    private String r1Supplement;

    @Data
    public static class RuleEvidence implements Serializable {
        private static final long serialVersionUID = 1L;
        private String ruleId;
        private String input;
        private String output;
        private Double weight;
        /** 证据来源：kb(知识库规则，全权重) | r1_inferred(R1 推断，半权重，未经知识库验证) */
        private String source;
    }
}
