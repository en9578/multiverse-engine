package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.CompetitorReactionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompetitorReactionDAO {
    int insert(CompetitorReactionDO reaction);
    List<CompetitorReactionDO> selectByUniverseId(@Param("universeId") Long universeId);
}
