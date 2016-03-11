package com.juju.app.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.juju.app.entity.MessageInfo;
import com.juju.app.golobal.Constants;
import com.juju.app.utils.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 项目名称：juju
 * 类描述：消息界面的公用业务逻辑处理
 * 创建人：gm
 * 日期：2016/3/3 17:47
 * 版本：V1.0.0
 */
public class MessageHelper {

    private static Logger logger = Logger.getLogger(MessageHelper.class);

    /**
     * @Description 生成消息对象
     * @param targetUserId
     * @param content
     * @return
     */
    public static MessageInfo obtainTextMessage(String targetUserId,
                                                String content) {
        logger.d("chat#text#generating text message, toId:%s, content:%s", targetUserId, content);

        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(targetUserId)) {
            return null;
        }

        MessageInfo msg = new MessageInfo();
        msg.setMsgContent(content);

        msg = setMsgBaseInfo(targetUserId, Constants.DISPLAY_TYPE_TEXT, msg);
//        CacheHub.getInstance().pushMsg(msg);
        return msg;
    }

    /**
     * @Description 设置消息的基本信息
     * @param userId
     * @param msgDisplayType
     * @param msg
     * @return
     */
    private static MessageInfo setMsgBaseInfo(String userId,
                                              int msgDisplayType, MessageInfo msg) {
        if (msg == null || TextUtils.isEmpty(userId)) {
            return null;
        }
//        msg.setMsgFromUserId(CacheHub.getInstance().getLoginUserId());
        msg.setIsSend(true);
        int createTime = (int) (System.currentTimeMillis() / 1000);
        msg.setMsgCreateTime(createTime);
        msg.setTargetId(userId);
        msg.setDisplayType(msgDisplayType);
        msg.setMsgType(Constants.MESSAGE_TYPE_TELETEXT); // 1语音或文本消息
        // 100 语音消息
        msg.setMsgLoadState(Constants.MESSAGE_STATE_LOADDING);
        msg.setMsgReadStatus(Constants.MESSAGE_ALREADY_READ);
        byte msgType = 1;
        msg.setMsgType(msgType);
        msg.setMsgAttachContent("");
//		int messageSendRequestNo = CacheHub.getInstance().obtainMsgId();
//		msg.setMsgId(messageSendRequestNo);
        return msg;
    }

}
