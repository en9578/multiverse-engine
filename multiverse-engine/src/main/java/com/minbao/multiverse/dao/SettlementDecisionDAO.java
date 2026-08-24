package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.SettlementDecisionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementDecisionDAO {
    int insert(SettlementDecisionDO decision);
    List<SettlementDecisionDO> selectByUniverseId(@Param("universeId") Long universeId);
    int updateConfirm(@Param("id") Long id, @Param("isConfirmed") Boolean isConfirmed);
}
