package com.yukms.redis;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.yukms.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @author yukms 2019/1/13
 */
public class TransactionTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String SILENCE = "silence:";

    @Test
    public void test_multi_and_exec_no_transaction() throws InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        CyclicBarrier barrier = new CyclicBarrier(2);
        service.submit(new StringAppend(stringRedisTemplate, "1", 0L, 200L, barrier));
        service.submit(new StringAppend(stringRedisTemplate, "2", 200L, 0L, barrier));
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        Thread.sleep(300L);
        Assert.assertEquals("1221", stringValueOperations.get(SILENCE));
    }

    @Test
    @Ignore
    public void test_multi_and_exec_transaction() throws InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        CyclicBarrier barrier = new CyclicBarrier(2);
        service.submit(new TransactionStringAppend(stringRedisTemplate, "1", 0L, 200L, barrier));
        service.submit(new TransactionStringAppend(stringRedisTemplate, "2", 200L, 0L, barrier));
        ValueOperations<String, String> stringValueOperations = stringRedisTemplate.opsForValue();
        Thread.sleep(300L);
        Assert.assertEquals("1122", stringValueOperations.get(SILENCE));
    }

    private static class StringAppend implements Runnable {
        protected StringRedisTemplate stringRedisTemplate;
        private String s;
        private long startSleepMillis;
        private long middleSleepMillis;
        private CyclicBarrier barrier;

        public StringAppend(StringRedisTemplate stringRedisTemplate, String s, long startSleepMillis,
            long middleSleepMillis, CyclicBarrier barrier) {
            this.stringRedisTemplate = stringRedisTemplate;
            this.s = s;
            this.startSleepMillis = startSleepMillis;
            this.middleSleepMillis = middleSleepMillis;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                Thread.sleep(startSleepMillis);
                ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
                valueOperations.append(SILENCE, s);
                Thread.sleep(middleSleepMillis);
                valueOperations.append(SILENCE, s);
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    private static class TransactionStringAppend extends StringAppend {

        public TransactionStringAppend(StringRedisTemplate stringRedisTemplate, String s, long startSleepMillis,
            long middleSleepMillis, CyclicBarrier barrier) {
            super(stringRedisTemplate, s, startSleepMillis, middleSleepMillis, barrier);
        }

        @Override
        public void run() {
            stringRedisTemplate.multi();
            super.run();
            stringRedisTemplate.exec();
        }
    }
}
