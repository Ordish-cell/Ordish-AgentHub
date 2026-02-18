package com.ordish.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordish.ai.entity.ChatMessageEntity;
import com.ordish.ai.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MySqlChatMemory implements ChatMemory {

    private final ChatMessageMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message msg : messages) {
            // 【关键拦截】：绝不把 System 提示词和 Tool 过程存入数据库，防止 AI 错乱
            if (msg instanceof SystemMessage || msg.getMessageType() == MessageType.TOOL) {
                continue;
            }

            String text = msg.getText();
            // 如果消息为空（比如 AI 只发起了工具调用但没说话），直接忽略
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            ChatMessageEntity entity = new ChatMessageEntity();
            entity.setConversationId(conversationId);
            entity.setRole(msg.getMessageType().getValue());
            entity.setContent(text); // 现在存入的绝对是干干净净的纯文本
            entity.setCreateTime(LocalDateTime.now());

            mapper.insert(entity);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMessageEntity> entities = mapper.selectByConversationId(conversationId);
        List<Message> result = new ArrayList<>();

        for (ChatMessageEntity entity : entities) {
            String rawContent = entity.getContent();
            String cleanText = rawContent;

            // 【自动清洗】：如果你数据库里还有之前残留的 {"messageType":"USER"...} 脏数据，在这里强行把它洗成纯文本
            if (rawContent != null && rawContent.trim().startsWith("{")) {
                try {
                    JsonNode node = objectMapper.readTree(rawContent);
                    if (node.hasNonNull("text")) {
                        cleanText = node.get("text").asText();
                    }
                } catch (Exception ignored) {}
            }

            if ("user".equals(entity.getRole())) {
                result.add(new UserMessage(cleanText));
            } else if ("assistant".equals(entity.getRole())) {
                result.add(new AssistantMessage(cleanText));
            }
        }

        if (lastN > 0 && result.size() > lastN) {
            return result.subList(result.size() - lastN, result.size());
        }
        return result;
    }

    @Override
    public void clear(String conversationId) {
        mapper.deleteByConversationId(conversationId);
    }
}