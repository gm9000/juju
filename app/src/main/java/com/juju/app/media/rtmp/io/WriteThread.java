package com.juju.app.media.rtmp.io;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.juju.app.media.rtmp.RtmpPublisher;
import com.juju.app.media.rtmp.packets.Command;
import com.juju.app.media.rtmp.packets.RtmpPacket;
import com.juju.app.media.rtmp.packets.Video;

/**
 * RTMPConnection's write thread
 * 
 * @author francois, leo
 */
public class WriteThread extends Thread {

    private static final String TAG = "WriteThread";

    private RtmpSessionInfo rtmpSessionInfo;
    private OutputStream out;
    private ConcurrentLinkedQueue<RtmpPacket> writeQueue = new ConcurrentLinkedQueue<RtmpPacket>();
    private final Object txPacketLock = new Object();
    private volatile boolean active = true;
    private int videoFrameCount;
    private int videoPackageSize;
    private long lastTimeMillis;
    private RtmpPublisher publisher;

    public WriteThread(RtmpSessionInfo rtmpSessionInfo, OutputStream out, RtmpPublisher publisher) {
        super("RtmpWriteThread");
        this.rtmpSessionInfo = rtmpSessionInfo;
        this.out = out;
        this.publisher = publisher;
    }

    @Override
    public void run() {

        while (active) {
            try {
                while (!writeQueue.isEmpty()) {
                    RtmpPacket rtmpPacket = writeQueue.poll();
                    ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(rtmpPacket.getHeader().getChunkStreamId());
                    chunkStreamInfo.setPrevHeaderTx(rtmpPacket.getHeader());
                    rtmpPacket.getHeader().setAbsoluteTimestamp((int) chunkStreamInfo.markAbsoluteTimestampTx());
                    rtmpPacket.writeTo(out, rtmpSessionInfo.getTxChunkSize(), chunkStreamInfo);
                    Log.d(TAG, "WriteThread: wrote packet: " + rtmpPacket + ", size: " + rtmpPacket.getHeader().getPacketLength());
                    if (rtmpPacket instanceof Command) {
                        rtmpSessionInfo.addInvokedCommand(((Command) rtmpPacket).getTransactionId(), ((Command) rtmpPacket).getCommandName());
                    }
                    if (rtmpPacket instanceof Video) {
                        publisher.getVideoFrameCacheNumber().getAndDecrement();
                        calcFps(rtmpPacket.getHeader().getPacketLength());
                    }
                }
                out.flush();
            } catch (SocketException se) {
                Log.e(TAG, "WriteThread: Caught SocketException during write loop, shutting down: " + se.getMessage());
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(this, se);
                active = false;
                continue;
            } catch (IOException ioe) {
                Log.e(TAG, "WriteThread: Caught IOException during write loop, shutting down: " + ioe.getMessage());
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(this, ioe);
                active = false;
                continue;
            }

            // Waiting for next packet
            Log.d(TAG, "WriteThread: waiting...");
            synchronized (txPacketLock) {
                try {
                    // isEmpty() may take some time, so time out should be set to wait next offer
                    txPacketLock.wait(500);
                } catch (InterruptedException ex) {
                    Log.w(TAG, "Interrupted", ex);
                    this.interrupt();
                }
            }
        }

        Log.d(TAG, "exit");
    }

    /** Transmit the specified RTMP packet (thread-safe) */
    public void send(RtmpPacket rtmpPacket) {
        if (rtmpPacket != null) {
            writeQueue.add(rtmpPacket);
        }
        synchronized (txPacketLock) {
            txPacketLock.notify();
        }
    }

    public void shutdown() {
        Log.d(TAG, "Stopping");
        writeQueue.clear();
        active = false;
        synchronized (txPacketLock) {
            txPacketLock.notify();
        }
    }

    private void calcFps(int frameSize) {
        if (videoFrameCount == 0) {
            lastTimeMillis = System.nanoTime() / 1000000;
            videoFrameCount++;
            videoPackageSize += frameSize;
        } else {
            videoPackageSize += frameSize;
            if (++videoFrameCount >= 45) {
                long diffTimeMillis = System.nanoTime() / 1000000 - lastTimeMillis;
                publisher.getEventHandler().onRtmpOutputFps(videoPackageSize*8 / diffTimeMillis);
                videoFrameCount = 0;
                videoPackageSize = 0;
            }
        }
    }
}
