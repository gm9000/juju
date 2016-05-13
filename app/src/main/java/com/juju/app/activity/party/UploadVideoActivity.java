package com.juju.app.activity.party;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.bean.json.LiveAddressResBean;
import com.juju.app.bean.json.LoginResBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.media.encoder.MediaEnCoderFactory;
import com.juju.app.media.util.YuvProcess;
import com.juju.app.service.BitRateInfo;
import com.juju.app.service.ConnectStatus;
import com.juju.app.service.MediaProcessService;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.CameraUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.CustomDialog;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;
import com.rey.material.app.BottomSheetDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.xutils.common.Callback;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

@ContentView(R.layout.activity_play_video)
public class UploadVideoActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, HttpCallBack {

    @ViewInject(R.id.layout_main)
    private RelativeLayout layoutMain;

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


    private SurfaceView videoPlayView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private int curCameraId;
    private Camera.Parameters parameters;



    private byte[] mPreBuffer = null;

    private static int queueSize = 64;
    private ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(queueSize);
    private int width = 720;
    private int height = 1280;

    private static int audioQueueSize = 256;
    private ArrayBlockingQueue<byte[]> AACQueue = new ArrayBlockingQueue<byte[]>(audioQueueSize);
    //    final int kSampleRates[] = { 8000, 11025, 22050, 44100, 48000 };
    private int sampleRate = 44100;
    //    final int kBitRates[] = { 64000, 128000 };
    private int maxAudioFrameSize;
    private AudioRecord mAudioRecord;

    private MediaProcessService mediaProcessService;

    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private float previewRate;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 防止锁屏
        initParam();
        initView();
        initListener();

        previewCameraVideo();
        initMediaProcessService();
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

    }

    @Override
    protected void onStop(){
        super.onStop();
        realaseResource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realaseResource();
        unbindService(conn);
        mediaProcessService.stopEncoding();
        realaseResource();
        EventBus.getDefault().unregister(this);
    }



    private void previewCameraVideo() {
        surfaceHolder = videoPlayView.getHolder();
        surfaceHolder.addCallback(this);
    }


    private void initListener() {
        layoutSliding.setPanelSlideListener(new SliderListener());
    }

    private void initParam() {
//        previewRate = ScreenUtil.getScreenRate(this);
        previewRate = 16f/9f;
    }

    private void initView() {
        videoPlayView = (SurfaceView) findViewById(R.id.video_play_view);

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


    private void initMediaProcessService(){
        Intent intent = new Intent(this,MediaProcessService.class);
        boolean bindFlag = bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Event(R.id.img_change)
    private void changeCamera(View view){
        if (camera == null) {
            return;
        }
        camera.setPreviewCallbackWithBuffer(null);
        camera.stopPreview();
        camera.release();
        camera = null;
        if (curCameraId == 0) {
            curCameraId = 1;
        } else {
            curCameraId = 0;
        }
        camera = getCamera(curCameraId);
        startCamera(camera);
    }

    @Event(R.id.img_close)
    private void closeVideo(View view){
        if (mediaProcessService.isProcess()) {
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setMessage(getString(R.string.close_live));
            builder.setPositiveButton(R.string.confirm,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            mediaProcessService.stopEncoding();
                            dialog.dismiss();
                            ActivityUtil.finish(UploadVideoActivity.this);
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
        JlmHttpClient client = new JlmHttpClient(12, HttpConstants.getLiveServerUrl() + "/apply_for_upload", this, null, LiveAddressResBean.class);
        try {
            client.sendGetNoCache();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.img_share)
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
    public void surfaceCreated(SurfaceHolder holder) {
        curCameraId = 0;
        camera = getCamera(curCameraId);
        startCamera(camera);
        startAudioRecorder();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        realaseResource();
    }

    private void startCamera(Camera mCamera) {
        if (mCamera != null) {
            try {
                parameters = mCamera.getParameters();

//                mCamera.setDisplayOrientation(90);
//                parameters.set("rotation", 90);
//                parameters.set("orientation","landscape");
//                parameters.setRotation(90);
//                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
//                    camera.setDisplayOrientation(90);//针对android2.2和之前的版本
//                    parameters.setRotation(180);//去掉android2.0和之前的版本
//                } else {
//                    parameters.set("orientation", "landscape");
//                    camera.setDisplayOrientation(0);
//                    parameters.setRotation(0);
//                }


                setCameraDisplayOrientation(curCameraId,camera);

                parameters.setPreviewFormat(ImageFormat.NV21);

                //设置PictureSize
                Camera.Size pictureSize = getPropPictureSize(parameters.getSupportedPictureSizes(), previewRate, 840);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);

                //设置PreviewSize
                Camera.Size previewSize = getPropPreviewSize(parameters.getSupportedPreviewSizes(), previewRate, 840);
                parameters.setPreviewSize(previewSize.width, previewSize.height);
                width = previewSize.width;
                height = previewSize.height;

                parameters.setPreviewFrameRate(25);
//                parameters.setAutoWhiteBalanceLock(true);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(surfaceHolder);

                mPreBuffer = new byte[width*height*3/2];
                mCamera.addCallbackBuffer(mPreBuffer);
                mCamera.setPreviewCallbackWithBuffer(this);
                mCamera.startPreview();

//                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ScreenUtil.ViewgetScreenWidth(this),ScreenUtil.ViewgetScreenWidth(this) *width / height);
//                layoutParams.topMargin = (ScreenUtil.getScreenWidth(this)*height/width - ScreenUtil.getScreenWidth(this)*width/height) / 2;
//                videoPlayView.setLayoutParams(layoutParams);
//
//                RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(ScreenUtil.ViewgetScreenWidth(this),ScreenUtil.getScreenHeight(this)-110-(ScreenUtil.ViewgetScreenWidth(this) * height / width));
//                listViewParams.topMargin = 50 + ScreenUtil.ViewgetScreenWidth(this) * height/width;
//                discussListView.setLayoutParams(listViewParams);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startAudioRecorder(){

        maxAudioFrameSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,maxAudioFrameSize);
        mAudioRecord.startRecording();
    }

    @TargetApi(9)
    private Camera getCamera(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    public Camera.Size getPropPreviewSize(List<Camera.Size> list, float th, int minWidth) {
        Collections.sort(list, sizeComparator);
        int i = 0;
        for (Camera.Size s : list) {
            if ((s.width >= minWidth) && equalRate(s, th)) {
                break;
            }
            i++;
        }
        if (i == list.size()) {
            if(list.size()>1){
                i = list.size()-2;
            }else{
                i--;
            }
        }

        return list.get(i);
    }

    public Camera.Size getPropPictureSize(List<Camera.Size> list, float th, int minWidth) {
        Collections.sort(list, sizeComparator);
        int i = 0;
        for (Camera.Size s : list) {
            if ((s.width >= minWidth) && equalRate(s, th)) {
                break;
            }
            i++;
        }
        if (i == list.size()) {
            if(list.size()>1){
                i = list.size()-2;
            }else{
                i--;
            }
           //如果没找到，就选次大的size
        }
        return list.get(i);
    }

    public boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        if (Math.abs(r - rate) <= 0.2) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onSuccess(Object obj, int accessId) {
        switch (accessId) {
            case 12:
                if(obj != null) {
                    LiveAddressResBean liveAddressBean = (LiveAddressResBean)obj;
                    mediaProcessService.setUpdateUrl(liveAddressBean.getUploadUrl());

                    Map<String, Object> valueMap = new HashMap<String, Object>();
                    valueMap.put("videoUrl", liveAddressBean.getDownloadUrl());
                    JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(22,
                            HttpConstants.getUserUrl() + "/publishVideo", this, valueMap, LoginResBean.class);

                    //增加注释
                    try {
                        client.sendPost();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mediaProcessService.startEncoding();
                    imgUpload.setVisibility(View.GONE);
                    layoutSliding.setVisibility(View.VISIBLE);
                }
                break;
            case 22:
                if(obj != null) {
                }
                break;
        }
    }

    @Override
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId) {
        System.out.println("accessId:" + accessId + "\r\n isOnCallback:" + isOnCallback );
        Log.e("UploadVideoActivity", "onFailure", ex);
        switch (accessId) {
            case 12:
                statusText.setText("连接失败!");
                break;
            case 22:
                statusText.setText("视频地址发布失败!");
                break;
        }
    }

    @Override
    public void onCancelled(Callback.CancelledException cex) {

    }

    @Override
    public void onFinished() {

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
                ToastUtil.showShortToast(this,"copy",1);
                shareDialog.dismiss();
                break;
            case R.id.txt_cancel:
                shareDialog.dismiss();
                break;
        }
    }

    public class CameraSizeComparator implements Comparator<Camera.Size> {
        //按升序排列
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        //  YUV数据旋转
        if (curCameraId==0) {
            data = YuvProcess.rotate90YUV240SP(data, width, height);
        }else{
            data = YuvProcess.rotate270YUV240SP(data, width, height);
        }
//            //  横屏裁剪
//            int cutVideoWidth = height;
//            int cutVideoHeight = cutVideoWidth * height / width;
//            while (cutVideoHeight % 16 != 0) {
//                cutVideoHeight++;
//            }
//            ByteBuffer cutByteBuffer = ByteBuffer.allocate(cutVideoWidth * cutVideoHeight * 3 / 2);
//            cutByteBuffer.put(data, cutVideoWidth * (width - cutVideoHeight) / 2, cutVideoWidth * cutVideoHeight);
//            cutByteBuffer.put(data, width * height + cutVideoWidth * (width - cutVideoHeight) / 4, cutVideoWidth * cutVideoHeight / 2);
//            putYUVData(cutByteBuffer.array());
        if(mediaProcessService.isProcess() && MediaEnCoderFactory.sSuggestedMode==MediaEnCoderFactory.MODE_MEDIACODEC_API) {
            putYUVData(data);
        }
        mPreBuffer = new byte[data.length];
        System.arraycopy(data,0,mPreBuffer,0,data.length);
        camera.addCallbackBuffer(mPreBuffer);
    }

    private void putYUVData(byte[] buffer) {
        if (YUVQueue.size() >= queueSize) {
            System.out.println(width + ":" + height + "------------------------------------- skip a frame YUV packege:" + YUVQueue.poll().length);
            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }


    private void realaseResource(){
        if (null != camera) {
            camera.setPreviewCallbackWithBuffer(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if(null != mAudioRecord) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }


    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            mediaProcessService = ((MediaProcessService.MediaProcessBinder)service).getService();

            mediaProcessService = new MediaProcessService();
            mediaProcessService.setCamera(camera);
            mediaProcessService.setSurfaceHolder(surfaceHolder);
            mediaProcessService.setContext(UploadVideoActivity.this);
            mediaProcessService.setWidth(height);
            mediaProcessService.setHeight(width);

            mediaProcessService.setYUVQueue(YUVQueue);
            mediaProcessService.setQueueSize(queueSize);

            mediaProcessService.setmAudioRecord(mAudioRecord);
            mediaProcessService.setMaxAudioFrameSize(maxAudioFrameSize);
            mediaProcessService.setAudioQueueSize(audioQueueSize);

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
      public void showStatus(ConnectStatus connStatus) {
        statusText.setText(connStatus.getConnectDesc());
        switch (connStatus.getStatus()){
            case 1:
                imgUpload.setVisibility(View.VISIBLE);
                layoutSliding.setVisibility(View.GONE);
                if(mediaProcessService!=null && mediaProcessService.isProcess()) {
                    mediaProcessService.stopEncoding();
                }
                break;
            case 2:
                if(mediaProcessService!=null && mediaProcessService.isProcess()) {
                    mediaProcessService.stopEncoding();
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showStatus(BitRateInfo bitRateInfo){
        statusText.setText("码率: "+bitRateInfo.getBitRate()+"kb");
    }

    private void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
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
