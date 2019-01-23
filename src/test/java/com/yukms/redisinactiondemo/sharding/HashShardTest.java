package com.yukms.redisinactiondemo.sharding;

import com.yukms.redisinactiondemo.BaseRedisServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yukms 2019/1/23
 */
public class HashShardTest extends BaseRedisServiceTest {

    @Autowired
    private HashShard hashShard;
    private static final String KEY = "hashkeytest";
    private static final long TOTAL_ELEMENTS = 100000;
    private static final long SHARD_SIZE = 1000;

    @Test
    public void test_shardKey() {
        Assert.assertEquals("hashkeytest:1", hashShard.shardKey(KEY, "1000", TOTAL_ELEMENTS, SHARD_SIZE));
        Assert.assertEquals("hashkeytest:8", hashShard.shardKey(KEY, "yukms", TOTAL_ELEMENTS, SHARD_SIZE));
    }

    @Test
    public void test_hget_integer_key() {
        test_set_and_get("1000", "1000");
    }

    @Test
    public void test_hget_string_key() {
        test_set_and_get("yukms", "yukms");
    }

    private void test_set_and_get(String hashKey, String value) {
        hashShard.set(KEY, hashKey, value, TOTAL_ELEMENTS, SHARD_SIZE);
        Assert.assertEquals(value, hashShard.get(KEY, hashKey, TOTAL_ELEMENTS, SHARD_SIZE));
    }

}