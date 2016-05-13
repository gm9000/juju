package com.juju.app.entity.chat;


import android.support.annotation.IdRes;

import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.utils.StringUtils;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;


/**
 * 未读session实体
 */
@Table(name = "unread_message", onCreated = "CREATE UNIQUE INDEX index_unread_message_session_key " +
        "ON unread_message(session_key);")
public class UnreadEntity {


    @Column(name = "session_key", isId = true, autoGen = false)
    private String sessionKey;

    @Column(name = "peer_id")
    private String peerId;

    @Column(name = "session_type")
    private int sessionType;

    @Column(name = "un_read_cnt")
    private int unReadCnt;

    @Column(name = "latest_msg_id")
    private int laststMsgId;

    @Column(name = "latest_msg_data")
    private String latestMsgData;

    @Column(name = "created")
    private long created;

    private boolean isForbidden = false;

//    @Transient
//    private String content;

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

    public int getUnReadCnt() {
        return unReadCnt;
    }

    public void setUnReadCnt(int unReadCnt) {
        this.unReadCnt = unReadCnt;
    }

    public int getLaststMsgId() {
        return laststMsgId;
    }

    public void setLaststMsgId(int laststMsgId) {
        this.laststMsgId = laststMsgId;
    }

    public String getLatestMsgData() {
        return latestMsgData;
    }

    public void setLatestMsgData(String latestMsgData) {
        this.latestMsgData = latestMsgData;
    }

    public boolean isForbidden() {
        return isForbidden;
    }

    public void setForbidden(boolean isForbidden) {
        this.isForbidden = isForbidden;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "UnreadEntity{" +
                "sessionKey='" + sessionKey + '\'' +
                ", peerId=" + peerId +
                ", sessionType=" + sessionType +
                ", unReadCnt=" + unReadCnt +
                ", created=" + created +
                ", laststMsgId=" + laststMsgId +
                ", latestMsgData='" + latestMsgData + '\'' +
                ", isForbidden=" + isForbidden +
                '}';
    }

    public String buildSessionKey(){
        if(sessionType <=0 || StringUtils.isBlank(peerId)){
            throw new IllegalArgumentException(
                    "SessionEntity buildSessionKey error,cause by some params <=0");
        }
        sessionKey = EntityChangeEngine.getSessionKey(peerId, sessionType);
        return sessionKey;
    }
}
