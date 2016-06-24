package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.OtherMessageEntity;

/**
 * 项目名称：juju
 * 类描述：其他消息DAO
 * 创建人：gm
 * 日期：2016/6/17 15:38
 * 版本：V1.0.0
 */
public class OtherMessageDaoImpl extends DaoSupport<OtherMessageEntity, Long> {
    public OtherMessageDaoImpl(Context context) {
        super(context);
    }
}
