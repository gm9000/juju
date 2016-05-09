package com.juju.app.service.im.manager;

import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.entity.chat.GroupEntity;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：处理非聊天相关的事件
 * 创建人：gm
 * 日期：2016/5/6 11:30
 * 版本：V1.0.0
 */
public class IMOtherManager extends IMManager {

    private volatile static IMOtherManager inst;

    private DaoSupport groupDao;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMOtherManager instance() {
        if(inst == null) {
            synchronized (IMOtherManager.class) {
                if (inst == null) {
                    inst = new IMOtherManager();
                }
            }
        }
        return inst;
    }

    @Override
    public void doOnStart() {
        groupDao = new GroupDaoImpl(ctx);
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
