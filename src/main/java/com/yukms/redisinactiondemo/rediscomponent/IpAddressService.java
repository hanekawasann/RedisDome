package com.yukms.redisinactiondemo.rediscomponent;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 实现IP所属地查找程序会用到两个查找表：
 * <ui>
 *     <li>根据输入的IP地址来查找IP所属城市的ID</li>
 *     <li>根据输入的城市ID来查找ID对应城市的实际信息（城市所在地区和国家相关信息）</li>
 * </ui>
 *
 * @author yukms 2019/1/18
 */
@Service
public class IpAddressService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public long ipToScore(String ipAddress) {
        Objects.requireNonNull(ipAddress);
        long score = 0;
        for (String str : ipAddress.split(".")) {
            score = score * 256 + Integer.valueOf(str);
        }
        return score;
    }
}
