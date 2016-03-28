package com.juju.app.activity.party;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.bean.json.LiveAddressResBean;
import com.juju.app.bean.json.LoginResBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.media.encoder.MediaEnCoderFactory;
import com.juju.app.media.util.YuvProcess;
import com.juju.app.service.BitRateInfo;
import com.juju.app.service.ConnectStatus;
import com.juju.app.service.MediaProcessService;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.ToastUtil;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

@ContentView(R.layout.activity_play_video)
public class UploadVideoActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback, Camera.PreviewCallback, HttpCallBack {

    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.statusText)
    private TextView statusText;

    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;

    @ViewInject(R.id.menu_play_icon)
    private ImageView playBtn;

    @ViewInject(R.id.videoUrlText)
    private TextView videoUrlText;

    private SurfaceView videoPlayView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private int curCameraId;
    private Camera.Parameters parameters;


    private static int queueSize = 64;
    private ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(queueSize);
    private int width = 1280;
    private int height = 720;

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
        ViewUtils.inject(this);
//        if (android.os.Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }
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
        txt_left.setOnClickListener(this);
        img_back.setOnClickListener(this);
        img_right.setOnClickListener(this);
    }

    private void initParam() {
//        previewRate = ScreenUtil.getScreenRate(this);
        previewRate = 4f/3f;
    }

    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.live);

        txt_title.setText("节目");
        img_right.setImageResource(R.mipmap.camera);
        img_right.setVisibility(View.VISIBLE);

        videoPlayView = (SurfaceView) findViewById(R.id.video_play_view);
    }


    private void initMediaProcessService(){
        Intent intent = new Intent(this,MediaProcessService.class);
        boolean bindFlag = bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    public void onTabClicked(View view) throws IOException {
        switch (view.getId()) {
            case R.id.menu_capture:
                ToastUtil.showShortToast(this, "点击抓拍", 1);
                break;
            case R.id.menu_play:
                if (mediaProcessService.isProcess()) {
                    ToastUtil.showShortToast(this, "点击停止", 1);

                    playBtn.setImageResource(R.mipmap.play);
                    mediaProcessService.stopEncoding();
                } else {
                    playBtn.setImageResource(R.mipmap.stop);
                    JlmHttpClient client = new JlmHttpClient(12, HttpConstants.getLiveServerUrl() + "/apply_for_upload", this, null, LiveAddressResBean.class);

                    try {
                        client.sendGetNoCache();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case R.id.menu_discuss:
                ToastUtil.showShortToast(this, "点击评论", 1);
                break;
        }
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

                ToastUtil.showShortToast(this, "更换摄像头", 1);
                changeCamera();
                break;
        }
    }

    private void changeCamera() {
        if (camera == null) {
            return;
        }
        camera.setPreviewCallback(null);
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
                mCamera.setPreviewCallback(this);
                if (parameters == null) {
                    parameters = mCamera.getParameters();
                }
                parameters = mCamera.getParameters();

                mCamera.setDisplayOrientation(90);
//                parameters.set("rotation", 90);
//                parameters.set("orientation","landscape");
//                parameters.setRotation(90);
//                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
////                    camera.setDisplayOrientation(90);//针对android2.2和之前的版本
//                    parameters.setRotation(90);//去掉android2.0和之前的版本
//
//                    setCameraDisplayOrientation(UploadVideoActivity.this,curCameraId,camera);
//                } else {
//                    parameters.set("orientation", "landscape");
//                    camera.setDisplayOrientation(0);
//                    parameters.setRotation(0);
//                }
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
                parameters.setAutoWhiteBalanceLock(true);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();



                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ScreenUtil.ViewgetScreenWidth(this),ScreenUtil.ViewgetScreenWidth(this) *width / height);
                layoutParams.topMargin = (ScreenUtil.getScreenWidth(this)*height/width - ScreenUtil.getScreenWidth(this)*width/height) / 2;
                videoPlayView.setLayoutParams(layoutParams);

                RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(ScreenUtil.ViewgetScreenWidth(this),ScreenUtil.getScreenHeight(this)-110-(ScreenUtil.ViewgetScreenWidth(this) * height / width));
                listViewParams.topMargin = 50 + ScreenUtil.ViewgetScreenWidth(this) * height/width;
                discussListView.setLayoutParams(listViewParams);

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
    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
        switch (accessId) {
            case 12:
                if(obj != null && obj.length > 0) {
                    LiveAddressResBean liveAddressBean = (LiveAddressResBean)obj[0];
                    mediaProcessService.setUpdateUrl(liveAddressBean.getUploadUrl());
                    videoUrlText.setText(liveAddressBean.getDownloadUrl());

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
                }
                break;
            case 22:
                if(obj != null && obj.length > 0) {
                }
                break;
        }
    }

    @Override
    public void onFailure(HttpException error, String msg, int accessId) {
        switch (accessId) {
            case 12:
                playBtn.setImageResource(R.mipmap.play);
                statusText.setText("服务器连接失败!");
                break;
            case 22:
                statusText.setText("视频地址发布失败!");
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
        if(mediaProcessService.isProcess() && MediaEnCoderFactory.sSuggestedMode==MediaEnCoderFactory.MODE_MEDIACODEC_API) {
            //  90度旋转
            data = YuvProcess.rotateYUV240SP(data, width, height);
            //  横屏裁剪
            int cutVideoWidth = height;
            int cutVideoHeight = cutVideoWidth * height / width;
            while (cutVideoHeight % 16 != 0) {
                cutVideoHeight++;
            }
            ByteBuffer cutByteBuffer = ByteBuffer.allocate(cutVideoWidth * cutVideoHeight * 3 / 2);
            cutByteBuffer.put(data, cutVideoWidth * (width - cutVideoHeight) / 2, cutVideoWidth * cutVideoHeight);
            cutByteBuffer.put(data, width * height + cutVideoWidth * (width - cutVideoHeight) / 4, cutVideoWidth * cutVideoHeight / 2);
            putYUVData(cutByteBuffer.array());
        }
    }

    private void putYUVData(byte[] buffer) {
        if (YUVQueue.size() >= queueSize) {
            System.out.println(width + ":" + height + "------------------------------------- skip a frame YUV packege:" + YUVQueue.poll().length);
//            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }


    private void realaseResource(){
        if (null != camera) {
            camera.setPreviewCallback(null);
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
            mediaProcessService.setWidth(width);
            mediaProcessService.setHeight(height);

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
                playBtn.setImageResource(R.mipmap.play);
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


}
