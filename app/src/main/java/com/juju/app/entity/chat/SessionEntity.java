package com.juju.app.entity.chat;


import com.juju.app.entity.base.BaseEntity;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.utils.StringUtils;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.UUID;


@Table(name = "session", onCreated = "CREATE UNIQUE INDEX index_session_session_key ON session(session_key);")
public class SessionEntity extends BaseEntity {

    /** Not-null value. */
    @Column(name = "session_key")
    private String sessionKey;

    @Column(name = "peer_id")
    private String peerId;

    @Column(name = "peer_type")
    private int peerType;

    @Column(name = "latest_msg_type")
    private int latestMsgType;

    @Column(name = "latest_msg_id")
    private int latestMsgId;

    /** Not-null value. */
    @Column(name = "latest_msg_data")
    private String latestMsgData;

    @Column(name = "talk_id")
    private String talkId;

    @Column(name = "created")
    private Long created;

    @Column(name = "updated")
    private Long updated;





    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public SessionEntity() {
    }

    public SessionEntity(Long localId) {
        this.localId = localId;
    }

    public SessionEntity(Long localId, String id, String sessionKey, String peerId, int peerType,
                         int latestMsgType, int latestMsgId, String latestMsgData, String talkId,
                         Long created, Long updated) {
        this.localId = localId;
        this.sessionKey = sessionKey;
        this.peerId = peerId;
        this.peerType = peerType;
        this.latestMsgType = latestMsgType;
        this.latestMsgId = latestMsgId;
        this.latestMsgData = latestMsgData;
        this.talkId = talkId;
        this.created = created;
        this.updated = updated;
    }


    /** Not-null value. */
    public String getSessionKey() {
        return sessionKey;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public int getPeerType() {
        return peerType;
    }

    public void setPeerType(int peerType) {
        this.peerType = peerType;
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

    /** Not-null value. */
    public String getLatestMsgData() {
        return latestMsgData;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setLatestMsgData(String latestMsgData) {
        this.latestMsgData = latestMsgData;
    }

    public String getTalkId() {
        return talkId;
    }

    public void setTalkId(String talkId) {
        this.talkId = talkId;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }


    // KEEP METHODS - put your custom methods here
    public String buildSessionKey(){
        if(peerType <=0 || StringUtils.isBlank(peerId)){
            throw new IllegalArgumentException(
                    "SessionEntity buildSessionKey error,cause by some params <=0");
        }
        sessionKey = EntityChangeEngine.getSessionKey(peerId, peerType);
        return sessionKey;
    }
    // KEEP METHODS END

    /**
     * 更新Session会话
     * @return
     */
    public static SessionEntity build4Update(String peerId, int peerType, int latestMsgType,
                                             String latestMsgData, String talkId, Long created) {
        SessionEntity sessionEntity = new SessionEntity();
        sessionEntity.setId(UUID.randomUUID().toString());
        sessionEntity.setPeerId(peerId);
        sessionEntity.setPeerType(peerType);
        sessionEntity.buildSessionKey();
        sessionEntity.setLatestMsgType(latestMsgType);
        sessionEntity.setLatestMsgData(latestMsgData);
        sessionEntity.setUpdated(created);
        sessionEntity.setLatestMsgId(SequenceNumberMaker.getInstance().makelocalUniqueMsgId(created));
        return sessionEntity;
    }



}
