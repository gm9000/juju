package com.juju.app.bean.groupchat;

import com.juju.app.R;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.golobal.Constants;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群聊初始化Bean
 * 创建人：gm
 * 日期：2016/2/22 12:16
 * 版本：V1.0.0
 */
public class GroupChatInitBean {

    /**
     * 群聊会话ID
     */
    private String sessionId;

    /**
     * 群组信息
     */
    private GroupEntity group;

    /**
     * 群组图像
     */
    private String headImage;

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
    private int unReadCnt;

    private int defaultAvatar = R.id.iv_avatar1;


    /**是否置顶*/
    private boolean isTop = false;

    private Long updateTime;

    /**
     * 图像集合
     */
    private List<String> avatar;



    public GroupChatInitBean() {

    }

    public GroupChatInitBean(String sessionId, GroupEntity group, String content,
                             Long updateTime, int unReadCnt, List<String> avatar) {
        this.sessionId = sessionId;
        this.group = group;
        this.content = content;
        this.updateTime = updateTime;
        this.unReadCnt = unReadCnt;
        this.avatar = avatar;
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

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public int getUnReadCnt() {
        return unReadCnt;
    }

    public void setUnReadCnt(int unReadCnt) {
        this.unReadCnt = unReadCnt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setIsTop(boolean isTop) {
        this.isTop = isTop;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public List<String> getAvatar() {
        return avatar;
    }

    public void setAvatar(List<String> avatar) {
        this.avatar = avatar;
    }
}
