package com.juju.app.biz;

import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.TextMessage;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：文本聊天记录
 * 创建人：gm
 * 日期：2016/4/14 14:29
 * 版本：V1.0.0
 */
public interface MessageDao {



    /**
     *******************************************扩展接口***************************************
     */

    /**
     * 查询历史消息
     * @param sessionKey
     * @param lastMsgId
     * @param lastCreateTime
     * @param count
     * @return
     */
    public List<MessageEntity> findHistoryMsgs(String sessionKey, int lastMsgId,
                                               long lastCreateTime, int count);


    /**
     * 获取最后回话的时间，便于获取联系人列表变化
     * 问题: 本地消息发送失败，依旧会更新session的时间 [存在会话、不存在的会话]
     * 本质上还是最后一条成功消息的时间
     * @return
     */
    public long getSessionLastTime();
}
