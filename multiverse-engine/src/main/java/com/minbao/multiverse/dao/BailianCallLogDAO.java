package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.BailianCallLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BailianCallLogDAO {
    int insert(BailianCallLogDO log);
    BailianCallLogDO selectByRequestId(@Param("requestId") String requestId);
}
