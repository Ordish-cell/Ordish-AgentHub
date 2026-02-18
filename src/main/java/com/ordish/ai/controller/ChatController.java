package com.ordish.ai.controller;

import com.ordish.ai.annotation.RateLimit; // 核心：引入你自定义的限流注解
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    // 核心装甲：限制同一个IP，60秒内最多只能调用10次这个接口，防止恶意刷大模型Token！
    @RateLimit(time = 60, count = 10)
    @RequestMapping(value = "/chat", produces ="text/html;charset=utf-8")
    public Flux<String> chat(String prompt, @RequestParam String chatId) {
        return chatClient.prompt()
                .user(prompt)
                // 3. 传入会话 ID
                // 告诉 Advisor：请把这句话存到 "chatId" 这个抽屉里，并读取该抽屉的历史
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content();
    }
}