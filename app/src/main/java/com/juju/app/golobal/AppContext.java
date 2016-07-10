package com.juju.app.golobal;

import android.app.Activity;

import com.juju.app.bean.UserInfoBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：App上下文信息 (
 * 创建人：gm
 * 日期：2016/7/10 19:01
 * 版本：V1.0.0
 */
public class AppContext {

    private static List<Activity> mActivities = new ArrayList<>();

    private static UserInfoBean userInfoBean = new UserInfoBean();

    public static UserInfoBean getUserInfoBean() {
        return userInfoBean;
    }

    public static List<Activity> getActivities() {
        return mActivities;
    }
}
