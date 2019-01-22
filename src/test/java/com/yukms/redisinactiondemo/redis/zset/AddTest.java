package com.yukms.redisinactiondemo.redis.zset;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/22
 */
public class AddTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String ADD_KEY = "addkeytest";
    private static final String ADD_VALUE = "addvaluetest";

    @Test
    public void test_add() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Boolean add = zSetOperations.add(ADD_KEY, ADD_VALUE, 1L);
        Assert.assertNotNull(add);
        Assert.assertEquals(true, add);

        Boolean add1 = zSetOperations.add(ADD_KEY, ADD_VALUE, 1L);
        Assert.assertNotNull(add1);
        Assert.assertEquals(false, add1);
    }

}
