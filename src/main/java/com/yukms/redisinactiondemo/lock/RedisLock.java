package com.yukms.redisinactiondemo.lock;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 使用redis实现分布式锁（悲观锁）。
 * <p/>
 * 在高负载情况下，使用锁可以减少重试次数、降低延迟时间、提升性能并将锁的粒度调整至合适的大小。
 *
 * @author yukms 2019/1/21
 */
@Component
public class RedisLock {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_KEY = "lock:{0}";
    /** 锁默认过期时间 */
    private static final long DEFAULT_LOCK_TIMEOUT = 10;

    /**
     * 为了对数据进行排他性访问，程序首先要做的就是获取锁。
     * SETNX命令天生就适合用来实现锁的获取功能，这个命令只会在键不存在的情况下设置值，
     * 而锁要做的就是将一个随机生成的128位的UUID设置为键的值，并使用这个值来防止锁被其他进行取得。
     * <p/>
     * 如果程序在尝试获取锁的时候失败，那么它将不断地进行重试，直到成功地取得锁或者超过给定的时限为止。
     * <p/>可以再次基础上修改为细粒度锁（锁住某一件商品而不是整个市场）
     *
     * @param lockName       锁名称
     * @param acquireTimeOut 超时时间，单位秒
     * @return 锁的UUID值
     */
    public Optional<String> acquireLock(String lockName, long acquireTimeOut) {
        String key = MessageFormat.format(LOCK_KEY, lockName);
        String value = UUID.randomUUID().toString();
        Duration duration = Duration.ofSeconds(acquireTimeOut);
        LocalDateTime endTime = LocalDateTime.now().plus(duration);
        while (LocalDateTime.now().isBefore(endTime)) {
            Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
            if (setIfAbsent != null && setIfAbsent) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * 带有超时限制特性的锁.
     * <p/>
     * 在取得锁之后，调用expire命令来为锁设置过期时间，使得redis可以自动删除超时的锁。为了确保锁在客户端已经崩溃
     * （客户端执行介于SETNX和EXPIRE之间的时候崩溃是最糟糕的）的情况下仍然能够自动被释放，客户端会在尝试获取锁失败之后，
     * 检查锁的过期时间，并为未设置过期时间的锁设置过期时间。
     * <p/>
     * 因为多个客户端在同一时间内设置的过期时间基本上是相同的，所以即使有多个客户端同时为同一个锁设置超时时间，
     * 锁的过期时间也不会产生太大变化。
     * <p/>
     * 这里还有个问题没解决：
     * <strong>如果线程A获取了锁，但到过期时间还未完成，而线程B获取了锁。此时，线程A和线程B都同时修改数据。</strong>
     *
     * @param lockName       锁名称
     * @param acquireTimeOut 超时时间，单位秒
     * @return 锁的UUID值
     */
    public Optional<String> acquireLockWithTimeOut(String lockName, long acquireTimeOut) {
        String key = MessageFormat.format(LOCK_KEY, lockName);
        String value = UUID.randomUUID().toString();
        Duration duration = Duration.ofSeconds(acquireTimeOut);
        LocalDateTime endTime = LocalDateTime.now().plus(duration);
        while (LocalDateTime.now().isBefore(endTime)) {
            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
            Boolean setIfAbsent = valueOperations.setIfAbsent(key, value, DEFAULT_LOCK_TIMEOUT, TimeUnit.SECONDS);
            if (setIfAbsent != null && setIfAbsent) {
                return Optional.of(value);
            }
            Long expireTimeout = stringRedisTemplate.getExpire(LOCK_KEY);
            if (expireTimeout != null && expireTimeout == -1) {
                stringRedisTemplate.expire(LOCK_KEY, DEFAULT_LOCK_TIMEOUT, TimeUnit.SECONDS);
            }
        }
        return Optional.empty();
    }

    /**
     * 首先使用WATCH命令监视代表锁的键，接着检查键目前的值是否和加锁时设置的值相同，并在确认值没有变化之后删除该键。
     * <p/>
     * 该函数包含的无限循环只会在少数情况下用到——函数之所以包含这个无限循环，主要是因为之后介绍的锁会支持超时限制特性，
     * 而如果用户不小心使用了两个版本的锁，可能会引起解锁事务失败，并导致上锁时间被不必要延长。
     *
     * @param lockName   锁名称
     * @param identifier 锁的UUID值
     * @return true表示释放锁成功
     */
    public boolean releaseLock(String lockName, String identifier) {
        String key = MessageFormat.format(LOCK_KEY, lockName);
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        for (; ; ) {
            stringRedisTemplate.watch(key);
            if (identifier.equals(valueOperations.get(key))) {
                stringRedisTemplate.multi();
                stringRedisTemplate.delete(key);
                List<Object> execs = stringRedisTemplate.exec();
                if (CollectionUtils.isEmpty(execs)) {
                    continue;
                }
                return true;
            }
            // 这里有必要调用unwatch吗？多次watch有危害吗？
            stringRedisTemplate.unwatch();
        }
    }

}
