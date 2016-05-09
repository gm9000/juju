package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.chat.UnreadEntity;

/**
 * 项目名称：juju
 * 类描述：未读消息实体DAO
 * 创建人：gm
 * 日期：2016/5/3 12:21
 * 版本：V1.0.0
 */
public class UnreadDaoImpl extends DaoSupport<UnreadEntity, String> {

    public UnreadDaoImpl(Context context) {
        super(context);
    }
}
