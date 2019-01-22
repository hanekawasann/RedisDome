package com.yukms.redisinactiondemo.redis.zset;

import java.util.ArrayList;
import java.util.Set;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/8.
 */
public class IncrementScoreTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test_zincrby_of_zset() {
        String key = "myzset";
        String member = "member";
        assertBeforeZincrby(key, member);
        stringRedisTemplate.opsForZSet().incrementScore(key, member, -1);
        assertAfterZincrby(key, member);
    }

    private void assertBeforeZincrby(String key, String member) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Long size = zSetOperations.size(key);
        Assert.assertTrue(null == size || size == 0L);

        Double score = zSetOperations.score(key, member);
        Assert.assertNull(score);
    }

    private void assertAfterZincrby(String key, String member) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Long size = zSetOperations.size(key);
        Assert.assertNotNull(size);
        Assert.assertEquals(1L, size.intValue());

        Set<String> members = zSetOperations.range(key, 0, 0);
        Assert.assertNotNull(members);
        Assert.assertEquals(member, new ArrayList<>(members).get(0));

        Double score = zSetOperations.score(key, member);
        Assert.assertNotNull(score);
        Assert.assertEquals(-1L, score.longValue());
    }


}
