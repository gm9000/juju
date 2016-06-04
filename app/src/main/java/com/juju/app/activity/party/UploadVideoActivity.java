package com.juju.app.activity.party;

import android.content.ClipboardManager;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
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
import com.juju.app.bean.json.LiveAddressResBean;
import com.juju.app.bean.json.LoginResBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.CameraUtil;
import com.juju.app.utils.ToastUtil;
import com.pili.pldroid.streaming.CameraStreamingManager;
import com.pili.pldroid.streaming.CameraStreamingSetting;
import com.pili.pldroid.streaming.StreamStatusCallback;
import com.pili.pldroid.streaming.StreamingEnv;
import com.pili.pldroid.streaming.StreamingProfile;
import com.pili.pldroid.streaming.widget.AspectFrameLayout;
import com.rey.material.app.BottomSheetDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ContentView(R.layout.activity_party_video)
@SystemColor(isApply=false)
public class UploadVideoActivity extends BaseActivity implements View.OnClickListener, CameraStreamingManager.StreamingStateListener, HttpCallBack4OK, StreamStatusCallback {

    private static final String TAG = "UploadVideoActivity";

    @ViewInject(R.id.img_change)
    private ImageView imgChange;

    @ViewInject(R.id.statusText)
    private TextView statusText;

    @ViewInject(R.id.img_upload)
    private ImageView imgUpload;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 防止锁屏
        initParam();
        initView();
        initListener();
        previewCameraVideo();
    }

    @Override
    protected void onStop(){
        super.onStop();
        mCameraStreamingManager.destroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraStreamingManager.destroy();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mCameraStreamingManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraStreamingManager.pause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void previewCameraVideo() {

        mediaId = UUID.randomUUID().toString();

        JSONObject mJSONObject = new JSONObject();
        //        this.streamId = var1.getString("id");
        try {
            mJSONObject.put("id",new String("123456"));
            mJSONObject.put("hub",new String("juju"));
            mJSONObject.put("title","12345");
//            mJSONObject.put("title",mediaId);
            mJSONObject.put("publishKey",new String("123456"));
            mJSONObject.put("publishSecurity",new String("static"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject hosts = new JSONObject();
        JSONObject publish = new JSONObject();
        try {
            publish.put("rtmp", GlobalVariable.liveServerIp+":"+GlobalVariable.liveServerPort);
            hosts.put("publish",publish);
            mJSONObject.put("hosts",hosts);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024);
        StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(15, 200*1000, 15);
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);

        StreamingProfile.Stream stream = new StreamingProfile.Stream(mJSONObject);

        StreamingProfile mProfile = new StreamingProfile();
        mProfile.setStream(stream);
        mProfile.setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
                .setPreferredVideoEncodingSize(640, 360)
                .setVideoQuality(StreamingProfile.VIDEO_QUALITY_LOW3)
                .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY)
                .setAVProfile(avProfile)
//                .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));

        CameraStreamingSetting mCameraStreamingSetting = new CameraStreamingSetting();
        mCameraStreamingSetting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
//                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(2000)
//                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);


        StreamingEnv.init(context);
        if(isSupportHWEncode()) {
            mCameraStreamingManager = new CameraStreamingManager(this, afl, glSurfaceView, CameraStreamingManager.EncodingType.HW_VIDEO_WITH_HW_AUDIO_CODEC);
        }else{
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

        layoutSliding.setSliderFadeColor(getResources().getColor(R.color.transparent));


        if(!CameraUtil.hasFrontFacingCamera()){
            imgChange.setVisibility(View.GONE);
        }
        layoutSliding.setVisibility(View.GONE);

    }


    @Event(R.id.img_change)
    private void changeCamera(View view){
        mCameraStreamingManager.switchCamera();
    }

    @Event(R.id.img_close)
    private void closeVideo(View view){
        mCameraStreamingManager.stopStreaming();

//        ActivityUtil.finish(this);
    }

    @Event(R.id.img_upload)
    private void uploadVideo(View view){
        mCameraStreamingManager.startStreaming();
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



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.txt_weixin:
                ToastUtil.showShortToast(this,"weixin",1);
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
                ClipboardManager cm = (ClipboardManager) getSystemService(this.CLIPBOARD_SERVICE);
                cm.setText("http://219.143.237.232:8080/hls/"+mediaId+".m3u8");
                ToastUtil.showShortToast(this,"视频地址已经复制到剪切板",1);
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
                        break;
                    case CameraStreamingManager.STATE.STREAMING:
                        statusText.setText("开始直播");
                        imgUpload.setVisibility(View.GONE);
                        layoutSliding.setVisibility(View.VISIBLE);
                        publishHlsVideo();
                        break;
                    case CameraStreamingManager.STATE.CAMERA_SWITCHED:
                        statusText.setText("切换摄像头");
                        break;
                    case CameraStreamingManager.STATE.CONNECTION_TIMEOUT:
                        statusText.setText("连接超时");
                        imgUpload.setVisibility(View.VISIBLE);
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
                switch(status) {
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
        valueMap.put("videoUrl", HttpConstants.getLiveServerUrl()+mediaId);
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
        valueMap.put("videoUrl", "http://219.143.237.232:8080/hls/"+mediaId+".m3u8");
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
                if(obj != null) {
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

}
