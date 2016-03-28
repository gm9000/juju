package com.juju.app.media.encoder;

import android.media.AudioRecord;

import java.util.concurrent.ArrayBlockingQueue;


public  abstract class AudioEncoder {
    private final static String TAG = "AudioEncoder";
    protected int sampleRate;
    protected int bitRate;
    protected int channelCount;
    protected int maxAudioFrameSize;

    protected ArrayBlockingQueue<Object[]> m_outputQueue;
    protected int m_queueSize;

    public AudioEncoder(int sampleRate, int bitRate, int channelCount, ArrayBlockingQueue<Object[]> outputQueue, int queueSize, int maxAudioFrameSize) {
        this.sampleRate = sampleRate;
        this.bitRate = bitRate;
        this.channelCount = channelCount;
        m_outputQueue = outputQueue;
        m_queueSize = queueSize;
        this.maxAudioFrameSize = maxAudioFrameSize;
    }

    public void setAudioRecord(AudioRecord audioRecord) {}
    protected boolean isRuning = false;
    public abstract void StopThread();
    public abstract void StartEncoderThread();

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    protected long computePresentationTime(long startNanoTime) {
        return (System.nanoTime()-startNanoTime)/100000*9;
    }
}
