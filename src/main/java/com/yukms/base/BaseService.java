package com.yukms.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author yukms 2019/1/9.
 */
@Service
public class BaseService {
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;
}
