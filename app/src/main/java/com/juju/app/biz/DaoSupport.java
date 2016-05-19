package com.juju.app.biz;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.juju.app.biz.base.IDAO;
import com.juju.app.config.CacheManager;
import com.juju.app.exceptions.JUJUSQLException;
import com.juju.app.golobal.DBConstant;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.StringUtils;


import org.xutils.DbManager;
import org.xutils.db.DbManagerImpl;
import org.xutils.db.Selector;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目名称：juju
 * 类描述：DAO父类，实现公共业务
 * 创建人：gm
 * 日期：2016/4/20 16:32
 * 版本：V1.0.0
 */
public abstract class DaoSupport<T, PK> implements IDAO<T, PK> {

    private final String TAG = getClass().getSimpleName();

    private Context context;

    protected DbManager db;

    protected Class<T> clazz;

    public DaoSupport(Context context) {
        this.clazz = (Class<T>)((ParameterizedType)this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
        this.context = context;
        //初始化数据库
        init();
    }



    @Override
    public void save(T entity) {
        try {
            db.save(entity);
        } catch (DbException e) {
            Log.e(TAG, "save error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void update(T entity)  {
        try {
            db.update(entity);
        } catch (DbException e) {
            Log.e(TAG, "execute update error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void saveOrUpdate(T entity) {
        try {
            db.saveOrUpdate(entity);
        } catch (DbException e) {
            Log.e(TAG, "execute saveOrUpdate error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void batchSaveOrUpdate(List<T> entityList) {
        try {
            db.saveOrUpdate(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute batchSaveOrUpdate error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void batchReplaceInto(List<T> entityList) {
        try {
            db.replace(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute batchReplaceInto error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void replaceInto(T entity) {
        try {
            db.replace(entity);
        } catch (DbException e) {
            Log.e(TAG, "execute replaceInto error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void delete(T entity) {
        try {
            db.delete(entity);
        } catch (DbException e) {
            Log.e(TAG, "execute delete error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public T findById(String id) {
        T entity = null;
        try {
            entity = db.findById(clazz, id);
        } catch (DbException e) {
            Log.e(TAG, "execute findById error:"+clazz.getSimpleName(), e);
        }
        return entity;
    }

    @Override
    public List<T> findAll() {
        List<T> list = null;
        try {
            list = db.findAll(clazz);
        } catch (DbException e) {
            Log.e(TAG, "execute findAll error:"+clazz.getSimpleName(), e);
        }
        if(list == null) list = new ArrayList<>();
        return list;
    }

    @Override
    public long getTotalCount() {
        long count = 0;
        try {
            count = db.selector(clazz).count();
        } catch (DbException e) {
            Log.e(TAG, "execute getTotalCount error:"+clazz.getSimpleName(), e);
        }
        return count;
    }

    private void init() {
        this.db = x.getDb(BaseApplication.getInstance().getDaoConfig());
    }

//    private void createTabled() {
//        try {
//            execAfterTableCreated();
//        } catch (DbException e) {
//            Log.e("TAG", "execute execAfterTableCreated error", e);
//        }
//    }

//    protected void execAfterTableCreated() throws DbException{
//
//    }

    @Override
    public void deleteById(PK id) {
        try {
            db.deleteById(clazz, id);
        } catch (DbException e) {
            Log.e(TAG, "execute deleteById error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void batchSave(List<T> entityList) {
        try {
            db.save(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute batchSave error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void deleteAll(List<T> entityList) {
        try {
            db.delete(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute deleteAll error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public List<T> findAll(Selector selector) {
        List<T> list = new ArrayList<T>();
        try {
            list = selector.findAll();
        } catch (DbException e) {
            Log.e(TAG, "execute deleteAll error:"+clazz.getSimpleName(), e);
        }
        return list;
    }

//    //参考DbUtils
//    @Override
//    public List<T> findAll(String sql) throws DbException {
//        long seq = CursorUtils.FindCacheSequence.getSeq();
//        findTempCache.setSeq(seq);
//        Object obj = findTempCache.get(sql);
//        if (obj != null) {
//            return (List<T>) obj;
//        }
//        List<T> result = new ArrayList<T>();
//        Cursor cursor = db.execQuery(sql);
//        if (cursor != null) {
//            try {
//                while (cursor.moveToNext()) {
//                    T entity = (T) CursorUtils.getEntity(db, cursor, clazz, seq);
//                    result.add(entity);
//                }
//                findTempCache.put(sql, result);
//            } catch (Throwable e) {
//                throw new DbException(e);
//            } finally {
//                IOUtils.closeQuietly(cursor);
//            }
//        }
//        return result;
//    }

    /**
     * 按属性排序
     *
     * @param orders
     */
    @Override
    public List<T> findAll4Order(String... orders) {
        List<T> list = new ArrayList<T>();
        if(orders == null || orders.length == 0)
            throw new JUJUSQLException("findAll4Order#orders cannot be empty");

        Selector selector = null;
        try {
            selector = db.selector(clazz);
            StringBuilder sbf = new StringBuilder();
            boolean lastOrderCmd = false;
            for (int i = 0; i < orders.length; i++) {
                String order = orders[i];
                String[] arr = order.split(":");
                if(arr.length < 2)
                    throw new JUJUSQLException("findAll4Order#orders format mismatch, need ':'");
                String key = arr[0];
                String value = arr[1];
                String orderCmd = "asc";
                boolean bool = false;
                if("desc".equalsIgnoreCase(value)) {
                    orderCmd = value;
                    bool = true;
                }
                selector.orderBy(key, bool);
//                sbf.append(key);
//                if(i < orders.length -1) {
//                    sbf.append(" "+orderCmd);
//                } else {
//                    lastOrderCmd = "desc".equalsIgnoreCase(orderCmd);
//                }
            }
//            selector.orderBy(sbf.toString(), lastOrderCmd);
            try {
                list = selector.findAll();
            } catch (DbException e) {
                Log.e(TAG, "execute findAll4Order error:" + clazz.getSimpleName(), e);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
        if(list == null) list = new ArrayList<>();
        return list;
    }

    @Override
    public List<T> findByProperty(String propertys, Object... values) {
        List<T> list = null;
        try {
            Selector selector = db.selector(clazz);
            if(StringUtils.isNotBlank(propertys)) {
                String[] propertysArr = propertys.split(",");
                if(values == null || propertysArr.length != values.length) {
                    throw new DbException("MISMATCH");
                }
                for (int i = 0; i <propertysArr.length; i++) {
                    if(i == 0) {
                        selector.where(propertysArr[i], "=", values[i]);
                    } else {
                        selector.and(propertysArr[i], "=", values[i]);
                    }
                }
            }
            list = selector.findAll();
        } catch (DbException e) {
            Log.e(TAG, "execute findByProperty error:"+clazz.getSimpleName(), e);
        }
        return list;
    }

    public T findUniByProperty(String propertys, Object... values) {
        T t = null;
        List<T> list = findByProperty(propertys, values);
        if(list != null && list.size() >0) {
            t = list.get(0);
        }
        return t;
    }


    /////////////////////// temp cache ////////////////////////////////////////////////////////////////
    private final FindTempCache findTempCache = new FindTempCache();

    private class FindTempCache {
        private FindTempCache() {
        }

        /**
         * key: sql;
         * value: find result
         */
        private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();

        private long seq = 0;

        public void put(String sql, Object result) {
            if (sql != null && result != null) {
                cache.put(sql, result);
            }
        }

        public Object get(String sql) {
            return cache.get(sql);
        }

        public void setSeq(long seq) {
            if (this.seq != seq) {
                cache.clear();
                this.seq = seq;
            }
        }
    }

//    private void makeDir(File dir) {
//        if(! dir.getParentFile().exists()) {
//            makeDir(dir.getParentFile());
//        }
//        dir.mkdir();
//    }


}
