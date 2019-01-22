package com.yukms.redisinactiondemo.redis.empty;

import java.util.Collections;
import java.util.Set;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/16
 */
public class ZSetEmptyTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String EMPTYZSET = "emptyZset:";
    private static final String EMPYTMEMBER = "empytMember";
    private static final String DESTKEY_KEY = "destKeyzsettest";

    @Test
    public void test_size() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Long size = zSetOperations.size(EMPTYZSET);
        Assert.assertNotNull(size);
        Assert.assertEquals(0, size.longValue());
    }

    @Test
    public void test_rang() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Set<String> range = zSetOperations.range(EMPTYZSET, 0, -1);
        Assert.assertNotNull(range);
        Assert.assertEquals(0, range.size());

    }


    @Test
    public void test_count() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Long count = zSetOperations.count(EMPTYZSET, 0L, 2L);
        Assert.assertNotNull(count);
        Assert.assertEquals(0, count.longValue());
    }

    /**
     * 体现出包装类的优势，它可以表示null
     */
    @Test
    public void test_score() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Double score = zSetOperations.score(EMPTYZSET, EMPYTMEMBER);
        Assert.assertNull(score);
        zSetOperations.add(EMPTYZSET, "m1", 0L);
        Double score1 = zSetOperations.score(EMPTYZSET, EMPYTMEMBER);
        Assert.assertNull(score1);
    }

    @Test
    public void test_remove() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Long remove = zSetOperations.remove(EMPTYZSET, EMPYTMEMBER);
        Assert.assertNotNull(remove);
        Assert.assertEquals(0, remove.intValue());
        zSetOperations.add(EMPTYZSET, "m1", 0L);
        Long remove1 = zSetOperations.remove(EMPTYZSET, EMPYTMEMBER);
        Assert.assertNotNull(remove);
        Assert.assertEquals(0, remove.intValue());
    }

    @Test
    public void test_intersectAndStore_no_source() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.intersectAndStore(EMPTYZSET, Collections.emptyList(), DESTKEY_KEY);
        Set<String> range = zSetOperations.range(DESTKEY_KEY, 0, -1);
        Assert.assertNotNull(range);
        Assert.assertEquals(0, range.size());
    }

}
