package com.juju.app.golobal;

import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.DBUtil;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;


public class JujuDbUtils {

    private static DbManager mInstance = null;

    public static DbManager getInstance() {
        if (mInstance == null) {
            synchronized (JujuDbUtils.class){
                if (mInstance == null) {
                    mInstance = x.getDb(DBUtil.instance().getDaoConfig());
                }
            }
        }
        return mInstance;
    }

    public static void delete(Object entity){
        try {
            getInstance().delete(entity);
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
            getInstance().saveOrUpdate(entity);
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

    public static void replace(Object entity){
        try {
            getInstance().replace(entity);
            GlobalVariable.put(entity.getClass().getSimpleName()+"needRefresh",true);
            if(entity instanceof PlanVote){
                GlobalVariable.put(Plan.class.getSimpleName()+"needRefresh",true);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }
}
