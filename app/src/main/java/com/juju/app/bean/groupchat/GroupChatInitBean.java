package com.juju.app.bean.groupchat;

import com.juju.app.entity.http.Group;

/**
 * 项目名称：juju
 * 类描述：群聊初始化Bean
 * 创建人：gm
 * 日期：2016/2/22 12:16
 * 版本：V1.0.0
 */
public class GroupChatInitBean {

    /**
     * 群组信息
     */
    private Group group;

    /**
     * 消息状态：送达 已读 失败
     */
    private String state;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间
     */
    private String time;

    /**
     * 消息总数
     */
    private String total;

    public GroupChatInitBean() {

    }

    public GroupChatInitBean(Group group, String state, String content,
                             String time, String total) {
        this.group = group;
        this.state = state;
        this.content = content;
        this.time = time;
        this.total = total;
    }


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }
}
