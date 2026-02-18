package com.ordish.ai.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageEntity {
    private Long id;
    private String conversationId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}