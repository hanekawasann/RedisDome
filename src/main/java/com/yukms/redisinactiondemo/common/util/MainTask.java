package com.yukms.redisinactiondemo.common.util;

import java.util.concurrent.BrokenBarrierException;

/**
 * 主任务
 *
 * @author yukms 2019/1/15
 */
@FunctionalInterface
public interface MainTask {
    /**
     * 执行任务
     *
     * @param controller 任务控制器
     * @throws InterruptedException InterruptedException
     * @throws BrokenBarrierException BrokenBarrierException
     */
    void process(TaskController controller) throws InterruptedException, BrokenBarrierException;
}
