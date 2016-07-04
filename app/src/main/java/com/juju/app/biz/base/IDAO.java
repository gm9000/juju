package com.juju.app.biz.base;

import android.util.Log;


import org.xutils.db.Selector;
import org.xutils.ex.DbException;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public void replaceInto(T entity);

    public void batchReplaceInto(List<T> entityList);

    public void delete(T entity);

    public void deleteAll(List<T> entityList);

    public void deleteById(PK id);

    public T findById(String id);

    public List<T> findAll();

    public long getTotalCount();

    public List<T> findAll(Selector selector);

    /**
     * 按属性排序
     * @param orders
     */
    public List<T> findAll4Order(String... orders);



//    //参考DbUtils
//    public List<T> findAll(String sql) throws DbException;

    /**
     * 通过属性查找，返回列表或对象
     * @param propertys
     * @param values
     * @return
     */
    public List<T> findByProperty(String propertys, Object[] values);


    /**
     * 通过属性查找，返回唯一对象
     * @param propertys
     * @param values
     * @return
     */
    public T findUniByProperty(String propertys, Object[] values);


    /**
     *
     * @param propertys
     * @param values
     * @return
     */
    public Object findUniByProperty4Or(String propertys, Object[] values);

}