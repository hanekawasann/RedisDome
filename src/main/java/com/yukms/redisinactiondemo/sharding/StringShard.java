package com.yukms.redisinactiondemo.sharding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 字符串分片
 *
 * @author yukms 2019/1/24
 */
@Component
public class StringShard {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

}
