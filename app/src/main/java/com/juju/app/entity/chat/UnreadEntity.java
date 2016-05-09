package com.juju.app.entity.chat;


import android.support.annotation.IdRes;

import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.utils.StringUtils;
import com.lidroid.xutils.db.annotation.Id;
import com.lidroid.xutils.db.annotation.Table;
import com.lidroid.xutils.db.annotation.Transient;

/**
 * 未读session实体
 */
@Table(name = "unread_message", execAfterTableCreated = "CREATE UNIQUE INDEX index_unread_message_session_key " +
        "ON unread_message(session_key);")
public class UnreadEntity {


    @Id(column = "session_key")
    private String sessionKey;

    @Id(column = "peer_id")
    private String peerId;

    @Id(column = "session_type")
    private int sessionType;

    @Id(column = "un_read_cnt")
    private int unReadCnt;

    @Id(column = "latest_msg_id")
    private int laststMsgId;

    @Id(column = "latest_msg_data")
    private String latestMsgData;

    @Id(column = "created")
    private long created;

    @Transient
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
