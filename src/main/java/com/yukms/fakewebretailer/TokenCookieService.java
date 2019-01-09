package com.yukms.fakewebretailer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.yukms.base.BaseService;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

/**
 * 令牌Cookie
 * <p>
 * 将会话和购物车都存储到Redis里面，这种做法不仅可以减少请求的体积，
 * 还使得我们可以根据用户浏览过的商品、用户放入购物车的商品以及用户最终购买的商品进行统计计算，
 * 构建起很多大型网络零售商都在提供的“在查看过这件商品的的用户当中，有X%的用户最终购买了这件商品”、
 * “购买了这件商品的用户也购买了某某其他商品”等功能，这些功能可以帮助用户查找其他相关的商品，
 * 并最终提升网站的销售业绩。
 *
 * @author yukms 2019/1/8.
 */
@Service
@ConfigurationProperties("fake.web.retailer")
public class TokenCookieService extends BaseService {
    /** 用户hash */
    private static final String LOGIN = "login:";
    /** 最后浏览页面记录zset */
    private static final String RECENT = "recent:";
    /** 浏览过的商品zset */
    private static final String VIEWED = "viewed:";
    /** 购物车 */
    private static final String CART = "cart:";
    /** 浏览页面记录最大条数 */
    private int maxRecent;

    public void setMaxRecent(int maxRecent) {
        this.maxRecent = maxRecent;
    }

    /**
     * 检查用户是否已经登录，需要根据给定的令牌来查找与之对应的用户，
     * 并在用户已经登录的情况下，返回该用户的ID
     *
     * @param token 令牌
     * @return 用户ID
     */
    public String checkToken(String token) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.get(LOGIN, token);
    }

    /**
     * 用户每次浏览页面的时候，程序都会对用户存储在登录散列里面的信息进行更新，
     * 并将用户的令牌和当前时间戳添加到最近登录用户的有序集合里面。
     * 如果用户正在浏览的是一个商品页面，那么程序还会将这个商品添加到
     * 记录这个用户最近浏览过的商品的有序集合里面，并在被记录商品的数量超过25个时，对这个有序集合进行修剪。
     *
     * @param token  令牌
     * @param userId 用户ID
     * @param itemId 商品ID
     */
    public void updateToken(String token, String userId, String itemId) {
        long timetamp = getNowTimetamp();
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.put(LOGIN, token, userId);
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.add(RECENT, token, timetamp);
        if (StringUtils.isNotEmpty(itemId)) {
            zSetOperations.add(VIEWED + token, itemId, timetamp);
            // 注意
            zSetOperations.removeRange(VIEWED + token, 0, -26);
        }
    }

    /**
     * 每个用户的购物车都是一个散列，这个散列存储了商品ID与商品订购数量之间的映射。
     * 在商品数量发生变化时，对购物车进行更新：
     * 如果用户订购某件商品的数量大于0，那么程序会将这件商品的ID以及用户订购的数量添加到散列里面,
     * 如果用户购买的商品已近存在与散列里面，那么新的订购数量将会覆盖已有的订购数量；
     * 相反地，如果用户订购某件商品的数量不大于0，那么程序将从散列里面移除该条目。
     *
     * @param token  令牌
     * @param itemId 商品ID
     * @param count  数量
     */
    public void addToCart(String token, String itemId, int count) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        String cartKey = CART + token;
        if (count <= 0) {
            hashOperations.delete(cartKey, itemId);
        } else {
            hashOperations.put(cartKey, itemId, String.valueOf(count));
        }
    }

    /**
     * 检查存储最近登录令牌的有序集合的大小，如果有序集合的大小超过的限制，
     * 那么程序就会从有序集合里面移除最多100个最旧的令牌，并从记录用户登录信息的散列里面，
     * 移除被删除令牌对应的用户的信息，并对存储了这些用户浏览商品记录的有序集合进行清理。
     * 除此之外，还要删除旧会话对应用户的购物车。
     */
    public void cheanSessions() {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        Long size = zSetOperations.size(RECENT);
        if (null != size && size > maxRecent) {
            long end = Math.min(size - maxRecent, 100);
            Optional.ofNullable(zSetOperations.range(RECENT, 0, end - 1))//
                .ifPresent(tokens -> tokens.forEach(token -> {
                    redisTemplate.delete(VIEWED + token);
                    redisTemplate.delete(CART + token);
                    redisTemplate.opsForHash().delete(LOGIN, token);
                    zSetOperations.remove(RECENT, token);
                }));
        }
    }

    private long getNowTimetamp() {
        Instant instant = Instant.now();
        ZoneId systemZoneId = ZoneId.systemDefault();
        return LocalDateTime.now().toEpochSecond(systemZoneId.getRules().getOffset(instant));
    }
}
