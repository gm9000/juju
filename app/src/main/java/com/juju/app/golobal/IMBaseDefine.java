package com.juju.app.golobal;

import com.juju.app.event.notify.ApplyInGroupEvent;
import com.juju.app.event.notify.DiscussNotifyEvent;
import com.juju.app.event.notify.ExitGroupEvent;
import com.juju.app.event.notify.InviteInGroupEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.notify.LiveEnterNotifyEvent;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.event.notify.LocationReportEvent;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.PlanVoteEvent;
import com.juju.app.event.notify.RemoveGroupEvent;
import com.juju.app.event.notify.SeizeNotifyEvent;

/**
 * 项目名称：juju
 * 类描述：聊天基础定义
 * 创建人：gm
 * 日期：2016/6/16 11:34
 * 版本：V1.0.0
 */
public final class IMBaseDefine {

    private IMBaseDefine() {
    }

    //群组变更类型
    public enum GroupModifyType {
        GROUP_MODIFY_TYPE_ADD,
        GROUP_MODIFY_TYPE_DEL;
    }

    /**
     * 消息枚举
     */
    public enum MsgType {
        MSG_TEXT {
            @Override
            public String code() {
                return "8000";
            }

            @Override
            public String desc() {
                return "文本消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        },
        MSG_IMAGE {
            @Override
            public String code() {
                return "8001";
            }

            @Override
            public String desc() {
                return "图片消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        },

        MSG_AUDIO {
            @Override
            public String code() {
                return "8003";
            }

            @Override
            public String desc() {
                return "短语音消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        },
        MSG_RECORD {
            @Override
            public String code() {
                return "8005";
            }

            @Override
            public String desc() {
                return "录音消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        },
        MSG_VIDEO {
            @Override
            public String code() {
                return "8007";
            }

            @Override
            public String desc() {
                return "短视频消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        },
        MSG_COMPLEX {
            @Override
            public String code() {
                return "8009";
            }

            @Override
            public String desc() {
                return "复合消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        };


        public abstract String code();

        public abstract String desc();

        public abstract Class getCls();


        public static MsgType getInstanceByCode(String code) {
            for (MsgType msgType : MsgType.values()) {
                if (msgType.code().equals(code)) {
                    return msgType;
                }
            }
            return null;
        }

    }

    //消息通知枚举
    public enum NotifyType {

        //群相关消息通知
        INVITE_USER {
            @Override
            public String code() {
                return "1001";
            }

            @Override
            public String desc() {
                return "加群邀请通知";
            }

            @Override
            public Class getCls() {
                return InviteUserEvent.InviteUserBean.class;
            }

            @Override
            public int MsgType() {
                return DBConstant.MSG_TYPE_SINGLE_NOTIFY;
            }

        },
        INVITE_PROCESS {
            @Override
            public String code() {
                return "1003";
            }

            @Override
            public String desc() {
                return "邀请消息处理通知";
            }

            @Override
            public Class getCls() {
                return InviteGroupNotifyResBean.class;
            }

            @Override
            public int MsgType() {
                return DBConstant.MSG_TYPE_SINGLE_NOTIFY;
            }


        },
        APPLY_JOIN {
            @Override
            public String code() {
                return "1005";
            }

            @Override
            public String desc() {
                return "申请加群通知";
            }

            @Override
            public Class getCls() {
                return null;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_SINGLE_NOTIFY;
            }

        },
        APPLY_PROCESS {
            @Override
            public String code() {
                return "1007";
            }

            @Override
            public String desc() {
                return "申请加群审核通知";
            }

            @Override
            public Class getCls() {
                return null;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_SINGLE_NOTIFY;
            }
        },
        APPLY_IN_GROUP {
            @Override
            public String code() {
                return "1009";
            }

            @Override
            public String desc() {
                return "申请方式加入群组通知";
            }

            @Override
            public Class getCls() {
                return ApplyInGroupEvent.ApplyInGroupBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },
        INVITE_IN_GROUP {
            @Override
            public String code() {
                return "1011";
            }

            @Override
            public String desc() {
                return "邀请方式加入群组通知";
            }

            @Override
            public Class getCls() {
                return InviteInGroupEvent.InviteInGroupBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },
        REMOVE_GROUP {
            @Override
            public String code() {
                return "1013";
            }

            @Override
            public String desc() {
                return "移除群组通知";
            }

            @Override
            public Class getCls() {
                return RemoveGroupEvent.RemoveGroupBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_SINGLE_NOTIFY;
            }
        },
        EXIT_GROUP {
            @Override
            public String code() {
                return "1015";
            }

            @Override
            public String desc() {
                return "退出群组通知";
            }

            @Override
            public Class getCls() {
                return ExitGroupEvent.ExitGroupBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },
        MASTER_TRANSFER {
            @Override
            public String code() {
                return "1017";
            }

            @Override
            public String desc() {
                return "群主转让通知";
            }

            @Override
            public Class getCls() {
                return MasterTransferEvent.MasterTransferBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        //聚会相关消息通知
        PARTY_RECRUIT {
            @Override
            public String code() {
                return "2001";
            }

            @Override
            public String desc() {
                return "聚会发起通知";
            }

            @Override
            public Class getCls() {
                return PartyNotifyEvent.PartyNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        PARTY_CANCEL {
            @Override
            public String code() {
                return "2003";
            }

            @Override
            public String desc() {
                return "聚会取消通知";
            }

            @Override
            public Class getCls() {
                return PartyNotifyEvent.PartyNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        PLAN_VOTE {
            @Override
            public String code() {
                return "2005";
            }

            @Override
            public String desc() {
                return "方案投票通知";
            }

            @Override
            public Class getCls() {
                return PlanVoteEvent.PlanVoteBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        PARTY_CONFIRM {
            @Override
            public String code() {
                return "2007";
            }

            @Override
            public String desc() {
                return "聚会启动通知";
            }

            @Override
            public Class getCls() {
                return PartyNotifyEvent.PartyNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        PARTY_END {
            @Override
            public String code() {
                return "2009";
            }

            @Override
            public String desc() {
                return "聚会结束通知";
            }

            @Override
            public Class getCls() {
                return PartyNotifyEvent.PartyNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        //地图位置相关消息通知
        LOCATION_REPORT {
            @Override
            public String code() {
                return "3001";
            }

            @Override
            public String desc() {
                return "位置报告";
            }

            @Override
            public Class getCls() {
                return LocationReportEvent.LocationReportBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }


        },

        //直播相关消息通知
        LIVE_START {
            @Override
            public String code() {
                return "4001";
            }

            @Override
            public String desc() {
                return "直播开始通知";
            }

            @Override
            public Class getCls() {
                return LiveNotifyEvent.LiveNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        LIVE_STOP {
            @Override
            public String code() {
                return "4003";
            }

            @Override
            public String desc() {
                return "直播结束通知";
            }

            @Override
            public Class getCls() {
                return LiveNotifyEvent.LiveNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        LIVE_CAPTURE {
            @Override
            public String code() {
                return "4005";
            }

            @Override
            public String desc() {
                return "直播截屏通知";
            }

            @Override
            public Class getCls() {
                return null;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        LIVE_DISCUSS {
            @Override
            public String code() {
                return "4007";
            }

            @Override
            public String desc() {
                return "直播评论";
            }

            @Override
            public Class getCls() {
                return DiscussNotifyEvent.DiscussNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        LIVE_RELAY {
            @Override
            public String code() {
                return "4009";
            }

            @Override
            public String desc() {
                return "直播接力";
            }

            @Override
            public Class getCls() {
                return SeizeNotifyEvent.SeizeNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        RELAY_COUNT {
            @Override
            public String code() {
                return "4011";
            }

            @Override
            public String desc() {
                return "接力计数";
            }

            @Override
            public Class getCls() {
                return SeizeNotifyEvent.SeizeNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_SINGLE_NOTIFY;
            }
        },


        LIVE_RELAY_START {
            @Override
            public String code() {
                return "4013";
            }

            @Override
            public String desc() {
                return "直播接力开始";
            }

            @Override
            public Class getCls() {
                return SeizeNotifyEvent.SeizeNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },

        LIVE_ENTER {
            @Override
            public String code() {
                return "4015";
            }

            @Override
            public String desc() {
                return "直播进入/离开";
            }

            @Override
            public Class getCls() {
                return LiveEnterNotifyEvent.LiveEnterNotifyBean.class;
            }

            public int MsgType() {
                return DBConstant.MSG_TYPE_GROUP_NOTIFY;
            }
        },;

        public abstract String code();

        public abstract String desc();

        public abstract Class getCls();

        public abstract int MsgType();

        public static NotifyType getInstanceByCode(String code) {
            for (NotifyType notifyType : NotifyType.values()) {
                if (notifyType.code().equals(code)) {
                    return notifyType;
                }
            }
            return null;
        }

    }

    //命名空间枚举
    public enum NameSpaceType {
        MESSAGE {
            @Override
            public String value() {
                return "com:jlm:message";
            }
        },
        NOTIFY {
            @Override
            public String value() {
                return "com:jlm:notify";
            }
        };

        public abstract String value();
    }


    //加群邀请回复Bean
    public static class InviteGroupNotifyResBean {
        public String code;
        public String groupId;
        public String groupName;
        public String userNo;
        //需要冗余此字段（系统通知使用）
        public String userName;

        //0：拒绝 1: 加入
        public int status;


        public static InviteGroupNotifyResBean valueOf(String code, String groupId, String groupName,
                                                       String userNo, String userName, int status) {
            InviteGroupNotifyResBean bean = new InviteGroupNotifyResBean();
            bean.code = code;
            bean.groupId = groupId;
            bean.groupName = groupName;
            bean.userNo = userNo;
            bean.userName = userName;
            bean.status = status;
            return bean;
        }
    }


}
