package com.juju.app.service.im.manager;

/**
 * 项目名称：juju
 * 类描述：最新消息管理服务
 * 创建人：gm
 * 日期：2016/3/22 09:43
 * 版本：V1.0.0
 */
public class IMRecentSessionManager extends IMManager {

    private final String TAG = getClass().getName();

    private volatile static IMRecentSessionManager inst;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMRecentSessionManager instance() {
        if(inst == null) {
            synchronized (IMRecentSessionManager.class) {
                if (inst == null) {
                    inst = new IMRecentSessionManager();
                }
            }
        }
        return inst;
    }

    @Override
    public void doOnStart() {

    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {

    }
}
