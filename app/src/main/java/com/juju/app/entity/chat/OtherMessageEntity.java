package com.juju.app.entity.chat;

import com.juju.app.entity.base.BaseEntity;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.StringUtils;

import org.jivesoftware.smack.packet.Message;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 项目名称：juju
 * 类描述：其他消息
 * 创建人：gm
 * 日期：2016/6/17 15:10
 * 版本：V1.0.0
 */
@Table(name = "other_message", onCreated = "CREATE UNIQUE INDEX index_other_message_id " +
        "ON other_message(id);")
public class OtherMessageEntity extends BaseEntity {

    /**
     * 自定义类型 参考IMBaseDefine.NotifyType
     */
    @Column(name = "notify_type")
    private String notifyType;


    /**
     * 消息发送人ID
     */
    @Column(name = "from_id")
    private String fromId;

    /**
     * 消息接收人
     */
    @Column(name = "to_id")
    private String toId;


    /** Not-null value. */

    /**
     * 消息类容
     */
    @Column(name = "content")
    private String content;

    /**
     * 消息类型(文本、声音、图片)
     */
    @Column(name = "msg_type")
    private int msgType;


    @Column(name = "status")
    private int status;

    /**
     * 创建时间
     */
    @Column(name = "created")
    private Long created;

    /**
     * 更新时间
     */
    @Column(name = "updated")
    private Long updated;

    // KEEP FIELDS - put your custom fields here


    public OtherMessageEntity() {
    }

    public OtherMessageEntity(Long id) {
        this.localId = id;
    }


    public String getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    /** Not-null value. */
    public String getContent() {
        return content;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setContent(String content) {
        this.content = content;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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


    @Override
    public String toString() {
        return "OtherMessageEntity{" +
                "id=" + id +
                "localId=" + localId +
                ", notifyType=" + notifyType +
                ", fromId=" + fromId +
                ", toId=" + toId +
                ", content='" + content + '\'' +
                ", msgType=" + msgType +
                ", status=" + status +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtherMessageEntity that = (OtherMessageEntity) o;

        if (msgType != that.msgType) return false;
        if (status != that.status) return false;
        if (notifyType != null ? !notifyType.equals(that.notifyType) : that.notifyType != null)
            return false;
        if (fromId != null ? !fromId.equals(that.fromId) : that.fromId != null) return false;
        if (toId != null ? !toId.equals(that.toId) : that.toId != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (created != null ? !created.equals(that.created) : that.created != null) return false;
        return updated != null ? updated.equals(that.updated) : that.updated == null;

    }

    @Override
    public int hashCode() {
        int result = notifyType != null ? notifyType.hashCode() : 0;
        result = 31 * result + (fromId != null ? fromId.hashCode() : 0);
        result = 31 * result + (toId != null ? toId.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + msgType;
        result = 31 * result + status;
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (updated != null ? updated.hashCode() : 0);
        return result;
    }

    /**
     * 获取会话的sessionId
     * @param isSend
     * @return
     */
    public String getPeerId(boolean isSend){
        if(isSend){
            /**自己发出去的*/
            return toId;
        }else{
            /**接受到的*/
            switch (msgType){
                case DBConstant.MSG_TYPE_SINGLE_TEXT:
                case DBConstant.MSG_TYPE_SINGLE_AUDIO:
                    return String.valueOf(fromId);
                case DBConstant.MSG_TYPE_GROUP_TEXT:
                case DBConstant.MSG_TYPE_GROUP_AUDIO:
                    return toId;
                default:
                    return toId;
            }
        }
    }

    public byte[] getSendContent(){
        return null;
    }


    public boolean isSend(String loginId){
        boolean isSend = (loginId.equals(fromId)) ? true : false;
        return isSend;
    }

    public static OtherMessageEntity buildMessage4Send(IMBaseDefine.NotifyType notifyType,
                                                Message message, String uuid) {
        OtherMessageEntity entity = new OtherMessageEntity();
        entity.setNotifyType(notifyType.code());
        entity.setId(uuid);
        entity.setStatus(MessageConstant.MSG_SENDING);
        entity.setCreated(DateUtil.getCurTimeMillis());
        entity.setUpdated(0l);
        entity.setContent(message.getBody());
        entity.setFromId(message.getFrom());
        entity.setToId(message.getTo());
        //目前只有此类型，后期可扩展
        entity.setMsgType(DBConstant.MSG_TYPE_SINGLE_TEXT);
        return entity;
    }

    public static OtherMessageEntity buildMessage4SendOnAck(OtherMessageEntity dbEntity, long time) {
        dbEntity.setStatus(MessageConstant.MSG_SUCCESS);
        dbEntity.setCreated(time);
        dbEntity.setUpdated(time);
        return dbEntity;
    }


    public static OtherMessageEntity buildMessage4Recv(IMBaseDefine.NotifyType notifyType,
                                                Message message, String uuid) {
        OtherMessageEntity entity = new OtherMessageEntity();
        entity.setId(uuid);
        entity.setNotifyType(notifyType.code());
        entity.setStatus(MessageConstant.MSG_SUCCESS);
        entity.setCreated(Long.parseLong(message.getThread()));
        entity.setUpdated(Long.parseLong(message.getThread()));
        entity.setContent(message.getBody());
        if(StringUtils.isNotBlank(message.getFrom())
                && message.getFrom().split("/").length >=2) {
            entity.setFromId(message.getFrom().split("/")[0]);
        } else {
            entity.setFromId(message.getFrom());
        }
        if(StringUtils.isNotBlank(message.getTo())
                && message.getTo().split("/").length >= 2) {
            entity.setToId(message.getTo().split("/")[0]);
        } else {
            entity.setToId(message.getTo());
        }
        //目前只有此类型，后期可扩展
        entity.setMsgType(DBConstant.MSG_TYPE_SINGLE_TEXT);
        return entity;
    }




}
