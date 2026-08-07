package com.minbao.multiverse.dao;

import com.minbao.multiverse.domain.entity.ConversationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationDAO {
    int insert(ConversationDO conversation);
    List<ConversationDO> selectByTaskAndSession(
            @Param("taskId") Long taskId,
            @Param("universeId") String universeId,
            @Param("sessionId") String sessionId);
}