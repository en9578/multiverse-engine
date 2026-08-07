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
    }
}
