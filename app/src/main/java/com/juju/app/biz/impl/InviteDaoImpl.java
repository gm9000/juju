package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.Invite;
import com.juju.app.entity.chat.OtherMessageEntity;

/**
 * 项目名称：juju
 * 类描述：邀请数据仓库
 * 创建人：gm
 * 日期：2016/6/20 18:19
 * 版本：V1.0.0
 */
public class InviteDaoImpl extends DaoSupport<Invite, Long> {

    public InviteDaoImpl(Context context) {
        super(context);
    }

}
