package com.yukms.common.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.function.Consumer;

/**
 * 平均时间计时器
 *
 * @author yukms 2019/1/16
 */
public final class AverageTimer {

    /**
     * 运行指定次数
     *
     * @param name     名称
     * @param times    次数
     * @param consumer 执行的命令
     */
    public static void time(String name, int times, Consumer<Integer> consumer) {
        time(name, times, consumer, ChronoUnit.NANOS);
    }

    /**
     * 运行指定次数
     *
     * @param name     名称
     * @param times    次数
     * @param consumer 执行的命令
     * @param unit     单位
     */
    public static void time(String name, int times, Consumer<Integer> consumer, TemporalUnit unit) {
        Timer timer = Timer.start(name);
        for (int i = 1; i <= times; i++) {
            consumer.accept(i);
        }
        timer.end();
        Duration duration = timer.getDuration();
        System.out.println(
            "The average time of the " + name + " is " + duration.get(unit) / times + " " + unit.toString() + ".");
    }

}
