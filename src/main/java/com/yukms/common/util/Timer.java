package com.yukms.common.util;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

/**
 * 计时器。实现依赖于{@link LocalDateTime}和{@link Duration}
 *
 * @author yukms 2019/1/16
 */
public class Timer implements Comparable<Timer> {
    private static final String COMPARE_FASTER = "The {0} timer is {1} faster than {2} timer.";
    private static final String COMPARE_AS_FASTER_AS = "The {0} timer is as fast as {1} timer.";
    private static final TemporalUnit DEFAULT_UNIT = ChronoUnit.NANOS;
    private String name;
    private LocalDateTime start;
    private LocalDateTime end;
    private Duration duration;

    private Timer(String name, LocalDateTime start) {
        this.name = name;
        this.start = start;
    }

    /**
     * 创建计时器并开始计时
     *
     * @param name 计时器名称
     * @return 计时器
     */
    public static Timer start(String name) {
        return new Timer(name, LocalDateTime.now());
    }

    /**
     * 结束计时
     */
    public void end() {
        end = LocalDateTime.now();
        duration = Duration.between(start, end);
    }

    /**
     * 重新开始计时
     */
    public void reStart() {
        start = LocalDateTime.now();
        end = null;
    }

    /**
     * 获取耗时
     *
     * @return 耗时数
     */
    public long getIntervals() {
        checkState();
        return duration.get(DEFAULT_UNIT);
    }

    /**
     * 获取耗时
     *
     * @param unit 单位
     * @return 耗时数
     */
    public long getIntervals(TemporalUnit unit) {
        checkState();
        return duration.get(unit);
    }

    /**
     * 打印计时器
     */
    public void print() {
        print(DEFAULT_UNIT);
    }

    /**
     * 打印计时器
     *
     * @param unit 单位
     */
    public void print(TemporalUnit unit) {
        System.out.println("The " + name + " timer spend " + getIntervals(unit) + " " + unit.toString() + ".");
    }

    /**
     * 比较计时器
     *
     * @param o 计时器
     * @return java.time.Duration#compareTo(java.time.Duration)
     */
    @Override
    public int compareTo(Timer o) {
        checkState();
        o.checkState();
        return duration.compareTo(o.duration);
    }

    /**
     * 打印计时器比较结果
     *
     * @param o 计时器
     */
    public void printlnCompare(Timer o) {
        printlnCompare(o, DEFAULT_UNIT);
    }

    /**
     * 打印计时器比较结果
     *
     * @param o    计时器
     * @param unit 单位
     */
    public void printlnCompare(Timer o, TemporalUnit unit) {
        int i = this.compareTo(o);
        if (i == 0) {
            System.out.println(MessageFormat.format(COMPARE_AS_FASTER_AS, name, o.name));
        } else {
            System.out.println(
                i > 0 ? MessageFormat.format(COMPARE_FASTER, o.name, duration.minus(o.duration).get(unit), name)
                    : MessageFormat.format(COMPARE_FASTER, name, o.duration.minus(duration).get(unit), o.name));
        }
    }

    public Duration getDuration() {
        checkState();
        return duration;
    }

    private void checkState() {
        Objects.requireNonNull(end, "The " + name + " timer is not over.");
    }
}
