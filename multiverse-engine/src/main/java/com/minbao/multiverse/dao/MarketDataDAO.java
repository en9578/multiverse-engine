package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.MarketDataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MarketDataDAO {
    /** INSERT ... ON DUPLICATE KEY UPDATE（幂等 upsert，retry 重采覆盖） */
    int upsert(MarketDataDO row);
    List<MarketDataDO> selectByTaskId(@Param("taskId") Long taskId);
}
