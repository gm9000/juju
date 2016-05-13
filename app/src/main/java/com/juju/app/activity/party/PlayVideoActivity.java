package com.juju.app.activity.party;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.juju.app.R;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.view.CustomDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;


@ContentView(R.layout.activity_play_video)
public class PlayVideoActivity extends BaseActivity{

    @ViewInject(R.id.video_play_win)
    private VideoView videoView;

    @ViewInject(R.id.img_play)
    private ImageView imgPlay;
    @ViewInject(R.id.img_change)
    private ImageView imgChange;

    private SurfaceView videoPlayView;

    @ViewInject(R.id.layout_right_menu)
    private RelativeLayout layoutRightMenu;
    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;
    @ViewInject(R.id.txt_discuss)
    private EditText txtDiscuss;
    @ViewInject(R.id.btn_send)
    private Button btnSend;
    @ViewInject(R.id.img_weixin)
    private ImageView imgWeixin;
    @ViewInject(R.id.img_pyq)
    private ImageView imgPyq;
    @ViewInject(R.id.img_weibo)
    private ImageView imgWeibo;
    @ViewInject(R.id.img_copy)
    private ImageView imgCopy;

    private boolean vieoIsPlaying;
    private Uri hlsUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListener();

        playVide();
    }

    private void initListener() {
    }

    private void initParam() {

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        hlsUri = Uri.parse(bundle.getString("videoUrl"));
    }

    private void initView() {
        videoPlayView = (SurfaceView)findViewById(R.id.video_play_view);
        videoPlayView.setVisibility(View.GONE);
        imgPlay.setVisibility(View.GONE);
        imgChange.setVisibility(View.GONE);

    }


    @Event(R.id.img_close)
    private void closeVideo(View view){
        stopPlay();
        ActivityUtil.finish(this);
    }

    private void playVide() {
        MediaController mc = new MediaController(this);
        videoView.setMediaController(mc);
        mc.setMediaPlayer(videoView);
        videoView.setVideoURI(hlsUri);
        videoView.start();
        vieoIsPlaying = true;

    }

    private void stopPlay() {
        videoView.stopPlayback();
        vieoIsPlaying = false;
    }
}
