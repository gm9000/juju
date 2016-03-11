package com.juju.app.bean.json;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：注册响应Bean
 * 创建人：gm
 * 日期：2016/3/11 14:59
 * 版本：V1.0.0
 */
public class RegistResBean extends BaseResBean {

    private String userNo;

    @JsonUnwrapped
    private List<InviteInfo> inviteInfo;


    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public List<InviteInfo> getInviteInfo() {
        return inviteInfo;
    }

    public void setInviteInfo(List<InviteInfo> inviteInfo) {
        this.inviteInfo = inviteInfo;
    }

    public static class InviteInfo {

        private String inviteNickName;
        private String inviteCode;
        private String groupId;
        private String groupName;

        public String getInviteNickName() {
            return inviteNickName;
        }

        public void setInviteNickName(String inviteNickName) {
            this.inviteNickName = inviteNickName;
        }

        public String getInviteCode() {
            return inviteCode;
        }

        public void setInviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
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
    }



}
