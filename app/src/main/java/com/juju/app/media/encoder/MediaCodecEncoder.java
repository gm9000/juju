package com.juju.app.media.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.juju.app.golobal.Constants;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;


public class MediaCodecEncoder extends MediaEncoder {
    private final static String TAG = "MeidaCodecEncoder";
    private MediaCodec mediaCodec;
    public byte[] configbyte;
    FileOutputStream fos;


    public MediaCodecEncoder(int width, int height, int framerate, ArrayBlockingQueue<byte[]> inputQueue, ArrayBlockingQueue<Object[]> outputQueue, int queueSize) {

        super(width, height, framerate, inputQueue, outputQueue, queueSize);
    }

    @Override
    @SuppressLint("NewApi")
    public void prepare() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", m_width,m_height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, m_width * m_height * 10);
        mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel2);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, m_framerate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constants.KEY_FRAME_INTERVAL);


        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch(Exception e){
            e.printStackTrace();
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }
        mediaCodec.start();
//        File h264File = new File(Environment.getExternalStorageDirectory()+"/juju_h264.h264");
//        if(h264File.exists()){
//            h264File.delete();
//        }
//        try {
//            h264File.createNewFile();
//            fos = new FileOutputStream(h264File);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void startStreamEncode() {
        Thread EncoderThread = new Thread(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                isEncoding = true;
                m_inputQueue.clear();
                byte[] input = null;
                long pts = 0;
                long startNanoTime = System.nanoTime();
                while (isEncoding) {
                    if (m_inputQueue.size() > 0) {
                        input = m_inputQueue.poll();
//                        byte[] yuv420sp = new byte[m_width * m_height * 3 / 2];
//                        NV21ToNV12(input, yuv420sp, m_width, m_height);
//                        input = yuv420sp;
                        try {
                            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                pts = computePresentationTime(startNanoTime);
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            while (outputBufferIndex >= 0) {

                                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                                byte[] outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);
                                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                    configbyte = new byte[bufferInfo.size];
                                    configbyte = outData;
                                } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {

                                    byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
                                    System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                                    System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
                                    if(m_outputQueue.size()>=m_queueSize){
                                        System.out.println("========================================= skip queue h264 frame package:" + m_outputQueue.poll().length);
                                    }
                                    m_outputQueue.add(new Object[]{bufferInfo.presentationTimeUs,keyframe});
//                                    fos.write(keyframe);
//                                    fos.flush();
                                } else {
                                    if(m_outputQueue.size()>=m_queueSize){
                                        System.out.println("========================================= skip current h264 frame package:"+outData.length);
                                    }else {
                                        m_outputQueue.add(new Object[]{bufferInfo.presentationTimeUs,outData});
                                    }
                                }

                                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            }

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        });
        EncoderThread.start();
    }

    @Override
    public void stopStreamEncode() {
        isEncoding = false;
    }

    @Override
    @SuppressLint("NewApi")
    public void stop() {
        isEncoding = false;
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mediaCodec != null){
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if(fos!=null){
            try {
                fos.flush();
                fos.close();
                fos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setWidth(int width){
        this.m_width = width;
    }
    @Override
    public void setHeight(int height){
        this.m_height = height;
    }

}
