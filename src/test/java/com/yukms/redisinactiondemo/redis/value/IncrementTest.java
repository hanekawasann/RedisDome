package com.yukms.redisinactiondemo.redis.value;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @author yukms 2019/1/22
 */
public class IncrementTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String VALUE_KEY = "valuekey";
    
    @Test
    public void test_increment() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        Long increment = valueOperations.increment(VALUE_KEY);
        Assert.assertNotNull(increment);
        Assert.assertEquals(1L, increment.longValue());
    }

}
