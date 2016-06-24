package com.juju.app.media.rtmp.encode;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.juju.app.media.rtmp.muxer.JlmFlvMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Leo Ma on 4/1/2016.
 */
public abstract class JlmEncoder {
    private static final String TAG = "JlmEncoder";

    public static final String VCODEC = "video/avc";
    public static final String ACODEC = "audio/mp4a-latm";
    public static int VPREV_WIDTH = 640;
    public static int VPREV_HEIGHT = 480;
    public static final int VCROP_WIDTH = 360;
    public static final int VCROP_HEIGHT = 640;
    public static int vCropWidth = VCROP_WIDTH;   // Note: the stride of resolution must be set as 16x for hard encoding with some chip like MTK
    public static int vCropHeight = VCROP_HEIGHT;  // Since Y component is quadruple size as U and V component, the stride must be set as 32x
    public static final int VBITRATE = 300 * 1000;  // 300kbps
    public static final int VFPS = 15;
    public static final int VGOP = 75;
    public static final int I_FRAME_INTERVAL = 2;
    public static int VFORMAT = ImageFormat.YV12;
    public static final int ASAMPLERATE = 44100;
    public static final int ACHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    public static final int AFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int ABITRATE = 48 * 1000;  // 48kbps

    protected int mVideoColorFormat;

    protected volatile int mOrientation = Configuration.ORIENTATION_PORTRAIT;

    protected JlmFlvMuxer flvMuxer;

    protected byte[] mRotatedFrameBuffer = new byte[VCROP_WIDTH * VCROP_HEIGHT * 3 / 2];
    protected byte[] mFlippedFrameBuffer = new byte[VCROP_WIDTH * VCROP_HEIGHT * 3 / 2];
    protected byte[] mCroppedFrameBuffer = new byte[VCROP_WIDTH * VCROP_HEIGHT * 3 / 2];

    protected boolean mCameraFaceFront = true;
    protected long mPresentTimeUs;
    private int videoFlvTrack;
    protected int audioFlvTrack;

    protected boolean encoding = false;

    protected ConcurrentLinkedQueue<byte[]> yuvQueue = new ConcurrentLinkedQueue<>();
    protected final Object yuvLock = new Object();
    protected AtomicInteger yuvCacheNum = new AtomicInteger(0);

    public JlmEncoder(JlmFlvMuxer flvMuxer) {
        this.flvMuxer = flvMuxer;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public int start() {
        // the referent PTS for video and audio encoder.
        mPresentTimeUs = System.nanoTime() / 1000;


        // setup the aencoder.
        // @see https://developer.android.com/reference/android/media/MediaCodec.html
        int ach = ACHANNEL == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        MediaFormat audioFormat = MediaFormat.createAudioFormat(ACODEC, ASAMPLERATE, ach);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, ABITRATE);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
//        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        initAudioEncoder(audioFormat);

        audioFlvTrack = flvMuxer.addTrack(audioFormat);

        // setup the vencoder.
        // Note: landscape to portrait, 90 degree rotation, so we need to switch width and height in configuration
        MediaFormat videoFormat = MediaFormat.createVideoFormat(VCODEC, vCropWidth, vCropHeight);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mVideoColorFormat);
        videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, VBITRATE);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VFPS);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        initVideoEncoder(videoFormat);

        videoFlvTrack = flvMuxer.addTrack(videoFormat);

        startAudioEncode();
        startVideoEncode();

        return 0;
    }

    protected abstract void initAudioEncoder(MediaFormat audioFormat);

    protected abstract void initVideoEncoder(MediaFormat videoFormat);

    protected abstract void startAudioEncode();

    protected abstract void startVideoEncode();


    public abstract void stop();

    public boolean isEncoding(){
        return this.encoding;
    }

    public void swithCamera(int cameraId) {
        if (cameraId == 0) {
            mCameraFaceFront = false;
        } else {
            mCameraFaceFront = true;
        }
    }

    public void setScreenOrientation(int orientation) {
        mOrientation = orientation;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            vCropWidth = VCROP_WIDTH;
            vCropHeight = VCROP_HEIGHT;
        } else if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            vCropWidth = VCROP_HEIGHT;
            vCropHeight = VCROP_WIDTH;
        }
    }

    // when got encoded h264 es stream.
    protected void onEncodedAnnexbFrame(ByteBuffer es, MediaCodec.BufferInfo bi) {
        try {
            flvMuxer.writeSampleData(videoFlvTrack, es, bi);
        } catch (Exception e) {
            Log.e(TAG, "muxer write video sample failed.");
            e.printStackTrace();
        }
    }

    public void onGetYuvFrame(byte[] data) {
        if (yuvCacheNum.get() < VGOP) {
            // Check video frame cache number to judge the networking situation.
            // Just cache GOP / FPS seconds data according to latency.
            if (flvMuxer.getVideoFrameCacheNumber().get() < VGOP) {
                yuvQueue.add(data);
                yuvCacheNum.getAndIncrement();
                synchronized (yuvLock) {
                    yuvLock.notifyAll();
                }
            } else {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(),
                        new IOException("网络不给力"));
            }
        }else{
            Log.w(TAG,"------encode slow :( skip yuv frame :"+data.length);
        }
    }

    public abstract void onGetPcmFrame(byte[] data, int size);

    protected void portraitPreprocessYuvFrame(byte[] data) {
        if (mCameraFaceFront) {
            switch (mVideoColorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    cropYUV420PlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropHeight, vCropWidth);
//                    flipYUV420PlannerFrame(mCroppedFrameBuffer, mFlippedFrameBuffer, vCropHeight, vCropWidth);
                    rotate270YUV420PlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, vCropHeight, vCropWidth);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    cropYUV420SemiPlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropHeight, vCropWidth);
//                    flipYUV420SemiPlannerFrame(mCroppedFrameBuffer, mFlippedFrameBuffer, vCropHeight, vCropWidth);
                    rotate270YUV420SemiPlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, vCropHeight, vCropWidth);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }
        } else {
            switch (mVideoColorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    cropYUV420PlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropHeight, vCropWidth);
                    rotateYUV420PlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, vCropHeight, vCropWidth);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    cropYUV420SemiPlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropHeight, vCropWidth);
                    rotateYUV420SemiPlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, vCropHeight, vCropWidth);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }
        }
    }

    protected void landscapePreprocessYuvFrame(byte[] data) {
        if (mCameraFaceFront) {
            switch (mVideoColorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    cropYUV420PlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropWidth, vCropHeight);
                    flipYUV420PlannerFrame(mCroppedFrameBuffer, mFlippedFrameBuffer, vCropHeight, vCropWidth);
                    unrotateYUV420PlannerFrame(mFlippedFrameBuffer, mRotatedFrameBuffer, vCropWidth, vCropHeight);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    cropYUV420SemiPlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropWidth, vCropHeight);
                    flipYUV420SemiPlannerFrame(mCroppedFrameBuffer, mFlippedFrameBuffer, vCropWidth, vCropHeight);
                    unrotateYUV420SemiPlannerFrame(mFlippedFrameBuffer, mRotatedFrameBuffer, vCropWidth, vCropHeight);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }
        } else {
            switch (mVideoColorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    cropYUV420PlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropWidth, vCropHeight);
                    unrotateYUV420PlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, vCropWidth, vCropHeight);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    cropYUV420SemiPlannerFrame(data, VPREV_WIDTH, VPREV_HEIGHT, mCroppedFrameBuffer, vCropWidth, vCropHeight);
                    unrotateYUV420SemiPlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, vCropWidth, vCropHeight);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }
        }
    }

    // Y, U (Cb) and V (Cr)
    // yuv420                     yuv yuv yuv yuv
    // yuv420p (planar)   yyyy*2 uu vv
    // yuv420sp(semi-planner)   yyyy*2 uv uv
    // I420 -> YUV420P   yyyy*2 uu vv
    // YV12 -> YUV420P   yyyy*2 vv uu
    // NV21 -> YUV420SP  yyyy*2 vu vu
    // NV12 -> YUV420SP  yyyy*2 uv uv
    // NV16 -> YUV422SP  yyyy uv uv
    // YUY2 -> YUV422SP  yuyv yuyv
    private byte[] cropYUV420SemiPlannerFrame(byte[] input, int iw, int ih, byte[] output, int ow, int oh) {
        if (iw < ow || ih < oh) {
            throw new AssertionError("Crop resolution size must be less than original one");
        }

        int iFrameSize = iw * ih;
        int oFrameSize = ow * oh;

        int i = 0;
        for (int row = (ih - oh) / 2; row < oh + (ih - oh) / 2; row++) {
            for (int col = (iw - ow) / 2; col < ow + (iw - ow) / 2; col++) {
                output[i++] = input[iw * row + col];  // Y
            }
        }

        i = 0;
        for (int row = (ih - oh) / 4; row < oh / 2 + (ih - oh) / 4; row++) {
            for (int col = (iw - ow) / 4; col < ow / 2 + (iw - ow) / 4; col++) {
                output[oFrameSize + 2 * i] = input[iFrameSize + iw * row + 2 * col];  // U
                output[oFrameSize + 2 * i + 1] = input[iFrameSize + iw * row + 2 * col + 1];  // V
                i++;
            }
        }

        return output;
    }

    private byte[] cropYUV420PlannerFrame(byte[] input, int iw, int ih, byte[] output, int ow, int oh) {
        if (iw < ow || ih < oh) {
            throw new AssertionError("Crop resolution size must be less than original one");
        }

        int iFrameSize = iw * ih;
        int iQFrameSize = iFrameSize / 4;
        int oFrameSize = ow * oh;
        int oQFrameSize = oFrameSize / 4;

        int i = 0;
        for (int row = (ih - oh) / 2; row < oh + (ih - oh) / 2; row++) {
            for (int col = (iw - ow) / 2; col < ow + (iw - ow) / 2; col++) {
                output[i++] = input[iw * row + col];  // Y
            }
        }

        i = 0;
        for (int row = (ih - oh) / 4; row < oh / 2 + (ih - oh) / 4; row++) {
            for (int col = (iw - ow) / 4; col < ow / 2 + (iw - ow) / 4; col++) {
                output[oFrameSize + i] = input[iFrameSize + iw / 2 * row + col];  // U
                i++;
            }
        }

        i = 0;
        for (int row = (ih - oh) / 4; row < oh / 2 + (ih - oh) / 4; row++) {
            for (int col = (iw - ow) / 4; col < ow / 2 + (iw - ow) / 4; col++) {
                output[oFrameSize + oQFrameSize + i] = input[iFrameSize + iQFrameSize + iw / 2 * row + col];  // V
                i++;
            }
        }

        return output;
    }

    // 1. rotate 90 degree clockwise
    // 2. convert NV21 to NV12
    private byte[] rotateYUV420SemiPlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;

        int i = 0;
        for (int col = 0; col < width; col++) {
            for (int row = height - 1; row >= 0; row--) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int col = 0; col < width / 2; col++) {
            for (int row = height / 2 - 1; row >= 0; row--) {
                output[frameSize + i * 2 + 1] = input[frameSize + width * row + col * 2]; // Cb (U)
                output[frameSize + i * 2] = input[frameSize + width * row + col * 2 + 1]; // Cr (V)
                i++;
            }
        }

        return output;
    }



    private byte[] rotate270YUV420SemiPlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;

        int i = 0;
        for (int col = width-1; col >= 0; col--) {
            for (int row = 0; row < height; row++) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int col = width / 2-1; col >= 0; col--) {
            for (int row = 0; row < height / 2; row++) {
                output[frameSize + i * 2 + 1] = input[frameSize + width * row + col * 2]; // Cb (U)
                output[frameSize + i * 2] = input[frameSize + width * row + col * 2 + 1]; // Cr (V)
                i++;
            }
        }

        return output;
    }

    // 1. rotate 90 degree clockwise
    // 2. convert YV12 to I420
    private byte[] rotateYUV420PlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;

        int i = 0;
        for (int col = 0; col < width; col++) {
            for (int row = height - 1; row >= 0; row--) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int col = 0; col < width / 2; col++) {
            for (int row = height / 2 - 1; row >= 0; row--) {
                output[frameSize + i] = input[frameSize + qFrameSize + width / 2 * row + col]; // Cb (U)
                i++;
            }
        }

        i = 0;
        for (int col = 0; col < width / 2; col++) {
            for (int row = height / 2 - 1; row >= 0; row--) {
                output[frameSize + qFrameSize + i] = input[frameSize + width / 2 * row + col]; // Cr (V)
                i++;
            }
        }

        return output;
    }


    private byte[] rotate270YUV420PlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;

        int i = 0;
        for (int col = width-1; col >= 0; col--) {
            for (int row = 0; row <height; row++) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int col = width/2 - 1; col >= 0; col--) {
            for (int row = 0; row < height / 2; row++) {
                output[frameSize + i] = input[frameSize + qFrameSize + width / 2 * row + col]; // Cb (U)
                i++;
            }
        }

        i = 0;
        for (int col = width/2 - 1; col >= 0; col--) {
            for (int row = 0; row < height / 2; row++) {
                output[frameSize + qFrameSize + i] = input[frameSize + width / 2 * row + col]; // Cr (V)
                i++;
            }
        }

        return output;
    }




    // convert NV21 to NV12
    private byte[] unrotateYUV420SemiPlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;

        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                output[frameSize + i * 2 + 1] = input[frameSize + width * row + col * 2]; // Cb (U)
                output[frameSize + i * 2] = input[frameSize + width * row + col * 2 + 1]; // Cr (V)
                i++;
            }
        }

        return output;
    }

    // convert YV12 to I420
    private byte[] unrotateYUV420PlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;

        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                output[frameSize + i] = input[frameSize + qFrameSize + width / 2 * row + col]; // Cb (U)
                i++;
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                output[frameSize + qFrameSize + i] = input[frameSize + width / 2 * row + col]; // Cr (V)
                i++;
            }
        }

        return output;
    }

    private byte[] flipYUV420SemiPlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;

        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = width - 1; col >= 0; col--) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = width / 2 - 1; col >= 0; col--) {
                output[frameSize + i * 2] = input[frameSize + width * row + col * 2]; // Cb (U)
                output[frameSize + i * 2 + 1] = input[frameSize + width * row + col * 2 + 1]; // Cr (V)
                i++;
            }
        }

        return output;
    }

    private byte[] flipYUV420PlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;

        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = width - 1; col >= 0; col--) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = width / 2 - 1; col >= 0; col--) {
                output[frameSize + i] = input[frameSize + width / 2 * row + col]; // Cr (V)
                i++;
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = width / 2 - 1; col >= 0; col--) {
                output[frameSize + qFrameSize + i] = input[frameSize + qFrameSize + width / 2 * row + col]; // Cb (U)
                i++;
            }
        }

        return output;
    }

}
