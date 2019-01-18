package com.yukms.redisinactiondemo.rediscomponent;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 知道我们的网站在最近5分钟内获得了10000次点击，或者数据在最近无秒内处理了200次写入和600次读取，是非常有用的。
 * 通过在一段时间内持续地记录这些信息，我们可以注意到流量的骤增或渐增情况，预测合适需要对服务器进行升级，
 * 从而防止系统因为负荷超载而下线。
 * <p/>
 * 为了收集指标数据并进行监视和分析，我们将构建一个能够持续创建并维护计数器的工具，这个工具创建的每个计数器都有
 * 自己名字（名字里带有网站点击量、销量或者数据库查询字样的计数器都是比较重要的计数器）。这些计数器会以不同的
 * 时间精度（如1秒、5秒、1分钟等）存储最新的120个数据样本，用户也可以根据自己的需要，对取样的数量和精度进行修改。
 *
 * @author yukms 2019/1/18
 */
@Service
public class CounterService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /** 以秒为单位的计数器精度，分别为1秒、5秒、5分钟、1小时、5小时、1天——用户可以按需调整这些精度 */
    private static final long[] PRECISION = { 1, 5, 60, 300, 3600, 1800, 86400 };
    /** 散列的每个键都是一个时间片的开始时间，而键对应的值则存储了网站在该时间片之内获得的点击量 */
    private static final String COUNT_KEY = "count:{0}:{1}";
    /** 为了能够清理计数器包含的旧数据，我们需要在使用计数器的同时，对被使用的计数器进行记录 */
    private static final String KNOWN_KEY = "known:";
    private static final String KNOWN_MEMBER = "{0}:{1}";
    private static final int SIMPLE_COUNT = 120;


    /**
     * @param countName 计数器名（点击量、销量或数据库查询字样等）
     * @param count     数量
     */
    public void updateCounter(String countName, long count) {
        // 通过取得当前时间来判断应该对哪个时间片执行自增操作
        Instant now = Instant.now();
        // 为我们记录的每种精度都创建一个计数器
        Arrays.stream(PRECISION).forEach(precision -> {
            // 取得当前时间片的开始时间
            Instant pnow = now.minusSeconds(precision);
            // 创建负责存储技术信息的散列
            String knownMember = MessageFormat.format(KNOWN_MEMBER, precision, countName);
            // 将计数器的引用信息添加到有序集合里面，并将其分值设为0，以便在之后执行清理操作
            stringRedisTemplate.opsForZSet().add(KNOWN_KEY, knownMember, 0);
            String countKey = MessageFormat.format(COUNT_KEY, precision, countName);
            // 对给定名字和精度的计数器进行更新
            stringRedisTemplate.opsForHash().increment(countKey, pnow.getEpochSecond(), count);
        });
    }

    /**
     * 程序首先使用使用HGETALL命令来获取整个散列，接着将命令返回的时间片和计数器的值从原来的字符串格式转换为数字格式，
     * 根据时间进行排序，最后返回排序后的数据。
     *
     * @param countName 计数器名（点击量、销量或数据库查询字样等）
     * @param precision 精度
     * @return 计数
     */
    public Map<Long, Long> getCounter(String countName, long precision) {
        String countKey = MessageFormat.format(COUNT_KEY, precision, countName);
        Map<String, String> counts = stringRedisTemplate.<String, String> opsForHash().entries(countKey);
        // 对数据进行排序，把旧的数据样本排在前面
        Map<Long, Long> result = new TreeMap<>();
        for (Map.Entry<String, String> count : counts.entrySet()) {
            Long key = Long.valueOf(count.getKey());
            Long value = Long.valueOf(count.getValue());
            result.put(key, value);
        }
        return result;
    }

    /**
     * 在处理和清理旧计数器的时候，有几件事是需要我们格外留心的，其中包括一下几件：
     * <ul>
     * <li>任何时候都可能有新的计数器被添加进来</li>
     * <li>同一时间可能会有多个不同的清理操作在执行</li>
     * <li>对于一个每天值更新一次的计数器来说，以每一分钟一次的频率尝试清理这个计数器只会浪费计算资源</li>
     * <li>如果一个计数器不包含任何数据，那么程序就不应该尝试对它进行清理</li>
     * </ul>
     * 清理程序通过对记录已知计数器的有序集合执行ZRANGE命令来一个接一个的遍历所有已知的计数器。
     * 在对计数器执行清理操作的时候，程序会取出计数器记录的所有计数样本的开始时间，
     * 并移除那些开始时间位于指定截止时间之前的样本，清理之后的计数器最多只会保留最新的120个样本。
     * 如果一个计数器在执行清理擦左之后不再包含任何样本，那么程序将从记录已知计数器的有序集合里面移除这个计数器的引用信息。
     * <p/>
     * 对于每秒更新一次或者每5秒更新一次的计数器，将以每分钟一次的频率清理这些计数器；
     * 而对于每5分钟更新一次的计数器，将以每5分钟一次的频率清理这些计数器。
     */
    public void cleanCounters() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        // 渐进遍历所有已知计数器
        for (long i = 0; zSetOperations.size(KNOWN_KEY) != null && i < zSetOperations.size(KNOWN_KEY); i++) {
            // 取得被检查的计数器的数据
            Set<String> members = zSetOperations.range(KNOWN_KEY, i, i);
            if (CollectionUtils.isEmpty(members)) {
                break;
            }
            String member = new ArrayList<>(members).get(0);
            String[] memberSp = member.split(":");
            // 取得计数器的精度
            long precision = Long.valueOf(memberSp[0]);
            /*
             * 因为清理程序每60秒就会循环一次，所以这里需要根据计数器的更新频率来判断是否真的有必要对计数器进行清理。
             * 如果这个计数器在这次循环里不需要进行清理，那么检查下一个计数器。（举个例子，如果清理程序只循环了3次，
             * 而计数器的更新频率为每5分钟一次，那么程序暂时还不需要对这个计数器进行清理。）
             */
            // 这里会对对是否应该进行清理做判断...
            String countKey = MessageFormat.format(COUNT_KEY, memberSp[0], memberSp[1]);
            HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
            Set<String> countHashKeys = new TreeSet<>(hashOperations.keys(countKey));
            // ....例子缺少上下文，下不下去了
        }
    }
}
