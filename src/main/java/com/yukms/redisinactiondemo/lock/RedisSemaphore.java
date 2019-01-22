package com.yukms.redisinactiondemo.lock;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

/**
 * 信号量
 * <p/>
 * 计数信号量和其他锁的区别在于，当客户端获取锁失败的时候，客户端通常会选择等待；
 * 而当客户端获取计数信号量失败的时候，客户端通常会选择立即返回失败结果。
 * <p/>
 * 基本信号量存在一些问题：
 * 它在获取信号量的是否会假设每个进程访问到的系统时间都是相同的，而这一假设在多主机环境下可能并不成立。
 * 对于系统A和B来说，如果A的系统时间要比B的系统时间快10毫秒，那么当A取得了最后一个信号量的时候，
 * B只需要在10毫秒内尝试获取信号量，就可以在A不知情的情况下，“偷走”A已经取得的信号量。
 * <p/>
 * 每当锁或者信号量因为系统时钟的细微不同而导致锁的获取结果出现剧烈的变化时，这个锁或者信号量就是不公平的。
 * 不公平的锁或信号量可能导致客户端永远也无法取得它原本得到的锁或信号量。
 *
 * @author yukms 2019/1/21
 */
@Component
public class RedisSemaphore {

    @Autowired
    private RedisLock redisLock;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String SEMAPHORE_KEY = "semaphore:{0}";
    private static final String SEMAPHORE_OWNER_KEY = "semaphore:{0}:owner";
    private static final String SEMAPHORE_COUNTER_KEY = "semaphore:{0}:counter";

    /**
     * 进程在尝试获取信号量时会生成一个标识符，并使用当前时间戳作为分值，将标识符添加到有序集合里面。
     * 接着进程会检查自己的标识符在有序集合中的排名。如果排名低于可获取的信号量总数（成员排名从0开始计算），
     * 那么表示进程成功地取得了信号量。反之，则表示进程未能取得信号量，它必须从有序集合里面移除自己的标识符。
     * 为了处理过期的信号量，程序在将标识符添加到有序集合之前，会先清理有序集合中所有时间戳大于超时数值的标识符。
     *
     * @param semName        信号量名称
     * @param limit          信号量限制
     * @param timeoutSeconds 过期时间
     * @return 信号量的UUID标识符
     */
    public Optional<String> acquireSemaphore(String semName, long limit, long timeoutSeconds) {
        String semaphoreKey = MessageFormat.format(SEMAPHORE_KEY, semName);
        String identifier = UUID.randomUUID().toString();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        // 清理过期的信号量持有者
        LocalDateTime now = LocalDateTime.now();
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        long maxScore = now.minus(Duration.ofSeconds(timeoutSeconds)).toEpochSecond(zoneOffset);
        zSetOperations.removeRangeByScore(semaphoreKey, 0, maxScore);
        zSetOperations.add(semaphoreKey, identifier, now.toEpochSecond(zoneOffset));
        // 检查是否成功取得了信号量
        Long rank = zSetOperations.rank(semaphoreKey, identifier);
        if (rank != null && rank < limit) {
            return Optional.of(identifier);
        }
        // 获取信号量失败，删除之前添加的标识符
        zSetOperations.remove(semaphoreKey, identifier);
        return Optional.empty();
    }

    /**
     * 释放信号量
     *
     * @param semName    信号量名
     * @param identifier 信号量标识符
     * @return 释放成功返回true
     */
    public boolean releaseSemphore(String semName, String identifier) {
        String semaphoreKey = MessageFormat.format(SEMAPHORE_KEY, semName);
        Long remove = stringRedisTemplate.opsForZSet().remove(semaphoreKey, identifier);
        // r如果信号量已经被正确地释放，那么返回1；如果信号量已经因为过期而被删除，那么返回0
        return remove != null && remove == 1;
    }

    /**
     * 为了尽可能地减少系统时间不一致带来的问题，我们需要给信号量实现添加一个计数器以及一个有序集合。
     * 其中，计数器通过持续地执行自增操作，创建出一种类似于计时器的机制，
     * 确保最先对计数器执行自增操作的客户端能够获得信号量。另外，为了满足“最先对计数器执行
     * 自增操作的客户端能够获得信号量”这一要求，程序会将计数器生成的值用作分值，存储到一个“信号量拥有者”有序集合里面，
     * 然后通过检查客户端生成的标识符在有序集合里面的排名来判断客户是否取得了信号量。
     * <p/>
     * 程序首先从超时有序集合里面移除过期元素的方式来移除超时的信号量，接着对超时有序集合和信号量拥有者有序集合执行交集计算，
     * 并将计算结果保存到信号量拥有者有序集合里面，覆盖有序集合中原有的数据。之后，程序会对计数器执行自增操作，
     * 并将计数器生成的值添加到信号量拥有者有序集合里面；与此同时，程序还会将当前的系统时间添加到超时有序集合里面。
     * 在完成以上操作之后，程序会检查当前客户端添加的标识符在信号量拥有者有序集合中的排名是否足够低，
     * 如果是的话就表示客户端成功取得了信号量。相反地，如果客户端未能取得信号量，那么程序将从信号量拥有者有序集合以及超时
     * 有序集合里面移除与该客户端相关的元素。
     *
     * @param semName        信号量名称
     * @param limit          信号量限制
     * @param timeoutSeconds 过期时间
     * @return 信号量的UUID标识符
     */
    public Optional<String> acquireFairSemaphore(String semName, long limit, long timeoutSeconds) {
        String identifier = UUID.randomUUID().toString();
        String semaphoreKey = MessageFormat.format(SEMAPHORE_KEY, semName);
        String semaphoreOwnerKey = MessageFormat.format(SEMAPHORE_OWNER_KEY, semName);
        String semaphoreCounterkey = MessageFormat.format(SEMAPHORE_COUNTER_KEY, semName);

        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        // 清理过期的信号量持有者
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        LocalDateTime now = LocalDateTime.now();
        long maxScore = now.minus(Duration.ofSeconds(timeoutSeconds)).toEpochSecond(zoneOffset);
        zSetOperations.removeRangeByScore(semaphoreKey, 0, maxScore);
        zSetOperations.intersectAndStore(semaphoreKey, Collections.emptyList(), semaphoreOwnerKey,
            RedisZSetCommands.Aggregate.SUM, RedisZSetCommands.Weights.of(0, 1));
        // 对计数器执行自增操作，并获取计数器在执行自增操作之后的值
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        Long counter = valueOperations.increment(semaphoreCounterkey);
        Objects.requireNonNull(counter);
        // 尝试获取信号量
        zSetOperations.add(semaphoreKey, identifier, now.toEpochSecond(zoneOffset));
        zSetOperations.add(semaphoreOwnerKey, identifier, counter);
        // 检查是否成功取得了信号量，注意这里是以semaphoreOwnerKey获取排名
        Long rank = zSetOperations.rank(semaphoreOwnerKey, identifier);
        if (rank != null && rank < limit) {
            // 客户端成功获取了信号量
            return Optional.of(identifier);
        }
        // 客户端未成功获取了信号量
        zSetOperations.remove(semaphoreKey, identifier);
        zSetOperations.remove(semaphoreOwnerKey, identifier);
        return Optional.empty();
    }

    /**
     * 公平信号量的释放操作需要同时从信号量拥有者有序集合以及超时有序集合里面删除当前客户端的标识符。
     * <p/>
     * 只从超时有序集合里面移除标识符可能会引发这样一个问题：
     * 当一个客户端执行acquireFairSemaphore函数，对信号量拥有者有序集合进行了更新，并正准备将自己的标识符添加到超时有序
     * 集合和信号量拥有者有序集合之际，如果有另一个客户端执行信号量释放函数，并将该客户端自己的标识符从超时有序集合中移除的话，
     * 这将导致原本能够成功执行的信号量获取操作变为执行失败。
     *
     * @param semName    信号量名
     * @param identifier 信号量标识符
     * @return 释放成功返回true
     */
    public boolean releaseFairSemphore(String semName, String identifier) {
        String semaphoreKey = MessageFormat.format(SEMAPHORE_KEY, semName);
        String semaphoreOwnerKey = MessageFormat.format(SEMAPHORE_OWNER_KEY, semName);
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Long remove = zSetOperations.remove(semaphoreKey, identifier);
        zSetOperations.remove(semaphoreOwnerKey, identifier);
        // r如果信号量已经被正确地释放，那么返回1；如果信号量已经因为过期而被删除，那么返回0
        return remove != null && remove == 1;
    }

    /**
     * 只要客户端持有的信号量没有因为过期而被删除，该函数就可以对信号量的超时时间进行刷新。
     * 另一方面，如果客户端持有的信号量已经因为超时而被删除，那么函数将释放信号量，并将信号量已经丢失的信息告知调用者。
     * 在长时间使用信号量的时候，我们必须以足够频繁的频率对信号量进行刷新，防止它因为过期而丢失。
     *
     * @param semName    信号量名
     * @param identifier 信号量标识符
     * @return 释放成功返回true
     */
    public boolean refreshFairSemphore(String semName, String identifier) {
        String semaphoreKey = MessageFormat.format(SEMAPHORE_KEY, semName);
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        LocalDateTime now = LocalDateTime.now();
        Boolean add = zSetOperations.add(semaphoreKey, identifier, now.toEpochSecond(zoneOffset));
        return !(add != null && add);
    }

    /**
     * 当两个线程A和B都在尝试获取剩余的一个信号量时，即使A首先对计数器执行了自增操作，但只要B能够抢先将自己的标识符添加到
     * 有序集合里，并检查标识符在有序集合中的排名，那么B就可以成功地取得信号量。之后当A也将自己的标识符添加到有序集合里，
     * 并检查标识符在有序集合中的排名时，A将“偷走”B已经取得的信号量，而B只有在尝试释放信号或者尝试刷新信号量的时候
     * 才会察觉这一点。
     *
     * @param semName        信号量名称
     * @param limit          信号量限制
     * @param timeoutSeconds 过期时间
     * @return 信号量的UUID标识符
     */
    public Optional<String> acquireFairSemaphoreWithLock(String semName, long limit, long timeoutSeconds) {
        Optional<String> identifierOp = redisLock.acquireLock(semName, 1);
        try {
            if (identifierOp.isPresent()) {
                return acquireFairSemaphore(semName, limit, timeoutSeconds);
            }
        } finally {
            identifierOp.ifPresent(identifier -> redisLock.releaseLock(semName, identifier));
        }
        return identifierOp;
    }

}
