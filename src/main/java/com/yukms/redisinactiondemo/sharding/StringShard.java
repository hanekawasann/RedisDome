package com.yukms.redisinactiondemo.sharding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 字符串分片。
 * <p/>
 * 当用户使用诸如namespace:id这样的字符串键去存储端字符串或者计数器时，使用分片散列可以有效降低存储这些数据所需的内存。
 * 但是如果被存储的是一些简短并且长度固定的连续ID，那么我们还有比使用分片散列更为节约内存的数据存储方法可用。
 *
 * @author yukms 2019/1/24
 */
@Component
public class StringShard {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

}
