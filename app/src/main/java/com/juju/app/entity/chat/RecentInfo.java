package com.juju.app.entity.chat;


import com.juju.app.entity.User;
import com.juju.app.golobal.DBConstant;
import com.juju.app.service.im.manager.IMContactManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 最新消息
 */
public class RecentInfo {
    /**sessionEntity*/
    private String sessionKey;
    private String peerId;
    private int sessionType;
    private int latestMsgType;
    private int latestMsgId;
    private String latestMsgData;
    private Long updateTime;

    /**unreadEntity*/
    private int unReadCnt;

    /**group/userEntity*/
    private String name;
    private List<String> avatar;

    /**是否置顶*/
    private boolean isTop = false;
    /**是否屏蔽信息*/
    private boolean isForbidden = false;

    private GroupEntity groupEntity;


    public RecentInfo(){}
//    public RecentInfo(SessionEntity sessionEntity, UserEntity entity, UnreadEntity unreadEntity){
//        sessionKey = sessionEntity.getSessionKey();
//        peerId = sessionEntity.getPeerId();
//        sessionType = DBConstant.SESSION_TYPE_SINGLE;
//        latestMsgType = sessionEntity.getLatestMsgType();
//        latestMsgId = sessionEntity.getLatestMsgId();
//        latestMsgData = sessionEntity.getLatestMsgData();
//        updateTime = sessionEntity.getUpdated();
//
//        if(unreadEntity !=null)
//        unReadCnt = unreadEntity.getUnReadCnt();
//
//        if(entity != null){
//            name = entity.getMainName();
//            ArrayList<String> avatarList = new ArrayList<>();
//            avatarList.add(entity.getAvatar());
//            avatar = avatarList;
//        }
//    }


    public RecentInfo(SessionEntity sessionEntity,
                      GroupEntity groupEntity, UnreadEntity unreadEntity){

        if(sessionEntity != null) {
            latestMsgType = sessionEntity.getLatestMsgType();
            latestMsgId = sessionEntity.getLatestMsgId();
            latestMsgData = sessionEntity.getLatestMsgData();
            updateTime = sessionEntity.getUpdated();
        } else {
            //按组创建时间
            if(groupEntity != null) {
                updateTime = groupEntity.getCreated();
            } else {
                updateTime = 0l;
            }

        }

        if(unreadEntity !=null)
            unReadCnt = unreadEntity.getUnReadCnt();

        if(groupEntity !=null) {
            this.groupEntity = groupEntity;

            sessionKey =  groupEntity.getSessionKey();
            peerId = groupEntity.getPeerId();
            sessionType = DBConstant.SESSION_TYPE_GROUP;

            ArrayList<String>  avatarList = new ArrayList<>();
            name = groupEntity.getMainName();

            // 免打扰的设定
            int status = groupEntity.getStatus();
            if (status == DBConstant.GROUP_STATUS_SHIELD){
                isForbidden = true;
            }

            ArrayList<String> list =  new ArrayList<String>();
            list.addAll(groupEntity.getlistGroupMemberIds());

            for(String userNo : list){
                User entity = IMContactManager.instance().findContact(userNo);
                if(entity != null){
                    avatarList.add(entity.getAvatar());
                }
                if(avatarList.size() >= 9){
                    break;
                }
            }
            avatar = avatarList;
        }
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public int getSessionType() {
        return sessionType;
    }

    public void setSessionType(int sessionType) {
        this.sessionType = sessionType;
    }

    public int getLatestMsgType() {
        return latestMsgType;
    }

    public void setLatestMsgType(int latestMsgType) {
        this.latestMsgType = latestMsgType;
    }

    public int getLatestMsgId() {
        return latestMsgId;
    }

    public void setLatestMsgId(int latestMsgId) {
        this.latestMsgId = latestMsgId;
    }

    public String getLatestMsgData() {
        return latestMsgData;
    }

    public void setLatestMsgData(String latestMsgData) {
        this.latestMsgData = latestMsgData;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public int getUnReadCnt() {
        return unReadCnt;
    }

    public void setUnReadCnt(int unReadCnt) {
        this.unReadCnt = unReadCnt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAvatar() {
        return avatar;
    }

    public void setAvatar(List<String> avatar) {
        this.avatar = avatar;
    }

    public boolean isTop() {
        return isTop;
    }
    public boolean isForbidden()
    {
        return isForbidden;
    }

    public void setTop(boolean isTop) {
        this.isTop = isTop;
    }

    public void setForbidden(boolean isForbidden)
    {
        this.isForbidden = isForbidden;
    }

    public GroupEntity getGroupEntity() {
        return groupEntity;
    }
}
