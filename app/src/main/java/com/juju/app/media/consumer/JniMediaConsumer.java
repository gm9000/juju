package com.juju.app.media.consumer;

import com.juju.app.golobal.GlobalVariable;
import com.juju.app.media.jni.NativeLiveProcess;

import java.util.concurrent.ArrayBlockingQueue;


public class JniMediaConsumer extends MediaConsumer{

    private ArrayBlockingQueue<byte[]> videoQueue;
    private ArrayBlockingQueue<byte[]> audioQueue;
    private NativeLiveProcess mediaProcesser;
    private int mediaChannel;


    public JniMediaConsumer(ArrayBlockingQueue<byte[]> videoQueue,ArrayBlockingQueue<byte[]> audioQueue) {

        this.videoQueue = videoQueue;
        this.audioQueue = audioQueue;
        this.mediaProcesser = new NativeLiveProcess();
    }


    public boolean isConsuming = false;

    public void StopConsume(){
        isConsuming = false;
        mediaProcesser.stop(mediaChannel);
        mediaProcesser.unInit();
    }

    public void StartMediaConsumeThread() {
        Thread consumerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                isConsuming = true;
//                HttpUtils uploadHttp = new HttpUtils();
                int initStauts = mediaProcesser.init("{\"mediaServerIp\":\""+ GlobalVariable.liveServerIp+"\",\"mediaServerPort\":"+GlobalVariable.liveServerPort+"}");
                mediaChannel = mediaProcesser.start("{\"width\":1280,\"height\":720,\"frameRate\":25,\"videBitRate\":8500000,\"sampleRate\":10000,\"audioBitRate\":8000000}");


                byte[] mediaData = null;
                while (isConsuming) {
                    if (videoQueue.size() > 0) {
                        mediaData = videoQueue.poll();
                        if (mediaData != null) {
                            mediaProcesser.inputVideoData(mediaData);
                        }
                    }
                    if (audioQueue.size() > 0) {
                        mediaData = audioQueue.poll();
                        if (mediaData != null) {
                            mediaProcesser.inputAudioData(mediaData);
                        }
                    }

                }
            }
        });
        consumerThread.start();
    }

}
