package com.ordish.ai.service;

import com.ordish.ai.entity.DidiOrder;

public interface IDidiService {
    DidiOrder callCar(String userId, String start, String end);
    DidiOrder getOrder(String orderId);
    boolean cancelOrder(String orderId);
}