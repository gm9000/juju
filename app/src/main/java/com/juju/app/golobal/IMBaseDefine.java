package com.juju.app.golobal;

/**
 * 项目名称：juju
 * 类描述：聊天基础定义
 * 创建人：gm
 * 日期：2016/6/16 11:34
 * 版本：V1.0.0
 */
public final class IMBaseDefine {

    private IMBaseDefine() {}

    //群组变更类型
    public enum GroupModifyType {
        GROUP_MODIFY_TYPE_ADD,
        GROUP_MODIFY_TYPE_DEL;
    }

    //消息枚举
    public enum NotifyType {
        NORMAL_MESSAGE {
            @Override
            public String code() {
                return "0000";
            }

            @Override
            public String desc() {
                return "普通消息";
            }

            @Override
            public Class getCls() {
                return null;
            }

        },
        INVITE_GROUP_NOTIFY_REQ {
            @Override
            public String code() {
                return "0001";
            }

            @Override
            public String desc() {
                return "加群邀请通知";
            }

            @Override
            public Class getCls() {
                return InviteGroupNotifyReqBean.class;
            }

        },
        INVITE_GROUP_NOTIFY_RES {
            @Override
            public String code() {
                return "0002";
            }

            @Override
            public String desc() {
                return "加群邀请通知";
            }

            @Override
            public Class getCls() {
                return InviteGroupNotifyResBean.class;
            }

        },
        VOTE_PARTY_NOTIFY {
            @Override
            public String code() {
                return "0003";
            }

            @Override
            public String desc() {
                return "聚会投票通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        COUNT_VOTE_NOTIFY {
            @Override
            public String code() {
                return "0004";
            }

            @Override
            public String desc() {
                return "聚会投票计数通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        SHARE_URL_NOTIFY {
            @Override
            public String code() {
                return "0005";
            }

            @Override
            public String desc() {
                return "分享群组直播地址通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        LIVE_COUNTDOWN_NOTIFY {
            @Override
            public String code() {
                return "0006";
            }

            @Override
            public String desc() {
                return "抢播倒计时通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        LIVE_CLICKNUM_NOTIFY {
            @Override
            public String code() {
                return "0007";
            }

            @Override
            public String desc() {
                return "抢播点击数上报";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        LIVE_CONTINUE_NOTIFY {
            @Override
            public String code() {
                return "0008";
            }

            @Override
            public String desc() {
                return "续播通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        LIVE_SWITCH_NOTIFY {
            @Override
            public String code() {
                return "0009";
            }

            @Override
            public String desc() {
                return "切换倒计时通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        },
        LIVE_COMMENT {
            @Override
            public String code() {
                return "0010";
            }

            @Override
            public String desc() {
                return "直播评论通知";
            }

            @Override
            public Class getCls() {
                return null;
            }
        };

        public static NotifyType getInstanceByCode(String code) {
            for(NotifyType notifyType : NotifyType.values()) {
                if(notifyType.code().equals(code)) {
                    return notifyType;
                }
            }
            return null;
        }

        public abstract String code();
        public abstract String desc();
        public abstract Class getCls();


    }

    //命名空间枚举
    public enum NameSpaceType {
        NOTIFYMESSAGE {
            @Override
            public String value() {
                return "com:jlm:notifymessage";
            }
        };
        public abstract String value();
    }



    //封装消息通知对象，防止多处定义json串
    //加群邀请通知Bean
    public static class InviteGroupNotifyReqBean {
        public String code;
        public String groupId;
        public String groupName;
        public String userNo;
        //需要冗余此字段（系统通知使用）
        public String userName;
        
//        //0: 开始 1：通过 2: 拒绝
//        public int status;

        public static InviteGroupNotifyReqBean valueOf(String code, String groupId, String groupName,
                                                    String userNo, String userName) {
            InviteGroupNotifyReqBean bean = new InviteGroupNotifyReqBean();
            bean.code = code;
            bean.groupId = groupId;
            bean.groupName = groupName;
            bean.userNo = userNo;
            bean.userName = userName;
            return bean;
        }
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
