package com.juju.app.media.rtmp.encode;

import android.media.MediaFormat;

import com.juju.app.media.rtmp.muxer.JlmFlvMuxer;

/**
 * Created by Administrator on 2016/6/13 0013.
 */
public class FFmpegEncoder extends JlmEncoder{

    public FFmpegEncoder(JlmFlvMuxer flvMuxer) {
        super(flvMuxer);
    }

    @Override
    protected void initAudioEncoder(MediaFormat audioFormat) {

    }

    @Override
    protected void initVideoEncoder(MediaFormat videoFormat) {

    }

    @Override
    protected void startAudioEncode() {

    }

    @Override
    protected void startVideoEncode() {
        this.encoding = true;
    }

    @Override
    public void stop() {
        this.encoding = false;
    }

    @Override
    public void onGetPcmFrame(byte[] data, int size) {

    }
}
