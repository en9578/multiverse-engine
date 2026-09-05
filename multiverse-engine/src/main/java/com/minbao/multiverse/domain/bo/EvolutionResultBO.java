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

        /** 证据来源：kb(知识库最新,全权重) | kb_stale(知识库过期,降权0.5x) | r1_inferred(R1/模型推断,未经知识库验证) | heuristic(确定性启发式规则,无KB依据) */
        public static final String SRC_KB = "kb";
        public static final String SRC_KB_STALE = "kb_stale";
        public static final String SRC_R1 = "r1_inferred";
        public static final String SRC_HEURISTIC = "heuristic";

        private String ruleId;
        private String input;
        private String output;
        private Double weight;
        private String source;
    }
}
