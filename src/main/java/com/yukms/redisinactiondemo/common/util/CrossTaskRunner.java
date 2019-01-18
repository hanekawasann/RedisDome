package com.yukms.redisinactiondemo.common.util;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 交叉任务并执行器。
 *
 * @author yukms 2019/1/15
 */
public final class CrossTaskRunner {
    private static final int TASK_NUMBER = 2;

    private CrossTaskRunner() {
    }

    /**
     * 执行主任务与插入任务
     *
     * @param mainTask   主任务
     * @param insterTask 插入任务
     * @throws ExecutionException ExecutionException
     * @throws InterruptedException InterruptedException
     */
    public static void run(MainTask mainTask, InsterTask insterTask) throws ExecutionException, InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(TASK_NUMBER);
        TaskController controller = getController();
        Future<Boolean> mainFuture = service.submit(new Main(mainTask, controller));
        Future<Boolean> insterFuture = service.submit(new Insert(insterTask, controller));
        mainFuture.get();
        insterFuture.get();
        service.shutdown();
    }

    private static TaskController getController() {
        TaskController controller = new TaskController();
        controller.setStartCyclicBarrier(new CyclicBarrier(TASK_NUMBER));
        controller.setMainCyclicBarrier(new CyclicBarrier(TASK_NUMBER));
        controller.setInsertCyclicBarrier(new CyclicBarrier(TASK_NUMBER));
        return controller;
    }

    /**
     * 包含主任务执行的流程
     */
    private static class Main implements Callable<Boolean> {
        private MainTask task;
        private TaskController controller;

        Main(MainTask task, TaskController controller) {
            this.task = task;
            this.controller = controller;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                controller.start();
                task.process(controller);
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            return Boolean.TRUE;
        }
    }

    /**
     * 包含插入任务执行的流程
     */
    private static class Insert implements Callable<Boolean> {
        private InsterTask task;
        private TaskController controller;

        Insert(InsterTask task, TaskController controller) {
            this.task = task;
            this.controller = controller;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                controller.start();
                controller.awaitInsert();
                task.process();
                controller.notifyMain();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            return Boolean.TRUE;
        }
    }

}
