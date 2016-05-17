package com.juju.app.https;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;


/**
 * 项目名称：juju
 * 类描述：OkHttpClient管理类
 * 创建人：gm   
 * 日期：2016/5/7 23:56
 * 版本：V1.0.0
 */
public class OkHttpClientManager {
    private volatile static OkHttpClientManager mInstance;

    private OkHttpClient mOkHttpClient;

    private static final String TAG = "OkHttpClientManager";


    private OkHttpClientManager() {
        mOkHttpClient = new OkHttpClient();
        //cookie enabled
        mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));

        //超时时间
        mOkHttpClient.setConnectTimeout(10, TimeUnit.SECONDS);

        ConnectionPool connectionPool = new ConnectionPool(32, 5 * 60 * 1000);

        mOkHttpClient.setConnectionPool(connectionPool);

//        mOkHttpClient.networkInterceptors().add(new StethoInterceptor());

    }

    public static OkHttpClientManager getInstance() {
        if (mInstance == null) {
            synchronized (OkHttpClientManager.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpClientManager();
                }
            }
        }
        return mInstance;
    }

    public OkHttpClient getmOkHttpClient() {
        return mOkHttpClient;
    }

}