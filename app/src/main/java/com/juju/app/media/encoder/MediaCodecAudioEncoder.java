package com.juju.app.media.encoder;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;


public class MediaCodecAudioEncoder extends AudioEncoder{
    private final static String TAG = "AacCodec";

    private int TIMEOUT_USEC = 12000;
    private MediaCodec mediaCodec;
    private AudioRecord audioRecord;


    @SuppressLint("NewApi")
    public MediaCodecAudioEncoder(int sampleRate, int bitRate, int channelCount, ArrayBlockingQueue<Object[]> outputQueue, int queueSize, int maxAudioFrameSize) {

        super(sampleRate,bitRate,channelCount,outputQueue,queueSize,maxAudioFrameSize);

        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,this.sampleRate,this.channelCount);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, this.bitRate);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();

    }

    @Override
    public void setAudioRecord(AudioRecord audioRecord) {
        this.audioRecord = audioRecord;
    }


    @SuppressLint("NewApi")
    private void StopEncoder() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void StopThread() {
        isRuning = false;
        try {
            Thread.currentThread().sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        StopEncoder();
    }

    public void StartEncoderThread() {
        Thread EncoderThread = new Thread(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                isRuning = true;
                byte[] input = new byte[maxAudioFrameSize];
                long startNanoTime = System.nanoTime();
                long pts = 0;
                while (isRuning) {
                    int audioFrameLen = audioRecord.read(input, 0, input.length);
                    if (audioFrameLen > 0) {
                        try {
                            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();
                                inputBuffer.put(input,0,audioFrameLen);
                                pts = computePresentationTime(startNanoTime);
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                            //trying to add a ADTS
                            while (outputBufferIndex >= 0) {
                                int outBitsSize = bufferInfo.size;
                                int outPacketSize = outBitsSize + 7; // 7 is ADTS size
                                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];


                                outputBuffer.position(bufferInfo.offset);
                                outputBuffer.limit(bufferInfo.offset + outBitsSize);


                                byte[] outData = new byte[outPacketSize];
                                addADTStoPacket(outData, outPacketSize);


                                outputBuffer.get(outData, 7, outBitsSize);
                                outputBuffer.position(bufferInfo.offset);


                                // byte[] outData = new byte[bufferInfo.size];

                                if(m_outputQueue.size()>= m_queueSize){
                                    System.out.println("skip aac frame size:" + m_outputQueue.poll().length+" bytes");
                                }
                                m_outputQueue.add(new Object[]{bufferInfo.presentationTimeUs,outData});
                                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            }

                            //  Without ADTS header
//                            while (outputBufferIndex >= 0) {
//                                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//                                byte[] outData = new byte[bufferInfo.size];
//                                outputBuffer.get(outData);
//                                if(m_outputQueue.size()>= m_queueSize){
//                                    System.out.println("skip aac frame size:" + m_outputQueue.poll().length+" bytes");
//                                }
//                                m_outputQueue.add(new Object[]{pts,outData});
//                                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
//                            }

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        EncoderThread.start();

    }



    /**
     * Add ADTS header at the beginning of each and every AAC packet. This is
     * needed as MediaCodec encoder generates a packet of raw AAC data.
     *
     * Note the packetLen must count in the ADTS header itself.
     **/
    public void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        // 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 1; // CPE


        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
