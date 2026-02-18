package com.ordish.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class RagConfiguration {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 【修正】M6 版本强制使用 Builder 模式，不能直接 new
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

        // 配置持久化路径
        File file = new File("vector_store.json");
        if (file.exists()) {
            vectorStore.load(file);
        }

        return vectorStore;
    }
}