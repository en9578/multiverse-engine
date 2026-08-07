package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MultiverseTaskDAO {
    int insert(MultiverseTaskDO task);
    MultiverseTaskDO selectById(@Param("id") Long id);
    MultiverseTaskDO selectByRequestId(@Param("requestId") String requestId);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
