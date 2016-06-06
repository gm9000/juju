package com.juju.app.media.consumer;


import com.juju.app.golobal.Constants;
import com.juju.app.service.MediaProcessCallback;

public abstract class MediaConsumer {

    protected int width = 1280;
    protected int height = 720;
    private int frameRate = Constants.FRAME_RATE;
    private int videBitRate = 8500 * 1000;

    private int sampleRate;
    private int audioBitRate;

    public int init(){return 0;};
    public int unInit(){return 0;};

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getVideBitRate() {
        return videBitRate;
    }

    public void setVideBitRate(int videBitRate) {
        this.videBitRate = videBitRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public void setAudioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public abstract void StopConsume();
    public abstract void StartMediaConsumeThread();

}
