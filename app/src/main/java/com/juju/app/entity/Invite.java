package com.juju.app.entity;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.entity.chat.OtherMessageEntity;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Date;

/**
 * 邀请实体
 * 邀请不需要验证
 */
@Table(name = "invite", onCreated = "CREATE UNIQUE INDEX index_invite_id ON invite(id)")
public class Invite extends BaseEntity {
    private static Logger logger = Logger.getLogger(Invite.class);

    @Column(name = "user_no")
    private String userNo;

    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "time")
    private Date time;

    //  0：申请  1：加入 2：拒绝
    @Column(name = "status")
    private int status;


    //  0：邀请   1：被邀请
    @Column(name = "flag")
    private int flag;


    @Column(name = "group_id")
    private String groupId;

    @Column(name = "group_name")
    private String groupName;

    //邀请码
//    @Column(name = "invite_code")
//    private String inviteCode;

    //消息状态
    @Column(name = "msg_status")
    private int msgStatus;

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

//    public String getInviteCode() {
//        return inviteCode;
//    }
//
//    public void setInviteCode(String inviteCode) {
//        this.inviteCode = inviteCode;
//    }

    public int getMsgStatus() {
        return msgStatus;
    }

    public void setMsgStatus(int msgStatus) {
        this.msgStatus = msgStatus;
    }




    //加群邀请发送
    public static Invite buildInviteReq4Send(OtherMessageEntity otherMessageEntity) {
        Invite invite = null;
        InviteUserEvent.InviteUserBean inviteUserBean = (InviteUserEvent.InviteUserBean)
                JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
                        IMBaseDefine.NotifyType.INVITE_USER.getCls());
        if(inviteUserBean != null) {
            invite = new Invite();
            invite.setId(otherMessageEntity.getId());
            String toId = otherMessageEntity.getToId();
            if(toId.indexOf("@") >= 0) {
                invite.setUserNo(toId.split("@")[0]);
            } else {
                invite.setUserNo(toId);
            }
//            invite.setInviteCode(inviteUserBean.code);
            invite.setNickName(inviteUserBean.nickName);
            invite.setGroupId(inviteUserBean.groupId);
            invite.setGroupName(inviteUserBean.groupName);
            invite.setFlag(0);
            //请求加入群聊，不处理状态
//            invite.setStatus(0);
            invite.setTime(new Date(otherMessageEntity.getCreated()));
            invite.setMsgStatus(MessageConstant.MSG_SENDING);
        } else {
            logger.e("buildInviteReq4Send#inviteGroupNotifyReqBean is null");
        }
        return invite;
    }



    //加群邀请发送确认
    public static Invite buildInviteReq4SendOnAck(Invite dbEntity, long time) {
        dbEntity.setMsgStatus(MessageConstant.MSG_SUCCESS);
        dbEntity.setTime(new Date(time));
        return dbEntity;
    }


    //构建被邀请信息
    public static Invite buildInviteReq4Recv(OtherMessageEntity otherMessageEntity) {
        Invite invite = null;
        InviteUserEvent.InviteUserBean inviteGroupNotifyBean = (InviteUserEvent.InviteUserBean)
                JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
                        IMBaseDefine.NotifyType.INVITE_USER.getCls());
        if(inviteGroupNotifyBean != null) {
            invite = new Invite();
            invite.setId(otherMessageEntity.getId());
            String fromId = otherMessageEntity.getFromId();
            if(fromId.indexOf("@") >= 0) {
                invite.setUserNo(fromId.split("@")[0]);
            } else {
                invite.setUserNo(fromId);
            }
//            invite.setInviteCode(inviteGroupNotifyBean.code);
            invite.setNickName(inviteGroupNotifyBean.nickName);
            invite.setGroupId(inviteGroupNotifyBean.groupId);
            invite.setGroupName(inviteGroupNotifyBean.groupName);
            invite.setFlag(1);
            //请求加入群聊，不处理状态
//            invite.setStatus(0);
            invite.setTime(new Date(otherMessageEntity.getUpdated()));
            invite.setMsgStatus(MessageConstant.MSG_SUCCESS);
        } else {
            logger.e("buildInvite4Recv#inviteGroupNotifyReqBean is null");
        }
        return invite;
    }

//    //加群、拒绝群发送
//    public static Invite buildInviteRes4Send(Invite dbEntity, OtherMessageEntity otherMessageEntity) {
//        IMBaseDefine.InviteGroupNotifyResBean inviteGroupNotifyResBean = (IMBaseDefine.InviteGroupNotifyResBean)
//                JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
//                        IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES.getCls());
//        if(inviteGroupNotifyResBean != null) {
//            //拒绝
//            if(inviteGroupNotifyResBean.status == 0) {
//                dbEntity.setStatus(2);
//            }
//            //同意
//            else {
//                dbEntity.setStatus(1);
//            }
//            dbEntity.setTime(new Date(otherMessageEntity.getCreated()));
//            dbEntity.setMsgStatus(MessageConstant.MSG_SENDING);
//        } else {
//            logger.e("buildInviteReq4Send#inviteGroupNotifyReqBean is null");
//        }
//        return dbEntity;
//    }
//
//    //加群、拒绝群发送回复
//    public static Invite buildInviteRes4SendOnAck(Invite dbEntity, long time) {
//        buildInviteReq4SendOnAck(dbEntity, time);
//        return dbEntity;
//    }
//
//
//    //加群、拒绝群响应
//    public static Invite buildInviteRes4Recv(Invite dbEntity, OtherMessageEntity otherMessageEntity) {
//        IMBaseDefine.InviteGroupNotifyResBean inviteGroupNotifyResBean = (IMBaseDefine.InviteGroupNotifyResBean)
//                JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
//                        IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES.getCls());
//        if(inviteGroupNotifyResBean != null) {
//            //拒绝
//            if(inviteGroupNotifyResBean.status == 0) {
//                dbEntity.setStatus(2);
//            }
//            //同意
//            else {
//                dbEntity.setStatus(1);
//            }
//            dbEntity.setTime(new Date(otherMessageEntity.getUpdated()));
//            dbEntity.setMsgStatus(MessageConstant.MSG_SUCCESS);
//        } else {
//            logger.e("buildInviteReq4Send#inviteGroupNotifyReqBean is null");
//        }
//        return dbEntity;
//    }

}
