package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.StressTestDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StressTestDAO {
    int insert(StressTestDO stressTest);
    List<StressTestDO> selectByUniverseId(@Param("universeId") Long universeId);
}
