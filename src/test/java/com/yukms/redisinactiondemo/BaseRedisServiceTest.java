package com.yukms.redisinactiondemo;

import org.junit.After;
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
public class BaseRedisServiceTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 每运行一个新的单元测试方法之前，都清理redis所有的数据库
     */
    @Before
    public void init() {
        flushDb();
    }

    /**
     * 每运行一个新的单元测试方法之后，都清理redis所有的数据库
     */
    @After
    public void clear() {
        flushDb();
    }

    protected void flushDb() {
        redisTemplate.execute((RedisCallback<String>) connection -> {
            connection.flushAll();
            return "ok";
        });
    }
}
