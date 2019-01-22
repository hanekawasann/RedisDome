package com.yukms.redisinactiondemo.redis.zset;

import java.util.Collections;
import java.util.Set;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/22
 */
public class IntersectAndStoreTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String SOURCE_KEY1 = "sourcezsettest1";
    private static final String SOURCE_KEY2 = "sourcezsettest2";
    private static final String DESTKEY_KEY = "destKeyzsettest";

    @Test
    public void test_intersectAndStore_default() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.intersectAndStore(SOURCE_KEY1, Collections.emptyList(), DESTKEY_KEY);
    }

    private static class ZSet {
        private String member;
        private long score;

        public ZSet(String member, long score) {
            this.member = member;
            this.score = score;
        }

        public String getMember() {
            return member;
        }

        public long getScore() {
            return score;
        }
    }

}
