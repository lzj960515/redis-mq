package com.github.lzj960515.redis.mq.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.omg.SendingContext.RunTime;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 下单5分钟后，支付超时取消订单案例
 *
 * @author Zijian Liao
 * @since 1.0.0
 */
@Slf4j
public class OrderDemoTest {

    @Test
    public void order() throws IOException {
        // 假设这里已经下单并得到了订单id
        String orderId = UUID.randomUUID().toString();
        new Thread(() -> {
            try {
                // 延时5分钟
                TimeUnit.SECONDS.sleep(5L);
                // 查询订单是否支付，未支付则取消
                boolean isPay = checkOrderPayState(orderId);
                log.info("订单支付状态：{}", isPay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        System.in.read();
    }

    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(60));

    @Test
    public void orderPro() throws IOException {
        // 假设这里已经下单并得到了订单id
        String orderId = UUID.randomUUID().toString();
        executor.execute(() -> {
            try {
                // 延时5分钟
                TimeUnit.SECONDS.sleep(5L);
                // 查询订单是否支付，未支付则取消
                boolean isPay = checkOrderPayState(orderId);
                log.info("订单支付状态：{}", isPay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void orderPlus() throws IOException {
        // 假设这里已经下单并得到了订单id
        String orderId = UUID.randomUUID().toString();
        // 计算订单检查是否支付超时时间点
        long checkTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        executor.execute(() -> {
            try {
                // 延时等待至订单超时时间
                Thread.sleep(checkTime - System.currentTimeMillis());
                // 查询订单是否支付，未支付则取消
                boolean isPay = checkOrderPayState(orderId);
                log.info("当前时间:{} 订单支付状态：{}", LocalDateTime.now(), isPay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }


    // 定义阻塞队列
    private static final BlockingQueue<Order> checkQueue = new PriorityBlockingQueue<>(100);

    @Test
    public void orderProMax() {
        // 假设这里已经下单并得到了订单id
        String orderId = UUID.randomUUID().toString();
        // 计算订单检查是否支付超时时间点
        long checkTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        // 将任务放入队列
        checkQueue.offer(new Order(orderId, checkTime));
    }
    // 定义静态变量用于唤醒线程时使用
    private static Thread thread = null;

    private void start() {
        // 开启线程从队列中获取任务
        thread = new Thread(() -> {
            while (true) {
                try{
                    Order order = take();
                    // 查询订单是否支付，未支付则取消
                    boolean isPay = checkOrderPayState(order.orderId);
                    log.info("执行完毕，订单id:{}, 检查时间：{}", order.orderId, df.format(new Date(order.checkTime)));
                    // 修改任务状态
                    updateTaskState(order.orderId);
                }catch (Exception e){
                    log.info(e.getMessage(), e);
                }
            }
        });
        thread.start();
    }

    private void rePushTask(){
        // 查询未执行的任务列表
        List<Task> taskList = queryTask();
        for (Task task : taskList) {
            // 将任务放入队列
            checkQueue.offer(task.getOrder());
        }
    }

    private List<Task> queryTask(){
        // 查询未执行的任务列表
        return new ArrayList<>();
    }

    private Order take() {
        while (true) {
            Order order = checkQueue.peek();
            // 无订单则直接阻塞等待
            if (order == null) {
                LockSupport.park(this);
            } else {
                if (order.getDelay() <= 0) {
                    return checkQueue.poll();
                }
                // 延时等待至订单超时时间
                LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(order.getDelay()));
            }
        }
    }

    private static final AtomicInteger orderIdGenerator = new AtomicInteger(0);
    private static final DateFormat df = new SimpleDateFormat("HH:mm:ss");

    @Test
    public void orderPlusMax() {
        // 假设这里已经下单并得到了订单id
        String orderId = String.valueOf(orderIdGenerator.incrementAndGet());
        // 随机一个20秒内的检查时间
        long checkTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(new Random().nextInt(20));
        // 将任务放入队列
        Order order = new Order(orderId, checkTime);
        if (checkQueue.offer(order)) {
            log.info("订单id:{}, 检查时间：{}", orderId, df.format(new Date(checkTime)));
            // 插入数据到MySQL
            saveTask(order);
            // 判断新放入的订单是否是第一个，是则说明新的订单是最早执行的
            if (orderId.equals(checkQueue.peek().orderId)) {
                // 唤醒线程
                LockSupport.unpark(thread);
            }
        }
    }

    private void saveTask(Order order){
        // 插入数据库
    }

    private void updateTaskState(String orderId){

    }

    private boolean checkOrderPayState(String orderId) {
        return new Random().nextInt(2) == 1;
    }

    @Test
    public void test() throws IOException, InterruptedException {
        start();
        for (int i = 0; i < 6; i++) {
            orderPlusMax();
        }
        System.in.read();
    }

    @Test
    public void testPark() throws InterruptedException, IOException {
        Thread thread = new Thread(() -> {
            log.info("线程开始休眠");
            LockSupport.park(this);
            log.info("线程已被唤醒");
        });
        thread.start();
        // 模拟2秒后有新的任务
        Thread.sleep(2000);
        log.info("放入新任务");
        // 唤醒线程
        LockSupport.unpark(thread);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 读取队列中的所有任务
            for (Order order : checkQueue) {
                // 持久化保存
                saveTask(order);
            }
        }));
        System.in.read();
    }
}
