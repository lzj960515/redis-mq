package com.github.lzj960515.redis.mq.test;

import lombok.Data;

/**
 * @author Zijian Liao
 * @since 1.0.0
 */
@Data
public class Task {

    private Order order;
    // 执行状态：1、未执行 2、执行完成
    private Integer state;
}
