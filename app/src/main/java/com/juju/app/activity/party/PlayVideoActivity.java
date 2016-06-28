package com.juju.app.activity.party;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.annotation.SystemColor;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.ToastUtil;
import com.rey.material.app.BottomSheetDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaPlayerProxy;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;


@ContentView(R.layout.activity_play_video)
@SystemColor(isApply=false)
public class PlayVideoActivity extends BaseActivity implements View.OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "PlayVideoActivity";

    @ViewInject(R.id.videoSurfaceView)
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private View loadingView;

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

    private String videoUrl;

    private BottomSheetDialog shareDialog;

    private long milliSecondes;

    private IjkMediaPlayer ijkMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private int aTrackIndex = -1;
    private int vTrackIndex = -1;

    private boolean audioOn = true;
    private boolean delayProcessing = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ijkMediaPlayer!=null && ijkMediaPlayer.isPlaying()) {
            ijkMediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        ijkMediaPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ijkMediaPlayer.stop();
        mHandler.removeMessages(MSG_UPDATE_DURATION);
        super.onDestroy();
    }

    private void initListener() {
        layoutSliding.setPanelSlideListener(new SliderListener());
    }

    private void initParam() {

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        videoUrl = bundle.getString("videoUrl");
    }

    private void initView() {
        if(isLiveStreaming(videoUrl)) {

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
        }else{
            layoutSliding.setVisibility(View.GONE);
        }
        loadingView = findViewById(R.id.LoadingView);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }


    @Event(R.id.img_close)
    private void closeVideo(View view){
        ijkMediaPlayer.stop();
        mHandler.removeMessages(MSG_UPDATE_DURATION);
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

    private void playVide(SurfaceHolder surfaceHolder) {

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.setSurface(surfaceHolder.getSurface());
        ijkMediaPlayer.setDisplay(surfaceHolder);
        ijkMediaPlayer.setVolume(1.0f,1.0f);


        if(!isLiveStreaming(videoUrl)) {
        }

        ijkMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        ijkMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        ijkMediaPlayer.setOnErrorListener(mOnErrorListener);
        ijkMediaPlayer.setOnInfoListener(mOnInfoListener);
        ijkMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);


//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-all-videos", 0);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", 1);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-mpeg2", 0);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "vn", 0);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "an", 0);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", 1);

//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frames", 15);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);


//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "video-pictq-size", 15);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 1);
//      ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "iformat", "H264");


        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"analyzeduration", 5000000);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"fflags", "nobuffer");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"probsize", 4096);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 100*1024);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames",5);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        if(isSupportHWEncode()) {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        }else{
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        }


        try {
            ijkMediaPlayer.setDataSource(videoUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ijkMediaPlayer.setScreenOnWhilePlaying(true);
        ijkMediaPlayer.prepareAsync();
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        playVide(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

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



    private boolean isLiveStreaming(String url) {
        if (url.startsWith("rtmp://")) {
            return true;
        }
        return false;
    }

    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener(){

        @Override
        public void onPrepared(IMediaPlayer mp) {
            loadingView.setVisibility(View.GONE);
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mSurfaceHolder.setFixedSize(mVideoWidth,mVideoHeight);
            fixSurfaceViewSize(mVideoWidth,mVideoHeight);
            IjkTrackInfo[] tracks = ijkMediaPlayer.getTrackInfo();
            for(int index = 0; index<tracks.length; index++){
                IjkTrackInfo trackInfo = tracks[index];
                switch(trackInfo.getTrackType()){
                    case IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                        aTrackIndex = index;
                        break;
                    case IjkTrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                        vTrackIndex = index;
                        break;
                }
            }
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DURATION, 500);
        }
    };

    private void fixSurfaceViewSize(int mVideoWidth, int mVideoHeight) {
        int screenWidth = ScreenUtil.getScreenWidth(this);
        int screenHeight = ScreenUtil.getScreenHeight(this);
        boolean widthFill = true;
        if(mVideoHeight*screenWidth>mVideoWidth*screenHeight){
            widthFill = false;
        }

        RelativeLayout.LayoutParams layout = null;
        if(widthFill) {
            layout = new RelativeLayout.LayoutParams(screenWidth, mVideoHeight*screenWidth/mVideoWidth);
        }else {
            layout = new RelativeLayout.LayoutParams(mVideoWidth*screenHeight/mVideoHeight, screenHeight);
        }
        layout.addRule(RelativeLayout.CENTER_IN_PARENT);
        mSurfaceView.setLayoutParams(layout);
    }

    private IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {

        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        }
    };

    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new IMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer mp) {

        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {

        }
    };

    private void showToastTips(String s) {
        ToastUtil.showShortToast(this,s,1);
    }

    private static boolean isSupportHWEncode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private static final int MSG_UPDATE_DURATION = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_DURATION: {

                    System.out.println("-------------------------------------------ad:"+ijkMediaPlayer.getAudioCachedDuration()+"-vd:"+ijkMediaPlayer.getVideoCachedDuration()+"-aps:"+ijkMediaPlayer.getAudioCachedPackets()+"-vps:"+ijkMediaPlayer.getVideoCachedPackets());
                    if(aTrackIndex>-1 && !delayProcessing && ijkMediaPlayer.getAudioCachedDuration()>1000){
                        ijkMediaPlayer.deselectTrack(aTrackIndex);
                        delayProcessing = true;
                    }
                    if(audioOn && delayProcessing && aTrackIndex>-1){
                        ijkMediaPlayer.selectTrack(aTrackIndex);
                        delayProcessing = false;
                    }
                    mHandler.removeMessages(MSG_UPDATE_DURATION);
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DURATION, 500);
                }
            }
        }
    };

}
