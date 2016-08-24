package com.juju.app.golobal;

import java.util.HashMap;
import java.util.Objects;

/**
 * Created by Administrator on 2016/1/11 0011.
 */
public final class GlobalVariable {
//    public static String userNo="100000001";
//    public static String token="100000001";
//    public static int cycle;
    public static final String serverIp = "219.143.237.229";


    public static final int serverPort = 8080;
    public static final String liveServerIp = "219.143.237.232";
    public static final int liveServerPort = 1935 ;
    public static String videoUrl = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";


    /**
     *  1：可以扩展多个TRACKER_SERVER1和GROUPNAME1
     *  2: GROUPNAME1可以包含多个STORAGE_SERVER（N）
     *  3：STORAGE_SERVER通过TRACKER_SERVER1获取
     */
    public static final String TRACKER_SERVER1 = "219.143.237.229:22122";
    public static final String GROUPNAME1 = "group1";

    public static final String defaultCity = "北京市";


//    public static boolean isAlive = false;

    private static HashMap dataMap = new HashMap<String,Object>();

    public static boolean isSkipLogin() {
        return false;
    }

    public static void put(String key, Object value){
        dataMap.put(key,value);
    }

    public static <T> T get(String key,Class<T> valueType,T defaultValue){
        Object rtValue = dataMap.get(key);
        if(rtValue == null){
            return defaultValue;
        }else {
            return (T)rtValue;
        }
    }

    public static void delete(String key){
        dataMap.remove(key);
    }
}
