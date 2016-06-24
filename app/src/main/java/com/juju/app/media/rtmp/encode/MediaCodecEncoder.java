package com.juju.app.media.rtmp.encode;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.juju.app.media.rtmp.muxer.JlmFlvMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2016/6/13 0013.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaCodecEncoder extends JlmEncoder {
    private static final String TAG = "MediaCodecEncoder";

    private MediaCodecInfo vmci;
    private MediaCodec vencoder;
    private MediaCodec aencoder;
    private MediaCodec.BufferInfo vebi = new MediaCodec.BufferInfo();
    private MediaCodec.BufferInfo aebi = new MediaCodec.BufferInfo();

    private Thread yuvPreprocessThread = null;


    public MediaCodecEncoder(JlmFlvMuxer flvMuxer) {
        super(flvMuxer);
        mVideoColorFormat = chooseVideoEncoder();
        if (mVideoColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            VFORMAT = ImageFormat.YV12;
        } else if (mVideoColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            VFORMAT = ImageFormat.NV21;
        } else {
            throw new IllegalStateException("Unsupported color format!");
        }
        if (vmci.getName().contains("MTK")) {
            vCropWidth = 384;
            mRotatedFrameBuffer = new byte[vCropWidth * VCROP_HEIGHT * 3 / 2];
            mFlippedFrameBuffer = new byte[vCropWidth * VCROP_HEIGHT * 3 / 2];
            mCroppedFrameBuffer = new byte[vCropWidth * VCROP_HEIGHT * 3 / 2];
        }
    }

    @Override
    protected void initAudioEncoder(MediaFormat audioFormat) {
        try {
            aencoder = MediaCodec.createEncoderByType(ACODEC);
        } catch (IOException e) {
            Log.e(TAG, "create aencoder failed.");
            e.printStackTrace();
        }
        aencoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    @Override
    protected void initVideoEncoder(MediaFormat videoFormat) {
        // vencoder yuv to 264 es stream.
        // requires sdk level 16+, Android 4.1, 4.1.1, the JELLY_BEAN
        try {
            vencoder = MediaCodec.createByCodecName(vmci.getName());
        } catch (IOException e) {
            Log.e(TAG, "create vencoder failed.");
            e.printStackTrace();
        }
        vencoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    @Override
    protected void startAudioEncode() {
        aencoder.start();
    }

    @Override
    protected void startVideoEncode() {
        vencoder.start();

        encoding = true;

        // better process YUV data in threading
        yuvPreprocessThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (!Thread.interrupted()) {
                    while (!yuvQueue.isEmpty()) {
                        byte[] data = yuvQueue.poll();

                        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                            portraitPreprocessYuvFrame(data);
                        } else {
                            landscapePreprocessYuvFrame(data);
                        }

                        ByteBuffer[] inBuffers = vencoder.getInputBuffers();
                        ByteBuffer[] outBuffers = vencoder.getOutputBuffers();

                        int inBufferIndex = vencoder.dequeueInputBuffer(-1);
                        if (inBufferIndex >= 0) {
                            ByteBuffer bb = inBuffers[inBufferIndex];
                            bb.clear();
                            bb.put(mRotatedFrameBuffer, 0, mRotatedFrameBuffer.length);
                            long pts = System.nanoTime() / 1000 - mPresentTimeUs;
                            vencoder.queueInputBuffer(inBufferIndex, 0, mRotatedFrameBuffer.length, pts, 0);
                        }

                        for (; ; ) {
                            int outBufferIndex = vencoder.dequeueOutputBuffer(vebi, 0);
                            if (outBufferIndex >= 0) {
                                ByteBuffer bb = outBuffers[outBufferIndex];
                                onEncodedAnnexbFrame(bb, vebi);
                                vencoder.releaseOutputBuffer(outBufferIndex, false);
                                yuvCacheNum.getAndDecrement();
                            } else {
                                break;
                            }
                        }
                    }
                    // Wait for next yuv
                    synchronized (yuvLock) {
                        try {
                            // isEmpty() may take some time, so time out should be set to wait the next one.
                            yuvLock.wait(200);
                        } catch (InterruptedException ex) {
                            yuvPreprocessThread.interrupt();
                        }
                    }
                }
            }
        });
        yuvPreprocessThread.start();

    }

    public void stop() {
        if (yuvPreprocessThread != null) {
            yuvPreprocessThread.interrupt();
            try {
                yuvPreprocessThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                yuvPreprocessThread.interrupt();
            }
            yuvPreprocessThread = null;
            yuvCacheNum.set(0);
        }

        if (aencoder != null) {
            Log.i(TAG, "stop aencoder");
            aencoder.stop();
            aencoder.release();
            aencoder = null;
        }

        if (vencoder != null) {
            Log.i(TAG, "stop vencoder");
            vencoder.stop();
            vencoder.release();
            vencoder = null;
        }
        this.encoding = false;
    }

    // when got encoded aac raw stream.
    private void onEncodedAacFrame(ByteBuffer es, MediaCodec.BufferInfo bi) {
        try {
            flvMuxer.writeSampleData(audioFlvTrack, es, bi);
        } catch (Exception e) {
            Log.e(TAG, "muxer write audio sample failed.");
            e.printStackTrace();
        }
    }

    @Override
    public void onGetPcmFrame(byte[] data, int size) {
        ByteBuffer[] inBuffers = aencoder.getInputBuffers();
        ByteBuffer[] outBuffers = aencoder.getOutputBuffers();

        int inBufferIndex = aencoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer bb = inBuffers[inBufferIndex];
            bb.clear();
            bb.put(data, 0, size);
            long pts = System.nanoTime() / 1000 - mPresentTimeUs;
            aencoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
        }

        for (; ; ) {
            int outBufferIndex = aencoder.dequeueOutputBuffer(aebi, 0);
            if (outBufferIndex >= 0) {
                ByteBuffer bb = outBuffers[outBufferIndex];
                onEncodedAacFrame(bb, aebi);
                aencoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }

    // choose the video encoder by name.
    private MediaCodecInfo chooseVideoEncoder(String name) {
        int nbCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < nbCodecs; i++) {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (!mci.isEncoder()) {
                continue;
            }

            String[] types = mci.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(VCODEC)) {
                    Log.i(TAG, String.format("vencoder %s types: %s", mci.getName(), types[j]));
                    if (name == null) {
                        return mci;
                    }

                    if (mci.getName().contains(name)) {
                        return mci;
                    }
                }
            }
        }

        return null;
    }

    // choose the right supported color format. @see below:
    private int chooseVideoEncoder() {
        // choose the encoder "video/avc":
        //      1. select default one when type matched.
        //      2. google avc is unusable.
        //      3. choose qcom avc.
        vmci = chooseVideoEncoder(null);
        //vmci = chooseVideoEncoder("google");
        //vmci = chooseVideoEncoder("qcom");

        int matchedColorFormat = 0;
        MediaCodecInfo.CodecCapabilities cc = vmci.getCapabilitiesForType(VCODEC);
        for (int i = 0; i < cc.colorFormats.length; i++) {
            int cf = cc.colorFormats[i];
            Log.i(TAG, String.format("vencoder %s supports color fomart 0x%x(%d)", vmci.getName(), cf, cf));

            // choose YUV for h.264, prefer the bigger one.
            // corresponding to the color space transform in onPreviewFrame
            if (cf >= cc.COLOR_FormatYUV420Planar && cf <= cc.COLOR_FormatYUV420SemiPlanar) {
                if (cf > matchedColorFormat) {
                    matchedColorFormat = cf;
                }
            }
        }

        for (int i = 0; i < cc.profileLevels.length; i++) {
            MediaCodecInfo.CodecProfileLevel pl = cc.profileLevels[i];
            Log.i(TAG, String.format("vencoder %s support profile %d, level %d", vmci.getName(), pl.profile, pl.level));
        }

        Log.i(TAG, String.format("vencoder %s choose color format 0x%x(%d)", vmci.getName(), matchedColorFormat, matchedColorFormat));
        return matchedColorFormat;
    }
}
