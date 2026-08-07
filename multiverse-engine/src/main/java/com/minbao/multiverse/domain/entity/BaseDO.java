package com.minbao.multiverse.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseDO {
    private Long id;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private String traceId;
}
