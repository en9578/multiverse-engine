package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.UniverseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UniverseDAO {
    int insert(UniverseDO universe);
    int updateRating(@Param("id") Long id, @Param("rating") String rating);
    UniverseDO selectById(@Param("id") Long id);
    List<UniverseDO> selectByTaskId(@Param("taskId") Long taskId);
}
