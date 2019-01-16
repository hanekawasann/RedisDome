package com.yukms.redis;

import com.yukms.BaseRedisServiceTest;
import com.yukms.common.util.AverageTimer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.lang.Nullable;

/**
 * 测试流水线功能
 *
 * @author yukms 2019/1/16
 */
public class NoTransactionalPipelinedTest extends BaseRedisServiceTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String PIPELINED = "pipelined";
    private static final int OUTER_TIMES = 100;
    private static final int INSIDE_TIMES = 1000;

    @Test
    public void test_no_pipelined() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        AverageTimer.time("no pipelined", OUTER_TIMES, time -> {
            for (int i = 0; i < INSIDE_TIMES; i++) {
                valueOperations.append(PIPELINED + time, "value");
            }
        });
    }

    @Test
    public void test_sessionCallback_of_pipelined() {
        AverageTimer.time("SessionCallback", OUTER_TIMES, time -> {
            stringRedisTemplate.executePipelined(new SessionCallback<Boolean>() {
                @Nullable
                @Override
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    ValueOperations<String, String> valueOperations = operations.opsForValue();
                    for (int i = 0; i < INSIDE_TIMES; i++) {
                        valueOperations.append(PIPELINED + time, "value");
                    }
                    return null;
                }
            });
        });
    }

    @Test
    public void test_redisCallback_of_pipelined() {
        AverageTimer.time("RedisCallback", OUTER_TIMES, time -> {
            stringRedisTemplate.executePipelined(new RedisCallback<Boolean>() {
                @Nullable
                @Override
                public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                    for (int i = 0; i < INSIDE_TIMES; i++) {
                        connection.append((PIPELINED + time).getBytes(), "value".getBytes());
                    }
                    return null;
                }
            });
        });
    }

}
