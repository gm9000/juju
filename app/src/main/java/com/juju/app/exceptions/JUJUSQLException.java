package com.juju.app.exceptions;

import android.annotation.TargetApi;
import android.database.SQLException;
import android.os.Build;

/**
 * 项目名称：juju
 * 类描述：SQL系统异常
 * 创建人：gm
 * 日期：2016/4/29 09:24
 * 版本：V1.0.0
 */
public class JUJUSQLException extends SQLException {

    public JUJUSQLException(String message) {
        super(message);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public JUJUSQLException(String message, Throwable cause) {
        super(message, cause);
    }


}
