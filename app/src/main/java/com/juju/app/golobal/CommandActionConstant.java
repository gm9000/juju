package com.juju.app.golobal;

import com.juju.app.config.*;
import com.juju.app.config.HttpConstants;
import com.juju.app.utils.HttpReqParamUtil;

import java.util.Map;

/**
 * 项目名称：juju
 * 类描述：协议操作常量
 * 创建人：gm
 * 日期：2016/6/18 13:13
 * 版本：V1.0.0
 */
public class CommandActionConstant {

    public enum HttpReqParam {

        /**
         * 获取用户详情
         */
        GETUSERINFO {
            @Override
            public int code() {
                return Integer.parseInt("0022");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getUserInfo";
            }
        },

        /**
         * 获取群组详情
         */
        GETGROUPINFO {
            @Override
            public int code() {
                return Integer.parseInt("0008");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getGroupInfo";
            }
        },

        /**
         * 加入群组
         */
        JOININGROUP {
            @Override
            public int code() {
                return Integer.parseInt("0012");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/joinInGroup";
            }
        },

        GETEXISTUSERS {
            @Override
            public int code() {
                return Integer.parseInt("0021");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getExistUsers";            }
        },

        //APP内部邀请加群
        INVITEUSER {
            @Override
            public int code() {
                return Integer.parseInt("0011");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/inviteUser";
            }
        },

        DELETEGROUPMEMBER {
            @Override
            public int code() {
                return Integer.parseInt("0015");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/deleteGroupMember";
            }
        },

        //群主转让
        UPDATEGROUPMASTER {
            @Override
            public int code() {
                return Integer.parseInt("0020");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/updateGroupMaster";
            }
        },

        //退出群组
        QUITGROUP {
            @Override
            public int code() {
                return Integer.parseInt("0015");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/quitGroup";
            }
        },

        //删除群组
        DELETEGROUP {
            @Override
            public int code() {
                return Integer.parseInt("0009");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/deleteGroup";
            }
        },

        //获取群组成员列表
        GETGROUPUSERS {
            @Override
            public int code() {
                return Integer.parseInt("0019");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getGroupUsers";
            }
        },

        //获取群组邀请码
        GETGROUPINVITECODE {
            @Override
            public int code() {
                return Integer.parseInt("0010");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getGroupInviteCode";
            }
        },

        //获取群组概况
        GETGROUPOUTLINE {
            @Override
            public int code() {
                return Integer.parseInt("0018");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getGroupOutline";
            }
        },

        /**
         * 获取聚会详情
         */
        GETPARTYINFO {
            @Override
            public int code() {
                return Integer.parseInt("0030");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getPartyInfo";
            }
        };



        public abstract int code();

        public abstract String url();

        public static HttpReqParam getInstance(int code) {
            for(HttpReqParam param : HttpReqParam.values()) {
                if(param.code() == code) {
                    return param;
                }
            }
            return null;
        }

//        public abstract Map<String, Object> buildMap(String keys, Object... objs);

    }
}
