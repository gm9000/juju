package com.juju.app.entity.chat;


import com.juju.app.entity.base.BaseEntity;
import com.juju.app.helper.chat.EntityChangeEngine;

import org.xutils.db.annotation.Column;

/**
 *
 * 聊天对象抽象类  may be user/group
 */
public abstract class PeerEntity extends BaseEntity {


    /**
     * 聊天对象ID（聚聚号或群组）
     */

    @Column(name = "peer_id")
    protected String peerId;

    @Column(name = "main_name")
    protected String mainName;

    @Column(name = "avatar")
    protected String avatar;

    @Column(name = "created")
    protected int created;

    @Column(name = "updated")
    protected int updated;

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getMainName() {
        return mainName;
    }

    public void setMainName(String mainName) {
        this.mainName = mainName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    // peer就能生成sessionKey
    public String getSessionKey(){
       return EntityChangeEngine.getSessionKey(peerId, getType());
    }

    public abstract int getType();
}
