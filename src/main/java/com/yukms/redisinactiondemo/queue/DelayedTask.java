package com.yukms.redisinactiondemo.queue;

import org.springframework.stereotype.Component;

/**
 * 延迟任务。
 * <p/>
 * 让玩家可以在未来某个时候才开始销售自己的商品，而不是立即就开始销售：
 * 把所有需要在未来执行的任务都添加到有序集合里面，并将任务的执行时间设置为分值，
 * 另外再使用一个进程来查找有序集合里面是否存在可以立即执行的任务，如果有的话，
 * 就从有序集合里面移除那个任务，并将它添加到适当的任务队列里面。
 *
 * @author yukms 2019/1/23
 */
@Component
public class DelayedTask {

}
