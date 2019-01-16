package com.yukms.common.util;

/**
 * 插入任务
 *
 * @author yukms 2019/1/15
 */
@FunctionalInterface
public interface InsterTask {
    /**
     * 执行任务
     */
    void process();
}
