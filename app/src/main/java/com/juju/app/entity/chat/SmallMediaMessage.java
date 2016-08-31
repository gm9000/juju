package com.juju.app.entity.chat;


import android.graphics.Bitmap;
import android.util.Base64;

import com.juju.app.entity.base.MessageEntity;
import com.juju.app.event.SmallMediaEvent;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.view.imagezoom.utils.BitmapUtils;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import org.apache.commons.lang.math.NumberUtils;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 项目名称：juju
 * 类描述：图片信息
 * 创建人：gm
 * 日期：2016/7/21 17:44
 * 版本：V1.0.0
 */
public class SmallMediaMessage extends MessageEntity implements Serializable {

    /**本地保存的path*/
    private String path = "";

    /**小视频的网络地址*/
    private String url = "";

    private String thumbnailLocalPath = "";
    /**
     * 截图地址
     */
    private String thumbnailLocalUrl = "";

    private int duration;

    private int size;

    private int loadStatus;

    //上传或下载百分百， 此值依赖于loadStatus
    private int progress;

    //存储图片消息
    private static java.util.HashMap<String, SmallMediaMessage> smallMediaMessageMap = new java.util.HashMap<>();
    private static ArrayList<SmallMediaMessage> smallMediaList = null;
    /**
     * 添加一条图片消息
     * @param msg
     */
    public static synchronized void addToSmallMediaMessageList(SmallMediaMessage msg){
        try {
            if(msg !=null && StringUtils.isNotBlank(msg.getId()))
            {
                smallMediaMessageMap.put(msg.getId(), msg);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 获取图片列表
     * @return
     */
    public static ArrayList<SmallMediaMessage> getImageMessageList(){
        smallMediaList = new ArrayList<>();
        java.util.Iterator it = smallMediaMessageMap.keySet().iterator();
        while (it.hasNext()) {
            smallMediaList.add(smallMediaMessageMap.get(it.next()));
        }
        Collections.sort(smallMediaList, new Comparator<SmallMediaMessage>(){
            public int compare(SmallMediaMessage image1, SmallMediaMessage image2) {
                Long a =  image1.getUpdated();
                Long b = image2.getUpdated();
                if(a.equals(b))
                {
                    return image2.getId().compareTo(image1.getId());
                }
                // 升序
                //return a.compareTo(b);
                // 降序
                return b.compareTo(a);
            }
        });
        return smallMediaList;
    }

    /**
     * 清除图片列表
     */
    public static synchronized void clearImageMessageList(){
        smallMediaMessageMap.clear();
    }



    public SmallMediaMessage(){
        msgId = SequenceNumberMaker.getInstance().makelocalUniqueMsgId();
    }

    /**消息拆分的时候需要*/
    private SmallMediaMessage(MessageEntity entity){
        /**父类的id*/
         id =  entity.getId();
         localId = entity.getLocalId();
         msgId  = entity.getMsgId();
         fromId = entity.getFromId();
         toId   = entity.getToId();
         sessionKey = entity.getSessionKey();
         content=entity.getContent();
         msgType=entity.getMsgType();
         displayType=entity.getDisplayType();
         status = entity.getStatus();
         created = entity.getCreated();
         updated = entity.getUpdated();
    }

    /**接受到网络包，解析成本地的数据*/
    public static SmallMediaMessage parseFromNet(MessageEntity entity) throws JSONException {
        String strContent = entity.getContent();
        // 判断开头与结尾
        if (strContent.startsWith(MessageConstant.IMAGE_MSG_START)
                && strContent.endsWith(MessageConstant.IMAGE_MSG_END)) {
            // image message todo 字符串处理下
            SmallMediaMessage imageMessage = new SmallMediaMessage(entity);
            imageMessage.setDisplayType(DBConstant.SHOW_IMAGE_TYPE);
            String imageUrl = strContent.substring(MessageConstant.IMAGE_MSG_START.length());
            imageUrl = imageUrl.substring(0,imageUrl.indexOf(MessageConstant.IMAGE_MSG_END));

            /**抽离出来 或者用gson*/
            JSONObject extraContent = new JSONObject();
            extraContent.put("path","");
            extraContent.put("url",imageUrl);
            extraContent.put("loadStatus", MessageConstant.IMAGE_UNLOAD);
            String imageContent = extraContent.toString();
            imageMessage.setContent(imageContent);

            imageMessage.setUrl(imageUrl.isEmpty() ? null : imageUrl);
            imageMessage.setContent(strContent);
            imageMessage.setLoadStatus(MessageConstant.IMAGE_UNLOAD);
            imageMessage.setStatus(MessageConstant.MSG_SUCCESS);
            return imageMessage;
        }else{
            throw new RuntimeException("no image type,cause by [start,end] is wrong!");
        }
    }


    public static SmallMediaMessage parseFromDB(MessageEntity entity)  {
        if(entity.getDisplayType() != DBConstant.SHOW_SMALL_MEDIA_TYPE){
            throw new RuntimeException("#ImageMessage# parseFromDB,not SHOW_SMALL_MEDIA_TYPE");
        }
        SmallMediaMessage imageMessage = new SmallMediaMessage(entity);
        String originContent = entity.getContent();
        JSONObject extraContent;
        try {
            extraContent = new JSONObject(originContent);
            imageMessage.setPath(extraContent.getString("path"));
            imageMessage.setUrl(extraContent.getString("url"));
            imageMessage.setThumbnailLocalUrl(extraContent.getString("thumbnailLocalUrl"));
            imageMessage.setThumbnailLocalPath(extraContent.getString("thumbnailLocalPath"));
            int loadStatus = extraContent.getInt("loadStatus");

            //todo temp solution
            if(loadStatus == MessageConstant.IMAGE_LOADING){
                loadStatus = MessageConstant.IMAGE_UNLOAD;
            }
            imageMessage.setLoadStatus(loadStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return imageMessage;
    }

    // 消息页面，发送图片消息
    public static SmallMediaMessage buildForSend(SmallMediaEvent.SmallMediaItem item, UserEntity fromUser,
                                                 PeerEntity peerEntity){
        SmallMediaMessage msg = new SmallMediaMessage();
        if (new File(item.mVideoPath).exists()) {
            msg.setPath(item.mVideoPath);
        }
        if(new File(item.mPicPath).exists()) {
            msg.setThumbnailLocalPath(item.mPicPath);
        }

        // 将图片发送至服务器
        long nowTime = System.currentTimeMillis();
        msg.setFromId(fromUser.getPeerId());
        msg.setToId(peerEntity.getPeerId());
        msg.setCreated(nowTime);
        msg.setUpdated(nowTime);
        msg.setDisplayType(DBConstant.SHOW_SMALL_MEDIA_TYPE);
        // content 自动生成的
        int peerType = peerEntity.getType();
        int msgType = peerType == DBConstant.SESSION_TYPE_GROUP ? DBConstant.MSG_TYPE_GROUP_TEXT :
                DBConstant.MSG_TYPE_SINGLE_TEXT;
        msg.setMsgType(msgType);
        msg.setStatus(MessageConstant.MSG_SENDING);
        msg.setLoadStatus(MessageConstant.SMALL_MEDIA_UNLOAD);
        msg.buildSessionKey(true);
        return msg;
    }



    public static SmallMediaMessage buildForReceive(Message message, String fromId, String toId)
            throws JSONException, UnsupportedEncodingException {
        SmallMediaMessage imageMessage = new SmallMediaMessage();
        long nowTime = 0;
        if(NumberUtils.isNumber(message.getThread())) {
            nowTime = Long.parseLong(message.getThread());
            imageMessage.setMsgId(SequenceNumberMaker.getInstance()
                    .makelocalUniqueMsgId(nowTime));
        }
        imageMessage.setId(message.getStanzaId());
        imageMessage.setFromId(fromId);
        imageMessage.setToId(toId);
        int msgType = DBConstant.MSG_TYPE_GROUP_TEXT;
        imageMessage.setMsgType(msgType);
        imageMessage.setStatus(MessageConstant.MSG_SUCCESS);
        imageMessage.setDisplayType(DBConstant.SHOW_IMAGE_TYPE);
        imageMessage.setCreated(nowTime);
        imageMessage.setUpdated(nowTime);
        imageMessage.buildSessionKey(false);
        if(StringUtils.isBlank(message.getBody())) {
//            imageMessage.setReadStatus(MessageConstant.AUDIO_READED);
//            imageMessage.setAudioPath("");
//            imageMessage.setAudiolength(0);

        } else {
            MsgSmallMediaContent msgSmallMediaContent = JacksonUtil.turnString2Obj(message.getBody(),
                    MsgSmallMediaContent.class);
            byte[] imageContent = Base64.decode(msgSmallMediaContent.capture, Base64.DEFAULT);
            imageMessage.setPath("");
            imageMessage.setUrl(msgSmallMediaContent.url);
            imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_SUCCESS);
        }
        JSONObject extraContent = new JSONObject();
        extraContent.put("path",imageMessage.getPath());
        extraContent.put("url",imageMessage.getUrl());
        extraContent.put("loadStatus",imageMessage.getLoadStatus());
        String imageContent = extraContent.toString();
        imageMessage.setContent(imageContent);
        return imageMessage;
    }

    /**
     * Not-null value.
     */
    @Override
    public String getContent() {
        JSONObject extraContent = new JSONObject();
        try {
            extraContent.put("path",path);
            extraContent.put("url",url);
            extraContent.put("thumbnailLocalPath", thumbnailLocalPath);
            extraContent.put("thumbnailLocalUrl", thumbnailLocalUrl);
            extraContent.put("loadStatus",loadStatus);
            String imageContent = extraContent.toString();
            return imageContent;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getSendContent() {
        Bitmap bitmap = ImageLoaderUtil.getImageLoaderInstance()
                .loadImageSync("file://"+thumbnailLocalPath, new ImageSize(480, 480));
        String small =  BitmapUtils.bitmapToBase64(bitmap);
        //格式==============
        //        {
//            “width”:320,
//            “height”:240,
//            “capture”:”视频缩略图Base64加密值”,
//            “duration”:30,  // 时长，单位：秒
//            “size”:2346,   // 大小，单位byte
//            “url”:”视频片段地址”
//        }
        //    int width, int height, String capture, int duration,
//        int size, String url
        MsgSmallMediaContent msgSmallMediaContent = MsgSmallMediaContent
                .valueOf(480, 480, small, duration, size, url);
        String sendContent = JacksonUtil.turnObj2String(msgSmallMediaContent);
        System.out.println("getSendContent -> content.length():"+sendContent.length());
        return sendContent;
    }

    /**-----------------------set/get------------------------*/
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailLocalUrl() {
        return thumbnailLocalUrl;
    }

    public void setThumbnailLocalUrl(String thumbnailLocalUrl) {
        this.thumbnailLocalUrl = thumbnailLocalUrl;
    }

    public int getLoadStatus() {
        return loadStatus;
    }

    public void setLoadStatus(int loadStatus) {
        this.loadStatus = loadStatus;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getThumbnailLocalPath() {
        return thumbnailLocalPath;
    }

    public void setThumbnailLocalPath(String thumbnailLocalPath) {
        this.thumbnailLocalPath = thumbnailLocalPath;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public static class MsgSmallMediaContent {

//        {
//            “width”:320,
//            “height”:240,
//            “capture”:”视频缩略图Base64加密值”,
//            “duration”:30,  // 时长，单位：秒
//            “size”:2346,   // 大小，单位byte
//            “url”:”视频片段地址”
//        }

        //宽度
        public int width;
        //高度
        public int height;

        public String capture;

        public int duration;
        //size
        public int size;

        //视频片段地址
        public String url;



        public static MsgSmallMediaContent valueOf(int width, int height, String capture, int duration,
                                              int size, String url) {
            MsgSmallMediaContent msgSmallMediaContent = new MsgSmallMediaContent();
            msgSmallMediaContent.width = width;
            msgSmallMediaContent.height = height;
            msgSmallMediaContent.capture = capture;
            msgSmallMediaContent.duration = duration;
            msgSmallMediaContent.size = size;
            msgSmallMediaContent.url = url;
            return msgSmallMediaContent;
        }
    }
}
