package com.github.lzj960515.redis.mq.test;

import lombok.Data;

@Data
public class Order implements Comparable<Order> {
    String orderId;
    long checkTime;

    Order(){}

    Order(String orderId, long checkTime) {
        this.orderId = orderId;
        this.checkTime = checkTime;
    }

    // 方便使用封装一个获取延时时间方法
    public long getDelay() {
        return checkTime - System.currentTimeMillis();
    }

    @Override
    public int compareTo(Order o) {
        return (int) (this.getDelay() - o.getDelay());
    }
}