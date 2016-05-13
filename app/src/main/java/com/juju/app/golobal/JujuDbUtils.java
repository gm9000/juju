package com.juju.app.golobal;

import android.content.Context;

import com.juju.app.config.CacheManager;
import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.File;


public class JujuDbUtils {

    private static DbManager mInstance = null;

    public static DbManager getInstance(final Context context) {
        if (mInstance == null) {
            synchronized (JujuDbUtils.class){
                if (mInstance == null) {
                    String dbDir =  CacheManager.getAppDatabasePath(context);
                    File dbFile = new File(dbDir);
                    DbManager.DaoConfig daoConfig = new DbManager.DaoConfig();
                    daoConfig.setDbName(DBConstant.DB_NAME);
                    daoConfig.setAllowTransaction(true);
                    daoConfig.setDbDir(dbFile);
                    mInstance = x.getDb(daoConfig);
                }
            }
        }
        return mInstance;
    }

    public static void delete(Object entity){
        try {
            mInstance.delete(entity);
            GlobalVariable.put(entity.getClass().getSimpleName() + "needRefresh", true);
            if(entity instanceof PlanVote){
                GlobalVariable.put(Plan.class.getSimpleName()+"needRefresh",true);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    public static void save(Object entity){
        saveOrUpdate(entity);
    }


    public static void saveOrUpdate(Object entity){
        try {
            mInstance.saveOrUpdate(entity);
            GlobalVariable.put(entity.getClass().getSimpleName()+"needRefresh",true);
            if(entity instanceof PlanVote){
                GlobalVariable.put(Plan.class.getSimpleName()+"needRefresh",true);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    public static void closeRefresh(Class entityClass){
        GlobalVariable.delete(entityClass.getSimpleName() + "needRefresh");
    }

    public static boolean needRefresh(Class entityClass){
        return GlobalVariable.get(entityClass.getSimpleName() + "needRefresh",Boolean.class,false);
    }
}
