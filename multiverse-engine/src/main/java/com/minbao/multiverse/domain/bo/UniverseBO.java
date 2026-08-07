package com.minbao.multiverse.domain.bo;

import com.minbao.multiverse.enums.UniverseRatingEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UniverseBO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long universeId;
    private Integer universeIndex;
    private String productName;
    private String targetMarket;
    private UniverseRatingEnum rating;
    private List<String> geneDefects;
    private String strategyPackage;
}
