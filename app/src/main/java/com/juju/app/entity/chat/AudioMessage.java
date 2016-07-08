package com.juju.app.entity.chat;

import android.text.TextUtils;
import android.util.Base64;


import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.utils.CommonUtil;
import com.juju.app.utils.FileUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.json.JSONUtils;

import org.apache.commons.lang.math.NumberUtils;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import okio.ByteString;


public class AudioMessage extends MessageEntity implements Serializable {

    private Logger logger = Logger.getLogger(AudioMessage.class);

    private String audioPath = "";
    private int audiolength =0 ;
    private int readStatus = MessageConstant.AUDIO_UNREAD;

    public AudioMessage(){
        msgId = SequenceNumberMaker.getInstance().makelocalUniqueMsgId();
    }

    private AudioMessage(MessageEntity entity){
        // 父类主键
        id =  entity.getId();
        msgId  = entity.getMsgId();
        fromId = entity.getFromId();
        toId   = entity.getToId();
        content=entity.getContent();
        msgType=entity.getMsgType();
        sessionKey = entity.getSessionKey();
        displayType=entity.getDisplayType();
        status = entity.getStatus();
        created = entity.getCreated();
        updated = entity.getUpdated();
    }


    public static AudioMessage parseFromDB(MessageEntity entity)  {
        if(entity.getDisplayType() != DBConstant.SHOW_AUDIO_TYPE){
           throw new RuntimeException("#AudioMessage# parseFromDB,not SHOW_AUDIO_TYPE");
        }
        AudioMessage audioMessage = new AudioMessage(entity);
        String originContent = entity.getContent();
        JSONObject extraContent = null;
        try {
            extraContent = new JSONObject(originContent);
            audioMessage.setAudioPath(JSONUtils.getString(extraContent, "audioPath"));
            audioMessage.setAudiolength(JSONUtils.getInt(extraContent,"audiolength", 0));
            audioMessage.setReadStatus(extraContent.getInt("readStatus"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return audioMessage;
    }

    public static AudioMessage buildForSend(float audioLen, String audioSavePath,
                                            UserEntity fromUser, PeerEntity peerEntity){
        int tLen = (int) (audioLen + 0.5);
        tLen = tLen < 1 ? 1 : tLen;
        if (tLen < audioLen) {
            ++tLen;
        }

        long nowTime = System.currentTimeMillis();
        AudioMessage audioMessage = new AudioMessage();
        audioMessage.setFromId(fromUser.getPeerId());
        audioMessage.setToId(peerEntity.getPeerId());
        audioMessage.setCreated(nowTime);
        audioMessage.setUpdated(nowTime);
        int peerType = peerEntity.getType();
        int msgType = peerType == DBConstant.SESSION_TYPE_GROUP ? DBConstant.MSG_TYPE_GROUP_AUDIO :
                DBConstant.MSG_TYPE_SINGLE_AUDIO;
        audioMessage.setMsgType(msgType);

        audioMessage.setAudioPath(audioSavePath);
        audioMessage.setAudiolength(tLen);
        audioMessage.setReadStatus(MessageConstant.AUDIO_READED);
        audioMessage.setDisplayType(DBConstant.SHOW_AUDIO_TYPE);
        audioMessage.setStatus(MessageConstant.MSG_SENDING);
        audioMessage.buildSessionKey(true);
        return audioMessage;
    }


    /**
     * Not-null value.
     * DB 存数解析的时候需要
     */
    @Override
    public String getContent() {
        JSONObject extraContent = new JSONObject();
        try {
            extraContent.put("audioPath",audioPath);
            extraContent.put("audiolength",audiolength);
            extraContent.put("readStatus",readStatus);
            String audioContent = extraContent.toString();
            return audioContent;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public String getSendContent() {
        byte[] bytes = null;
        try {
            bytes = FileUtil.toByteArray(audioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.d("getSendContent -> audioPath:%s", audioPath);
        String sendContent = "";
        String content = "";
        if (bytes != null && bytes.length >0) {
            if(bytes.length == 171) {
                 logger.d("getSendContent bytes length is 171");
                 return getSendContent();
            }
            int contentLength = bytes.length;
            byte[] byteAduioContent = new byte[contentLength];
            System.arraycopy(bytes, 0, byteAduioContent, 0, contentLength);
            logger.d("getSendContent -> byteAduioContent.length():%d", byteAduioContent.length);
            content = Base64.encodeToString(bytes, Base64.DEFAULT);
            MsgAudioContent msgAudioContent = MsgAudioContent
                    .valueOf(contentLength, audiolength, content);
            sendContent = JacksonUtil.turnObj2String(msgAudioContent);
            logger.d("getSendContent -> content.length():%d", content.length());
        }
        return sendContent;
    }


    /***-------------------------------set/get----------------------------------*/
    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public int getAudiolength() {
        return audiolength;
    }

    public void setAudiolength(int audiolength) {
        this.audiolength = audiolength;
    }

    public int getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(int readStatus) {
        this.readStatus = readStatus;
    }



    public static AudioMessage buildForReceive(Message message, String fromId, String toId)
            throws JSONException, UnsupportedEncodingException {
        AudioMessage audioMessage = new AudioMessage();
        long nowTime = 0;
        if(NumberUtils.isNumber(message.getThread())) {
            nowTime = Long.parseLong(message.getThread());
            audioMessage.setMsgId(SequenceNumberMaker.getInstance()
                    .makelocalUniqueMsgId(nowTime));
        }
        audioMessage.setFromId(fromId);
        audioMessage.setToId(toId);
        int msgType = DBConstant.MSG_TYPE_GROUP_AUDIO;
        audioMessage.setMsgType(msgType);
        audioMessage.setStatus(MessageConstant.MSG_SUCCESS);
        audioMessage.setReadStatus(MessageConstant.AUDIO_UNREAD);
        audioMessage.setDisplayType(DBConstant.SHOW_AUDIO_TYPE);
        audioMessage.setCreated(nowTime);
        audioMessage.setUpdated(nowTime);
        audioMessage.buildSessionKey(false);
        if(StringUtils.isBlank(message.getBody())) {
            audioMessage.setReadStatus(MessageConstant.AUDIO_READED);
            audioMessage.setAudioPath("");
            audioMessage.setAudiolength(0);
        } else {
            MsgAudioContent msgAudioContent = JacksonUtil.turnString2Obj(message.getBody(),
                    MsgAudioContent.class);
            byte[] audioContent = Base64.decode(msgAudioContent.content, Base64.DEFAULT);
            String audioSavePath = FileUtil.saveAudioResourceToFile(audioContent, audioMessage.getFromId());
            audioMessage.setAudiolength(msgAudioContent.duration);
            audioMessage.setAudioPath(audioSavePath);
        }
        JSONObject extraContent = new JSONObject();
        extraContent.put("audioPath",audioMessage.getAudioPath());
        extraContent.put("audiolength",audioMessage.getAudiolength());
        extraContent.put("readStatus",audioMessage.getReadStatus());
        String audioContent = extraContent.toString();
        audioMessage.setContent(audioContent);
        return audioMessage;
    }


    public static class MsgAudioContent {

        //短语音大小（byte）
        public long size;
        //短语音时长
        public int duration;
        //语音内容 base64字符串
        public String content;

        public static MsgAudioContent valueOf(long size, int duration, String content) {
            MsgAudioContent msgAudioContent = new MsgAudioContent();
            msgAudioContent.size = size;
            msgAudioContent.duration = duration;
            msgAudioContent.content = content;
            return msgAudioContent;
        }
    }

}
