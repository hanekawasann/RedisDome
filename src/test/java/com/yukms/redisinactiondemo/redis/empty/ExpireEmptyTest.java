package com.yukms.redisinactiondemo.redis.empty;

import java.util.UUID;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * getExpire在没有key或者没有过期时间时居然不是返回null？这不跟其他的empty设计冲突了吗？
 * 这里没法测试出刚好过期的情况。
 *
 * @author yukms 2019/1/21
 */
public class ExpireEmptyTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String EXPIRE_KEY = "expireKey";

    @Test
    public void test_getExpire_no_key() {
        Long expire = stringRedisTemplate.getExpire(EXPIRE_KEY);
        Assert.assertNotNull(expire);
        Assert.assertEquals(-2, expire.intValue());
    }

    @Test
    public void test_getExpire_no_expire_time() {
        stringRedisTemplate.opsForValue().set(EXPIRE_KEY, UUID.randomUUID().toString());
        Long expire = stringRedisTemplate.getExpire(EXPIRE_KEY);
        Assert.assertNotNull(expire);
        Assert.assertEquals(-1, expire.intValue());
    }

}
