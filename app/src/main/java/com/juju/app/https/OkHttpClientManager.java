package com.juju.app.https;




import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private Handler mDelivery;


    private OkHttpClientManager() {
        mOkHttpClient = new OkHttpClient();
        //cookie enabled
        mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));

        //超时时间
        mOkHttpClient.setConnectTimeout(10, TimeUnit.SECONDS);

        mOkHttpClient.networkInterceptors().add(new StethoInterceptor());

        mDelivery = new Handler(Looper.getMainLooper());

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

    public Handler getmDelivery() {
        return mDelivery;
    }
}