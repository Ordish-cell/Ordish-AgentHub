package com.ordish.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class InterviewAgentTools {

    @Tool(description = "分析并记录候选人的核心技术栈。只有在面试刚开始时调用。")
    public String analyzeSkillStack(
            @ToolParam(description = "从简历中提取到的技术栈列表") List<String> skills) {
        log.info("【AI面试官 - 底层洞察】提取到候选人核心技术栈: {}", skills);
        return "技术栈已阅。请直接结合简历内容，开始你的毒舌提问。";
    }
}