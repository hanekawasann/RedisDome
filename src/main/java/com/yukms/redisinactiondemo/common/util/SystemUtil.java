package com.yukms.redisinactiondemo.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author yukms 2019/1/10.
 */
public final class SystemUtil {
    private SystemUtil() {
    }

    public static long getNowTimetamp() {
        Instant instant = Instant.now();
        ZoneId systemZoneId = ZoneId.systemDefault();
        return LocalDateTime.now().toEpochSecond(systemZoneId.getRules().getOffset(instant));
    }
}
