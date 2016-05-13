package com.juju.app.activity.party;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.juju.app.R;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.view.CustomDialog;
import com.juju.app.utils.ToastUtil;
import com.rey.material.app.BottomSheetDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;


import java.lang.reflect.Field;

@ContentView(R.layout.activity_play_video)
public class PlayVideoActivity extends BaseActivity implements View.OnClickListener {

    @ViewInject(R.id.video_play_win)
    private VideoView videoView;

    @ViewInject(R.id.img_upload)
    private ImageView imgPlay;
    @ViewInject(R.id.img_change)
    private ImageView imgChange;
    @ViewInject(R.id.video_play_view)
    private SurfaceView videoPlayView;

    @ViewInject(R.id.layout_sliding)
    private SlidingPaneLayout layoutSliding;
    @ViewInject(R.id.layout_right_menu)
    private RelativeLayout layoutRightMenu;
    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;
    @ViewInject(R.id.txt_discuss)
    private EditText txtDiscuss;
    @ViewInject(R.id.btn_send)
    private Button btnSend;
    @ViewInject(R.id.img_share)
    private ImageView imgShare;
    private TextView txtWeixin;
    private TextView txtPyq;
    private TextView txtWeibo;
    private TextView txtCopy;
    private TextView txtCancel;

    private boolean vieoIsPlaying;
    private Uri hlsUri;

    private BottomSheetDialog shareDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListener();
        playVide();
    }

    private void initListener() {
        layoutSliding.setPanelSlideListener(new SliderListener());
    }

    private void initParam() {

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        hlsUri = Uri.parse(bundle.getString("videoUrl"));
    }

    private void initView() {
        videoPlayView.setVisibility(View.GONE);
        imgPlay.setVisibility(View.GONE);
        imgChange.setVisibility(View.GONE);

        try {
            Field f_overHang = SlidingPaneLayout.class.getDeclaredField("mOverhangSize");
            f_overHang.setAccessible(true);
            //设置左菜单离右边屏幕边缘的距离为0，设置全屏
            f_overHang.set(layoutSliding, 150);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        layoutSliding.setSliderFadeColor(getResources().getColor(R.color.transparent));
    }


    @Event(R.id.img_close)
    private void closeVideo(View view){
        stopPlay();
        ActivityUtil.finish(this);
    }

    @Event(R.id.img_share)
    private void showDialog(View view){
        shareDialog = new BottomSheetDialog(this);
        shareDialog.contentView(R.layout.layout_video_share)
                .inDuration(300);
        txtWeixin = (TextView)shareDialog.findViewById(R.id.txt_weixin);
        txtPyq = (TextView)shareDialog.findViewById(R.id.txt_pyq);
        txtWeibo = (TextView)shareDialog.findViewById(R.id.txt_weibo);
        txtCopy = (TextView)shareDialog.findViewById(R.id.txt_copy);
        txtCancel = (TextView)shareDialog.findViewById(R.id.txt_cancel);
        txtWeixin.setOnClickListener(this);
        txtPyq.setOnClickListener(this);
        txtWeibo.setOnClickListener(this);
        txtCopy.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        shareDialog.show();
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.txt_weixin:
                ToastUtil.showShortToast(this, "weixin", 1);
                shareDialog.dismiss();
                break;
            case R.id.txt_pyq:
                ToastUtil.showShortToast(this,"pyq",1);
                shareDialog.dismiss();
                break;
            case R.id.txt_weibo:
                ToastUtil.showShortToast(this,"weibo",1);
                shareDialog.dismiss();
                break;
            case R.id.txt_copy:
                ToastUtil.showShortToast(this,"copy",1);
                shareDialog.dismiss();
                break;
            case R.id.txt_cancel:
                shareDialog.dismiss();
                break;
        }
    }

    private class SliderListener extends SlidingPaneLayout.SimplePanelSlideListener {
        @Override
        public void onPanelOpened(View panel) {
//            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txtDiscuss.getLayoutParams();
//            layoutParams.leftMargin = 100;
//            txtDiscuss.setLayoutParams(layoutParams);
            txtDiscuss.setVisibility(View.GONE);
        }

        @Override
        public void onPanelClosed(View panel) {
//            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txtDiscuss.getLayoutParams();
//            layoutParams.leftMargin = 10;
//            txtDiscuss.setLayoutParams(layoutParams);
            txtDiscuss.setVisibility(View.VISIBLE);
        }
    }
}
