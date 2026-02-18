package com.ordish.ai.controller;

import com.ordish.ai.annotation.RateLimit; // 引入限流注解
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class DidiController {

    @Qualifier("didiClient")
    private final ChatClient didiClient;

    // 【核心装甲】：防止恶意刷单或调用AI接口
    @RateLimit(time = 60, count = 10)
    @GetMapping(value = "/didi", produces = "text/html;charset=utf-8")
    public String didiChat(@RequestParam String prompt,
                           @RequestParam String chatId) {

        log.info("收到打车请求 - 用户: {}, 内容: {}", chatId, prompt);

        return didiClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }
}