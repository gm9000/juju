package com.juju.app.activity.chat;

import android.app.Activity;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;


/**
 * 项目名称：juju
 * 类描述：创建群聊
 * 创建人：gm   
 * 日期：2016/5/5 15:04
 * 版本：V1.0.0
 */
@ContentView(R.layout.activity_add_group_chat)
public class AddGroupChatActivity extends Activity  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ViewUtils.inject(this);
    }
}
