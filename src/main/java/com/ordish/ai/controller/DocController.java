package com.ordish.ai.controller;

import com.ordish.ai.annotation.RateLimit; // 引入限流注解
import com.ordish.ai.common.CommonResult; // 引入企业级规范
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/doc")
public class DocController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public DocController(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem("你是一个智能文档助手。请严格根据提供的上下文信息回答用户的问题。如果上下文中没有答案，请诚实地说不知道。")
                .build();
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    // 1. 【企业级改造】：用 CommonResult 包装上传返回值
    @PostMapping("/upload")
    public CommonResult<String> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam("chatId") String chatId) throws Exception {

        PagePdfDocumentReader reader = new PagePdfDocumentReader(file.getResource());
        List<Document> documents = reader.get();
        if (documents == null || documents.isEmpty()) return CommonResult.error(500, "错误：无法读取 PDF 内容");

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);
        if (splitDocuments.isEmpty()) return CommonResult.error(500, "错误：PDF 拆分后内容为空");

        for (Document doc : splitDocuments) {
            doc.getMetadata().put("chatId", chatId);
        }

        vectorStore.add(splitDocuments);

        if (vectorStore instanceof SimpleVectorStore) {
            ((SimpleVectorStore) vectorStore).save(new File("vector_store.json"));
        }

        return CommonResult.success("上传成功！文档已绑定到当前会话。");
    }

    // 2. 【核心装甲】：防刷大模型 Token！
    @RateLimit(time = 60, count = 10)
    @GetMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public String chat(@RequestParam String query, @RequestParam String chatId) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.1)
                .filterExpression("chatId == '" + chatId + "'")
                .build();

        return chatClient.prompt()
                .user(query)
                .advisors(
                        new QuestionAnswerAdvisor(vectorStore, searchRequest),
                        new MessageChatMemoryAdvisor(chatMemory, chatId, 10)
                )
                .call()
                .content();
    }

    // 3. 【企业级改造】：规范历史记录返回
    @GetMapping("/history")
    public CommonResult<List<Map<String, String>>> getHistory(@RequestParam String chatId) {
        List<Message> messages = chatMemory.get(chatId, 100);
        List<Map<String, String>> history = messages.stream().map(msg -> {
            String role = (msg instanceof UserMessage) ? "user" : "ai";
            return Map.of("role", role, "content", msg.getText());
        }).collect(Collectors.toList());
        return CommonResult.success(history);
    }

    // 4. 【企业级改造】：规范清空操作返回
    @GetMapping("/clear")
    public CommonResult<String> clearHistory(@RequestParam String chatId) {
        chatMemory.clear(chatId);
        return CommonResult.success("success");
    }
}