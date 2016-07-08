package com.juju.app.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.juju.app.golobal.Constants;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/3/2 16:27
 * 版本：V1.0.0
 */
public class CommonUtil {


    public static int getDefaultPannelHeight(Context context) {
        if (context != null) {
            int size = (int) (getElementSzie(context) * 5.5);
            return size;
        } else {
            return 300;
        }
    }

    public static int getElementSzie(Context context) {
        if (context != null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int screenHeight = px2dip(dm.heightPixels, context);
            int screenWidth = px2dip(dm.widthPixels, context);
            int size = screenWidth / 6;
            if (screenWidth >= 800) {
                size = 60;
            } else if (screenWidth >= 650) {
                size = 55;
            } else if (screenWidth >= 600) {
                size = 50;
            } else if (screenHeight <= 400) {
                size = 20;
            } else if (screenHeight <= 480) {
                size = 25;
            } else if (screenHeight <= 520) {
                size = 30;
            } else if (screenHeight <= 570) {
                size = 35;
            } else if (screenHeight <= 640) {
                if (dm.heightPixels <= 960) {
                    size = 35;
                } else if (dm.heightPixels <= 1000) {
                    size = 45;
                }
            }
            return size;
        }
        return 40;
    }

    private static int px2dip(float pxValue, Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * @Description 链接跳转处理
     * @param content
     */
    public static void skipLink(Context cxt, String content) {
//        String detailUrl = "";
//        String orderUrl = "";
//        String httpUrl = "";
//        if (!TextUtils.isEmpty(detailUrl = getMatchUrl(content, SysConstant.DETAIL_HOST))) {
//            toDetailPage(cxt, getGoodsId(detailUrl));
//        } else if (!TextUtils.isEmpty(orderUrl = getMatchUrl(content, SysConstant.ORDER_HOST))) {
//            toOrderPage(cxt, getOrderId(orderUrl));
//            // toWebPage(cxt, orderUrl);
//        } else if (!TextUtils.isEmpty(httpUrl = matchUrl(content))) {
//            toWebPage(cxt, httpUrl);
//        }
    }

    /**
     * @Description 判断是否是url
     * @param text
     * @return
     */
    public static String matchUrl(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        Pattern p = Pattern.compile(
                "[http]+[://]+[0-9A-Za-z:/[-]_#[?][=][.][&]]*",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }


    public static String getAudioSavePath(String userNo) {
        String path = getSavePath(Constants.FILE_SAVE_TYPE_AUDIO) + userNo
                + "_" + String.valueOf(System.currentTimeMillis())
                + ".spx";
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return path;
    }


    public static String getSavePath(int type) {
        String path;
        String floder = (type == Constants.FILE_SAVE_TYPE_IMAGE) ? "images"
                : "audio";
        if (CommonUtil.checkSDCard()) {
            path = Environment.getExternalStorageDirectory().toString()
                    + File.separator + "juju" + File.separator + floder
                    + File.separator;

        } else {
            path = Environment.getDataDirectory().toString() + File.separator
                    + "juju" + File.separator + floder + File.separator;
        }
        return path;
    }

    /**
     * @Description 判断存储卡是否存在
     * @return
     */
    public static boolean checkSDCard() {
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        }

        return false;
    }

    public static byte[] intToBytes(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (24 - i * 8));
        }
        return b;
    }

    // check again
    public static int getAudioBkSize(int sec, Context context) {
        int size = getElementSzie(context) * 2;
        if (sec <= 0) {
            return -1;
        } else if (sec <= 2) {
            return size;
        } else if (sec <= 8) {
            return (int) (size + ((float) ((sec - 2) / 6.0)) * size);
        } else if (sec <= 60) {
            return (int) (2 * size + ((float) ((sec - 8) / 52.0)) * size);
        } else {
            return -1;
        }
    }

}
