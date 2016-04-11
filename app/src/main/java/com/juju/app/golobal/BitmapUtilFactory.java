package com.juju.app.golobal;

import android.content.Context;

import com.lidroid.xutils.BitmapUtils;

public class BitmapUtilFactory {

    private static BitmapUtils mInstance = null;

    public static BitmapUtils getInstance(final Context context) {
        if (mInstance == null) {
            synchronized (BitmapUtilFactory.class){
                if (mInstance == null) {
                    mInstance = new BitmapUtils(context.getApplicationContext());
                    mInstance.configDiskCacheEnabled(true);
                    mInstance.configMemoryCacheEnabled(true);
                }
            }
        }
        return mInstance;
    }
}
