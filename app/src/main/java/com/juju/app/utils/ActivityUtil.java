package com.juju.app.utils;

import android.app.Activity;
import android.content.Intent;

import com.juju.app.R;

import org.apache.http.message.BasicNameValuePair;

/**
 * 项目名称：juju
 * 类描述：Activity工具类
 * 创建人：gm
 * 日期：2016/2/18 17:33
 * 版本：V1.0.0
 */
public class ActivityUtil {

    /**
     * 打开Activity
     *
     * @param activity
     * @param cls
     * @param name
     */
    public static void start_Activity(Activity activity, Class<?> cls,
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
     * 关闭 Activity
     *
     * @param activity
     */
    public static void finish(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.push_right_in,
                R.anim.push_right_out);
    }
}
