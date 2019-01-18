package com.yukms.redisinactiondemo.fakewebretailer;

import java.util.ArrayList;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.yukms.redisinactiondemo.common.util.SystemUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 即使是那些无法被整个缓存起来的页面——比如用户帐号页面、记录用户以往购买商品的页面等等，
 * 程序也可以通过缓存页面载入时所需的数据库行来减少载入页面所需的时间。
 * <p/>
 * 促销活动每天都会推出一些特价商品供用户抢购，所有特价商品的数量都是限定的，卖完即止。
 * 在这种情况下，网站是不能对整个促销页面进行缓存的，因为这可能会导致用户看到错误的特价商品数量，
 * 但是每次载入页面都从数据库里面取出特价商品的剩余数量的话，又会给数据库带来巨大的压力，
 * 并导致我们需要花费额外的成本来扩展数据库。
 * <p/>
 * 具体做法是编写一个持续运行的守护进程函数，让这个函数将指定的数据行缓存到redis里面，
 * 并不定期地对这些缓存进行更新。
 * <p/>
 * 程序使用了两个有序集合来记录应该在何时对缓存进行更新：
 * 第一个有序集合为调度有序集合，它的成员为数据行ID，
 * 而分值是一个时间戳，这个时间戳记录了应该在何时将指定的数据行缓存到Redis里面；
 * 第二个有序集合为延迟有序集合，它的成员也是数据行的行ID，
 * 而分值则记录了指定数据行的缓存需要每隔多少秒更新一次。
 * <p/>
 * 通过组合使用调度函数和持续运行缓存函数，我们实现了一种重复进行调度的自动缓存机制，
 * 并且可以随心所欲地控制数据行缓存的更新频率：如果数据行记录的是特价促销商品的剩余数量，
 * 并且参与促销活动的用户非常多的话，那么我们最好每隔几秒更新一次数据行缓存；
 * 另一方面，如果数据并不经常改变，或者商品缺货时可以接受的话，那么我们可以每分钟更新一次缓存。
 *
 * @author yukms 2019/1/10.
 */
@Service
public class DateCacheService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /** 延迟值 */
    private static final String DELAY = "delay:";
    /** 调度时间 */
    private static final String SCHEDULE = "schedule:";
    /** 缓存 */
    private static final String INV = "inv:";

    /**
     * 为了让缓存函数定期地缓存数据行，程序首先要将行ID和给定的延迟值添加到延迟有序集合里面，
     * 然后将行ID和当前时间的时间戳添加到调度有序集合里面。
     * <p/>
     * 实际执行缓存操作的函数需要用到数据行的延迟值，如果某个数据行的延迟值不存在，
     * 那么程序将取消对这个数据行的调度。如果我们想要移除某个数据行已有的缓存，
     * 并且让缓存数据不再缓存那个数据行，那么只需要把那个数据行的延迟值设置为小于或等于0。
     * </p>
     * <strong>这个函数应该在项目启动时和新增活动商品时调用，这个延迟/调度有序集合的数据
     * 应该也要持久化到数据库的吧</strong>
     *
     * @param rowId 行ID
     * @param delay 延迟值
     */
    public void scheduleRowCache(String rowId, long delay) {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        zSetOperations.add(DELAY, rowId, delay);
        // 立即对需要缓存的数据进行调度
        zSetOperations.add(SCHEDULE, rowId, SystemUtil.getNowTimetamp());
    }

    /**
     * 负责缓存数据行的函数会尝试读取调度有序集合的第一个元素以及该元素的分值，
     * 如果调度有序集合没有包含任何元素，或者分值存储的时间戳所制定的时间尚未来临，
     * 那么函数会先休眠50毫秒，然后再重新进行检查。
     * <p/>
     * 当缓存函数发现一个需要立即进行更新的数据行时，缓存函数会检查这个数据行的延迟值：
     * 如果数据行的延迟值小于或者等于0，那么缓存函数会从延迟有序集合和调度有序集合里面移除这个数据行的ID，
     * 并从缓存里面删除这个数据行以后的缓存数据，然后再重新进行检查；
     * 对于延迟大于0的数据行来说，缓存函数会从数据库里面取出这些行，将它们编码为JSON格式并存储到Redis里面，
     * 然后更新这些行的调度时间。
     */
    public void cacheRows() throws InterruptedException {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        for (; ; ) {
            Set<String> rowIds = zSetOperations.range(SCHEDULE, 0, 0);
            if (CollectionUtils.isEmpty(rowIds)) {
                Thread.sleep(50);
                continue;
            }
            String rowId = new ArrayList<>(rowIds).get(0);
            Double scheduleScore = zSetOperations.score(SCHEDULE, rowId);
            long now = SystemUtil.getNowTimetamp();
            if (null == scheduleScore || scheduleScore > now) {
                Thread.sleep(50);
                continue;
            }
            Double delayScore = zSetOperations.score(DELAY, rowId);
            if (null == delayScore || delayScore <= 0) {
                zSetOperations.remove(DELAY, rowId);
                zSetOperations.remove(SCHEDULE, rowId);
                stringRedisTemplate.delete(INV + rowId);
                continue;
            }
            Row row = queryRow(rowId);
            zSetOperations.add(SCHEDULE, rowId, now + delayScore);
            valueOperations.set(INV + rowId, JSON.toJSONString(row));
        }
    }

    public Row queryRow(String rowId) {
        return new Row(rowId);
    }

    public static class Row {
        private String rowId;

        public Row(String rowId) {
            this.rowId = rowId;
        }

        public String getRowId() {
            return rowId;
        }

        public void setRowId(String rowId) {
            this.rowId = rowId;
        }
    }
}
