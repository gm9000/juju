package com.juju.app.service.im.manager;

/**
 * 项目名称：juju
 * 类描述：消息管理服务
 * 创建人：gm
 * 日期：2016/3/22 09:45
 * 版本：V1.0.0
 */
public class IMMessageManager extends IMManager {

    private final String TAG = getClass().getName();

    private static IMMessageManager inst;

    public static IMMessageManager instance() {
        synchronized (IMMessageManager.class) {
            if (inst == null) {
                inst = new IMMessageManager();
            }
            return inst;
        }
    }

    @Override
    public void doOnStart() {

    }
}
