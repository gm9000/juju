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
    public static final String CONTENT_TYPE = "application/json;charset=utf-8";
    public static final String CONNECTION_CLOSE = "close";


    public static final String UPLOAD_TYPE = "multipart/form-data";



    private static String USER_URL = "";

    private static String A_USER_URL = "";

    private static String LIVE_SERVER_URL = "";

    private static String PORTRAIT_URL = "";

    public static void initURL() {
        USER_URL = "http://" + GlobalVariable.serverIp + ":" + GlobalVariable.serverPort
                + "/juju/bServer/user";
        LIVE_SERVER_URL = "http://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/stream";

        initURL_A();
        initPortraitURL();
    }

    public static void initURL_A() {
        A_USER_URL = "http://" + GlobalVariable.serverIp + ":" + GlobalVariable.serverPort
                + "/juju/aServer/user";
    }

    //    http://219.143.237.229:8080/juju/bServer/user/getPortraitSmall?targetNo=100000006


    private static void initPortraitURL() {
        PORTRAIT_URL =  "http://" + GlobalVariable.serverIp + ":" + GlobalVariable.serverPort
                + "/juju/bServer/user/getPortraitSmall?targetNo=";
    }

    public static String getUserUrl() {
        return USER_URL;
    }
    public static String getAUserUrl() {
        return A_USER_URL;
    }
    public static String getLiveServerUrl() {
        return LIVE_SERVER_URL;
    }

    public static String getPortraitUrl() {
        return PORTRAIT_URL;
    }


}
