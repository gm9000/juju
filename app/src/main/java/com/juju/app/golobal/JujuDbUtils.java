package com.juju.app.golobal;

import android.content.Context;

import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.exception.DbException;

public class JujuDbUtils {

    private static DbUtils mInstance = null;

    public static DbUtils getInstance(final Context context) {
        if (mInstance == null) {
            synchronized (JujuDbUtils.class){
                if (mInstance == null) {
                    mInstance = DbUtils.create(context.getApplicationContext(),DBConstant.DB_NAME);
                    mInstance.configAllowTransaction(true);
                    mInstance.configDebug(true);
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
