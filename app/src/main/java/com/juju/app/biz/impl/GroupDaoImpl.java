package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.SessionEntity;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/4/21 15:46
 * 版本：V1.0.0
 */
public class GroupDaoImpl extends DaoSupport<GroupEntity, Long> {

    public GroupDaoImpl(Context context) {
        super(context);
    }
}
