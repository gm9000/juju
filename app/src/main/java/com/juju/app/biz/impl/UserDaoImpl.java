package com.juju.app.biz.impl;

import android.content.Context;

import com.juju.app.biz.UserDao;
import com.juju.app.entity.User;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.db.sqlite.WhereBuilder;
import com.lidroid.xutils.exception.DbException;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：用户仓库 (测试使用)
 * 创建人：gm
 * 日期：2016/2/17 12:01
 * 版本：V1.0.0
 */
public class UserDaoImpl implements UserDao {

    private DbUtils db;

    private Context context;

    public UserDaoImpl(Context context, String type) {
        this.context = context;
        init(type);
    }

    @Override
    public void insert(User user) {
        try {
            User findItem = db.findFirst(Selector.from(User.class).where("userNo", "=",
                    user.getUserNo()));
            if (findItem != null) {
                db.update(user, WhereBuilder.b("userNo", "=", user.getUserNo()));
            } else {
                db.save(user);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void insert(List<User> list) {
        try {
            db.saveOrUpdateAll(list);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public User query(String userNo) {
        try {
            return db.findFirst(Selector.from(User.class).where("userNo", "=", userNo));
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> queryAll() {
        try {
            List<User> userlist = db.findAll(Selector.from(User.class).orderBy("updateTime", true));
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> query(int pageIndex, int pageSize) {
        try {
            List<User> userlist = db.findAll(Selector.from(User.class).orderBy("updateTime", true)
                    .limit(pageSize).offset(pageIndex * pageSize));
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
}

    @Override
    public void delete(User user) {
        try {
            db.delete(user);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAll(List<User> list) {
        try {
            db.delete(list);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAll() {
        try {
            db.deleteAll(User.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(String type) {
        this.db = DbUtils.create(context, "user" + type);
    }
}
