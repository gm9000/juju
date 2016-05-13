package com.juju.app.ui.base;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.activeandroid.ActiveAndroid;
import com.baidu.mapapi.SDKInitializer;
import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.inspector.database.DatabaseFilesProvider;
import com.facebook.stetho.inspector.database.DefaultDatabaseFilesProvider;
import com.facebook.stetho.inspector.database.SqliteDatabaseDriver;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.juju.app.BuildConfig;
import com.juju.app.activity.MainActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.CacheManager;
import com.juju.app.golobal.DBConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.utils.ImageLoaderUtil;
import com.rey.material.app.ThemeManager;


import org.xutils.x;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：juju
 * 类描述：应用Application类
 * 创建人：gm
 * 日期：2016/2/17 11:36
 * 版本：V1.0.0
 */
public class BaseApplication extends Application {

    private static BaseApplication mInstance;

    private UserInfoBean userInfoBean = new UserInfoBean();

//    private String mAccount = "admin@219.143.237.230";
//
//    private String

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
        super.onCreate();
        x.Ext.init(this);
        x.Ext.setDebug(BuildConfig.DEBUG);
        SDKInitializer.initialize(this);
        init();
    }


    private void init() {
        startIMService();
        //初始化图片加载器
        ImageLoaderUtil.initImageLoaderConfig(getApplicationContext());

        ThemeManager.init(getApplicationContext(), 2, 0, null);

//        Stetho.initializeWithDefaults(this);

        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableWebKitInspector(new InspectorModulesProvider() {
                    @Override
                    public Iterable<ChromeDevtoolsDomain> get() {
                        return new Stetho.DefaultInspectorModulesBuilder(getApplicationContext()).provideDatabaseDriver(
                                new SqliteDatabaseDriver(getApplicationContext(), new DatabaseFilesProvider() {

                                    @Override
                                    public List<File> getDatabaseFiles() {
                                        List<File> files = new ArrayList<File>();
                                        files.add(getDatabasePath(DBConstant.DB_NAME));
                                        return files;
                                    }
                                })
                        ).finish();
                    }
                })
                .build());


        ActiveAndroid.initialize(this);


//        OkHttpUtils.getInstance().debug("OkHttpUtils")
//                .setConnectTimeout(100000, TimeUnit.MILLISECONDS);

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
        return new File(CacheManager.getAppDatabasePath(this)+"/"+name);
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
        for (Activity activity : mActivities) {
            activity.finish();
        }
        stopIMService();
        System.exit(0);
        super.onTerminate();
    }

    public UserInfoBean getUserInfoBean() {
        return userInfoBean;
    }


    //启动IM服务
    private void startIMService() {
        Intent intent = new Intent();
        intent.setClass(this, IMService.class);
        startService(intent);
    }

    //关闭IM服务
    private void stopIMService() {
        Intent intent = new Intent();
        intent.setClass(this, IMService.class);
        stopService(intent);
    }
}
