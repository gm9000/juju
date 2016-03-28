package com.juju.app.activity.party;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.juju.app.R;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.ToastUtil;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;

import java.io.IOException;

@ContentView(R.layout.activity_play_video)
public class PlayVideoActivity extends AppCompatActivity implements View.OnClickListener{

    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.video_play_win)
    private VideoView videoView;

    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;

    private SurfaceView videoPlayView;

    @ViewInject(R.id.menu_play_icon)
    private ImageView playBtn;

    private boolean vieoIsPlaying;
    private Uri hlsUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.inject(this);
        initParam();
        initView();
        initListener();

        playVide();
    }

    private void initListener() {
        txt_left.setOnClickListener(this);
        img_back.setOnClickListener(this);
        img_right.setOnClickListener(this);
    }

    private void initParam() {

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        hlsUri = Uri.parse(bundle.getString("videoUrl"));
    }

    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.live);
        txt_title.setText("节目");

        videoPlayView = (SurfaceView)findViewById(R.id.video_play_view);
        videoPlayView.setVisibility(View.GONE);

//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ScreenUtil.ViewgetScreenWidth(this),ScreenUtil.ViewgetScreenWidth(this) * 9/16);
//        videoView.setLayoutParams(layoutParams);

    }

    public void onTabClicked(View view) throws IOException {
        switch (view.getId()) {
            case R.id.menu_capture:
                ToastUtil.showShortToast(this, "点击抓拍", 1);
                break;
            case R.id.menu_play:
                if (vieoIsPlaying) {
                    ToastUtil.showShortToast(this, "点击停止", 1);
                    //TODO  增加视频停止播放或者上传的业务处理
                    stopPlay();
                } else {
                    ToastUtil.showShortToast(this, "点击播放", 1);
                    //TODO  增加视频开始播放或者上传的业务处理;
                        playVide();
                }
                break;
            case R.id.menu_discuss:
                ToastUtil.showShortToast(this, "点击评论", 1);
                break;
        }
    }

    private void playVide() {
        MediaController mc = new MediaController(this);
        videoView.setMediaController(mc);
        mc.setMediaPlayer(videoView);
        videoView.setVideoURI(hlsUri);
        videoView.start();
        playBtn.setImageResource(R.mipmap.stop);
        vieoIsPlaying = true;

    }

    private void stopPlay() {
        videoView.stopPlayback();
        vieoIsPlaying = false;
        playBtn.setImageResource(R.mipmap.play);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_left:
                ActivityUtil.finish(this);
                break;
            case R.id.img_back:
                ActivityUtil.finish(this);
                break;
            case R.id.img_right:
                ToastUtil.showShortToast(this, "抢播", 1);
                break;
        }
    }
}
