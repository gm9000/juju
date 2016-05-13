package com.juju.app.https;


import com.squareup.okhttp.Response;

import org.xutils.common.Callback;


/**
 * 项目名称：juju
 * 类描述：Http回调接口
 * 创建人：gm
 * 日期：2016/2/17 16:46
 * 版本：V1.0.0
 */
public interface HttpCallBack {

    public void onSuccess(Object obj, int accessId);

    public void onFailure(Throwable ex, boolean isOnCallback, int accessId);

    public void onCancelled(Callback.CancelledException cex);

    public void onFinished();
}
