package com.ordish.ai.controller;

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

    /**
     * 上传接口：【关键修复】增加了 chatId 参数
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("chatId") String chatId) throws Exception { // 👈 必须要有这个参数

        PagePdfDocumentReader reader = new PagePdfDocumentReader(file.getResource());
        List<Document> documents = reader.get();
        if (documents == null || documents.isEmpty()) return "错误：无法读取 PDF 内容";

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);
        if (splitDocuments.isEmpty()) return "错误：PDF 拆分后内容为空";

        // 【核心逻辑】给文档打上“防伪标签”，只属于当前 chatId
        for (Document doc : splitDocuments) {
            doc.getMetadata().put("chatId", chatId);
        }

        vectorStore.add(splitDocuments);

        // 持久化保存
        if (vectorStore instanceof SimpleVectorStore) {
            ((SimpleVectorStore) vectorStore).save(new File("vector_store.json"));
        }

        return "上传成功！文档已绑定到当前会话。";
    }

    /**
     * 对话接口：【关键修复】增加了 filterExpression 过滤
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String query, @RequestParam String chatId) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.1)
                // 【核心逻辑】只允许检索“防伪标签”等于当前 chatId 的内容
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

    // 历史记录接口（不变）
    @GetMapping("/history")
    public List<Map<String, String>> getHistory(@RequestParam String chatId) {
        List<Message> messages = chatMemory.get(chatId, 100);
        return messages.stream().map(msg -> {
            String role = (msg instanceof UserMessage) ? "user" : "ai";
            return Map.of("role", role, "content", msg.getText());
        }).collect(Collectors.toList());
    }

    // 清空接口（不变）
    @GetMapping("/clear")
    public String clearHistory(@RequestParam String chatId) {
        chatMemory.clear(chatId);
        return "success";
    }
}