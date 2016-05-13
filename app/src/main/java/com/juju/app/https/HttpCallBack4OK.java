package com.juju.app.https;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/5/7 17:21
 * 版本：V1.0.0
 */
public interface HttpCallBack4OK {

    public void onSuccess4OK(Object obj, int accessId);

    public void onFailure4OK(Exception e, int accessId);
}
