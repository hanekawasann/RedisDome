package com.yukms.redisinactiondemo.queue;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 简陋的先进先出队列
 *
 * @author yukms 2019/1/22
 */
@Component
public class FIFOQueue {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String EMAIL_KEY = "queue:email";
    private static final String QUEUE_KEY = "queue:{0}";

    public void sendSoldEmailViaQueue(SoldEmail email) {
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        listOperations.rightPush(EMAIL_KEY, JSON.toJSONString(email));
    }

    public void processSoldEmailQueue() {
        for (; ; ) {
            ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
            String jsonString = listOperations.leftPop(EMAIL_KEY, 1, TimeUnit.SECONDS);
            if (StringUtils.isBlank(jsonString)) {
                continue;
            }
            SoldEmail email = JSON.parseObject(jsonString, SoldEmail.class);
            // 发送邮件
        }
    }

    /**
     * 一个队列可以执行多种任务
     *
     * @param queue     队列名
     * @param callbacks 回调函数
     */
    public void workerWatchQueue(String queue, Consumer<String> callbacks) {
        String key = MessageFormat.format(QUEUE_KEY, queue);
        for (; ; ) {
            ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
            String jsonString = listOperations.leftPop(key, 1, TimeUnit.SECONDS);
            if (StringUtils.isBlank(jsonString)) {
                continue;
            }
            callbacks.accept(jsonString);
        }
    }

    /**
     * 任务优先级
     * <p/>
     * 高优先级任务再出现之后会第一时间被执行，而中等优先级任务会在没有任何高优先级任务存在的情况下被执行，
     * 而低优先级任务则会在既没有任何高优先级任务，有没有任何中等优先级任务的情况下被执行。
     * <p/>
     * 同时使用多个队列可以降低实现优先级特性的难度。除此之外，多队列有时候也被用于分隔不同的任务，在这种情况下，
     * 处理不同队列时可能会出现不公平的现象，为此，我们可以偶尔重新排列各个队列的顺序，使得针对队列的处理变得
     * 更为公平一些——当某个队列的增长速度比其他队列的增长速度块的时候，这种重拍操作尤为重要。
     *
     * @param queue     队列名
     * @param callbacks 回调函数
     */
    public void workerWatchQueues(String queue, Consumer<String> callbacks) {
        String key = MessageFormat.format(QUEUE_KEY, queue);
        for (; ; ) {
            ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
            // leftPop对应于blpop命令，但是多key不支持吗？
            String jsonString = listOperations.leftPop(key, 1, TimeUnit.SECONDS);
            if (StringUtils.isBlank(jsonString)) {
                continue;
            }
            callbacks.accept(jsonString);
        }
    }

}
