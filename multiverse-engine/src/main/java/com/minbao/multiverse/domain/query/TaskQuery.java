package com.minbao.multiverse.domain.query;

import com.minbao.multiverse.enums.TaskStatusEnum;
import lombok.Data;

@Data
public class TaskQuery {
    private String requestId;
    private TaskStatusEnum status;
    private String productName;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
