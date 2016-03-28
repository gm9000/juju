package com.juju.app.service;

/**
 * Created by Administrator on 2016/3/21 0021.
 */
public class ConnectStatus {
    private int status;
    public ConnectStatus(int status){
        this.status = status;
    }

    public int getStatus(){
        return this.status;
    }

    public String getConnectDesc(){
        String info = "";
        switch (status){
            case -2:
                info = "服务器连接中断，重连中...";
                break;
            case -1:
                info = "服务器地址解析错误！";
                break;
            case 0:
                info = "服务器连接成功！";
                break;
            case 1:
                info = "服务器连接失败！";
                break;
            case 2:
                info = "";
                break;
        }
        return info;
    }
}
