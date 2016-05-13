package com.juju.app.utils;

import java.util.concurrent.ArrayBlockingQueue;
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

    private ThreadPoolUtil() {
        messagePool = new ThreadPoolExecutor(1, 10, 5, TimeUnit.SECONDS,
                //缓冲队列
                new ArrayBlockingQueue<Runnable>(8),
                //重复添加
                new ThreadPoolExecutor.CallerRunsPolicy());

//        messagePool.setThreadFactory(new ThreadFactory() {
//            @Override
//            public Thread newThread(Runnable r) {
//                ((Thread) r).setDaemon(true);
//                return (Thread) r;
//            }
//        });
    }

    /**
     * 消息服务相关线程池
     */
    ThreadPoolExecutor messagePool = null;



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

    public void addMessageTask(Runnable runnable) {
        messagePool.submit(runnable);
    }

    public void shutdownMessagePool() {
        messagePool.shutdown();
    }

    public ThreadPoolExecutor getMessagePool() {
        return messagePool;
    }
}
