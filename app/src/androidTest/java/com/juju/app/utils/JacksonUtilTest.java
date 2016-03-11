package com.juju.app.utils;

import android.test.InstrumentationTestCase;

import com.juju.app.bean.json.RegistResBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PDCAR on 2016/2/15.
 */
public class JacksonUtilTest extends InstrumentationTestCase {

    public void test2() {
//        User user = new User(1, "gm", 31);
//        System.out.println(JacksonUtil.turnObj2String(user));
        RegistResBean bean = new RegistResBean();
        bean.setUserNo("9000001");

        List<RegistResBean.InviteInfo> inviteInfos = new ArrayList<RegistResBean.InviteInfo>();

        RegistResBean.InviteInfo inviteInfo = new RegistResBean.InviteInfo();
        inviteInfo.setGroupId("1");
        inviteInfo.setGroupName("开发者联盟");
        inviteInfo.setInviteCode("110");
        inviteInfo.setInviteNickName("gm");

        inviteInfos.add(inviteInfo);

        System.out.println(JacksonUtil.turnObj2String(bean));


    }
}
