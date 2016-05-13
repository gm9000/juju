package com.juju.app.golobal;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;


import org.xutils.ImageManager;
import org.xutils.image.ImageOptions;
import org.xutils.x;

public class BitmapUtilFactory {

    private static ImageManager mInstance = null;

    public static ImageManager getInstance(final Context context) {
        if (mInstance == null) {
            synchronized (BitmapUtilFactory.class){
                if (mInstance == null) {
//                    mInstance = new BitmapUtils(context.getApplicationContext());
//                    mInstance.configDiskCacheEnabled(true);
//                    mInstance.configMemoryCacheEnabled(true);
                    mInstance = x.image();
                }
            }
        }
        return mInstance;
    }

    public static class Option {
        public static ImageOptions imageOptions() {
            ImageOptions imageOptions = new ImageOptions.Builder()
                    .setUseMemCache(true)
                    .setIgnoreGif(false)
                    .setConfig(Bitmap.Config.ARGB_4444)
                    .setImageScaleType(ImageView.ScaleType.CENTER).build();
            return imageOptions;
        }

        public static ImageOptions imageOptions4Party() {

            ImageOptions imageOptions = new ImageOptions.Builder()
                    .setUseMemCache(true)
                    .setIgnoreGif(false)
                    .setImageScaleType(ImageView.ScaleType.CENTER).build();
            return imageOptions;
        }
    }

//            bitmapUtils.configDefaultCacheExpiry(128 * 1024);
//        bitmapUtils.configDiskCacheEnabled(true);
//        bitmapUtils.configDefaultCacheExpiry(2048 * 1024);
//
//        bdConfig = new BitmapDisplayConfig();
//
//        //设置显示图片特性
//        bdConfig.setBitmapConfig(Bitmap.Config.ARGB_4444);
//        bdConfig.setBitmapMaxSize(BitmapCommonUtils.getScreenSize(getActivity())); //图片的最大尺寸
////        bdConfig.setLoadingDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载时显示的图片
////        bdConfig.setLoadFailedDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载失败时显示的图片
//        bdConfig.setShowOriginal(false); //不显示源图片
////        bdConfig.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_from_top));
//        bitmapUtils.configDefaultDisplayConfig(bdConfig);



}
