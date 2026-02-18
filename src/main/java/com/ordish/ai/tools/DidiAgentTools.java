package com.ordish.ai.tools;

import com.ordish.ai.entity.DidiOrder;
import com.ordish.ai.service.IDidiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component; // 【重点】必须有

@Component // 必须加这个注解
@RequiredArgsConstructor
@Slf4j
public class DidiAgentTools {

    private final IDidiService didiService; // 如果上面没加 @Component，这里的 Service 就会是空的，导致无法入库

    @Tool(description = "用户想要打车时调用。必须获取出发地(start)和目的地(end)。")
    public String callCar(@ToolParam(description = "出发地") String start,
                          @ToolParam(description = "目的地") String end) {

        log.info("【Java后端】AI 触发打车工具: {} -> {}", start, end);

        // 这里去调用 Service 里的真逻辑
        DidiOrder order = didiService.callCar("didi_user_001", start, end);

        // 返回给 AI 看的结果
        return String.format(
                "下单成功！订单号：%s，司机：%s (%s)，预估价格：%s元，状态：%s",
                order.getOrderId(),
                order.getDriverName(),
                order.getPlateNumber(),
                order.getPrice(),
                "已接单 (司机正在赶来)" // 帮 AI 翻译一下状态
        );
    }
    @Tool(description = "根据订单号查询订单状态")
    public String queryOrder(@ToolParam(description = "订单号") String orderId) {
        log.info("【Java后端】执行查单: {}", orderId);

        DidiOrder order = didiService.getOrder(orderId);
        String statusDesc = order.getStatus();
        if ("ACCEPTED".equals(order.getStatus()) || "DISPATCHED".equals(order.getStatus())) {
            statusDesc = "已接单 (司机正在赶来)";
        } else if ("PENDING".equals(order.getStatus())) {
            statusDesc = "正在派单中 (暂无司机)";
        }

        return String.format(
                "订单查询成功！状态：%s，司机姓名：%s，车牌号：%s，价格：%s元",
                statusDesc, // 使用翻译后的中文描述
                order.getDriverName(),
                order.getPlateNumber(),
                order.getPrice()
        );
    }

    @Tool(description = "取消订单")
    public String cancelOrder(@ToolParam(description = "订单号") String orderId) {
        log.info("【Java后端】执行取消: {}", orderId);

        boolean success = didiService.cancelOrder(orderId);
        if (success) {
            return "系统提示：订单 " + orderId + " 已成功取消。";
        } else {
            return "系统提示：取消失败，订单不存在或状态不允许取消（如已完成）。";
        }
    }
}