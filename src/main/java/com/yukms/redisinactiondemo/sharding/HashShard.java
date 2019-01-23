package com.yukms.redisinactiondemo.sharding;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 对散列进行分片首先需要选择一个方法来对数据进行划分。因为散列本身就存储这一些键，
 * 所以程序在对键进行划分的时候，可以把散列存储的键用作其中一个信息源，并使用散列函数为键计算出一个数组散列值。
 * 然后程序会根据需要存储的键的总数量以及每个分片需要存储的键数量，
 * 并使用这个分片数量和键的散列值来决定应该把键存储到哪个分片里面。
 * <p/>
 *
 *
 * @author yukms 2019/1/23
 */
@Component
public class HashShard {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String SHARD_KEY = "{0}:{1}";
    private static final String INTEGER_PATTERN = "\\d+";

    /**
     * 根据基础键以及散列包含的键计算出分片键的函数。
     * <p/>
     * 对于数字键，程序会假设它们在某种程度上是连续而且密集地出现，并且会基于数字键本身的数值来指派分片ID，
     * 从而使得数值上相似的键可以被存储到同一个分片里面。
     *
     * @param key           散列名称
     * @param hashkey       hash键
     * @param totalElements 预计的元素总数量
     * @param shardSize     分片中元素的数量
     * @return 分片键
     */
    public String shardKey(String key, String hashkey, long totalElements, long shardSize) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(hashkey);
        long shardId;
        // todo：是不是可以使用hashcode？
        if (Pattern.matches(INTEGER_PATTERN, hashkey)) {
            // 1. 如果值是一个整数或者看上去像是整数的字符串，那么它将被直接用于计算分片ID
            shardId = Math.round(Double.valueOf(hashkey) / shardSize);
        } else {
            // 2. 对于不是整数的键，程序将基于预计的元素总数以及请求的分片数量，计算出实际所需的分片总数量
            CRC32 crc32 = new CRC32();
            crc32.update(hashkey.getBytes());
            // 这里为什么*2呢？
            long shards = Math.round(2.0D * totalElements / shardSize);
            shardId = crc32.getValue() % shards;
        }
        return MessageFormat.format(SHARD_KEY, key, shardId);
    }

    public void set(String key, String hashkey, String value, long totalElements, long shardSize) {
        String shardKey = shardKey(key, hashkey, totalElements, shardSize);
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        hashOperations.put(shardKey, hashkey, value);
    }

    public String get(String key, String hashkey, long totalElements, long shardSize) {
        String shardKey = shardKey(key, hashkey, totalElements, shardSize);
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        return hashOperations.get(shardKey, hashkey);
    }

}
