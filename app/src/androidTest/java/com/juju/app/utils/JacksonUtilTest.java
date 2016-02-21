package com.juju.app.utils;

import android.test.InstrumentationTestCase;

/**
 * Created by PDCAR on 2016/2/15.
 */
public class JacksonUtilTest extends InstrumentationTestCase {

    public void test2() {
        User user = new User(1, "gm", 31);
        System.out.println(JacksonUtil.turnObj2String(user));

    }
}
