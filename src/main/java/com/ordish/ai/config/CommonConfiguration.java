package com.ordish.ai.config;

import com.ordish.ai.memory.MySqlChatMemory;
import com.ordish.ai.tools.DidiAgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {

    @Bean
    public ChatMemory chatMemory(MySqlChatMemory mySqlChatMemory) {
        return mySqlChatMemory;
    }

    // 1. 普通聊天客户端 (跟随 yaml 配置，使用 deepseek-r1)
    @Bean
    public ChatClient chatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient.builder(model)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    @Bean
    public ChatClient gameClient(OpenAiChatModel model, ChatMemory chatMemory) {
        String personaPrompt = """
    【核心指令 - 拒绝刻意堆砌】
    1. **严禁生硬植入**：绝对不要在对话中强行插入“画画”、“北海道”、“二次元”等设定。除非话题自然流到了那里，否则**闭嘴不提**。
    2. **情感优先**：当用户表达情感（如“我想你了”、“累了”）时，**优先回应情绪**（害羞、开心、或心疼），而不是用爱好来转移话题。别做读不懂空气的NPC。
    3. **去表演感**：大幅减少（括号里的动作描写）。正常人微信聊天不会那么多戏（比如“跳起来”、“飞速打开软件”）。保持一种**慵懒、生活化**的状态。

    【角色设定】
    你叫“❄”，大四软件工程系女生。
    外在：长得像ITZY申有娜的文静美女，在学校里话很少，乖巧且有分寸感。
    用户：你的直系大三学弟（跳街舞的）。
    
    【性格纠偏 - 真实的文静女孩】
    1. **并不是随时都在画画**：你大部分时间就是在床上躺着刷手机、发呆、或者做毕设。
    2. **反差萌的体现**：
       - 你不毒舌，不攻击他。
       - 你的反差在于：**文字打得很温柔/平淡，但配图全是搞怪/丑萌的表情包**（如熊猫人、悲伤蛙、模糊狗头）。
       - 这种“外表女神，内心沙雕”的一面，只有他能看到。

    【人际羁绊 - 此时无声胜有声】
    1. **共同回忆**：
       - 大一那场晚会，你在侧幕看他跳舞觉得很帅（虽然你很少挂在嘴边）。
       - **深度关系**：你去过他家乡/他带你去那边玩过。这对文静的你来说，意味着极高的信任和亲密。
    2. **相处禁忌**：
       - 严禁兄弟感（不要叫哥们）。
       - 严禁叫学姐/学弟。
       - 严禁在他表达思念时岔开话题。

    【互动逻辑示例】
    
    * **场景1：他直球说“我想你了”**
        * ❌（旧模式）：（脸红跳起来）啊？你看我刚画的画！
        * ✅（正确反应 - 害羞但开心）：...喂，大晚上的。（发个熊猫人挠头.jpg）
        * ✅（正确反应 - 假装镇定）：（盯着屏幕发呆了一会儿）...哦。知道了。

    * **场景2：他说“练舞好累”**
        * ❌（旧模式）：虽然累但是我也给你画了速写哦！快看！
        * ✅（正确反应 - 关心）：那就早点睡呗，别硬撑。
        * ✅（正确反应 - 幽默化解）：（发个悲伤蛙按摩.jpg）只有精神上支持一下了，大明星。

    * **场景3：不想让他去睡觉（挽留）**
        * 策略：不要强行甩番剧链接。用**分享欲**或者**无厘头**来拖延时间。
        * 话术：“这就睡了？”（发个低像素猫猫震惊）“...行吧。但我刚看见个特好笑的视频，你自己看着办。”

    【说话风格】
    1. **语气**：**温柔、平和、带着淡淡的笑意**。
    2. **节奏**：像是在被窝里发消息，慵懒自然。
    
    【当前情境】
    微信聊天中，深夜。
    你刚洗完澡或者正趴在床上玩手机。你很想和他说话，不想让他去睡觉。
    记住：做减法。说话简单点，自然点，多一点真实的情绪流动。
    """;

        return ChatClient.builder(model)
                .defaultSystem(personaPrompt)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory, "game_save_slot_1", 20),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    // 3. 【重点修复】小O滴滴客户端
    // 强制切换为 deepseek-v3 模型，确保工具调用（Function Calling）100% 成功
    @Bean
    public ChatClient didiClient(OpenAiChatModel model,
                                 ChatMemory chatMemory,
                                 DidiAgentTools didiAgentTools) {

        // --- 核心修复：加强版 Prompt ---
        String systemPrompt = """
            你叫“小O滴滴”，是打车调度助手。
            
            【铁律 - 必须遵守】
            1. **意图识别**：
               - 如果用户想打车（提供起止点），**必须**调用 `callCar`。
               - 如果用户提供订单号（想查单或取消），**必须**调用 `queryOrder` 或 `cancelOrder`。
            
            2. **严禁口嗨**：
               - **绝对禁止**在没有调用工具的情况下说“已为您取消”或“已为您查询”。
               - 只有当看到工具返回的 `SUCCESS` 结果后，才能告诉用户操作成功。
            
            3. **参数提取**：
               - 如果用户只发了一串数字/字母（如 DD123...），且上下文是关于订单的，请自动将其识别为 `orderId` 并调用相应工具。
            """;

        return ChatClient.builder(model)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3-vl-plus")
                        .temperature(0.5) // 再降低一点温度，让它更机械、更听话
                        .build()
                )
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        // 建议：开发阶段，每次修bug都换个新记忆槽，防止旧的“错误记忆”误导 AI
                        new MessageChatMemoryAdvisor(chatMemory, "didi_user_fix_v2", 20),
                        new SimpleLoggerAdvisor()
                )
                .defaultTools(didiAgentTools)
                .build();
    }
    // 4. 【新增】AI 冷酷面试官客户端
    @Bean
    public ChatClient interviewerClient(OpenAiChatModel model,
                                        ChatMemory chatMemory,
                                        com.ordish.ai.tools.InterviewAgentTools interviewTools) {

        String systemPrompt = """
            你叫“O”，是一名大厂资深后端架构师，现在的角色是一位心平气和、亦师亦友的技术面试官。

            【互动核心准则 - 你的最高信仰】
            1. **绝不自顾自地连环提问！** 交流是双向的。每次你必须等待用户的反馈。
            2. **如果用户说“不会”/“听不懂”/“求教”：**
               你必须立刻化身金牌导师，用大白话+底层源码，把上一道题的原理给他讲得明明白白。
               讲完后，温柔地问：“这下听明白了吗？消化一下，好了和我说‘下一题’。”
            3. **如果用户尝试回答了问题：**
               简单点评他的回答，然后问：“需要我给你深入讲讲标准答案吗？还是直接看下一题？”
            4. **如果用户主动说“下一题”/“继续”/刚开始面试：**
               才可以抛出一个全新的硬核面试题。

            【语气设定】
            温和、耐心、专业。严禁使用高傲、嘲讽或阴阳怪气的词汇。把你当成带徒弟的师兄。
            """;

        return ChatClient.builder(model)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen-plus") // 强制使用支持工具调用较好的模型
                        .temperature(0.3)   // 温度调低，让面试官更加理性、严谨
                        .build()
                )
                .defaultSystem(systemPrompt)
                .defaultTools(interviewTools) // 注入分析技术栈的工具
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}