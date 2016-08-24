package com.juju.app.event;

/**
 * 项目名称：juju
 * 类描述：小视频发送通知事件
 * 创建人：gm
 * 日期：2016/8/9 18:50
 * 版本：V1.0.0
 */
public class SmallMediaEvent {
    //视频位置
    public String mVideoPath;

    //视频截图位置
    public String mPicPath;

    //时间
    public int duration;

    //视频文件大小
    public long size;

    public SmallMediaEvent(String mVideoPath, String mPicPath, int duration, long size) {
        this.mVideoPath = mVideoPath;
        this.mPicPath = mPicPath;
        this.duration = duration;
        this.size = size;
    }

}
