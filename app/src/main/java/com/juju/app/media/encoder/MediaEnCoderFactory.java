package com.juju.app.media.encoder;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Administrator on 2016/3/16 0016.
 */
public class MediaEnCoderFactory {

    private final static String TAG = "MediaEncoderFactory";
    public static final int MODE_MEDIACODEC_API = 0x00;
    public static final int MODE_MEDIARECORDER_API = 0x01;
    public static int sSuggestedMode;

    static {
        // We determine wether or not the MediaCodec API should be used
        try {
            Class.forName("android.media.MediaCodec");
            // Will be set to MODE_MEDIACODEC_API at some point...
            sSuggestedMode = MODE_MEDIACODEC_API;
            Log.i(TAG, "Phone supports the MediaCoded API");
        } catch (ClassNotFoundException e) {
            sSuggestedMode = MODE_MEDIARECORDER_API;
            Log.i(TAG,"Phone does not support the MediaCodec API");
        }
        if(sSuggestedMode == MODE_MEDIACODEC_API){

//            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 320,240);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 320 * 240 * 5);
//            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
//            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
////            MediaCodecList.find
//            MediaCodecInfo.CodecCapabilities
        }
    }

    public static MediaEncoder generateMediaEncoder(int width, int height, int framerate, ArrayBlockingQueue<byte[]> inputQueue, ArrayBlockingQueue<Object[]> outputQueue, int queueSize){
        MediaEncoder encoder = null;
        switch (sSuggestedMode){
            case MODE_MEDIACODEC_API:
                encoder = new MediaCodecEncoder(width, height, framerate,inputQueue, outputQueue, queueSize);
//                encoder = new MediaRecorderEncoder(width, height, framerate, inputQueue, outputQueue, queueSize);
                break;
            case MODE_MEDIARECORDER_API:
                encoder = new MediaRecorderEncoder(width, height, framerate, inputQueue, outputQueue, queueSize);
                break;
        }
        return encoder;
    }

    public static AudioEncoder generateAudioEncoder(int sampleRate, int bitRate, int channelCount, ArrayBlockingQueue<Object[]> outputQueue, int queueSize, int maxAudioFrameSize){
        AudioEncoder encoder = null;
        switch (sSuggestedMode){
            case MODE_MEDIACODEC_API:
                encoder = new MediaCodecAudioEncoder(sampleRate,bitRate,channelCount,outputQueue,queueSize,maxAudioFrameSize);
                break;
            case MODE_MEDIARECORDER_API:
                encoder = new MediaCodecAudioEncoder(sampleRate,bitRate,channelCount,outputQueue,queueSize,maxAudioFrameSize);
                break;
        }
        return encoder;
    }


}
