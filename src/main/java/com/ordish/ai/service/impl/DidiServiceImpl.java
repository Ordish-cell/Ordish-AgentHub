package com.ordish.ai.service.impl;

import com.ordish.ai.entity.DidiOrder;
import com.ordish.ai.mapper.DidiOrderMapper;
import com.ordish.ai.service.IDidiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class DidiServiceImpl implements IDidiService {

    private final DidiOrderMapper didiOrderMapper;

    @Override
    @Transactional
    public DidiOrder callCar(String userId, String start, String end) {
        // 1. 智能计价逻辑
        // 尝试从地址中推断城市（简单模拟：如果包含"大理"就是大理，否则默认）
        String city = "DEFAULT";
        if (start.contains("北京") || end.contains("北京")) city = "北京";
        else if (start.contains("上海") || end.contains("上海")) city = "上海";
        else if (start.contains("大理") || end.contains("大理")) city = "大理";

        // 2. 从数据库查询费率
        BigDecimal basePrice = didiOrderMapper.selectBasePrice(city);
        if (basePrice == null) basePrice = didiOrderMapper.selectBasePrice("DEFAULT"); // 兜底

        BigDecimal perKmPrice = didiOrderMapper.selectPricePerKm(city);
        if (perKmPrice == null) perKmPrice = didiOrderMapper.selectPricePerKm("DEFAULT");

        // 3. 从数据库查询距离 (如果没有记录，随机生成一个 5-30km 的距离)
        Integer distance = didiOrderMapper.selectDistance(start, end);
        if (distance == null) {
            distance = new Random().nextInt(25) + 5;
            log.info("未找到预设路线，模拟距离: {} km", distance);
        } else {
            log.info("命中数据库预设路线: {} -> {}, 距离: {} km", start, end, distance);
        }

        // 4. 核心公式：价格 = 起步价 + (距离 * 单价)
        BigDecimal totalPrice = basePrice.add(perKmPrice.multiply(new BigDecimal(distance)));

        // 5. 模拟司机信息
        String[] drivers = {"王师傅", "赵师傅", "刘师傅"};
        String[] plates = {"京A·88888", "云L·66666", "沪C·12345"};
        Random r = new Random();

        // 6. 落库
        DidiOrder order = DidiOrder.builder()
                .orderId("DD" + System.currentTimeMillis())
                .userId(userId)
                .startAddr(start)
                .endAddr(end)
                .status("ACCEPTED")
                .driverName(drivers[r.nextInt(drivers.length)])
                .plateNumber(plates[r.nextInt(plates.length)])
                .price(totalPrice) // 存入精确计算的价格
                .createTime(LocalDateTime.now())
                .build();

        didiOrderMapper.insert(order);
        return order;
    }

    @Override
    public DidiOrder getOrder(String orderId) {
        return didiOrderMapper.selectById(orderId);
    }

    @Override
    public boolean cancelOrder(String orderId) {
        // 简单模拟
        return didiOrderMapper.updateStatus(orderId, "CANCELLED") > 0;
    }
}