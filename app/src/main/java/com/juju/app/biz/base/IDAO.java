package com.juju.app.biz.base;

import android.util.Log;

import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/4/20 16:44
 * 版本：V1.0.0
 */
public interface IDAO<T, PK> {

    /**
     * ******************************************基础接口***************************************
     */

    public void save(T entity);

    public void update(T entity);

    public void saveOrUpdate(T entity);

    public void batchSave(List<T> entityList);

    public void batchSaveOrUpdate(List<T> entityList);

    public void delete(T entity);

    public void deleteAll(List<T> entityList);

    public void deleteById(PK id);

    public T findById(String id);

    public List<T> findAll();

    public long getTotalCount();

    public void init();

    public void findAll(Selector selector);

    //参考DbUtils
    public List<T> findAll(String sql) throws DbException;


}