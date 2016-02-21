package com.juju.app.biz;

import com.juju.app.entity.User;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：用户数据库
 * 创建人：gm
 * 日期：2016/2/17 11:52
 * 版本：V1.0.0
 */
public interface UserDao {

    /**
     * 插入用户
     *
     * @param user
     */
    public void insert(User user);

    /**
     * 批量插入用户
     *
     * @param list
     */
    public void insert(List<User> list);

    /**
     * 查询某个用户
     *
     * @param userNo
     * @return
     */
    public User query(String userNo);

    /**
     * 查询所有用户
     *
     * @return
     */
    public List<User> queryAll();

    /**
     * 查询用户（分页）
     *
     * @param pageIndex
     * @param pageSize
     * @return
     */
    List<User> query( int pageIndex, int pageSize);

    /**
     * 删除用户
     *
     * @param user
     */
    public void delete(User user);

    /**
     * 删除博主列表
     *
     * @param list
     */
    public void deleteAll(List<User> list);

    /***
     * 删除所有用户
     *
     */
    public void deleteAll();

    /**
     * 初始化用户数据库
     * @param type
     */
    public void init(String type);
}
