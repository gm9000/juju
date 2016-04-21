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

    private static IMRecentSessionManager inst;

    public static IMRecentSessionManager instance() {
        synchronized (IMRecentSessionManager.class) {
            if (inst == null) {
                inst = new IMRecentSessionManager();
            }
            return inst;
        }
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
