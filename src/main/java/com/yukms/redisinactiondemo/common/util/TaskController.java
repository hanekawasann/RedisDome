package com.yukms.redisinactiondemo.common.util;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * 任务控制器，用于控制任务执行的流程。
 *
 * @author yukms 2019/1/15
 */
public class TaskController {
    /** 控制主任务与启动任务同时运行 */
    private CyclicBarrier startCyclicBarrier;
    /** 用于主任务 */
    private CyclicBarrier mainCyclicBarrier;
    /** 用于插入任务 */
    private CyclicBarrier insertCyclicBarrier;

    /**
     * 唤醒插入任务并且使主任务等待。
     * <p/>
     * 注意该方法只能被执行一次，执行多次将导致线程阻塞。
     *
     * @throws BrokenBarrierException BrokenBarrierException
     * @throws InterruptedException InterruptedException
     */
    public void notifyInsertAndAwaitMain() throws BrokenBarrierException, InterruptedException {
        insertCyclicBarrier.await();
        mainCyclicBarrier.await();
    }

    /**
     * 使主任务与插入任务同时运行
     *
     * @throws BrokenBarrierException BrokenBarrierException
     * @throws InterruptedException InterruptedException
     */
    void start() throws BrokenBarrierException, InterruptedException {
        startCyclicBarrier.await();
    }

    /**
     * 唤醒主任务
     *
     * @throws BrokenBarrierException BrokenBarrierException
     * @throws InterruptedException InterruptedException
     */
    void notifyMain() throws BrokenBarrierException, InterruptedException {
        mainCyclicBarrier.await();
    }

    /**
     * 使插入任务等待
     *
     * @throws BrokenBarrierException BrokenBarrierException
     * @throws InterruptedException InterruptedException
     */
    void awaitInsert() throws BrokenBarrierException, InterruptedException {
        insertCyclicBarrier.await();
    }

    CyclicBarrier getStartCyclicBarrier() {
        return startCyclicBarrier;
    }

    void setStartCyclicBarrier(CyclicBarrier startCyclicBarrier) {
        this.startCyclicBarrier = startCyclicBarrier;
    }

    CyclicBarrier getMainCyclicBarrier() {
        return mainCyclicBarrier;
    }

    void setMainCyclicBarrier(CyclicBarrier mainCyclicBarrier) {
        this.mainCyclicBarrier = mainCyclicBarrier;
    }

    CyclicBarrier getInsertCyclicBarrier() {
        return insertCyclicBarrier;
    }

    void setInsertCyclicBarrier(CyclicBarrier insertCyclicBarrier) {
        this.insertCyclicBarrier = insertCyclicBarrier;
    }

}
