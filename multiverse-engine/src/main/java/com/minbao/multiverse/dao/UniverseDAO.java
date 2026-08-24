package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.UniverseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UniverseDAO {
    int insert(UniverseDO universe);
    int updateRating(@Param("id") Long id, @Param("rating") String rating);
    int updateEvolution(@Param("id") Long id, @Param("rating") String rating,
                        @Param("subState") String subState,
                        @Param("survivalRate") java.math.BigDecimal survivalRate,
                        @Param("evolutionData") String evolutionData);
    UniverseDO selectById(@Param("id") Long id);
    List<UniverseDO> selectByTaskId(@Param("taskId") Long taskId);
    List<UniverseDO> selectByTaskIdAndDimension(@Param("taskId") Long taskId,
                                               @Param("dimension") String dimension);
}
