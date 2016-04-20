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
                    mInstance = DbUtils.create(context.getApplicationContext());
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
            GlobalVariable.put(entity.getClass().getName() + "needRefresh", true);
            if(entity instanceof PlanVote){
                GlobalVariable.put(Plan.class.getName()+"needRefresh",true);
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
            GlobalVariable.put(entity.getClass().getName()+"needRefresh",true);
            if(entity instanceof PlanVote){
                GlobalVariable.put(Plan.class.getName()+"needRefresh",true);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    public static void closeRefresh(Class entityClass){
        GlobalVariable.delete(entityClass.getName()+"needRefresh");
    }

    public static boolean needRefresh(Class entityClass){
        return GlobalVariable.get(entityClass.getName() + "needRefresh",Boolean.class,false);
    }
}
