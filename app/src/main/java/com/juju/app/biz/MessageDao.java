package com.juju.app.biz;

import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.TextMessage;
import com.lidroid.xutils.exception.DbException;

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
                                               int lastCreateTime, int count);


}
