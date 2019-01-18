package com.yukms.redisinactiondemo.rediscomponent;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

/**
 * 日志服务
 *
 * @author yukms 2019/1/17
 */
@Service
public class LogServer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String RECENT_KEY = "recent:{0}:{1}";
    private static final String COMMON_KEY = "common:{0}:{1}";
    private static final String COMMON_KEY_LAST = "common:{0}:{1}:last";
    private static final String COMMON_KEY_START = "common:{0}:{1}:start";
    private static final String COMMON_KEY_PSTART = "common:{0}:{1}:pstart";

    /**
     * 维持一个包含最新日志的列表
     *
     * @param name    日志名
     * @param message 日志信息
     * @param level   日志等级
     */
    public void logRecent(String name, String message, LogLevel level) {
        String logKey = MessageFormat.format(RECENT_KEY, name, level.getName());
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        listOperations.leftPush(logKey, dateTime + " " + message);
        // 对日志列表进行修剪，让它只包含最新的100条消息
        listOperations.trim(logKey, 0, 99);
    }

    /**
     * 将消息作为成员存储到有序集合里面，并将消息出现的频率设置为成员的分值。
     * 为了确保我们看见的常见消息都是最新的，程序会以每小时一次的频率对消息进行轮换，
     * 并在轮换日志的时候保留上一个小时记录的最新消息。
     *
     * @param name    日志名
     * @param message 日志信息
     * @param level   日志等级
     * @return ture表示添加成功
     */
    public boolean logCommon(String name, String message, LogLevel level) {
        String levelName = level.getName();
        String commonKey = MessageFormat.format(COMMON_KEY, name, levelName);
        // 因为程序每小时需要轮换一次日志，所以它使用了一个键来记录当前所处的小时数
        String startKey = MessageFormat.format(COMMON_KEY_START, name, levelName);
        // 对当前小时数的键进行监视，确保轮换操作可以正确地执行
        stringRedisTemplate.watch(startKey);
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        stringRedisTemplate.multi();
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String startTimeStr = valueOperations.get(startKey);
        if (StringUtils.isBlank(startTimeStr)) {
            // 第一次启动没有上一次的时间
            valueOperations.set(startKey, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            LocalDateTime previous = LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(startTimeStr));
            if (isOneHourApart(previous, now)) {
                // 如果消息列表记录的是上一个小时的日志，那么将这些旧的常见日志消息归档
                stringRedisTemplate.rename(commonKey, MessageFormat.format(COMMON_KEY_LAST, name, levelName));
                stringRedisTemplate.rename(startKey, MessageFormat.format(COMMON_KEY_PSTART, name, levelName));
                // 更新当前所出的小时数
                valueOperations.set(startKey, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        // 对记录日志出现次数的计数器执行自增操作（其实也是记录常用日志）
        stringRedisTemplate.opsForZSet().incrementScore(commonKey, message, 1L);
        logRecent(name, message, level);
        return stringRedisTemplate.exec().isEmpty();
    }

    private boolean isOneHourApart(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).get(ChronoUnit.HOURS) >= 1L;
    }
}
