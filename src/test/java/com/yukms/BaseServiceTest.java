package com.yukms;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yukms 2019/1/8.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BaseServiceTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 没运行一个新的单元测试方法，都清理redis
     */
    @Before
    public void init() {
        flushDb();
    }

    protected void flushDb() {
        redisTemplate.execute((RedisCallback<String>) connection -> {
            connection.flushDb();
            return "ok";
        });
    }
}
