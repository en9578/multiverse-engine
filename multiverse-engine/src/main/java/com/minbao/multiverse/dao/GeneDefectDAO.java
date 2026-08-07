package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.GeneDefectDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GeneDefectDAO {
    int insert(GeneDefectDO defect);
    List<GeneDefectDO> selectByUniverseId(@Param("universeId") Long universeId);
}
