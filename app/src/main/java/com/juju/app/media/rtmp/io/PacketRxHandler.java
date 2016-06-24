package com.juju.app.media.rtmp.io;


import com.juju.app.media.rtmp.packets.RtmpPacket;

/**
 * Handler interface for received RTMP packets
 * @author francois
 */
public interface PacketRxHandler {
    
    public void handleRxPacket(RtmpPacket rtmpPacket);
    
    public void notifyWindowAckRequired(final int numBytesReadThusFar);    
}
