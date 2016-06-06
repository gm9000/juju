package com.juju.app.activity.chat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;

import org.xutils.view.annotation.ContentView;




@ContentView(R.layout.activity_group_qr)
@CreateUI(showTopView = true)
public class GroupQrActivity extends BaseActivity implements CreateUIHelper {


    @Override
    public void loadData() {

    }

    @Override
    public void initView() {
        setTopTitle(R.string.group_qr_code);
        showTopLeftAll(R.string.chat_detail, 0);
    }
}
