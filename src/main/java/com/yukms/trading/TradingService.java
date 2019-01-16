package com.yukms.trading;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author yukms 2019/1/14
 */
@Service
public class TradingService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /** 用户hash */
    private static final String USERS = "users:";
    /** 库存set */
    private static final String INVENTORY = "inventory:";
    /** 市场zset */
    private static final String MARKET = "market:";

    /**
     * 在将一件商品放到市场上进行销售的时候，程序需要将被销售的商品添加到记录市场正在销售商品的有序集合里面，
     * 并且在添加操作执行的过程中，监视卖家的包裹以确保被销售的商品的确存在于卖家的包裹中。
     *
     * @param itemId 商品ID
     * @param sellerId 卖家ID
     * @param price 价格
     */
    public boolean listItem(String itemId, String sellerId, long price) {
        String inventoryKey = INVENTORY + sellerId;
        String marketMember = itemId + "." + sellerId;
        // 监视用户包裹发生变化
        stringRedisTemplate.watch(inventoryKey);
        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        // 检查用户是否仍然持有将要被销售的商品
        Boolean isMember = setOperations.isMember(inventoryKey, itemId);
        if (isMember == null || !isMember) {
            // 如果指定的商品不在用户的包裹里面，那么停止对包括键的监视
            stringRedisTemplate.unwatch();
            return false;
        }
        // 把被销售的商品添加到商品买卖市场里面
        stringRedisTemplate.multi();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        // 添加到市场
        zSetOperations.add(MARKET, marketMember, price);
        // 从库存移除
        setOperations.remove(inventoryKey, itemId);
        List<Object> execs = stringRedisTemplate.exec();
        return CollectionUtils.isEmpty(execs);
    }
}
