package com.juju.app.entity;



import com.juju.app.entity.base.MessageEntity;
import com.juju.app.golobal.Constants;
import com.juju.app.utils.Logger;

import java.io.Serializable;
import java.util.Date;


public class MessageInfo extends MessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return "MessageInfo [ownerId=" + ownerId + ", isSend=" + isSend
				+ ", msgFromUserId=" + msgFromUserId + ", msgFromName="
				+ msgFromName + ", msgFromUserNick=" + msgFromUserNick
				+ ", msgFromUserAvatar=" + msgFromUserAvatar + ", msgType="
				+ msgType + ", msgOverview=" + msgOverview + ", msgContent="
				+ msgContent + ", msgLoadState=" + msgLoadState + ", targetId="
				+ targetId + ", msgRenderType=" + msgRenderType
				+ ", msgAttachContent=" + msgAttachContent + ", savePath="
				+ savePath + ", url=" + url + ", displayType=" + displayType
				+ ", playTime=" + playTime + ", created=" + created
				+ ", updated=" + updated + ", readStatus=" + readStatus
				+ ", seqNo=" + seqNo + ", fromId=" + fromId + ", toId=" + toId
				+ ", createTime=" + createTime + ", type=" + type + ", msgLen="
				+ msgLen + ", attach=" + attach + ", msgId=" + msgId
				+ ", sessionId=" + sessionId + ", sessionType=" + sessionType
				+ ", resend=" + resend + "]";
	}

	protected String ownerId = "1000000001"; // 用户id
	private Boolean isSend = false; // 消息是发送还是接收
	protected int relateId; // 联系id
	private String msgFromUserId = ""; // 发送信息的用户id
	private String msgFromName = ""; // 发送信息的用户名
	private String msgFromUserNick = ""; // 发送信息的用户昵称
	private String msgFromUserAvatar = ""; // 用户头像URL链接
	private byte msgType = Constants.MESSAGE_TYPE_TELETEXT;// 1普通信息（或含图片），100表示语音（是否包含有图片使用占位符来判断）
	private String msgOverview = ""; // 消息内容概要 显示联系人最后一条历史消息时用
	private String msgContent = ""; // 消息内容
	private int msgLoadState = Constants.MESSAGE_STATE_FINISH_SUCCESSED;

	// private int msgId = -1; // 数据库中存储的消息唯一ID
	private int msgParentId = -1; // 每条消息的唯一ID，消息可能是图文混排，由对个msgId组合成一条消息,如果是单条消息则为默认值，否则为图文混排的第一条消息的ID
	private String targetId = ""; // 接收信息的用户id
	private byte msgRenderType;
	private String msgAttachContent = "";
	private String savePath = null; // 图片或语音保存路径
	private String url = null; // 图片或语音链接
	private int displayType = Constants.DISPLAY_TYPE_TEXT; // 消息显示类型，本地显示用
	private int playTime = 0; // 语音播放时长
	private byte[] audiocontent = null; // 语音本地缓存

	private int created = 0; // 创建时间
	private int updated = 0; // 更新时间
	private boolean resend = false;

	public boolean isResend() {
		return resend;
	}

	public void setResend(boolean resend) {
		this.resend = resend;
	}

	private int readStatus = Constants.MESSAGE_UNREAD;

	public MessageInfo() {
		// todo eric
		msgInfo = this;
	}

	public MessageInfo(MessageEntity msg) {
		msgInfo = this;

		seqNo = msg.seqNo;
		fromId = msg.fromId;
		toId = msg.toId;
		createTime = msg.createTime;
		type = msg.type;
		msgLen = msg.msgLen;
		msgData = msg.msgData;
		msg.attach = msg.attach;

		generateMsgId();
		generateSessionId(false);

		// setMsgId(messageId); // todo eric this is right?why i don't use
		setMsgFromUserId(msg.fromId);
		setTargetId(msg.toId);
		setMsgCreateTime(msg.createTime);
		setMsgType(msg.type);
		// todo eric
		// msgInfo.setMsgRenderType(msgRenderType);
		setMsgAttachContent(null);

		if (isAudioType()) {
			logger.d("chat#recv audio msg");
			setAudioContent(msg.msgData);
		} 
	}
	public boolean isMyMsg() {
		return true;
//		return msgFromUserId.equals(CacheHub.getInstance().getLoginUserId());
	}

	// @Override
	// public String toString() {
	// // TODO Auto-generated method stub
	// //return super.toString();
	//
	// //return
	// String.format("serialVersionUID:%d, ownerId:%s, isSend:%s, relateId:%d, msgFromUserId:%s, msgFromName:%s, msgFromUserNick:%s, msgFromUserAvatar:%s, msgType:%d, msgOverview:%s, ",
	// )
	// }

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	public int getRelateId() {
		return relateId;
	}

	public void setRelateId(int relateId) {
		this.relateId = relateId;
	}

	// public int getMsgId() {
	// return msgId;
	// }
	//
	// public void setMsgId(int msgId) {
	// this.msgId = msgId;
	// this.msgParentId = msgId;
	// }

	public int getMsgParentId() {
		return msgParentId;
	}

	public void setMsgParentId(int msgParentId) {
		this.msgParentId = msgParentId;
	}

	public int getDisplayType() {
		return displayType;
	}

	public void setDisplayType(int displayType) {
		this.displayType = displayType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
		toId = targetId;
	}

	public byte getMsgRenderType() {
		return msgRenderType;
	}

	public void setMsgRenderType(byte msgRenderType) {
		this.msgRenderType = msgRenderType;
	}

	public String getMsgAttachContent() {
		return msgAttachContent;
	}

	public void setMsgAttachContent(String msgAttachContent) {
		this.msgAttachContent = msgAttachContent;
	}

	public void setMsgType(byte msgType) {
		this.msgType = msgType;
		type = msgType;
	}

	public byte getMsgType() {
		return msgType;
	}

	public int getMsgLoadState() {
		return msgLoadState;
	}

	public void setMsgLoadState(int msgLoadState) {
		Logger.getLogger(MessageInfo.class).d(" setMsgLoadState  = "
				+ msgLoadState);
		this.msgLoadState = msgLoadState;
	}

	public String getMsgFromUserId() {
		return msgFromUserId;
	}

	public void setMsgFromUserId(String msgFromUserId) {
		this.msgFromUserId = msgFromUserId;
		fromId = msgFromUserId;
	}

	public String getMsgFromName() {
		return msgFromName;
	}

	public void setMsgFromName(String msgFromName) {
		this.msgFromName = msgFromName;
	}

	public String getMsgFromUserNick() {
		return msgFromUserNick;
	}

	public void setMsgFromUserNick(String msgFromUserNick) {
		this.msgFromUserNick = msgFromUserNick;
	}

	public String getMsgFromUserAvatar() {
		return msgFromUserAvatar;
	}

	public void setMsgFromUserAvatar(String msgFromUserAvatar) {
		this.msgFromUserAvatar = msgFromUserAvatar;
	}

	public Date getMsgCreateTime() {
		return new Date(Long.valueOf(created) * 1000);
	}

	public void setMsgCreateTime(int msgCreateTime) {
		this.created = msgCreateTime;
		this.createTime = msgCreateTime;
	}

	public String getMsgOverview() {
		switch (this.getDisplayType()) {
			case Constants.DISPLAY_TYPE_TEXT :
				msgOverview = this.getMsgContent(); // 文本消息用消息体
				break;
			case Constants.DISPLAY_TYPE_IMAGE :
				msgOverview = Constants.MSG_OVERVIEW_DISPLAY_TYPE_IMAGE;
				break;
			case Constants.DISPLAY_TYPE_AUDIO :
				msgOverview = Constants.MSG_OVERVIEW_DISPLAY_TYPE_AUDIO;
				break;
			default :
				msgOverview = Constants.MSG_OVERVIEW_DISPLAY_TYPE_OTHERS;
				break;
		}

		return msgOverview;
	}

	public void setMsgOverview(String msgOverview) {
		this.msgOverview = msgOverview;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;

		// msgData = msgContent.getBytes(Charset.forName("utf8"));
		// msgLen = msgData.length;
	}

	public int getPlayTime() {
		return playTime;
	}

	public void setPlayTime(int playTime) {
		this.playTime = playTime;
	}

	public int getCreated() {
		return created;
	}

	public void setCreated(int created) {
		this.created = created;
		createTime = created;
	}

	public int getUpdated() {
		return updated;
	}

	public void setUpdated(int updated) {
		this.updated = updated;
	}

	public byte[] getAudioContent() {
		return audiocontent;
	}

	public void setAudioContent(byte[] AudioContent) {
		audiocontent = AudioContent;
	}

	public void copyFromOtherMsgInfo(MessageInfo other) {
		if (other == null)
			return;

		this.relateId = other.relateId; // 联系id
		this.msgFromUserId = other.msgFromUserId; // 发送信息的用户id
		this.msgFromName = other.msgFromName; // 发送信息的用户名
		this.msgFromUserNick = other.msgFromUserNick; // 发送信息的用户昵称
		this.msgFromUserAvatar = other.msgFromUserAvatar; // 用户头像URL链接
		this.msgType = other.msgType;// 1普通信息（或含图片），100表示语音（是否包含有图片使用占位符来判断）
		this.msgOverview = other.msgOverview; // 消息内容概要 显示联系人最后一条历史消息时用
		this.msgContent = other.msgContent; // 消息内容
		this.msgLoadState = other.msgLoadState;
		this.msgId = other.msgId;
		this.targetId = other.targetId; // 接收信息的用户id
		this.msgRenderType = other.msgRenderType;
		this.msgAttachContent = other.msgAttachContent;
		this.savePath = other.savePath; // 图片或语音保存路径
		this.url = other.url; // 图片或语音链接
		this.displayType = other.displayType; // 消息显示类型，本地显示用
		this.playTime = other.playTime; // 语音播放时长
		this.audiocontent = other.audiocontent; // 语音本地缓存
		this.created = other.created; // 创建时间
		this.updated = other.updated; // 更新时间
		this.readStatus = other.readStatus;
		this.msgParentId = other.msgParentId;
	}

	public int getMsgReadStatus() {
		return readStatus;
	}

	public void setMsgReadStatus(int readStatus) {
		this.readStatus = readStatus;
	}

	/**
	 * @return the isSend
	 */
	public Boolean getIsSend() {
		return isSend;
	}

	/**
	 * @param isSend
	 *            the isSend to set
	 */
	public void setIsSend(Boolean isSend) {
		this.isSend = isSend;
	}
}
