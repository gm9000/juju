package com.juju.app.bean.json;

/**
 * 项目名称：juju
 * 类描述：登陆响应Bean
 * 创建人：gm
 * 日期：2016/2/17 18:34
 * 版本：V1.0.0
 */

public class GetVideoUrlsResBean extends BaseResBean {

    private String videoUrl1;

    private String videoUrl2;

    private String videoUrl3;

    public String getVideoUrl1() {
        return videoUrl1;
    }

    public void setVideoUrl1(String videoUrl1) {
        this.videoUrl1 = videoUrl1;
    }

    public String getVideoUrl2() {
        return videoUrl2;
    }

    public void setVideoUrl2(String videoUrl2) {
        this.videoUrl2 = videoUrl2;
    }

    public String getVideoUrl3() {
        return videoUrl3;
    }

    public void setVideoUrl3(String videoUrl3) {
        this.videoUrl3 = videoUrl3;
    }
}
