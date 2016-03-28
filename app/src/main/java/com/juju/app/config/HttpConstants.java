package com.juju.app.config;

import com.juju.app.golobal.GlobalVariable;

/**
 * 项目名称：juju
 * 类描述：Http常量
 * 创建人：gm
 * 日期：2016/2/16 19:25
 * 版本：V1.0.0
 */
public class HttpConstants {

    //HTTP请求、响应类型
    public static final String CONTENT_TYPE = "application/json";

    private static String USER_URL = "";
    private static String LIVE_SERVER_URL = "";

    public static synchronized void initURL() {
        USER_URL = "http://" + GlobalVariable.serverIp + ":" + GlobalVariable.serverPort
                + "/juju/bServer/user";
        LIVE_SERVER_URL = "http://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/stream";
    }

    public static String getUserUrl() {
        return USER_URL;
    }

    public static String getLiveServerUrl() {
        return LIVE_SERVER_URL;
    }


}
