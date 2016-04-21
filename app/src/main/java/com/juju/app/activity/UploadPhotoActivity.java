package com.juju.app.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.juju.app.R;
import com.juju.app.ui.base.BaseActivity;
import com.lidroid.xutils.view.annotation.ContentView;

@ContentView(R.layout.activity_upload_photo)
public class UploadPhotoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loading();
    }
}
