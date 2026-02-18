package com.ordish.ai.mapper;

import com.ordish.ai.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_history(conversation_id, role, content, create_time) " +
            "VALUES(#{conversationId}, #{role}, #{content}, #{createTime})")
    void insert(ChatMessageEntity entity);

    @Select("SELECT * FROM chat_history WHERE conversation_id = #{conversationId} ORDER BY create_time ASC")
    List<ChatMessageEntity> selectByConversationId(String conversationId);

    @Delete("DELETE FROM chat_history WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(String conversationId);
}