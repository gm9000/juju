package com.juju.app.bean.json;

/**
 * 项目名称：juju
 * 类描述：Json响应公共Bean
 * 创建人：gm
 * 日期：2016/2/17 19:30
 * 版本：V1.0.0
 */
public class BaseResBean {

    /**
     * 返回消息状态
     */
    private int status;

    private String description;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
