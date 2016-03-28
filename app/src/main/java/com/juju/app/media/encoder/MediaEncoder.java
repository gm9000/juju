package com.juju.app.media.encoder;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import java.util.concurrent.ArrayBlockingQueue;


public abstract class MediaEncoder {

    private final static String TAG = "MediaEncoder";
    protected int TIMEOUT_USEC = 12000;

    protected int m_width;
    protected int m_height;
    protected  int m_framerate;

    protected ArrayBlockingQueue<byte[]> m_inputQueue;
    protected ArrayBlockingQueue<Object[]> m_outputQueue;
    protected int m_queueSize;
    protected boolean isEncoding = false;

    protected MediaEncoder(int width, int height, int framerate, ArrayBlockingQueue<byte[]> inputQueue, ArrayBlockingQueue<Object[]> outputQueue, int queueSize) {

        m_width = width;
        m_height = height;
        m_framerate = framerate;
        m_inputQueue = inputQueue;
        m_outputQueue = outputQueue;
        m_queueSize = queueSize;

    }

    public abstract void prepare();

    public abstract void startStreamEncode();

    public abstract void stopStreamEncode();

    public abstract void stop();

    protected void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    protected long computePresentationTime(long startNanoTime) {
        return (System.nanoTime()-startNanoTime)/100000*9;
    }

    public void setCamera(Camera camera){}
    public void setSurfaceHolder(SurfaceHolder holder){}
    public void setContext(Context context){}
    public void setWidth(int width){}
    public void setHeight(int height){}

    public int getWidth(){
       return m_width;
    }
    public int getHeight() {
        return m_height;
    }

}
