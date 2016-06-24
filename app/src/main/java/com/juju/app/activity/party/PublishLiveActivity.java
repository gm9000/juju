package com.juju.app.activity.party;

import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.juju.app.media.rtmp.RtmpPublisher;
import com.juju.app.media.rtmp.encode.JlmEncoder;
import com.juju.app.media.rtmp.encode.MediaCodecEncoder;
import com.juju.app.media.rtmp.muxer.JlmFlvMuxer;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.CameraUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.CustomDialog;
import com.rey.material.app.BottomSheetDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;


@ContentView(R.layout.activity_publish_live)
public class PublishLiveActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "PublishLiveActivity";

    @ViewInject(R.id.layout_main)
    private RelativeLayout layoutMain;

    @ViewInject(R.id.videoSurfaceView)
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private int mCameraId = 0;
    private byte[] mYuvFrameBuffer;

    private int videoWidth = 360;
    private int videoHeight = 640;
    private float previewRate = 16f/9f;
    private int frameRate = 15;
    private String liveUrl = "rtmp://219.143.237.232/juju/12345";

    private AudioRecord audioRecord = null;
    private boolean aloop;
    private boolean soundOn = true;

    @ViewInject(R.id.img_change)
    private ImageView imgChange;
    @ViewInject(R.id.img_sound)
    private ImageView imgSound;

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

    private JlmFlvMuxer flvMuxer = new JlmFlvMuxer(new RtmpPublisher.EventHandler() {

        @Override
        public void onRtmpConnecting(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText("连接中...");
                }
            });
        }

        @Override
        public void onRtmpConnected(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("开始直播");
                            imgUpload.setVisibility(View.GONE);
                            imgSound.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {
        }

        @Override
        public void onRtmpAudioStreaming(String msg) {
        }

        @Override
        public void onRtmpStopped(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText("停止直播");
                }
            });
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imgUpload.setVisibility(View.VISIBLE);
                    imgSound.setVisibility(View.GONE);
                    statusText.setText("onRtmpDisconnected");
                }
            });
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText(String.valueOf(fps)+"Kb/s");
                }
            });
        }
    });

    private JlmEncoder mEncoder;
    private Thread aworker;

    private Handler uiHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 防止锁屏

        mEncoder = new MediaCodecEncoder(flvMuxer);

        initView();
        initListener();
        startCamera();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                final String notifyMsg = ex.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopPublish();
                        //  Broken pipe  Not connected to RTMP server
                        //  No get result Publish.start
                        //  xxxxxx kb/s
                        statusText.setText(notifyMsg);

                        uiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startPublish();
                            }
                        }, 500);
                    }
                });
            }
        });


    }

    @Override
    protected void onStop(){
        super.onStop();
        stopPublish();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPublish();
        stopCamera();
    }

    @Override
    protected void onResume(){
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initListener() {
        layoutSliding.setPanelSlideListener(new SliderListener());
        mSurfaceHolder.addCallback(this);
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
//        layoutSliding.setVisibility(View.GONE);

        mSurfaceHolder = mSurfaceView.getHolder();

    }


    private void startCamera() {
        if (mCamera != null) {
            Log.d(TAG, "start camera, already started. return");
            return;
        }
        if (mCameraId > (Camera.getNumberOfCameras() - 1) || mCameraId < 0) {
            Log.e(TAG, "####### start camera failed, inviald params, camera No.="+ mCameraId);
            return;
        }
        mCamera = Camera.open(mCameraId);
        mEncoder.swithCamera(mCameraId);
        Camera.Parameters params = mCamera.getParameters();
		/* preview size  */
        Camera.Size previewSize = getPropSize(params.getSupportedPreviewSizes(), previewRate, 640);
        Camera.Size pictureSize = getPropSize(params.getSupportedPictureSizes(), previewRate, 640);

        fixSurfaceViewSize(ScreenUtil.getScreenWidth(this), ScreenUtil.getScreenHeight(this),previewSize.height,previewSize.width);

        if (previewSize == null) {
            Log.e(TAG,"phone not support preview size :"+previewSize.width+"*"+previewSize.height);
        }

        /* picture size  */
        if (pictureSize == null) {
            Log.e(TAG,"phone not support picture size :"+pictureSize.width+"*"+pictureSize.height);
        }

        /***** set parameters *****/
        //params.set("orientation", "portrait");
        //params.set("orientation", "landscape");
        //params.setRotation(90);
        params.setPictureSize(pictureSize.width,pictureSize.height);
        params.setPreviewSize(previewSize.width,previewSize.height);

        int[] range = findClosestFpsRange(frameRate,params.getSupportedPreviewFpsRange());
        params.setPreviewFpsRange(range[0], range[1]);
        params.setPreviewFormat(JlmEncoder.VFORMAT);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(90);

        mYuvFrameBuffer = new byte[previewSize.width * previewSize.height * 3 / 2];
        mCamera.addCallbackBuffer(mYuvFrameBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    private void fixSurfaceViewSize(int screenWidth, int screenHeight, int videoWidth, int videoHeight) {

        if(screenHeight*videoWidth - screenWidth*videoHeight < 17){
            return;
        }

        int surfaceWidth = screenWidth;
        int surfaceHeight = screenHeight;
        int yMargin = 0;
        int xMargin = 0;
        if(screenHeight*videoWidth > screenWidth*videoHeight){
            //  屏幕高于视频,需要充满屏幕高度
            surfaceWidth = surfaceHeight*videoWidth/videoHeight;
            xMargin = (screenWidth-surfaceWidth)/2;
        }else{
            //  屏幕低于视频,需要充满屏幕宽度
            surfaceHeight = surfaceWidth*videoHeight/videoWidth;
            yMargin = (screenHeight-surfaceHeight)/2;
        }
        RelativeLayout.LayoutParams layoutParam = new RelativeLayout.LayoutParams(surfaceWidth,surfaceHeight);
        layoutParam.topMargin = yMargin;
        layoutParam.bottomMargin = yMargin;
        layoutParam.leftMargin = xMargin;
        layoutParam.rightMargin = xMargin;
        mSurfaceView.setLayoutParams(layoutParam);

    }

    private void stopCamera() {
        if (mCamera != null) {
            // need to SET NULL CB before stop preview!!!
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }




    @Event(R.id.img_change)
    private void changeCamera(View view){
        if (mCamera == null) {
            return;
        }
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        if (mCameraId == 0) {
            mCameraId = 1;
        } else {
            mCameraId = 0;
        }
        startCamera();
    }


    @Event(R.id.img_sound)
    private void soundOnOff(View view){
        if(soundOn){
            imgSound.setImageResource(R.mipmap.unsound);
            soundOn = false;
        }else{
            soundOn = true;
            imgSound.setImageResource(R.mipmap.sound);
        }
    }

    @Event(R.id.img_close)
    private void closeVideo(View view){
        if (mEncoder.isEncoding()) {
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setMessage(getString(R.string.close_live));
            builder.setPositiveButton(R.string.confirm,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            stopPublish();
                            dialog.dismiss();
                            ActivityUtil.finish(PublishLiveActivity.this);
                        }
                    });
            builder.setNegativeButton(R.string.negative,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
        }else{
            ActivityUtil.finish(this);
        }
    }

    @Event(R.id.img_upload)
    private void uploadVideo(View view){
        startPublish();
    }

    //  start avEncode and start rtmppublish
    private void startPublish(){
        try {
            flvMuxer.start(liveUrl);
        } catch (IOException e) {
            Log.e(TAG, "start FLV muxer failed.");
            e.printStackTrace();
            return;
        }
        flvMuxer.setVideoResolution(videoWidth, videoHeight);
        startEncoder();
    }


    //  stop avEncode and stop rtmppublish
    private void stopPublish(){
        stopAudio();
        mEncoder.stop();
        flvMuxer.stop();
        imgUpload.setVisibility(View.VISIBLE);
        imgSound.setVisibility(View.GONE);
        statusText.setText("");
    }



    private void startEncoder() {

        startCamera();

        int ret = mEncoder.start();
        if (ret < 0) {
            return;
        }

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                startAudio();
            }
        });
        aloop = true;
        aworker.start();
    }

    private void startAudio() {
        if (audioRecord != null) {
            return;
        }

        int bufferSize = 2 * AudioRecord.getMinBufferSize(JlmEncoder.ASAMPLERATE, JlmEncoder.ACHANNEL, JlmEncoder.AFORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, JlmEncoder.ASAMPLERATE, JlmEncoder.ACHANNEL, JlmEncoder.AFORMAT, bufferSize);
        audioRecord.startRecording();

        byte pcmBuffer[] = new byte[4096];
        while (aloop && !Thread.interrupted()) {
            int size = audioRecord.read(pcmBuffer, 0, pcmBuffer.length);
            if (size <= 0) {
                Log.e(TAG, "***** audio ignored, no data to read.");
                break;
            }
            onGetPcmFrame(pcmBuffer, size);
        }
    }

    private void onGetPcmFrame(byte[] pcmBuffer, int size) {
        if(soundOn) {
            mEncoder.onGetPcmFrame(pcmBuffer, size);
        }
    }
    private void stopAudio() {
        aloop = false;
        aworker = null;

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
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
                cm.setText("http://219.143.237.232:8080/hls/12345.m3u8");
                ToastUtil.showShortToast(this,"视频地址已经复制到剪切板",1);
                shareDialog.dismiss();
                break;
            case R.id.txt_cancel:
                shareDialog.dismiss();
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        onGetYuvFrame(data);
        camera.addCallbackBuffer(mYuvFrameBuffer);

    }

    private void onGetYuvFrame(byte[] data) {
        if(mEncoder!=null && mEncoder.isEncoding()) {
            mEncoder.onGetYuvFrame(data);
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


    private static int[] findClosestFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }


    public Camera.Size getPropSize(List<Camera.Size> list, float th, int minWidth) {
        int index = -1;
        float diffValue = Float.MAX_VALUE;
        for (int i=0; i<list.size();i++) {
            Camera.Size s = list.get(i);
            if(s.width!=minWidth){
                continue;
            }
            if(diffRate(s,th)<diffValue){
                index = i;
            }

        }
        if(index > -1) {
            return list.get(index);
        }else{
            return null;
        }
    }

    public float diffRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        return Math.abs(r - rate);
    }


}
