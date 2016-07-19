package com.juju.app.activity.party;

import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.DiscussListAdapter;
import com.juju.app.annotation.SystemColor;
import com.juju.app.bean.json.LoginResBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.VideoProgram;
import com.juju.app.event.notify.DiscussNotifyEvent;
import com.juju.app.event.notify.LiveEnterNotifyEvent;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.event.notify.SeizeNotifyEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.golobal.StatusCode;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.notify.LiveDiscussNotify;
import com.juju.app.service.notify.LiveSeizeStartNotify;
import com.juju.app.service.notify.LiveSeizeStopNotify;
import com.juju.app.service.notify.LiveStartNotify;
import com.juju.app.service.notify.LiveStopNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.CameraUtil;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.view.dialog.WarnTipDialog;
import com.pili.pldroid.streaming.CameraStreamingManager;
import com.pili.pldroid.streaming.CameraStreamingSetting;
import com.pili.pldroid.streaming.StreamStatusCallback;
import com.pili.pldroid.streaming.StreamingEnv;
import com.pili.pldroid.streaming.StreamingProfile;
import com.pili.pldroid.streaming.widget.AspectFrameLayout;
import com.rey.material.app.BottomSheetDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ContentView(R.layout.activity_party_video)
@SystemColor(isApply = false)
public class UploadVideoActivity extends BaseActivity implements View.OnClickListener, CameraStreamingManager.StreamingStateListener, HttpCallBack4OK, StreamStatusCallback {

    private static final String TAG = "UploadVideoActivity";

    @ViewInject(R.id.img_change)
    private ImageView imgChange;

    @ViewInject(R.id.statusText)
    private TextView statusText;

    @ViewInject(R.id.img_upload)
    private ImageView imgUpload;

    @ViewInject(R.id.img_seize)
    private ImageView imgSeize;
    @ViewInject(R.id.txt_count)
    private TextView txtCount;

    @ViewInject(R.id.layout_sliding)
    private SlidingPaneLayout layoutSliding;

    @ViewInject(R.id.layout_right_menu)
    private RelativeLayout layoutRight;
    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;
    @ViewInject(R.id.txt_discuss)
    private EditText txtDiscuss;
    @ViewInject(R.id.btn_send)
    private Button btnSend;
    @ViewInject(R.id.img_share)
    private ImageView imgShare;

    private BottomSheetDialog shareDialog;
    private TextView txtWeixin;
    private TextView txtPyq;
    private TextView txtWeibo;
    private TextView txtCopy;
    private TextView txtCancel;

    @ViewInject(R.id.cameraPreview_afl)
    private AspectFrameLayout afl;
    @ViewInject(R.id.cameraPreview_surfaceView)
    private GLSurfaceView glSurfaceView;

    private CameraStreamingManager mCameraStreamingManager;

    private String mediaId;
    private String groupId;
    private String partyId;
    private String liveToken;
    private String liveId;

    private IMService imService;

    private String relayUserNo;
    private int relayCount;


    private DiscussListAdapter discussListAdapter;
    private List discussList;

    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("UploadVieoActivity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            initParam();
            initView();
            initListener();
            previewCameraVideo();
            mCameraStreamingManager.resume();
        }

        @Override
        public void onServiceDisconnected() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(this);
        EventBus.getDefault().register(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 防止锁屏
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        mCameraStreamingManager.pause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestory");
        super.onDestroy();
        imServiceConnector.disconnect(this);
        EventBus.getDefault().unregister(this);
        if (liveId != null) {
            releaseLive(true);
        }else{
            mCameraStreamingManager = null;
        }


    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (mCameraStreamingManager != null) {
            mCameraStreamingManager.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraStreamingManager.pause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void previewCameraVideo() {

        if(liveId != null) {
            VideoProgram videoProgram = null;
            try {
                videoProgram = JujuDbUtils.getInstance().selector(VideoProgram.class).where("id","=",liveId).findFirst();
            } catch (DbException e) {
                e.printStackTrace();
            }
            mediaId = videoProgram.getVideoUrl().substring(videoProgram.getVideoUrl().lastIndexOf("/")+1);
        }

        if(mediaId == null){
            mediaId = UUID.randomUUID().toString();
        }

        JSONObject mJSONObject = new JSONObject();
        //        this.streamId = var1.getString("id");
        try {
            mJSONObject.put("id", new String("123456"));
            mJSONObject.put("hub", new String("juju"));
//            mJSONObject.put("title","12345");
            mJSONObject.put("title", mediaId);
            mJSONObject.put("publishKey", new String("123456"));
            mJSONObject.put("publishSecurity", new String("static"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject hosts = new JSONObject();
        JSONObject publish = new JSONObject();
        try {
            publish.put("rtmp", GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort);
            hosts.put("publish", publish);
            mJSONObject.put("hosts", hosts);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 48 * 1000);
        StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(15, 200 * 1000, 30);
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);

        StreamingProfile.Stream stream = new StreamingProfile.Stream(mJSONObject);

        StreamingProfile mProfile = new StreamingProfile();
        mProfile.setStream(stream);
        mProfile.setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
                .setPreferredVideoEncodingSize(640, 360)
                .setVideoQuality(StreamingProfile.VIDEO_QUALITY_LOW3)
                .setEncoderRCMode(StreamingProfile.EncoderRCModes.BITRATE_PRIORITY)
                .setAVProfile(avProfile)
//                .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));

        CameraStreamingSetting mCameraStreamingSetting = new CameraStreamingSetting();
        mCameraStreamingSetting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setFrontCameraMirror(false)
                .setResetTouchFocusDelayInMs(2000)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);

        if (isSupportHWEncode()) {
            mCameraStreamingSetting.setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCameraStreamingSetting.setContinuousFocusModeEnabled(true);
        }

        StreamingEnv.init(context);
        if (isSupportHWEncode()) {
            mCameraStreamingManager = new CameraStreamingManager(this, afl, glSurfaceView, CameraStreamingManager.EncodingType.HW_VIDEO_WITH_HW_AUDIO_CODEC);
        } else {
            mCameraStreamingManager = new CameraStreamingManager(this, afl, glSurfaceView, CameraStreamingManager.EncodingType.SW_VIDEO_WITH_SW_AUDIO_CODEC);
        }
        mCameraStreamingManager.prepare(mCameraStreamingSetting, mProfile);
        mCameraStreamingManager.setStreamingStateListener(this);
        mCameraStreamingManager.setStreamStatusCallback(this);
    }


    private void initListener() {
        layoutSliding.setPanelSlideListener(new SliderListener());
    }

    private void initParam() {
        groupId = getIntent().getStringExtra(Constants.GROUP_ID);
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
        liveId = getIntent().getStringExtra(Constants.LIVE_ID);
    }

    private void initView() {
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

        imgUpload.setVisibility(View.GONE);

        layoutSliding.setSliderFadeColor(getResources().getColor(R.color.transparent));


        if (!CameraUtil.hasFrontFacingCamera()) {
            imgChange.setVisibility(View.GONE);
        }
        layoutSliding.setVisibility(View.GONE);

        discussList = new ArrayList();

        discussListAdapter = new DiscussListAdapter(this);
        discussListAdapter.setDiscussList(discussList);
        discussListView.setAdapter(discussListAdapter);

    }


    @Event(R.id.img_change)
    private void changeCamera(View view) {
        mCameraStreamingManager.switchCamera();
    }

    @Event(R.id.img_close)
    private void closeVideo(View view) {
        releaseLive(false);
    }

    @Event(R.id.btn_send)
    private void addDiscuss(View view) {
        //TODO  发送通知

        if (StringUtils.empty(txtDiscuss.getText())) {
            return;
        }

        DiscussNotifyEvent.DiscussNotifyBean discussNotifyBean = new DiscussNotifyEvent.DiscussNotifyBean();
        discussNotifyBean.setGroupId(groupId);
        discussNotifyBean.setLiveId(liveId);
        discussNotifyBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
        discussNotifyBean.setNickName(AppContext.getUserInfoBean().getNickName());
        discussNotifyBean.setContent(txtDiscuss.getText().toString());
        LiveDiscussNotify.instance().executeCommand4Send(discussNotifyBean);
    }

    @Event(R.id.img_seize)
    private void notifySeize(View view) {

        WarnTipDialog tipdialog = new WarnTipDialog(this, "确定要发起续播竞赛吗？");
        tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                imgSeize.setClickable(false);
                SeizeNotifyEvent.SeizeNotifyBean seizeStartBean = new SeizeNotifyEvent.SeizeNotifyBean();
                seizeStartBean.setGroupId(groupId);
                seizeStartBean.setLiveId(liveId);
                seizeStartBean.setType(0);
                seizeStartBean.setSeconds(25);
                LiveSeizeStartNotify.instance().executeCommand4Send(seizeStartBean);
            }
        });
        tipdialog.show();

    }

    private void notifySeize() {
    }

    private void releaseLive(final boolean onDestoryCall) {

        if (liveId == null) {
            ActivityUtil.finish(this);
            return;
        }

        CommandActionConstant.HttpReqParam INVITEUSER = CommandActionConstant.HttpReqParam.END_LIVE;
        Map<String, Object> paramMap = HttpReqParamUtil.instance().buildMap();
        Map<String, Object> liveMap = new HashMap<String, Object>();
        liveMap.put("id", liveId);
        liveMap.put("videoUrl", "rtmp://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/juju/" + mediaId);
        liveMap.put("captureUrl", "http://219.143.237.229:8080/capture.jpg");
        liveMap.put("width", 320);
        liveMap.put("height", 180);
        paramMap.put("live", liveMap);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                INVITEUSER.code(), INVITEUSER.url(),
                new HttpCallBack4OK() {
                    @Override
                    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                        if (obj != null && obj instanceof JSONObject) {
                            JSONObject jsonRoot = (JSONObject) obj;
                            int status = JSONUtils.getInt(jsonRoot, "status", -1);
                            if (status == 0) {
                                mCameraStreamingManager.stopStreaming();
                                mCameraStreamingManager.destroy();
                                mCameraStreamingManager = null;

                                VideoProgram videoProgram = null;
                                try {
                                    videoProgram = JujuDbUtils.getInstance().selector(VideoProgram.class).where("id", "=", liveId).findFirst();
                                    videoProgram.setEndTime(new Date());
                                    videoProgram.setStatus(1);
                                    //  TODO   设置历史视频请求的URL
                                    videoProgram.setVideoUrl("rtmp://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/juju/" + mediaId);
                                    JujuDbUtils.saveOrUpdate(videoProgram);

                                } catch (DbException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastUtil.showShortToast(UploadVideoActivity.this, getString(R.string.operation_fail), 1);
                                        }
                                    });
                                }

                                LiveNotifyEvent.LiveNotifyBean liveStopBean = new LiveNotifyEvent.LiveNotifyBean();
                                liveStopBean.setGroupId(groupId);
                                liveStopBean.setLiveId(liveId);
                                liveStopBean.setVideoUrl("rtmp://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/juju/" + mediaId);
                                liveStopBean.setPartyId(partyId);
                                liveStopBean.setCaptureUrl("http://219.143.237.229:8080/capture.jpg");
                                liveStopBean.setWidth(320);
                                liveStopBean.setHeight(180);
                                LiveStopNotify.instance().executeCommand4Send(liveStopBean);
                                liveId = null;
                                if (!onDestoryCall) {
                                    ActivityUtil.finish(UploadVideoActivity.this);
                                }
                                EventBus.getDefault().post(new LiveNotifyEvent(LiveNotifyEvent.Event.LIVE_STOP_OK));

                            } else {
                                String desc = JSONUtils.getString(jsonRoot, "desc");
                                ToastUtil.showShortToast(UploadVideoActivity.this, getString(getResValue(desc)), 1);
                                mCameraStreamingManager = null;
                            }
                        }
                    }

                    @Override
                    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                        mCameraStreamingManager = null;
                    }
                }, paramMap, JSONObject.class);
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
            mCameraStreamingManager = null;
        } catch (JSONException e) {
            mCameraStreamingManager = null;
        }

    }

    @Event(R.id.img_upload)
    private void applyLiveToken(View view) {
        imgUpload.setClickable(false);
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("partyId", partyId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GET_LIVE_TOKEN;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if (obj instanceof JSONObject) {
                    JSONObject jsonRoot = (JSONObject) obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    if (status == 0) {
                        liveToken = JSONUtils.getString(jsonRoot, "liveToken");
                        publishLive();
                    } else if (status == StatusCode.LIVE_HAS_EXIST) {
                        String preUserNo = JSONUtils.getString(jsonRoot, "userNo");
                        final String preUserNickName = imService.getContactManager().findContact(preUserNo).getNickName();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtil.showLongToast(UploadVideoActivity.this, preUserNickName + " 抢先了一步,下次不让TA了");
                            }
                        });
                        ActivityUtil.finish(UploadVideoActivity.this);
                    } else {
                        imgUpload.setClickable(true);
                        final String desc = JSONUtils.getString(jsonRoot, "desc");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtil.showShortToast(UploadVideoActivity.this, getString(getResValue(desc)), 1);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                imgUpload.setClickable(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.showShortToast(UploadVideoActivity.this, getString(R.string.operation_fail), 1);
                    }
                });
            }
        }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {

        } catch (JSONException e) {

        }
    }

    private void publishLive() {

        CommandActionConstant.HttpReqParam INVITEUSER = CommandActionConstant.HttpReqParam.ADD_LIVE;
        Map<String, Object> paramMap = HttpReqParamUtil.instance()
                .buildMap("partyId,liveToken", partyId, liveToken);
        Map<String, Object> liveMap = new HashMap<String, Object>();
        liveMap.put("liveUrl", "rtmp://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/juju/" + mediaId);
        liveMap.put("captureUrl", "http://219.143.237.229:8080/capture.jpg");
        liveMap.put("width", 320);
        liveMap.put("height", 180);
        paramMap.put("live", liveMap);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                INVITEUSER.code(), INVITEUSER.url(),
                new HttpCallBack4OK() {
                    @Override
                    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                        if (obj != null && obj instanceof JSONObject) {
                            JSONObject jsonRoot = (JSONObject) obj;
                            int status = JSONUtils.getInt(jsonRoot, "status", -1);
                            if (status == 0) {
                                liveId = JSONUtils.getString(jsonRoot, "liveId");
                                mCameraStreamingManager.startStreaming();

                                VideoProgram videoProgram = new VideoProgram();
                                videoProgram.setId(liveId);
                                videoProgram.setPartyId(partyId);
                                videoProgram.setCaptureUrl("http://219.143.237.229:8080/capture.jpg");
                                videoProgram.setVideoUrl("rtmp://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/juju/" + mediaId);
                                videoProgram.setStartTime(new Date());
                                videoProgram.setCreatorNo(AppContext.getUserInfoBean().getUserNo());
                                videoProgram.setStatus(0);
                                JujuDbUtils.save(videoProgram);

                                LiveNotifyEvent.LiveNotifyBean liveStartBean = new LiveNotifyEvent.LiveNotifyBean();
                                liveStartBean.setGroupId(groupId);
                                liveStartBean.setLiveId(liveId);
                                liveStartBean.setLiveUrl("rtmp://" + GlobalVariable.liveServerIp + ":" + GlobalVariable.liveServerPort + "/juju/" + mediaId);
                                liveStartBean.setPartyId(partyId);
                                liveStartBean.setCaptureUrl("http://219.143.237.229:8080/capture.jpg");
                                liveStartBean.setWidth(320);
                                liveStartBean.setHeight(180);
                                liveStartBean.setUserNo(AppContext.getUserInfoBean().getUserNo());
                                liveStartBean.setNickName(AppContext.getUserInfoBean().getNickName());
                                LiveStartNotify.instance().executeCommand4Send(liveStartBean);

                            } else {
                                imgUpload.setClickable(true);
                                final String desc = JSONUtils.getString(jsonRoot, "desc");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ToastUtil.showShortToast(UploadVideoActivity.this, getString(getResValue(desc)), 1);
                                    }
                                });

                            }
                        }
                    }

                    @Override
                    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                    }
                }, paramMap, JSONObject.class);
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
        } catch (JSONException e) {
        }
    }

    @Event(R.id.img_share)
    private void showDialog(View view) {
        shareDialog = new BottomSheetDialog(this);
        shareDialog.contentView(R.layout.layout_video_share)
                .inDuration(300);
        txtWeixin = (TextView) shareDialog.findViewById(R.id.txt_weixin);
        txtPyq = (TextView) shareDialog.findViewById(R.id.txt_pyq);
        txtWeibo = (TextView) shareDialog.findViewById(R.id.txt_weibo);
        txtCopy = (TextView) shareDialog.findViewById(R.id.txt_copy);
        txtCancel = (TextView) shareDialog.findViewById(R.id.txt_cancel);
        txtWeixin.setOnClickListener(this);
        txtPyq.setOnClickListener(this);
        txtWeibo.setOnClickListener(this);
        txtCopy.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        shareDialog.show();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_weixin:
                ToastUtil.showShortToast(this, "weixin", 1);
                shareDialog.dismiss();
                break;
            case R.id.txt_pyq:
                ToastUtil.showShortToast(this, "pyq", 1);
                shareDialog.dismiss();
                break;
            case R.id.txt_weibo:
                ToastUtil.showShortToast(this, "weibo", 1);
                shareDialog.dismiss();
                break;
            case R.id.txt_copy:
                ClipboardManager cm = (ClipboardManager) getSystemService(this.CLIPBOARD_SERVICE);
                cm.setText("http://219.143.237.232:8080/hls/" + mediaId + ".m3u8");
                ToastUtil.showShortToast(this, "视频地址已经复制到剪切板", 1);
                shareDialog.dismiss();
                break;
            case R.id.txt_cancel:
                shareDialog.dismiss();
                break;
        }
    }

    @Override
    public void onStateChanged(final int status, Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case CameraStreamingManager.STATE.READY:
                        statusText.setText("准备就绪");
                        if(liveId!=null){
                            mCameraStreamingManager.startStreaming();
                        }else {
                            imgUpload.setVisibility(View.VISIBLE);
                        }
                        break;
                    case CameraStreamingManager.STATE.STREAMING:
                        statusText.setText("开始直播");
                        imgUpload.setVisibility(View.GONE);
                        imgUpload.setClickable(true);
                        imgSeize.setVisibility(View.VISIBLE);
                        layoutSliding.setVisibility(View.VISIBLE);
                        publishHlsVideo();
                        break;
                    case CameraStreamingManager.STATE.CAMERA_SWITCHED:
                        statusText.setText("切换摄像头");
                        break;
                    case CameraStreamingManager.STATE.CONNECTION_TIMEOUT:
                        statusText.setText("连接超时");
                        imgUpload.setVisibility(View.VISIBLE);
                        imgUpload.setClickable(true);
                        imgSeize.setVisibility(View.GONE);
                        layoutSliding.setVisibility(View.GONE);
                        mCameraStreamingManager.stopStreaming();
                        break;
                    case CameraStreamingManager.STATE.NETBLOCKING:
                        statusText.setText("网络状况不好");
                        break;
                    case CameraStreamingManager.STATE.SENDING_BUFFER_FULL:
                        statusText.setText("缓存区满");
                        break;
                    case CameraStreamingManager.STATE.DISCONNECTED:
                        statusText.setText("连接中断");
                        imgUpload.setVisibility(View.VISIBLE);
                        imgUpload.setClickable(true);
                        imgSeize.setVisibility(View.GONE);
                        layoutSliding.setVisibility(View.GONE);
                        mCameraStreamingManager.startStreaming();
                        break;

                }
            }
        });

    }


    @Override
    public boolean onStateHandled(final int status, Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case CameraStreamingManager.STATE.SENDING_BUFFER_HAS_FEW_ITEMS:
                        break;
                    case CameraStreamingManager.STATE.SENDING_BUFFER_HAS_MANY_ITEMS:
                        break;
                }
            }
        });
        return true;
    }

    @Override
    public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(streamStatus.totalAVBitrate / 1024 + " kb/s");
            }
        });
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


    private void publishVideo() {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("videoUrl", HttpConstants.getLiveServerUrl() + mediaId);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(22,
                HttpConstants.getUserUrl() + "/publishVideo", this, valueMap, LoginResBean.class);

        //增加注释
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void publishHlsVideo() {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("videoUrl", "http://219.143.237.232:8080/hls/" + mediaId + ".m3u8");
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(21,
                HttpConstants.getUserUrl() + "/publishVideo", this, valueMap, LoginResBean.class);

        //增加注释
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case 22:
                if (obj != null) {
                }
                break;
            case 21:
                publishVideo();
                break;
        }
    }

    @Override
    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {

    }


    private static boolean isSupportHWEncode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

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
                discussListView.setSelection(discussList.size());
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
                discussListView.setSelection(discussList.size());
                txtDiscuss.setText("");
                break;
            case DISCUSS_NOTIFY_FAILED:
                ToastUtil.showLongToast(this, getString(R.string.operation_fail));
                break;
            case RECEIVE_DISCUSS_NOTIFY_OK:
                discussList.add(bean);
                discussListAdapter.notifyDataSetChanged();
                discussListView.setSelection(discussList.size());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4SeizeNotifyEvent(SeizeNotifyEvent event) {
        SeizeNotifyEvent.SeizeNotifyBean bean = event.bean;
        if (!bean.getLiveId().equals(liveId)) {
            return;
        }
        switch (event.event) {
            case LIVE_SEIZE_START_OK:
                ToastUtil.showLongToast(this, "已发布抢播通知");
                //  渲染倒计时数据，清除当前胜出者。
                imgSeize.setClickable(true);
                imgSeize.setVisibility(View.GONE);
                txtCount.setVisibility(View.VISIBLE);
                txtCount.setText(String.valueOf(30));
                new TimeCount(30000, 1000).start();
                relayUserNo = null;
                relayCount = 0;
                break;
            case LIVE_SEIZE_REPORT_OK:
                // 决出抢播胜出者
                if (bean.getCount() > relayCount) {
                    relayCount = bean.getCount();
                    relayUserNo = bean.getUserNo();
                }
                break;
            case LIVE_SEIZE_STOP_OK:
                ToastUtil.showLongToast(this, "已决出续播者(" + imService.getContactManager().findContact(relayUserNo).getNickName() + ")，并发出通知！");
                imgSeize.setVisibility(View.VISIBLE);
                //TODO  关闭播放界面开启播放界面，需要过场控制

                VideoProgram videoProgram = null;
                try {
                    videoProgram = JujuDbUtils.getInstance().selector(VideoProgram.class).where("id", "=", liveId).findFirst();
                } catch (DbException e) {
                    e.printStackTrace();
                }

                Intent intent = getIntent();
                intent.putExtra(Constants.LIVE_ID,videoProgram.getId());
                intent.putExtra(Constants.VIDEO_URL,videoProgram.getVideoUrl());
                mCameraStreamingManager.stopStreaming();
                mCameraStreamingManager.destroy();
                this.setResult(RESULT_OK,intent);
                liveId = null;
                ActivityUtil.finish(this);
                break;
        }
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

            //计数统计完毕，通知续播
            txtCount.setVisibility(View.GONE);
            SeizeNotifyEvent.SeizeNotifyBean seizeStartBean = new SeizeNotifyEvent.SeizeNotifyBean();
            seizeStartBean.setGroupId(groupId);
            seizeStartBean.setLiveId(liveId);
            seizeStartBean.setCount(relayCount);
            if (relayUserNo == null) {
                relayUserNo = "19400000005";
            }
            seizeStartBean.setUserNo(relayUserNo);
            LiveSeizeStopNotify.instance().executeCommand4Send(seizeStartBean);
        }

        @Override
        public void onTick(long millisUntilFinished) {//计时过程显示
            long leftTime = millisUntilFinished / 1000;
            txtCount.setText(String.valueOf(leftTime));
        }
    }

}
