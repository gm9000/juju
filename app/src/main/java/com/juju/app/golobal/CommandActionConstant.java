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
                return Integer.parseInt("0010");
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
         * 获取群组详情
         */
        JOININGROUP {
            @Override
            public int code() {
                return Integer.parseInt("0018");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/joinInGroup";
            }
        },

        GETEXISTUSERS {
            @Override
            public int code() {
                return Integer.parseInt("0019");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/getExistUsers";            }
        },

        INVITEUSER {
            @Override
            public int code() {
                return Integer.parseInt("0020");
            }

            @Override
            public String url() {
                return HttpConstants.getUserUrl()+"/inviteUser";            }
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
