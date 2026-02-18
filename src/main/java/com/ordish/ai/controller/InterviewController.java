package com.ordish.ai.controller;

import lombok.RequiredArgsConstructor;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController {

    @Qualifier("interviewerClient")
    private final ChatClient interviewerClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file,
                               @RequestParam("chatId") String chatId) throws Exception {

        PagePdfDocumentReader reader = new PagePdfDocumentReader(file.getResource());
        List<Document> documents = reader.get();
        if (documents == null || documents.isEmpty()) return "错误：无法读取简历内容";

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);

        for (Document doc : splitDocuments) {
            doc.getMetadata().put("chatId", chatId);
            doc.getMetadata().put("type", "resume"); // 增加简历标识
        }

        vectorStore.add(splitDocuments);
        if (vectorStore instanceof SimpleVectorStore) {
            ((SimpleVectorStore) vectorStore).save(new File("vector_store.json"));
        }

        return "简历解析完毕。面试官已拿着你的简历就位，深呼吸，说句“面试官好”开始吧。";
    }

    @GetMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public String chat(@RequestParam String query, @RequestParam String chatId) {

        // 【架构师级修复】：意图路由 (Intent Routing)
        // 判断用户是不是在求助、认怂、或者顺着聊天
        boolean isAskingForHelp = query.matches(".*(不会|不懂|解答|讲|解释|答案|继续|下一题|懂了).*");

        if (isAskingForHelp) {
            // 如果用户在求教或走流程，【绝对不查询简历库】，彻底断绝简历长文本的干扰！
            String pureInstruction = query + "\n\n【系统最高指令】：用户当前处于互动/求教状态。请立刻收起一切提问冲动！充分调用你的先验知识为他解答，或者顺应他的话往下走。绝对禁止在此刻长篇大论地抛出新问题！";

            return interviewerClient.prompt()
                    .user(pureInstruction)
                    .advisors(new MessageChatMemoryAdvisor(chatMemory, chatId, 20))
                    .call()
                    .content();
        }

        // 只有当用户真的需要被提问时，才触发 RAG 查询简历
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(3) // 减少干扰，只取前3个最相关的片段
                .similarityThreshold(0.1)
                .filterExpression("chatId == '" + chatId + "' && type == 'resume'")
                .build();

        String customRagPrompt = """
                【候选人简历参考】：
                {question_answer_context}
                ---------------------
                【面试官指令】：
                请结合上方简历，找一个全新的硬核技术点发起提问。一次只准问一个问题！
                """;

        return interviewerClient.prompt()
                .user(query)
                .advisors(
                        new QuestionAnswerAdvisor(vectorStore, searchRequest, customRagPrompt),
                        new MessageChatMemoryAdvisor(chatMemory, chatId, 20)
                )
                .call()
                .content();
    }

    @GetMapping("/history")
    public List<Map<String, String>> getHistory(@RequestParam String chatId) {
        List<Message> messages = chatMemory.get(chatId, 100);
        return messages.stream().map(msg -> {
            String role = (msg instanceof UserMessage) ? "user" : "ai";
            return Map.of("role", role, "content", msg.getText());
        }).collect(Collectors.toList());
    }

    @GetMapping("/clear")
    public String clearHistory(@RequestParam String chatId) {
        chatMemory.clear(chatId);
        return "success";
    }
}