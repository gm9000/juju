package com.juju.app.entity.base;


import com.juju.app.entity.chat.AudioMessage;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.chat.EntityChangeEngine;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;


@Table(name = "message", onCreated = "CREATE UNIQUE INDEX index_message_id ON message(id);")
public class MessageEntity extends BaseEntity implements java.io.Serializable {

	/**
	 * 消息ID　APP维护
	 */
	@Column(name = "msg_id")
	protected int msgId;


	/**
	 * 消息发送人ID
	 */
	@Column(name = "from_id")
	protected String fromId;

	/**
	 * 消息接收人
	 */
	@Column(name = "to_id")
	protected String toId;

	/** Not-null value. */
	/**
	 * sessionType + "_" + peerId;  消息类型_消息发送人ID
	 */
	@Column(name = "session_key")
	protected String sessionKey;

	/** Not-null value. */

	/**
	 * 消息类容
	 */
	@Column(name = "content")
	protected String content;

	/**
	 * 消息类型
	 */
	@Column(name = "msg_type")
	protected int msgType;

	/**
	 * 显示类型
	 */
	@Column(name = "display_type")
	protected int displayType;


	@Column(name = "status")
	protected int status;

	/**
	 * 创建时间
	 */
	@Column(name = "created")
	protected Long created;

	/**
	 * 更新时间
	 */
	@Column(name = "updated")
	protected Long updated;

	// KEEP FIELDS - put your custom fields here


	/**
	 * 此属性为临时使用
	 */
	protected boolean isGIfEmo;
	// KEEP FIELDS END

	public MessageEntity() {
	}

	public MessageEntity(Long id) {
		this.localId = id;
	}

	public MessageEntity(Long localId, String id, int msgId, String fromId, String toId, String sessionKey,
						 String content, int msgType, int displayType,
						 int status, Long created, Long updated) {
		this.localId = localId;
		this.id = id;
		this.msgId = msgId;
		this.fromId = fromId;
		this.toId = toId;
		this.sessionKey = sessionKey;
		this.content = content;
		this.msgType = msgType;
		this.displayType = displayType;
		this.status = status;
		this.created = created;
		this.updated = updated;
	}


	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
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
	public String getSessionKey() {
		return sessionKey;
	}

	/** Not-null value; ensure this value is available before it is saved to the database. */
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
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

	public int getDisplayType() {
		return displayType;
	}

	public void setDisplayType(int displayType) {
		this.displayType = displayType;
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

	// KEEP METHODS - put your custom methods here
	/**
	 * -----根据自身状态判断的---------
	 */
	public int getSessionType() {
		switch (msgType) {
			case  DBConstant.MSG_TYPE_SINGLE_TEXT:
			case  DBConstant.MSG_TYPE_SINGLE_AUDIO:
			case DBConstant.MSG_TYPE_SINGLE_NOTIFY:
				return DBConstant.SESSION_TYPE_SINGLE;
			case DBConstant.MSG_TYPE_GROUP_TEXT:
			case DBConstant.MSG_TYPE_GROUP_AUDIO:
			case DBConstant.MSG_TYPE_GROUP_NOTIFY:
				return DBConstant.SESSION_TYPE_GROUP;
			default:
				return DBConstant.SESSION_TYPE_SINGLE;
		}
	}


	public String getMessageDisplay() {
		switch (displayType){
			case DBConstant.SHOW_AUDIO_TYPE:
				return DBConstant.DISPLAY_FOR_AUDIO;
			case DBConstant.SHOW_ORIGIN_TEXT_TYPE:
				return content;
			case DBConstant.SHOW_IMAGE_TYPE:
				return DBConstant.DISPLAY_FOR_IMAGE;
			case DBConstant.SHOW_MIX_TEXT:
				return DBConstant.DISPLAY_FOR_MIX;
			default:
				return DBConstant.DISPLAY_FOR_ERROR;
		}
	}

	@Override
	public String toString() {
		return "MessageEntity{" +
				"id=" + id +
				"localId=" + localId +
				", msgId=" + msgId +
				", sessionKey=" + sessionKey +
				", fromId=" + fromId +
				", toId=" + toId +
				", content='" + content + '\'' +
				", msgType=" + msgType +
				", displayType=" + displayType +
				", status=" + status +
				", created=" + created +
				", updated=" + updated +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MessageEntity)) return false;

		MessageEntity that = (MessageEntity) o;

		if (created != that.created) return false;
		if (displayType != that.displayType) return false;
		if (fromId != that.fromId) return false;
		if (msgId != that.msgId) return false;
		if (msgType != that.msgType) return false;
		if (status != that.status) return false;
		if (toId != that.toId) return false;
		if (updated != that.updated) return false;
		if (!content.equals(that.content)) return false;
		if (!id.equals(that.id)) return false;
		if (!sessionKey.equals(that.sessionKey)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + msgId;
		result = 31 * result + fromId.hashCode();
		result = 31 * result + toId.hashCode();
		result = 31 * result + sessionKey.hashCode();
		result = 31 * result + content.hashCode();
		result = 31 * result + msgType;
		result = 31 * result + displayType;
		result = 31 * result + status;
		result = 31 * result + created.hashCode();
		result = 31 * result + updated.hashCode();
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
			switch (getSessionType()){
				case DBConstant.SESSION_TYPE_SINGLE:
					return String.valueOf(fromId);
				case DBConstant.SESSION_TYPE_GROUP:
					return toId;
				default:
					return toId;
			}
		}
	}

	public String getSendContent(){
		return null;
	}

	public boolean isGIfEmo() {
		return isGIfEmo;
	}

	public void setGIfEmo(boolean isGIfEmo) {
		this.isGIfEmo = isGIfEmo;
	}

	public boolean isSend(String loginId){
		boolean isSend = (loginId.equals(fromId)) ? true : false;
		return isSend;
	}

	public String buildSessionKey(boolean isSend){
		int sessionType = getSessionType();
		String peerId = getPeerId(isSend);
		sessionKey = EntityChangeEngine.getSessionKey(peerId, sessionType);
		return sessionKey;
	}
	// KEEP METHODS END


	public MessageEntity clone() {
		MessageEntity entry;
		if(this instanceof AudioMessage) {
			entry = new MessageEntity(localId, id, msgId, fromId, toId,  sessionKey,
					getContent(),  msgType,  displayType, status,  created,  updated);
		} else {
			entry = new MessageEntity(localId, id, msgId, fromId, toId,  sessionKey,
					content,  msgType,  displayType, status,  created,  updated);
		}
		return entry;
	}
}
