package com.juju.app.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：juju
 * 类描述：线程辅助类
 * 创建人：gm
 * 日期：2016/5/10 14:26
 * 版本：V1.0.0
 */
public class ThreadPoolUtil {

    private volatile static ThreadPoolUtil inst;

    ExecutorService imExecutorService;

//    /**
//     * 消息服务相关线程池
//     */
//    ThreadPoolExecutor messagePool = null;
//
//
//
//    /**
//     * 1:如果此时线程池中的数量小于corePoolSize，即使线程池中的线程都处于空闲状态，也要创建新的线程来处理被添加的任务。
//     * 2:如果此时线程池中的数量等于 corePoolSize，但是缓冲队列 workQueue未满，那么任务被放入缓冲队列。
//     * 3:如果此时线程池中的数量大于corePoolSize，缓冲队列workQueue满，并且线程池中的数量小于maximumPoolSize，建新的线程来处理被添加的任务
//     * 4:如果此时线程池中的数量大于corePoolSize，缓冲队列workQueue满，并且线程池中的数量等于maximumPoolSize，
//     * 那么通过 handler所指定的策略来处理此任务。也就是：处理任务的优先级为：核心线程corePoolSize、任务队列workQueue、
//     * 最大线程maximumPoolSize，如果三者都满了，使用handler处理被拒绝的任务。
//     *
//     */
//    private ThreadPoolUtil() {
//        messagePool = new ThreadPoolExecutor(1, 10, 5, TimeUnit.SECONDS,
//                //缓冲队列(有界队列)
//                new ArrayBlockingQueue<Runnable>(8),
//                //执行失败重复添加
//                new ThreadPoolExecutor.CallerRunsPolicy());
//    }

    private ThreadPoolUtil() {
        //使用系统提供的无界线程池，可以进行自动线程回收
        imExecutorService = Executors.newCachedThreadPool();
    }


    //双重验证+volatile（禁止JMM重排序）保证线程安全
    public static ThreadPoolUtil instance() {
        if(inst == null) {
            synchronized (ThreadPoolUtil.class) {
                if (inst == null) {
                    inst = new ThreadPoolUtil();
                }
            }
        }
        return inst;
    }

    //使用无界线程池执行IM相关任务
    public void executeImTask(Runnable runnable) {
        imExecutorService.execute(runnable);
    }



//    public void shutdownMessagePool() {
//        executorService.shutdown();
//    }

}
