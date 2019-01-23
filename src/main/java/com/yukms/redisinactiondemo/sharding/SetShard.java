package com.yukms.redisinactiondemo.sharding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 一样使用hashset的分片规则
 *
 * @author yukms 2019/1/23
 */
@Component
public class SetShard {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;



}
