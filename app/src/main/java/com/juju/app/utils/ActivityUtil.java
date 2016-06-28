package com.juju.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.juju.app.R;
import com.juju.app.activity.LoginActivity;
import com.juju.app.activity.chat.ChatActivity;

import org.apache.http.message.BasicNameValuePair;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 项目名称：juju
 * 类描述：Activity工具类
 * 创建人：gm
 * 日期：2016/2/18 17:33
 * 版本：V1.0.0
 */
public class ActivityUtil {

    /**
     * 打开Activity(startActivityNew(Context context, Class<?> cls,Object... parameter))
     *
     * @param activity
     * @param cls
     * @param name
     */
    @Deprecated
    public static void startActivity(Activity activity, Class<?> cls,
                                      BasicNameValuePair... name) {
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        if (name != null)
            for (int i = 0; i < name.length; i++) {
                intent.putExtra(name[i].getName(), name[i].getValue());
            }
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_left_in,
                R.anim.push_left_out);

    }


    /**
     * 打开Activity（推荐使用startActivityNew(Context context, Class<?> cls,Object... parameter)）
     *
     * @param context
     * @param cls
     * @param name
     */
    @Deprecated
    public static void startActivity(Context context, Class<?> cls,
                                     BasicNameValuePair... name) {
        Intent intent = new Intent();
        intent.setClass(context, cls);
        if (name != null)
            for (int i = 0; i < name.length; i++) {
                intent.putExtra(name[i].getName(), name[i].getValue());
            }
        if(context instanceof Activity) {
            ((Activity)context).startActivity(intent);
            ((Activity)context).overridePendingTransition(R.anim.push_left_in,
                    R.anim.push_left_out);

        }
    }


    /**
     *
     * @param context
     * @param cls
     * @param parameter (支持 MAP )
     */
    public static void startActivityNew(Context context, Class<?> cls,
                                     Object... parameter) {
        //参数
        if(parameter != null && parameter.length >= 3)
            throw new IllegalArgumentException("startActivity parameter is error");

        //context
        if(!(context instanceof Activity))
            throw new IllegalArgumentException("startActivity context is error");


        Intent intent = new Intent();
        intent.setClass(context, cls);
//        if(context instanceof LoginActivity) {
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        }

        if(parameter != null) {
            if(parameter.length == 1
                    && parameter[0] instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)parameter[0];
                Set<Map.Entry<String, Object>> entrySet = map.entrySet();
                for(Map.Entry<String, Object> entry : entrySet) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if(value instanceof String) {
                        intent.putExtra(key, (String)value);
                    } else if (value instanceof Integer) {
                        intent.putExtra(key, (Integer)value);
                    } else if (value instanceof Long) {
                        intent.putExtra(key, (Long)value);
                    } else if (value instanceof Boolean) {
                        intent.putExtra(key, (Boolean)value);
                    } else if (value instanceof Double) {
                        intent.putExtra(key, (Double)value);
                    } else if (value instanceof Short) {
                        intent.putExtra(key, (Short)value);
                    }
                }
            } else if (parameter.length == 2) {
                String key = (String)parameter[0];
                Object value = parameter[1];
                if(value instanceof String) {
                    intent.putExtra(key, (String)value);
                } else if (value instanceof Integer) {
                    intent.putExtra(key, (Integer)value);
                } else if (value instanceof Long) {
                    intent.putExtra(key, (Long)value);
                } else if (value instanceof Boolean) {
                    intent.putExtra(key, (Boolean)value);
                } else if (value instanceof Double) {
                    intent.putExtra(key, (Double)value);
                } else if (value instanceof Short) {
                    intent.putExtra(key, (Short)value);
                } else if (value instanceof Serializable) {
                    intent.putExtra(key, (Serializable)value);
                } else if (value instanceof Parcelable) {
                    intent.putExtra(key, (Parcelable)value);
                }
            }
        }
        ((Activity)context).startActivity(intent);
        ((Activity)context).overridePendingTransition(R.anim.push_left_in,
                R.anim.push_left_out);

    }

    /**
     * 打开Activity
     *
     * @param activity
     * @param cls
     * @param name
     */
    public static void startActivity4UP(Activity activity, Class<?> cls,
                                        BasicNameValuePair... name) {
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        if (name != null)
            for (int i = 0; i < name.length; i++) {
                intent.putExtra(name[i].getName(), name[i].getValue());
            }
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_up_in,
                R.anim.push_up_out);

    }


    /**
     * 打开Activity
     *
     * @param activity
     * @param cls
     * @param name
     */
    public static void startActivityWithParent(Activity activity, Class<?> cls,
                                        BasicNameValuePair... name) {
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        if (name != null)
            for (int i = 0; i < name.length; i++) {
                intent.putExtra(name[i].getName(), name[i].getValue());
            }
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_up_in,
                R.anim.push_up_out);

    }




    /**
     * 关闭 Activity
     *
     * @param activity
     */
    public static void finish(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.push_right_in,
                R.anim.push_right_out);
    }

    /**
     * 关闭 Activity
     *
     * @param activity
     */
    public static void finish4UP(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.push_down_in,
                R.anim.push_down_out);
    }




    /**
     * 打开Activity
     *
     * @param activity
     * @param cls
     * @param name
     */
    public static void startActivityForResult(Activity activity, Class<?> cls, int requestCode,
                                     BasicNameValuePair... name) {
        Intent intent = new Intent();
        intent.setClass(activity, cls);
        if (name != null)
            for (int i = 0; i < name.length; i++) {
                intent.putExtra(name[i].getName(), name[i].getValue());
            }
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.push_left_in,
                R.anim.push_left_out);

    }


}
