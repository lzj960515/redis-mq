package com.github.lzj960515.redis.mq.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Zijian Liao
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
public class OrderRedisTest {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testZSet(){
        String key = "task";
        // 往集合中添加一个元素 参数分别为：集合key, 元素值, 分数
        redisTemplate.opsForZSet().add(key, "2", 2);
        redisTemplate.opsForZSet().add(key, "1", 1);
        redisTemplate.opsForZSet().add(key, "3", 3);
        // 从集合中按从小到大的方式取出元素，参数分别为：集合key, 分数最小值, 分数最大值
        // 最小值和最大值是边界，-1, 6表示取出集合中分数为-1到6的元素
        Set<String> set = redisTemplate.opsForZSet().rangeByScore(key, -1, 6);
        // 取出元素并带分数 TypedTuple类型有两个变量：value, score
        Set<TypedTuple<String>> set2 = redisTemplate.opsForZSet().rangeByScoreWithScores(key, -1, 6);
        // 取出元素，最小值-1，最大值6，从offset 0开始，总数取1个
        Set<String> set3 = redisTemplate.opsForZSet().rangeByScore(key, -1, 6, 1, 1);
        for (String s : set3) {
            // 取出的是2
            System.out.println(s);
        }
        redisTemplate.delete(key);
    }

    private static final AtomicInteger orderIdGenerator = new AtomicInteger(0);
    private static final DateFormat df = new SimpleDateFormat("HH:mm:ss");
    private static final String key = "task";

    private ZSetOperations<String, String> zSetOperations;
    // 定义静态变量用于唤醒线程时使用
    private static Thread thread = null;

    private void start() {
        // 开启线程从队列中获取任务
        thread = new Thread(() -> {
            while (true) {
                try{
                    Order order = take();
                    // 查询订单是否支付，未支付则取消
                    log.info("执行完毕，订单id:{}, 检查时间：{}", order.orderId, df.format(new Date(order.checkTime)));
                }catch (Exception e){
                    log.info(e.getMessage(), e);
                }
            }
        });
        thread.start();
    }

    private Order take() {
        while (true) {
            // 获取第一个订单
            Set<TypedTuple<String>> typedTuples = zSetOperations.rangeByScoreWithScores(key, 0, Long.MAX_VALUE, 0, 1);
            Optional<TypedTuple<String>> first = typedTuples.stream().findFirst();
            // 无订单则直接阻塞等待
            if (!first.isPresent()) {
                LockSupport.park(this);
            } else {
                String orderId = first.get().getValue();
                Double checkTime = first.get().getScore();
                Order order = new Order(orderId, checkTime.longValue());

                if (order.getDelay() <= 0) {
                   // 删除zset中的订单
                   zSetOperations.remove(key, orderId);
                   return order;
                }
                // 延时等待至订单超时时间
                LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(order.getDelay()));
            }
        }
    }

    @Test
    public void orderPlusMax() {
        // 假设这里已经下单并得到了订单id
        String orderId = String.valueOf(orderIdGenerator.incrementAndGet());
        // 随机一个20秒内的检查时间
        long checkTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(new Random().nextInt(20));
        // 将任务放入队列
        if (Boolean.TRUE.equals(zSetOperations.add(key, orderId, checkTime))) {
            log.info("订单id:{}, 检查时间：{}", orderId, df.format(new Date(checkTime)));
            // 获取第一个订单
            Set<String> orderSet = zSetOperations.rangeByScore(key, 0, Long.MAX_VALUE, 0, 1);
            Optional<String> first = orderSet.stream().findFirst();
            String redisOrderId = first.get();

            // 判断新放入的订单是否是第一个，是则说明新的订单是最早执行的
            if (orderId.equals(redisOrderId)) {
                // 唤醒线程
                LockSupport.unpark(thread);
            }
        }
    }

    @Test
    public void test() throws IOException, InterruptedException {
        zSetOperations = redisTemplate.opsForZSet();
        start();
        for (int i = 0; i < 6; i++) {
            orderPlusMax();
        }
        System.in.read();
    }
}
