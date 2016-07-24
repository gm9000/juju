package com.juju.app.activity.party;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.common.Logger;
import com.juju.app.R;
import com.juju.app.adapters.DiscussListAdapter;
import com.juju.app.annotation.SystemColor;
import com.juju.app.event.notify.DiscussNotifyEvent;
import com.juju.app.event.notify.LiveEnterNotifyEvent;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.event.notify.SeizeNotifyEvent;
import com.juju.app.fragment.party.LiveMenuFragment;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.notify.LiveDiscussNotify;
import com.juju.app.service.notify.LiveEnterNotify;
import com.juju.app.service.notify.LiveSeizeReportNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.dialog.WarnTipDialog;
import com.rey.material.app.BottomSheetDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;


@ContentView(R.layout.activity_play_video)
@SystemColor(isApply = false)
public class PlayVideoActivity extends BaseActivity implements SurfaceHolder.Callback {

    private static final String TAG = "PlayVideoActivity";


    @ViewInject(R.id.videoSurfaceView)
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private View loadingView;

    @ViewInject(R.id.live_menu_container)
    private ViewPager liveMenuPager;

    private LiveMenuAdapter liveMenuAdapter;

    @ViewInject(R.id.layout_count)
    private LinearLayout layoutCount;
    @ViewInject(R.id.txt_count)
    private TextView txtCount;

    @ViewInject(R.id.txt_relay_count)
    private TextView txtRelayCount;

    private DiscussListAdapter discussListAdapter;
    private List discussList;

    private String groupId;
    private String liveId;
    private String videoUrl;
    private int relayCount;

    private BottomSheetDialog shareDialog;

    private long milliSecondes;

    private IjkMediaPlayer ijkMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private int aTrackIndex = -1;
    private int vTrackIndex = -1;

    private boolean audioOn = true;
    private boolean delayProcessing = false;

    private IMService imService;
    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("UploadVieoActivity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
        }

        @Override
        public void onServiceDisconnected() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        imServiceConnector.connect(this);
        initParam();
        initView();
        initListener();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (ijkMediaPlayer != null && isPlaying) {
            ijkMediaPlayer.start();
        }
    }

    @Override
    protected void onPause(){
        Log.d(TAG, "onPause");
        super.onPause();
        if(isPlaying){
            ijkMediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestory");
        if (ijkMediaPlayer != null ) {
            if(ijkMediaPlayer.isPlaying()) {
                ijkMediaPlayer.stop();
            }
            ijkMediaPlayer.release();
        }
        mHandler.removeMessages(MSG_UPDATE_DURATION);
        EventBus.getDefault().unregister(this);
        imServiceConnector.disconnect(this);

        if (isPlaying) {
            isPlaying = false;
            LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnterNotifyBean = new LiveEnterNotifyEvent.LiveEnterNotifyBean();
            liveEnterNotifyBean.setGroupId(groupId);
            liveEnterNotifyBean.setLiveId(liveId);
            liveEnterNotifyBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
            liveEnterNotifyBean.setNickName(AppContext.getUserInfoBean().getNickName());
            liveEnterNotifyBean.setType(1);
            LiveEnterNotify.instance().executeCommand4Send(liveEnterNotifyBean);
        }
        super.onDestroy();
    }

    @Override
    protected void onStop(){
        Log.d(TAG, "onStop");
        super.onStop();
    }

    private void initListener() {
        txtRelayCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtRelayCount.setText(String.valueOf(++relayCount));
            }
        });
    }

    private void initParam() {

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        groupId = bundle.getString(Constants.GROUP_ID);
        liveId = bundle.getString(Constants.LIVE_ID);
        videoUrl = bundle.getString(Constants.VIDEO_URL);
    }

    private void initView() {
        if (isLiveStreaming(videoUrl)) {
            discussList = new ArrayList();
            discussListAdapter = new DiscussListAdapter(this);
            discussListAdapter.setDiscussList(discussList);

            liveMenuAdapter = new LiveMenuAdapter(getSupportFragmentManager(),this,discussListAdapter);
            liveMenuPager.setAdapter(liveMenuAdapter);
            liveMenuPager.setCurrentItem(1);
        } else {
            liveMenuPager.setVisibility(View.GONE);
        }

        loadingView = findViewById(R.id.LoadingView);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

    }


    @Event(R.id.img_close)
    private void closeVideo(View view) {
        ijkMediaPlayer.stop();
        ijkMediaPlayer.release();
        mHandler.removeMessages(MSG_UPDATE_DURATION);
        ActivityUtil.finish(this);
    }

    private void playVide(SurfaceHolder surfaceHolder) {

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.setSurface(surfaceHolder.getSurface());
        ijkMediaPlayer.setDisplay(surfaceHolder);
        ijkMediaPlayer.setVolume(1.0f, 1.0f);


        if (!isLiveStreaming(videoUrl)) {
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


        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 5000000);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probsize", 4096);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 100 * 1024);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 5);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        if (isSupportHWEncode()) {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        } else {
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
    public void surfaceCreated(SurfaceHolder holder) {
        playVide(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (isPlaying && ijkMediaPlayer != null && ijkMediaPlayer.isPlaying()) {
            ijkMediaPlayer.stop();
            isPlaying = false;
            LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnterNotifyBean = new LiveEnterNotifyEvent.LiveEnterNotifyBean();
            liveEnterNotifyBean.setGroupId(groupId);
            liveEnterNotifyBean.setLiveId(liveId);
            liveEnterNotifyBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
            liveEnterNotifyBean.setNickName(AppContext.getUserInfoBean().getNickName());
            liveEnterNotifyBean.setType(1);
            LiveEnterNotify.instance().executeCommand4Send(liveEnterNotifyBean);
        }
    }


    private boolean isLiveStreaming(String url) {
        if (url.startsWith("rtmp://")) {
            return true;
        }
        return false;
    }

    private boolean isPlaying = false;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer mp) {
            loadingView.setVisibility(View.GONE);
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
            fixSurfaceViewSize(mVideoWidth, mVideoHeight);
            IjkTrackInfo[] tracks = ijkMediaPlayer.getTrackInfo();
            for (int index = 0; index < tracks.length; index++) {
                IjkTrackInfo trackInfo = tracks[index];
                switch (trackInfo.getTrackType()) {
                    case IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                        aTrackIndex = index;
                        break;
                    case IjkTrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                        vTrackIndex = index;
                        break;
                }
            }
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DURATION, 500);

            LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnterNotifyBean = new LiveEnterNotifyEvent.LiveEnterNotifyBean();
            liveEnterNotifyBean.setGroupId(groupId);
            liveEnterNotifyBean.setLiveId(liveId);
            liveEnterNotifyBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
            liveEnterNotifyBean.setNickName(AppContext.getUserInfoBean().getNickName());
            liveEnterNotifyBean.setType(0);
            LiveEnterNotify.instance().executeCommand4Send(liveEnterNotifyBean);
            isPlaying = true;
            ijkMediaPlayer.start();
        }
    };

    private void fixSurfaceViewSize(int mVideoWidth, int mVideoHeight) {
        int screenWidth = ScreenUtil.getScreenWidth(this);
        int screenHeight = ScreenUtil.getScreenHeight(this);
        boolean widthFill = true;
        if (mVideoHeight * screenWidth > mVideoWidth * screenHeight) {
            widthFill = false;
        }

        RelativeLayout.LayoutParams layout = null;
        if (widthFill) {
            layout = new RelativeLayout.LayoutParams(screenWidth, mVideoHeight * screenWidth / mVideoWidth);
        } else {
            layout = new RelativeLayout.LayoutParams(mVideoWidth * screenHeight / mVideoHeight, screenHeight);
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
        ToastUtil.showShortToast(this, s, 1);
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

                    if (aTrackIndex > -1 && !delayProcessing && ijkMediaPlayer.getAudioCachedDuration() > 1000) {
                        ijkMediaPlayer.deselectTrack(aTrackIndex);
                        delayProcessing = true;
                    }
                    if (audioOn && delayProcessing && aTrackIndex > -1) {
                        ijkMediaPlayer.selectTrack(aTrackIndex);
                        delayProcessing = false;
                    }
                    mHandler.removeMessages(MSG_UPDATE_DURATION);
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DURATION, 500);
                }
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLiveEnter(LiveEnterNotifyEvent event) {
        LiveEnterNotifyEvent.LiveEnterNotifyBean bean = event.bean;
        if (!bean.getLiveId().equals(liveId)) {
            return;
        }
        switch (event.event) {
            case LIVE_ENTER_NOTIFY_OK:
                discussList.add(bean);
                discussListAdapter.notifyDataSetChanged();
                liveMenuAdapter.getFragment(1).updateDiscuss(false);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLiveDiscuss(DiscussNotifyEvent event) {
        DiscussNotifyEvent.DiscussNotifyBean bean = event.bean;
        if (!bean.getLiveId().equals(liveId)) {
            return;
        }
        switch (event.event) {
            case DISCUSS_NOTIFY_OK:
                discussList.add(bean);
                discussListAdapter.notifyDataSetChanged();
                liveMenuAdapter.getFragment(1).updateDiscuss(true);
                break;
            case DISCUSS_NOTIFY_FAILED:
                ToastUtil.showLongToast(this, getString(R.string.operation_fail));
                break;
            case RECEIVE_DISCUSS_NOTIFY_OK:
                discussList.add(bean);
                discussListAdapter.notifyDataSetChanged();
                liveMenuAdapter.getFragment(1).updateDiscuss(false);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4SeizeNotify(SeizeNotifyEvent event) {
        SeizeNotifyEvent.SeizeNotifyBean bean = event.bean;
        if (!bean.getLiveId().equals(liveId)) {
            return;
        }
        switch (event.event) {
            case LIVE_SEIZE_START_OK:
                ToastUtil.showLongToast(this, "抢播开始");
                txtRelayCount.setClickable(true);
                layoutCount.setVisibility(View.VISIBLE);
                txtRelayCount.setVisibility(View.VISIBLE);
                txtRelayCount.setText(R.string.click_hint);
                txtCount.setText(String.valueOf(bean.getSeconds()));
                new TimeCount(bean.getSeconds() * 1000, 1000).start();
                break;
            case LIVE_SEIZE_STOP_OK:

                if (bean.getUserNo().equals(AppContext.getUserInfoBean().getUserNo())) {
                    ToastUtil.showLongToast(this, "恭喜！您获得了续播权！");
                    Intent intent = getIntent();
                    intent.putExtra(Constants.LIVE_ID,liveId);
                    this.setResult(RESULT_OK,intent);
                    isPlaying = false;
                    ActivityUtil.finish(this);

                } else {
                    ToastUtil.showLongToast(this, imService.getContactManager().findContact(bean.getUserNo()).getNickName() + " 获得了续播权！");
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4LiveNotify(LiveNotifyEvent event) {
        LiveNotifyEvent.LiveNotifyBean bean = event.bean;
        if (!bean.getLiveId().equals(liveId)) {
            return;
        }
        switch (event.event) {
            case LIVE_STOP_OK:
                WarnTipDialog tipdialog = new WarnTipDialog(context,"直播已结束！");
                tipdialog.hiddenBtnCancel();
                tipdialog.setBtnOkLinstener( new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityUtil.finish(PlayVideoActivity.this);
                    }
                });
                tipdialog.show();
                break;
        }
    }

    public void sendDiscuss(String message) {
        DiscussNotifyEvent.DiscussNotifyBean discussNotifyBean = new DiscussNotifyEvent.DiscussNotifyBean();
        discussNotifyBean.setGroupId(groupId);
        discussNotifyBean.setLiveId(liveId);
        discussNotifyBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
        discussNotifyBean.setNickName(AppContext.getUserInfoBean().getNickName());
        discussNotifyBean.setContent(message);
        LiveDiscussNotify.instance().executeCommand4Send(discussNotifyBean);
    }

    /**
     * ******************************************内部类******************************************
     */
    //计时器
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {
            layoutCount.setVisibility(View.GONE);
            txtRelayCount.setVisibility(View.GONE);
            txtRelayCount.setClickable(false);
            if (relayCount == 0) {
                return;
            }
            //计数统计完毕，通知续播
            SeizeNotifyEvent.SeizeNotifyBean seizeStartBean = new SeizeNotifyEvent.SeizeNotifyBean();
            seizeStartBean.setGroupId(groupId);
            seizeStartBean.setLiveId(liveId);
            seizeStartBean.setCount(relayCount);
            seizeStartBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
            LiveSeizeReportNotify.instance().executeCommand4Send(seizeStartBean);
        }

        @Override
        public void onTick(long millisUntilFinished) {//计时过程显示
            long leftTime = millisUntilFinished / 1000;
            txtCount.setText(String.valueOf(leftTime));
        }
    }


    private static final class LiveMenuAdapter extends FragmentStatePagerAdapter {

        private BaseActivity activity;
        private DiscussListAdapter discussListAdapter;
        private LiveMenuFragment[] fragments = new LiveMenuFragment[]{null,null};

        public LiveMenuAdapter(FragmentManager fragmentManager, BaseActivity activity, DiscussListAdapter discussAdapter) {
            super(fragmentManager);
            this.activity = activity;
            this.discussListAdapter = discussAdapter;
        }


        @Override
        public Fragment getItem(int position) {
            final LiveMenuFragment fragment = new LiveMenuFragment(activity,false,position,"");
            if(position == 1){
                fragment.setDiscussAdapter(discussListAdapter);
            }
            fragments[position] = fragment;
            return fragment;
        }

        public LiveMenuFragment getFragment(int position){
            return fragments[position];
        }

        @Override
        public int getCount() {
            return 2;
        }

    }

}
