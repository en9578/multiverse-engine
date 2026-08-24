package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.UniverseWeatherDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UniverseWeatherDAO {
    int insert(UniverseWeatherDO weather);
    UniverseWeatherDO selectByUniverseId(@Param("universeId") Long universeId);
}
