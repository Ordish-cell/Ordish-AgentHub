package com.ordish.ai.mapper;

import com.ordish.ai.entity.DidiOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;

@Mapper
public interface DidiOrderMapper {

    // --- 订单相关 ---
    @Insert("INSERT INTO didi_orders(order_id, user_id, start_addr, end_addr, status, driver_name, plate_number, price, create_time) " +
            "VALUES(#{orderId}, #{userId}, #{startAddr}, #{endAddr}, #{status}, #{driverName}, #{plateNumber}, #{price}, #{createTime})")
    int insert(DidiOrder order);

    @Select("SELECT * FROM didi_orders WHERE order_id = #{orderId}")
    DidiOrder selectById(String orderId);

    @Update("UPDATE didi_orders SET status = #{status} WHERE order_id = #{orderId}")
    int updateStatus(String orderId, String status);

    // --- 新增：计价规则查询 ---

    // 1. 查距离：看看数据库有没有存这条路有多远
    @Select("SELECT distance_km FROM didi_mock_routes WHERE start_addr = #{start} AND end_addr = #{end}")
    Integer selectDistance(String start, String end);

    // 2. 查单价：根据城市查价格 (起步价, 每公里价)
    // 这是一个简单的 Map 映射，实际开发建议用实体类，这里偷懒用 Map 或者对象数组
    @Select("SELECT price_per_km FROM didi_price_rules WHERE city = #{city}")
    BigDecimal selectPricePerKm(String city);

    @Select("SELECT base_price FROM didi_price_rules WHERE city = #{city}")
    BigDecimal selectBasePrice(String city);
}