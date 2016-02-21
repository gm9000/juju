package com.juju.app.ui.base;

import android.app.Activity;
import android.app.Application;

import com.juju.app.config.CacheManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：应用Application类
 * 创建人：gm
 * 日期：2016/2/17 11:36
 * 版本：V1.0.0
 */
public class BaseApplication extends Application {

    private static BaseApplication mInstance;

    private List<Activity> mActivities = new ArrayList<Activity>();

    // 单例模式中获取唯一的ExitApplication 实例
    public static BaseApplication getInstance() {
        if (null == mInstance) {
            mInstance = new BaseApplication();
        }
        return mInstance;

    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        init();
    }


    private void init() {

    }


    /**
     * 重载系统获取缓存目录
     */
    @Override
    public File getCacheDir() {
        return new File(CacheManager.getAppCachePath(this));
    }

    @Override
    public File getDatabasePath(String name) {
        return new File(CacheManager.getAppDatabasePath(this));
    }

    /**
     * 把Activity加入历史堆栈
     *
     * @param activity
     */
    public void addActivity(Activity activity) {
        mActivities.add(activity);
    }

    /**
     * 结束
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        for (Activity activity : mActivities) {
            activity.finish();
        }
        System.exit(0);
    }

}
