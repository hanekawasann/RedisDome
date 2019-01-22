package com.yukms.redisinactiondemo.redis.zset;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author yukms 2019/1/22
 */
public class RemoveRangeTest extends BaseRedisServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * {@code zSetOperations.removeRange(key, 0, -6)}与
     * {@code zSetOperations.removeRange(key, 0, 4)}效果相同，但语义不同。
     */
    @Test
    public void test_removeRange_of_zset_001() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
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

        flushDb();

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

    /**
     * 验证书中的一处错误
     */
    @Test
    public void test_removeRange_of_zset_002() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        String key = "UUID:";
        int length = 10;
        List<String> uuids = getUUIDs(length);
        // 初始化数据
        for (int i = 0; i < length; i++) {
            String uuid = uuids.get(i);
            Stream.generate(() -> StringUtils.EMPTY).limit(length - i)
                .forEach(s -> zSetOperations.incrementScore(key, uuid, -1));
        }
        // 打印初始化数据
        Set<String> members = zSetOperations.range(key, 0, -1);
        Assert.assertNotNull(members);
        Assert.assertEquals(10, members.size());
        members.forEach(member -> {
            Double score = zSetOperations.score(key, member);
            System.out.println(score + "\t" + member);
        });
        Stream.generate(() -> "-").limit(uuids.get(0).length()).forEach(System.out::print);
        System.out.println();
        // 保留最高5个元素
        zSetOperations.removeRange(key, 0, -6);
        // 获取剩余元素
        Set<String> members2 = zSetOperations.range(key, 0, -1);
        Assert.assertNotNull(members2);
        Assert.assertEquals(5, members2.size());
        // 打印剩余元素
        members2.forEach(member -> {
            Double score = zSetOperations.score(key, member);
            System.out.println(score + "\t" + member);
        });
        // 验证剩余元素
        List<String> uusidSubList = uuids.subList(5, 10);
        ArrayList<String> listMembers2 = new ArrayList<>(members2);
        for (int i = 0; i < listMembers2.size(); i++) {
            Assert.assertEquals(uusidSubList.get(i), listMembers2.get(i));
        }
    }

    private List<String> getUUIDs(int length) {
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            uuids.add(UUID.randomUUID().toString());
        }
        return uuids;
    }

}
