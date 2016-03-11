package com.juju.app.activity.party;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.media.AvcEncoder;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.CustomerVideoView;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

@ContentView(R.layout.activity_play_video)
public class PlayVideoActivity extends AppCompatActivity implements View.OnClickListener,SurfaceHolder.Callback,Camera.PreviewCallback {

    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.video_play_win)
    private CustomerVideoView videoView;
    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;

    @ViewInject(R.id.menu_play_icon)
    private ImageView playBtn;

    private SurfaceView videoPlayView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Camera.Parameters parameters;
    private static int yuvqueuesize = 25;
    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);
    private int width = 1280;
    private int height = 720;
    int framerate = 25;
    int biterate = 8500*1000;

    private AvcEncoder avcCodec;

    private boolean vieoIsPlaying;
    private boolean isUploading;

    private String playMode;

    private Uri hlsUri;

    private int curCameraId;

    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private float previewRate;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.inject(this);
        initParam();
        initView();
        initListener();
        if(playMode.equals("watch")){
            playVide();
        }
        if(playMode.equals("upload")){
            previewCameraVideo();
        }

        SupportAvcCodec();

    }

    private void previewCameraVideo() {
        videoPlayView = (SurfaceView)findViewById(R.id.video_play_view);
        surfaceHolder = videoPlayView.getHolder();
        surfaceHolder.addCallback(this);
    }

    private void initListener() {
        txt_left.setOnClickListener(this);
        img_back.setOnClickListener(this);
        img_right.setOnClickListener(this);
    }

    private void initParam() {
        playMode = this.getIntent().getStringExtra("playMode");
        if(playMode == null){
            playMode = "watch";
        }
        ToastUtil.showShortToast(this, playMode, 1);
        if(playMode.equals("watch")){
            hlsUri = Uri.parse("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");
        }else{
            isUploading = false;
        }

        previewRate = ScreenUtil.getScreenRate(this);
    }

    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.live);

        txt_title.setText("节目");
        if(playMode.equals("upload")){
            videoView.setVisibility(View.GONE);
            img_right.setImageResource(R.mipmap.camera);
            img_right.setVisibility(View.VISIBLE);
        }


    }

    public void onTabClicked(View view) throws IOException {
        switch (view.getId()) {
            case R.id.menu_capture:
                ToastUtil.showShortToast(this, "点击抓拍", 1);
                break;
            case R.id.menu_play:
                if (vieoIsPlaying || isUploading) {
                    ToastUtil.showShortToast(this, "点击停止", 1);
                    //TODO  增加视频停止播放或者上传的业务处理

                    if(playMode.equals("watch")) {
                        stopPlay();
                    }else{
                        isUploading = false;
                        playBtn.setImageResource(R.mipmap.play);
                        avcCodec.StopThread();
                    }
                } else {
                    ToastUtil.showShortToast(this, "点击播放", 1);
                    //TODO  增加视频开始播放或者上传的业务处理;
                    if(playMode.equals("watch")) {
                        playVide();
                    }else{
                        isUploading = true;
                        playBtn.setImageResource(R.mipmap.stop);
                        avcCodec = new AvcEncoder(width,height,framerate,biterate);
                        avcCodec.StartEncoderThread();
                    }

                }
                break;
            case R.id.menu_discuss:
                ToastUtil.showShortToast(this, "点击评论", 1);
                break;
        }
    }

    private void playVide() {

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
        switch(v.getId()){
            case R.id.txt_left:
                ActivityUtil.finish(this);
                break;
            case R.id.img_back:
                ActivityUtil.finish(this);
                break;
            case R.id.img_right:

                ToastUtil.showShortToast(this,"更换摄像头",1);
                changeCamera();
                break;
        }
    }

    private void changeCamera() {
        if(camera == null){
            return;
        }
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
        if(curCameraId == 0) {
            curCameraId = 1;
        }else{
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

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != camera) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if(isUploading){
            isUploading = false;
            avcCodec.StopThread();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        putYUVData(data, data.length);
    }

    public void putYUVData(byte[] buffer, int length) {
        if (YUVQueue.size() >= yuvqueuesize) {
            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }



    private void startCamera(Camera mCamera){
        if(mCamera != null){
            try {
                mCamera.setPreviewCallback(this);
                if (parameters == null){
                    parameters = mCamera.getParameters();
                }
                parameters = mCamera.getParameters();

                if(this.getResources().getConfiguration().orientation!= Configuration.ORIENTATION_LANDSCAPE){
                    parameters.set("orientation", "portrait");
                    camera.setDisplayOrientation(90);//针对android2.2和之前的版本
                    parameters.setRotation(90);//去掉android2.0和之前的版本
                }else{
                    parameters.set("orientation", "landscape");
                    camera.setDisplayOrientation(0);
                    parameters.setRotation(0);
                }
                parameters.setPreviewFormat(ImageFormat.NV21);
//                parameters.setPictureFormat(ImageFormat.NV21);
                //设置PictureSize
                Camera.Size pictureSize = getPropPictureSize(parameters.getSupportedPictureSizes(), previewRate, 352);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);
                width = pictureSize.width;
                height = pictureSize.height;
                //设置PreviewSize
                Camera.Size previewSize = getPropPreviewSize(parameters.getSupportedPreviewSizes(), previewRate, 352);

                parameters.setPreviewSize(previewSize.width, previewSize.height);
                width = pictureSize.width;
                height = pictureSize.height;
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ScreenUtil.ViewgetScreenWidth(this),ScreenUtil.ViewgetScreenWidth(this) * height/width);
                videoPlayView.setLayoutParams(layoutParams);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    public Camera.Size getPropPreviewSize(List<Camera.Size> list, float th, int minWidth){
        Collections.sort(list, sizeComparator);
        int i = 0;
        for(Camera.Size s:list){
            if((s.width >= minWidth) && equalRate(s, th)){
               break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;//如果没找到，就选最小的size
        }
        return list.get(i);
    }

    public Camera.Size getPropPictureSize(List<Camera.Size> list, float th, int minWidth){
        Collections.sort(list, sizeComparator);
        int i = 0;
        for(Camera.Size s:list){
            if((s.width >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;//如果没找到，就选最小的size
        }
        return list.get(i);
    }

    public boolean equalRate(Camera.Size s, float rate){
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.2)
        {
            return true;
        }
        else{
            return false;
        }
    }

    public  class CameraSizeComparator implements Comparator<Camera.Size> {
        //按升序排列
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width > rhs.width){
                return 1;
            }
            else{
                return -1;
            }
        }

    }

    @SuppressLint("NewApi")
    private boolean SupportAvcCodec(){
        if(Build.VERSION.SDK_INT>=18){
            for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--){
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);

                String[] types = codecInfo.getSupportedTypes();
                for (int i = 0; i < types.length; i++) {
                    if (types[i].equalsIgnoreCase("video/avc")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
