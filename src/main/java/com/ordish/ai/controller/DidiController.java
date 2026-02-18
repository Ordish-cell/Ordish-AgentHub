package com.ordish.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class DidiController {

    /**
     * 注入我们在配置类 DidiConfiguration 中定义的 "didiClient" Bean。
     * * 注意：
     * 如果你系统里有多个 ChatClient，Spring 可能会不知道注入哪一个。
     * 这里变量名 "didiClient" 必须和 @Bean 的方法名保持一致，
     * 或者使用 @Qualifier("didiClient") 强制指定。
     */
    @Qualifier("didiClient")
    private final ChatClient didiClient;

    /**
     * 小O滴滴的专属接口
     * * @param prompt 用户输入，例如 "我要去天安门"
     * @param chatId 用户会话ID，例如 "user_123"，用于记忆上下文
     */
    @GetMapping(value = "/didi", produces = "text/html;charset=utf-8")
    public String didiChat(@RequestParam String prompt,
                                 @RequestParam String chatId) {

        log.info("收到打车请求 - 用户: {}, 内容: {}", chatId, prompt);

        return didiClient.prompt()
                .user(prompt)
                // 将 chatId 传入 Advisor，保证 AI 记得住“上句话说了啥”
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }
}