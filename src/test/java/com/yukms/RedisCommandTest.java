package com.yukms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/8.
 */
public class RedisCommandTest extends BaseServiceTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * {@code zSetOperations.removeRange(key, 0, -6)}与{@code zSetOperations.removeRange(key, 0, 4)}效果相同，但语义不同。
     */
    @Test
    public void test_removeRange_of_zset() {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        int length = 10;
        List<String> uuids = getUUIDs(length);
        uuids.forEach(System.out::println);
        Stream.generate(() -> "-").limit(uuids.get(0).length()).forEach(System.out::print);
        System.out.println();
        String key = "myzset";
        for (int i = 0; i < length; i++) {
            zSetOperations.add(key, uuids.get(i), i);
        }
        Set<String> beforeRemove = zSetOperations.range(key, 0, -1);
        Assert.assertNotNull(beforeRemove);
        Assert.assertEquals(length, beforeRemove.size());
        zSetOperations.removeRange(key, 0, -6);
        Set<String> afterRemove = zSetOperations.range(key, 0, -1);
        Assert.assertNotNull(afterRemove);
        afterRemove.forEach(System.out::println);
        Assert.assertEquals(5, afterRemove.size());
        LinkedList<String> linkedList = new LinkedList<>(afterRemove);
        Assert.assertEquals(uuids.get(5), linkedList.peekFirst());
        Assert.assertEquals(uuids.get(9), linkedList.peekLast());
        Stream.generate(() -> "-").limit(uuids.get(0).length()).forEach(System.out::print);
        System.out.println();

        redisTemplate.delete(key);
        for (int i = 0; i < length; i++) {
            zSetOperations.add(key, uuids.get(i), i);
        }
        zSetOperations.removeRange(key, 0, 4);
        Set<String> afterRemove2 = zSetOperations.range(key, 0, -1);
        Assert.assertNotNull(afterRemove2);
        afterRemove2.forEach(System.out::println);
        Assert.assertEquals(5, afterRemove2.size());
        LinkedList<String> linkedList2 = new LinkedList<>(afterRemove2);
        Assert.assertEquals(uuids.get(5), linkedList2.peekFirst());
        Assert.assertEquals(uuids.get(9), linkedList2.peekLast());
    }

    public List<String> getUUIDs(int length) {
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            uuids.add(UUID.randomUUID().toString());
        }
        return uuids;
    }
}
