package com.yukms.redisinactiondemo.fakewebretailer;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

/**
 * 对于每天很少改变的页面，这些页面的内容实际上并不需要动态地生成，
 * 而我们的工作就是想办法不再生成这些页面。减少网站在动态生成内容上面所花时间，
 * 可以降低网站处理相同负载所需的服务器数量，并让网站的速度变得更快。
 * <p/>
 * 商品的数量太多，贸然地缓存所有商品页面将耗尽整个网站的内存，所以只对其中10000件商品进行缓存。
 *
 * @author yukms 2019/1/10.
 */
@Service
public class PageCacheService {
    @Autowired
    private RedisTemplate redisTemplate;
    /** 页面缓存 */
    private static final String CACHE = "cache:";

    /**
     * 对于一个不能被缓存的请求，函数将直接生成并返回页面；而对于可以被缓存的请求，
     * 函数首先会尝试从缓存里面取出并返回被缓存的页面，如果缓存页面不存在，
     * 那么函数会生成页面并将其缓存在Redis里面5分钟，最后再将页面返回给函数调用者。
     *
     * @param request http请求
     * @param callback 真实的请求处理逻辑
     * @return 页面
     */
    public Page cacheRequest(HttpServletRequest request, Function<HttpServletRequest, Page> callback) {
        if (canNotCache(request)) {
            return callback.apply(request);
        }
        String pageKey = CACHE + hashRequest(request);
        ValueOperations<String, Page> valueOperations = redisTemplate.opsForValue();
        Page page = valueOperations.get(pageKey);
        if (null == page) {
            page = callback.apply(request);
            valueOperations.set(pageKey, page, 5L, TimeUnit.MINUTES);
        }
        return page;
    }

    private String hashRequest(HttpServletRequest request) {
        return null;
    }

    private boolean canNotCache(HttpServletRequest request) {
        return false;
    }

    public static class Page implements Serializable {
        private static final long serialVersionUID = 2089275542828353815L;
    }
}
