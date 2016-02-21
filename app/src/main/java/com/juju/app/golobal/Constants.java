package com.juju.app.golobal;

import android.os.Environment;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/2/16 16:00
 * 版本：V1.0.0
 */
public class Constants {

    /**
     ******************************************* 参数设置信息******************************************
     */



    // 应用名称
    public static String APP_NAME = "";

    public static final String SHARED_PREFERENCE_NAME = "ele_prefs";

    // SDCard路径
    public static final String SD_PATH = Environment
            .getExternalStorageDirectory().getAbsolutePath();

    // 图片存储路径
    public static final String BASE_PATH = SD_PATH + "/juju/";

    // 缓存图片路径
    public static final String BASE_IMAGE_CACHE = BASE_PATH + "cache/images/";


    /**
     ******************************************* 参数设置信息结束 ******************************************
     */



}
