package com.juju.app.entity.base;



import com.juju.app.entity.MessageInfo;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.ProtocolConstant;
import com.juju.app.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class MessageEntity {
	public int seqNo;
	public String fromId;
	public String toId;
	public int createTime;
	public byte type;
	public int msgLen;
	public byte[] msgData;
	public String attach;

	// non-meta members
	protected Logger logger = Logger.getLogger(MessageEntity.class);
	public String msgId;
	public String sessionId;
	public int sessionType = -1;

	public boolean isMy() {
		return fromId.equals("1");
}

	public void copy(MessageEntity anotherEntity) {
		seqNo = anotherEntity.seqNo;
		fromId = anotherEntity.fromId;
		toId = anotherEntity.toId;
		createTime = anotherEntity.createTime;
		type = anotherEntity.type;
		msgLen = anotherEntity.msgLen;
		msgData = anotherEntity.msgData;
		attach = anotherEntity.attach;
		generateMsgId();
		sessionId = anotherEntity.sessionId;
		sessionType = anotherEntity.sessionType;
	}

	private String getMsgDataDescription() {
		if (type == ProtocolConstant.MSG_TYPE_P2P_TEXT) {
			return new String(msgData);
		} else {
			return "";
		}
	}

	public boolean isTextType() {
		// return (msgType == ProtocolConstant.MSG_TYPE_GROUP_TEXT || msgType ==
		// ProtocolConstant.MSG_TYPE_P2P_TEXT);
		return msgInfo.getDisplayType() == Constants.DISPLAY_TYPE_TEXT;
	}

	public boolean isAudioType() {
		// return (msgType == ProtocolConstant.MSG_TYPE_GROUP_AUDIO || msgType
		// == ProtocolConstant.MSG_TYPE_P2P_AUDIO);
		return msgInfo.getDisplayType() == Constants.DISPLAY_TYPE_AUDIO;
	}

	public boolean isImage() {
		return msgInfo.getDisplayType() == Constants.DISPLAY_TYPE_IMAGE;
	}

	public String getText() {
		if (isTextType()) {
			return new String(msgData);
		} else {
			return "";
		}
	}

	public void generateMsgId(/* boolean sending */) {
		// logger.d("chat#generateMessageId -> sending:%s", sending);
		//
		// // unique session, unique time, seqNo+fromId
		// msgId = String.format("%s_%d_%d_%s", getSessionId(sending),
		// createTime,
		// seqNo, fromId);
		msgId = UUID.randomUUID().toString();
	}

	public void generateMsgIdIfEmpty(/* boolean sending */) {
		if (msgId == null || msgId.isEmpty()) {
			msgId = UUID.randomUUID().toString();
		}
	}

	public void generateSessionId(boolean sending) {
		logger.d("chat#generateSessionId sending:%s", sending);

		sessionId = getSessionId(sending);

		logger.d("chat#session id:%s", sessionId);
	}

	public void generateSessionType(int sessionType) {
		this.sessionType = sessionType;
	}

	public boolean isGroupMsg() {
		// todo eric consider flag &
		if (type == ProtocolConstant.MSG_TYPE_GROUP_AUDIO
				|| type == ProtocolConstant.MSG_TYPE_GROUP_TEXT) {
			return true;
		}

		return false;
	}

	public boolean isP2PMsg() {
		if (type == ProtocolConstant.MSG_TYPE_P2P_AUDIO
				|| type == ProtocolConstant.MSG_TYPE_P2P_TEXT) {
			return true;
		}

		return false;
	}

	@Override
	public String toString() {
		// todo eric make createtime readble
		// todo eric if the content is text, should i logging here
		// todo eric fix all warnings, like locale param in String.format
		return String.format("seqNo:%d,  fromId:%s, toId:%s, createTime:%d, msgType:%d, msgLen:%d, msgData:%s, attach:%s, msgId:%s", seqNo, fromId, toId, createTime, type, msgLen, getMsgDataDescription(), (attach == null)
				? ""
				: attach, (msgId == null) ? "" : msgId);
	}

	public String getSessionId(boolean sending) {
		if (type == ProtocolConstant.MSG_TYPE_P2P_TEXT
				|| type == ProtocolConstant.MSG_TYPE_P2P_AUDIO) {
			return sending ? toId : fromId;
		}

		if (type == ProtocolConstant.MSG_TYPE_GROUP_TEXT
				|| type == ProtocolConstant.MSG_TYPE_GROUP_AUDIO) {
			return toId;
		}

		logger.e("chat#getSessionId failed");

		return null;
	}

	public static String createAudioInfo(MessageInfo msgInfo) {
		JSONObject jo = new JSONObject();
		try {
			jo.put("path", msgInfo.getSavePath());
			jo.put("length", msgInfo.getPlayTime());
			jo.put("readStatus", msgInfo.getMsgReadStatus());
			return jo.toString();
		} catch (JSONException e) {
			Logger logger = Logger.getLogger(MessageEntity.class);
			logger.e("audio#createAudioInfo failed");
		}

		return "";
	}

	public static String createPicInfo(MessageInfo msgInfo) {
		Logger logger = Logger.getLogger(MessageEntity.class);
		logger.d("pic#createPicInfo getSavePath:%s", msgInfo.getSavePath());
		JSONObject jo = new JSONObject();
		try {
			String savePath = msgInfo.getSavePath();
			if (savePath == null) {
				savePath = "";
			}

			jo.put("path", savePath);

			String url = msgInfo.getUrl();
			if (url == null) {
				url = "";
			}
			
			logger.d("pic#save pic to db, path:%s, url:%s", savePath, url);

			jo.put("url", url);

			return jo.toString();
		} catch (JSONException e) {

			logger.e("pic#createPicInfo failed");
		}

		return "";
	}
	public static class AudioInfo {
		private String path;
		private int length;
		private int readStatus;
		
		public int getReadStatus() {
			return readStatus;
		}

		public void setReadStatus(int readStatus) {
			this.readStatus = readStatus;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public AudioInfo(String path, int length, int readStatus) {
			this.path = path;
			this.length = length;
			this.readStatus = readStatus;

			Logger.getLogger(MessageEntity.class).d("audio#path:%s, length:%d, readStatus", path, length, readStatus);
		}

		public static AudioInfo create(String info) {
			Logger logger = Logger.getLogger(AudioInfo.class);
			String path = "";
			int length = 0;
			int readStatus = Constants.MESSAGE_UNREAD;
			try {
				JSONObject jo = new JSONObject(info);
				
				path = jo.getString("path");
				length = jo.getInt("length");
				readStatus = jo.getInt("readStatus");

			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				logger.w("audio#createAudioInfo failed");
			}
			
			logger.d("audio#read audio info from db -> path:%s, length:%d, readStatus:%d", path, length, readStatus);
			return new AudioInfo(path, length, readStatus);
		}
	}

	public static class PicInfo {
		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		private String path = "";
		private String url = "";

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public PicInfo(String path, String url) {
			this.path = path;
			this.url = url;

			Logger.getLogger(PicInfo.class).d("pic#read picture content path:%s, url:%s", path, url);
		}

		public static PicInfo create(String info) {
			String path = "";
			String url = "";

			try {
				JSONObject jo = new JSONObject(info);
				
				url = (String) jo.get("url");
				path = (String) jo.get("path");
			} catch (JSONException e1) {
			}

			Logger.getLogger(PicInfo.class).d("pic#read pic info from db, url:%s, path:%s", url, path);
			return new PicInfo(path, url);
		}
	}

	public String getContent() {
		if (isTextType()) {
			return new String(msgData);
		} else if (isAudioType()) {
			return createAudioInfo(msgInfo);
		} else if (isImage()) {
			return createPicInfo(msgInfo);
		}

		return "";
	}

	// todo eric
	public MessageInfo msgInfo;

}
