package com.juju.app.media.rtmp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.juju.app.media.rtmp.io.RtmpConnection;

/**
 * Srs implementation of an RTMP publisher
 * 
 * @author francois, leoma
 */
public class JlmRtmpPublisher implements RtmpPublisher {

    private RtmpConnection rtmpConnection;

    public JlmRtmpPublisher(RtmpPublisher.EventHandler handler) {
        rtmpConnection = new RtmpConnection(handler);
    }

    @Override
    public void connect(String url) throws IOException {
        rtmpConnection.connect(url);
    }

    @Override
    public void shutdown() {
        rtmpConnection.shutdown();
    }

    @Override
    public void publish(String publishType) throws IllegalStateException, IOException {
        if (publishType == null) {
            throw new IllegalStateException("No publish type specified");
        }
        rtmpConnection.publish(publishType);
    }

    @Override
    public void closeStream() throws IllegalStateException {
        rtmpConnection.closeStream();
    }

    @Override
    public void publishVideoData(byte[] data) throws IllegalStateException {
        if (data == null || data.length == 0) {
            throw new IllegalStateException("Invalid Video Data");
        }
        rtmpConnection.publishVideoData(data);
    }

    @Override
    public void publishAudioData(byte[] data) throws IllegalStateException {
        if (data == null || data.length == 0) {
            throw new IllegalStateException("Invalid Audio Data");
        }
        rtmpConnection.publishAudioData(data);
    }

    @Override
    public final AtomicInteger getVideoFrameCacheNumber() {
        return rtmpConnection.getVideoFrameCacheNumber();
    }

    @Override
    public final EventHandler getEventHandler() {
        return rtmpConnection.getEventHandler();
    }

    @Override
    public final String getServerIpAddr() {
        return rtmpConnection.getServerIpAddr();
    }

    @Override
    public final int getServerPid() {
        return rtmpConnection.getServerPid();
    }

    @Override
    public final int getServerId() {
        return rtmpConnection.getServerId();
    }

    @Override
    public void setVideoResolution(int width, int height) {
        rtmpConnection.setVideoResolution(width, height);
    }
}
