package com.yukms.redisinactiondemo.trading;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 市场交易
 *
 * @author yukms 2019/1/14
 */
@Service
public class TradingService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /** 用户hash */
    private static final String USERS = "users:";
    /** 用户hash中余额 */
    private static final String FUNDS = "funds";
    /** 库存set */
    private static final String INVENTORY = "inventory:";
    /** 市场zset */
    private static final String MARKET = "market:";

    /**
     * 在将一件商品放到市场上进行销售的时候，程序需要将被销售的商品添加到记录市场正在销售商品的有序集合里面，
     * 并且在添加操作执行的过程中，监视卖家的包裹以确保被销售的商品的确存在于卖家的包裹中。
     *
     * @param itemId   商品ID
     * @param sellerId 卖家ID
     * @param price    价格
     * @return 如果操作成功返回true
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
        // 如果被监视的包裹数据发生了变化，那么这里执行的命令为0条
        return !CollectionUtils.isEmpty(execs);
    }

    /**
     * 使用WATCH对市场以及买家的个人信息进行监视，然后获取买家拥有的钱数以及商品的售价，
     * 并检查买家是否有足够的钱来购买该商品。如果买家没有足够的钱，那么程序会取消事务；
     * 相反地，如果买家的钱足够，那么程序首先会将买家支付的钱转移给卖家，然后将售出的商品移动至买家的包裹，
     * 并将该商品从市场中移除。
     *
     * @param buyerId  买家ID
     * @param itemId   商品ID
     * @param sellerId 卖家ID
     * @param lprice   买家购买时的价格
     * @return 如果操作成功范围true
     */
    public boolean purchaseItem(String buyerId, String itemId, String sellerId, double lprice) {
        String buyerKey = USERS + buyerId;
        String sellerKey = USERS + sellerId;
        String marketMember = itemId + "." + sellerId;
        String buyerInventoryKey = INVENTORY + buyerId;
        stringRedisTemplate.watch(Arrays.asList(marketMember, buyerKey));
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Double price = zSetOperations.score(MARKET, marketMember);
        if (price == null) {
            // 商品被别人买下
            stringRedisTemplate.unwatch();
            return false;
        }
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String fundsObj = hashOperations.get(buyerKey, FUNDS);
        if (fundsObj == null) {
            // 买家没有钱
            stringRedisTemplate.unwatch();
            return false;
        }
        double funds = Double.valueOf(fundsObj);
        if (price != lprice || price > funds) {
            // 价格有变化 || 买家钱不够
            stringRedisTemplate.unwatch();
            return false;
        }
        stringRedisTemplate.multi();
        // 转移钱
        hashOperations.increment(sellerKey, FUNDS, price);
        hashOperations.increment(buyerKey, FUNDS, -price);
        // 转移商品
        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        setOperations.add(buyerInventoryKey, itemId);
        zSetOperations.remove(MARKET, marketMember);
        stringRedisTemplate.exec();
        return true;
    }
}
