package com.juju.app.ui.base;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.baidu.mapapi.SDKInitializer;
import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.inspector.database.DatabaseFilesProvider;
import com.facebook.stetho.inspector.database.SqliteDatabaseDriver;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.CacheManager;
import com.juju.app.golobal.DBConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.SpfUtil;
import com.rey.material.app.ThemeManager;

import org.xutils.DbManager;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;
import org.xutils.x;

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

    private UserInfoBean userInfoBean = new UserInfoBean();

    private List<Activity> mActivities = new ArrayList<>();


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
        long begin = System.currentTimeMillis();
        initFramework();
        initConfig();
        initDebug();
        initService();
        long end = System.currentTimeMillis();
        Log.d("BaseApplication","onCreate#costTime:"+(end-begin)+"ms");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        long begin = System.currentTimeMillis();
        MultiDex.install(BaseApplication.this);
        long end = System.currentTimeMillis();
        Log.d("BaseApplication","attachBaseContext#costTime:"+(end-begin)+"ms");

    }

//    private void afterMultiDexInstall() {
//
//    }

    //初始化系统框架
    private void initFramework() {
        x.Ext.init(this);
        SDKInitializer.initialize(this);
        //初始化图片加载器
        ImageLoaderUtil.initImageLoaderConfig(getApplicationContext());
        //初始化皮肤管理器
        ThemeManager.init(getApplicationContext(), 2, 0, null);
    }

    //初始化系统配置
    private void initConfig() {
//        File file = null;
//        String dbDir =  CacheManager.getAppDatabasePath(getApplicationContext());
//        file = new File(dbDir);
//        if(!file.isDirectory()) {
//            file.mkdirs();
//        }
//        DbManager.DaoConfig daoConfig = new DbManager.DaoConfig()
//                .setDbDir(file)
//                .setDbName(DBConstant.DB_NAME)
//                .setTableCreateListener(new DbManager.TableCreateListener() {
//                    @Override
//                    public void onTableCreated(DbManager db, TableEntity<?> table) {
//                        if("message".equalsIgnoreCase(table.getName())) {
//                            try {
//                                db.execNonQuery("CREATE INDEX index_message_created ON message(created)");
//                                db.execNonQuery("CREATE UNIQUE INDEX index_message_session_key_msg_id " +
//                                        "on message(session_key, msg_id)");
//                            } catch (DbException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                });
//        BaseApplication.getInstance().setDaoConfig(daoConfig);
    }

    //初始化DEBUG模式
    private void initDebug() {
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableWebKitInspector(new InspectorModulesProvider() {
                    @Override
                    public Iterable<ChromeDevtoolsDomain> get() {
                        return new Stetho.DefaultInspectorModulesBuilder(getApplicationContext()).provideDatabaseDriver(
                                new SqliteDatabaseDriver(getApplicationContext(), new DatabaseFilesProvider() {

                                    @Override
                                    public List<File> getDatabaseFiles() {
                                        List<File> files = new ArrayList<>();
                                        files.add(getDatabasePath("jlm_19400000001"));
                                        files.add(getDatabasePath("jlm_19400000002"));
                                        files.add(getDatabasePath("jlm_19400000003"));
                                        files.add(getDatabasePath("jlm_19400000004"));
                                        files.add(getDatabasePath("jlm_19400000005"));
                                        files.add(getDatabasePath("jlm_15800000024"));
                                        files.add(getDatabasePath("jlm_19600000018"));
                                        return files;
                                    }
                                })
                        ).finish();
                    }
                }).build());

//        Stetho.initializeWithDefaults(this);

        //监测内存泄漏
//        LeakCanary.install(this);
//        x.Ext.setDebug(BuildConfig.DEBUG);
    }

    //初始化系统服务
    private void initService() {
        startIMService();
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

    boolean isFirst = true;

    private boolean isFirstLaunch() {
        return isFirst;
    }

}
