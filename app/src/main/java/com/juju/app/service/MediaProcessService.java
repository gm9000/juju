package com.juju.app.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.view.SurfaceHolder;

import com.juju.app.media.consumer.HttpMediaConsumer;
import com.juju.app.media.consumer.MediaConsumer;
import com.juju.app.media.consumer.OutputStreamMediaConsumer;
import com.juju.app.media.encoder.AudioEncoder;
import com.juju.app.media.encoder.MediaEnCoderFactory;
import com.juju.app.media.encoder.MediaEncoder;
import com.juju.app.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

public class MediaProcessService extends Service {


    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private Context context;
    private static int queueSize = 64;
    private static int audioQueueSize = 256;

    private ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(queueSize);
    private ArrayBlockingQueue<Object[]> H264Queue = new ArrayBlockingQueue<Object[]>(queueSize);
    private int width = 720;
    private int height = 1280;
    int framerate = 25;

    private ArrayBlockingQueue<Object[]> AACQueue = new ArrayBlockingQueue<Object[]>(audioQueueSize);
    //    final int kSampleRates[] = { 8000, 11025, 22050, 44100, 48000 };
    private int sampleRate = 44100;
    //    final int kBitRates[] = { 64000, 128000 };
    private int bitRate = 64000;
    private int maxAudioFrameSize;
    private int channelCount = 1;
    private AudioRecord mAudioRecord;

    private MediaEncoder mediaEncoder;
    private AudioEncoder audioEncoder;
    private MediaConsumer mediaConsumer;

    private String updateUrl;
    private boolean isProcess = false;

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setQueueSize(int queueSize) {
        MediaProcessService.queueSize = queueSize;
    }

    public void setAudioQueueSize(int audioQueueSize) {
        MediaProcessService.audioQueueSize = audioQueueSize;
    }

    public void setYUVQueue(ArrayBlockingQueue<byte[]> YUVQueue) {
        this.YUVQueue = YUVQueue;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setMaxAudioFrameSize(int mxAudioFrameSize){
        maxAudioFrameSize = mxAudioFrameSize;
    }

    public void setmAudioRecord(AudioRecord mAudioRecord) {
        this.mAudioRecord = mAudioRecord;
    }

    public MediaProcessService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaProcessBinder();
    }

    public void setUpdateUrl(String url) {
        this.updateUrl = url;
    }

    public class MediaProcessBinder extends Binder {
        /**
         * 获取当前Service的实例
         * @return
         */
        public MediaProcessService getService(){
            return MediaProcessService.this;
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent,int startId){
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopAllEncoderProcess();
    }

    public void startEncoding(){
        isProcess = true;
        YUVQueue.clear();
        H264Queue.clear();
//        int cutVideoWidth = height;
//        int cutVideoHeight = cutVideoWidth * height/width;
//        while(cutVideoHeight%16 != 0){
//            cutVideoHeight++;
//        }

//        mediaEncoder = new MediaRecorderEncoder(width, height, framerate, biterate, YUVQueue, H264Queue, queueSize);
        mediaEncoder = MediaEnCoderFactory.generateMediaEncoder(width, height,framerate, YUVQueue, H264Queue, queueSize);
        mediaEncoder.setCamera(camera);
        mediaEncoder.setSurfaceHolder(surfaceHolder);
        mediaEncoder.setContext(context);
        mediaEncoder.setWidth(width);
        mediaEncoder.setHeight(height);



        //      启动音视频传输
        mediaConsumer = new HttpMediaConsumer(H264Queue,AACQueue,updateUrl);

//        String fd = Environment.getExternalStorageDirectory()+"/juju_h264.h264";
//        File file = new File(fd);
//        if(!file.exists()){
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        OutputStream ouputStream = null;
//        try {
//            ouputStream = new FileOutputStream(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        mediaConsumer = new OutputStreamMediaConsumer(H264Queue,ouputStream);
        mediaConsumer.setWidth(mediaEncoder.getWidth());
        mediaConsumer.setHeight(mediaEncoder.getHeight());
        mediaConsumer.setFrameRate(framerate);


        new AsyncTask<String, Void, Object>() {

            //在doInBackground 执行完成后，onPostExecute 方法将被UI 线程调用，
            // 后台的计算结果将通过该方法传递到UI 线程，并且在界面上展示给用户.
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
            }

            //该方法运行在后台线程中，因此不能在该线程中更新UI，UI线程为主线程
            protected Object doInBackground(String... params) {
                if(mediaConsumer.init()==0){
                    //  启动音视频网络传输
                    mediaConsumer.StartMediaConsumeThread();
                    //  启动视频编码
                    mediaEncoder.prepare();
                    mediaEncoder.startStreamEncode();
                    //  启动音频编码
                    AACQueue.clear();
                    audioEncoder = MediaEnCoderFactory.generateAudioEncoder(sampleRate, bitRate, channelCount, AACQueue, audioQueueSize, maxAudioFrameSize);
                    audioEncoder.setAudioRecord(mAudioRecord);
                    audioEncoder.StartEncoderThread();
                    EventBus.getDefault().post(new ConnectStatus(0));
                }else{
                    EventBus.getDefault().post(new ConnectStatus(1));
                }
                return null;
            }

        }.execute();

    }

    public void stopEncoding(){
        isProcess = false;
        stopAllEncoderProcess();
    }

    public boolean isProcess(){
        return isProcess;
    }


    private void stopAllEncoderProcess() {
        if(mediaConsumer != null) {
            mediaConsumer.StopConsume();
            mediaConsumer = null;
        }
        if(mediaEncoder!=null){
            mediaEncoder.stop();
            mediaEncoder = null;
        }
        if(audioEncoder!=null) {
            audioEncoder.StopThread();
            audioEncoder = null;
        }
    }



}
