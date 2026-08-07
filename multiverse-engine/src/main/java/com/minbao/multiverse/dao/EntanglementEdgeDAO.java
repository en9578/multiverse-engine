package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.EntanglementEdgeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EntanglementEdgeDAO {
    int insert(EntanglementEdgeDO edge);
    List<EntanglementEdgeDO> selectByUniverseId(@Param("universeId") Long universeId);
}
