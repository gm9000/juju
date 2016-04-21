package com.juju.app.biz;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.juju.app.biz.base.IDAO;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.golobal.DBConstant;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.db.sqlite.CursorUtils;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.util.IOUtils;

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

    protected DbUtils db;

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
            db.update(entity);
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
            db.saveOrUpdateAll(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute batchSaveOrUpdate error:"+clazz.getSimpleName(), e);
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
    public List<T> findAll() {
        List<T> list = null;
        try {
            list = db.findAll(clazz);
        } catch (DbException e) {
            Log.e(TAG, "execute findAll error:"+clazz.getSimpleName(), e);
        }
        return list;
    }

    @Override
    public long getTotalCount() {
        long count = 0;
        try {
            count = db.count(clazz);
        } catch (DbException e) {
            Log.e(TAG, "execute getTotalCount error:"+clazz.getSimpleName(), e);
        }
        return count;
    }

    @Override
    public void init() {
        this.db = DbUtils.create(context, DBConstant.DB_NAME);
        try {
            db.createTableIfNotExist(clazz);
        } catch (DbException e) {
            Log.e(TAG, "execute init error:" + clazz.getSimpleName(), e);
        }
    }

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
            db.saveAll(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute batchSave error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void deleteAll(List<T> entityList) {
        try {
            db.deleteAll(entityList);
        } catch (DbException e) {
            Log.e(TAG, "execute deleteAll error:"+clazz.getSimpleName(), e);
        }
    }

    @Override
    public void findAll(Selector selector) {
        try {
            db.findAll(selector);
        } catch (DbException e) {
            Log.e(TAG, "execute deleteAll error:"+clazz.getSimpleName(), e);
        }
    }

    //参考DbUtils
    @Override
    public List<T> findAll(String sql) throws DbException {
        long seq = CursorUtils.FindCacheSequence.getSeq();
        findTempCache.setSeq(seq);
        Object obj = findTempCache.get(sql);
        if (obj != null) {
            return (List<T>) obj;
        }
        List<T> result = new ArrayList<T>();
        Cursor cursor = db.execQuery(sql);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    T entity = (T) CursorUtils.getEntity(db, cursor, clazz, seq);
                    result.add(entity);
                }
                findTempCache.put(sql, result);
            } catch (Throwable e) {
                throw new DbException(e);
            } finally {
                IOUtils.closeQuietly(cursor);
            }
        }
        return result;
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


}
