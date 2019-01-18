package com.yukms.redisinactiondemo.redis;

import java.util.concurrent.ExecutionException;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import com.yukms.redisinactiondemo.common.util.CrossTaskRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 测试事务命令
 *
 * @author yukms 2019/1/13
 */
public class TransactionTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String SILENCE = "silence:";

    @Before
    public void openTransaction() {
        stringRedisTemplate.setEnableTransactionSupport(true);
    }

    @Test
    public void test_no_transaction() throws InterruptedException, ExecutionException {
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        CrossTaskRunner.run(controller -> {
            stringValueOperations.append(SILENCE, "1");
            controller.notifyInsertAndAwaitMain();
            stringValueOperations.append(SILENCE, "1");
        }, () -> stringValueOperations.append(SILENCE, "2"));
        Assert.assertEquals("121", stringValueOperations.get(SILENCE));
    }

    @Test
    public void test_multi_and_exec() throws InterruptedException, ExecutionException {
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        CrossTaskRunner.run(controller -> {
            stringRedisTemplate.multi();
            stringValueOperations.append(SILENCE, "1");
            controller.notifyInsertAndAwaitMain();
            stringValueOperations.append(SILENCE, "1");
            stringRedisTemplate.exec();
        }, () -> stringValueOperations.append(SILENCE, "2"));
        Assert.assertEquals("211", stringValueOperations.get(SILENCE));
    }

    @Test
    public void test_watch() throws ExecutionException, InterruptedException {
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        CrossTaskRunner.run(controller -> {
            stringRedisTemplate.watch(SILENCE);
            stringRedisTemplate.multi();
            stringValueOperations.append(SILENCE, "1");
            controller.notifyInsertAndAwaitMain();
            stringValueOperations.append(SILENCE, "1");
            stringRedisTemplate.exec();
        }, () -> stringValueOperations.append(SILENCE, "2"));
        Assert.assertEquals("2", stringValueOperations.get(SILENCE));
    }
}
