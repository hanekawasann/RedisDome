package com.yukms.redisinactiondemo.redis.list;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author yukms 2019/1/22
 */
public class RightPushTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String LIST_KEY = "listkeytest";

    @Test
    public void test_rightPush() {
        String value = "value";
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        Long aLong = listOperations.rightPush(LIST_KEY, value);
        Assert.assertNotNull(aLong);
        Assert.assertEquals(1L, aLong.longValue());
        Long aLong1 = listOperations.rightPush(LIST_KEY, value);
        Assert.assertNotNull(aLong1);
        Assert.assertEquals(2L, aLong1.longValue());
    }

}
