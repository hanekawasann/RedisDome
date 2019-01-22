package com.yukms.redisinactiondemo.redis.zset;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/22
 */
public class IntersectAndStoreTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String SOURCE1_KEY = "source1zsettest";
    private static final String SOURCE2_KEY = "source2zsettest";
    private static final String DESTKEY_KEY = "destKeyzsettest";
    private static final String[] UUIDS = { getUUID(), getUUID(), getUUID() };
    private static final ZSet[] SOURCE1_MEMBER =//
        {//
            new ZSet(UUIDS[0], 1L),//
            new ZSet(UUIDS[1], 2L),//
            new ZSet(UUIDS[2], 3L),//
            new ZSet(getUUID(), 4L)//
        };
    private static final ZSet[] SOURCE2_MEMBER =//
        {//
            new ZSet(UUIDS[0], 5L),//
            new ZSet(UUIDS[1], 6L),//
            new ZSet(UUIDS[2], 7L),//
            new ZSet(getUUID(), 8L)//
        };

    @Before
    public void initData() {
        initData(SOURCE1_KEY, SOURCE1_MEMBER);
        initData(SOURCE2_KEY, SOURCE2_MEMBER);
    }

    @Test
    public void test_intersectAndStore_default() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.intersectAndStore(SOURCE1_KEY, Collections.singletonList(SOURCE2_KEY), DESTKEY_KEY);
        assertIntersectAndStore((source1, source2) -> source1 + source2);
    }

    @Test
    public void test_intersectAndStore_with_aggregate() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.intersectAndStore(SOURCE1_KEY, Collections.singletonList(SOURCE2_KEY), DESTKEY_KEY,
            RedisZSetCommands.Aggregate.MAX);
        assertIntersectAndStore(Math::max);
    }

    @Test
    public void test_intersectAndStore_with_weights() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.intersectAndStore(SOURCE1_KEY, Collections.singletonList(SOURCE2_KEY), DESTKEY_KEY,
            RedisZSetCommands.Aggregate.MAX, RedisZSetCommands.Weights.of(0, .1));
        assertIntersectAndStore((source1, source2) -> Math.max(source1 * 0, source2 * .1));
    }

    private void initData(String key, ZSet[] zets) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        for (ZSet zSet : zets) {
            zSetOperations.add(key, zSet.getValue(), zSet.getScore());
        }
    }

    private void assertIntersectAndStore(BiFunction<Double, Double, Double> biFunction) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.rangeWithScores(DESTKEY_KEY, 0, -1);
        Assert.assertNotNull(typedTuples);
        Assert.assertEquals(3, typedTuples.size());
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String value = typedTuple.getValue();
            Double score = typedTuple.getScore();
            Assert.assertNotNull(score);
            Double sum = biFunction
                .apply(findZSet(SOURCE1_MEMBER, value).getScore(), findZSet(SOURCE2_MEMBER, value).getScore());
            Assert.assertEquals(sum, score);
        }
    }

    private static ZSet findZSet(ZSet[] zSets, String value) {
        for (ZSet zSet : zSets) {
            if (zSet.getValue().equals(value)) {
                return zSet;
            }
        }
        throw new RuntimeException("Not find.");
    }

    private static String getUUID() {
        return UUID.randomUUID().toString();
    }

    private static class ZSet {
        private String value;
        private double score;

        public ZSet(String value, double score) {
            this.value = value;
            this.score = score;
        }

        public String getValue() {
            return value;
        }

        public double getScore() {
            return score;
        }
    }

}
