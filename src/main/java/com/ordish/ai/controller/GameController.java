package com.ordish.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType; // 导入这个
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class GameController {

    private final ChatClient gameClient;
    private final ChatMemory chatMemory;

    // 1. 修改 produces 为 TEXT_PLAIN_VALUE，解决 "data:" 乱码问题
    @GetMapping(value = "/game", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public Flux<String> chat(@RequestParam String prompt, @RequestParam String chatId) {
        return gameClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content();
    }

    @DeleteMapping("/game/reset")
    public String reset(@RequestParam String chatId) {
        chatMemory.clear(chatId);
        return "success";
    }
}