package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationDO extends BaseDO {
    private Long taskId;
    private String universeId;
    private String sessionId;
    private String role;
    private String content;
    private Boolean isDeleted;
}