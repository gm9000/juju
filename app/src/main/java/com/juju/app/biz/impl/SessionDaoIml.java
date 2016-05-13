package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.golobal.MessageConstant;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/4/20 16:32
 * 版本：V1.0.0
 */
public class SessionDaoIml extends DaoSupport<SessionEntity, Long> {


    public SessionDaoIml(Context context) {
        super(context);
    }


}
