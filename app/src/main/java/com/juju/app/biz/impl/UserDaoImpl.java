package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.User;


/**
 * 项目名称：juju
 * 类描述：用户仓库 (测试使用)
 * 创建人：gm
 * 日期：2016/2/17 12:01
 * 版本：V1.0.0
 */
public class UserDaoImpl extends DaoSupport<User, Long> {


    public UserDaoImpl(Context context) {
        super(context);
    }

}
