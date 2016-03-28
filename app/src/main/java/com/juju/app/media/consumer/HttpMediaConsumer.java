package com.juju.app.media.consumer;

import com.juju.app.service.BitRateInfo;
import com.juju.app.service.ConnectStatus;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;


public class HttpMediaConsumer extends MediaConsumer {


    private Thread consumerThread = null;
    private int retryNum = 0;

    private ArrayBlockingQueue<Object[]> videoQueue;
    private ArrayBlockingQueue<Object[]> audioQueue;
    private String consumerUrl;
    private Socket socket;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;


    private String serverIp;
    private int serverPort;
    private String url;
//    private String localFileUrl = Environment.getExternalStorageDirectory()+"/video.h264";
//    private OutputStream fileOutputStream;


    private final byte MEDIA_TYPE_VIDEO = 0x01;
    private final byte MEDIA_TYPE_VIDEO_H264 = 0x01;
    private final byte MEDIA_TYPE_AUDIO = 0x02;
    private final byte MEDIA_TYPE_AUDIO_AAC = 0x01;
    private final byte MEDIA_TYPE_IMAGE = 0x03;
    private final byte MEDIA_TYPE_IMAGE_JPEG = 0x01;

    private final byte[] ACCESS_UNIT_DELIMITER = new byte[]{0x00,0x00,0x00,0x01,0x09,(byte)0xF0};


    public HttpMediaConsumer(ArrayBlockingQueue<Object[]> mediaDataQueue, ArrayBlockingQueue<Object[]> audioDataQueue, String uploadUrl) {

        videoQueue = mediaDataQueue;
        audioQueue = audioDataQueue;
        consumerUrl = uploadUrl;
    }

    public int init() {
        retryNum = 0;
        return connectServer();
    }

    private int connectServer(){
        if (!parseUploadUrl(consumerUrl)) {
            return -1;
        }
        socket = new Socket();
        SocketAddress remoteAddr = new InetSocketAddress(serverIp, serverPort);
        try {
            socket.connect(remoteAddr, 30000);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;

        }
        return 0;
    }

    public boolean isConsuming = false;

    public void StopConsume() {
        if(retryNum==0) {
            isConsuming = false;
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(socketInputStream != null){
            try {
                socketInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socketInputStream = null;
        }

        if(socketOutputStream != null){
            try {
                socketOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socketOutputStream = null;
        }

//        if(fileOutputStream != null){
//            try {
//                fileOutputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

    public void StartMediaConsumeThread() {
        consumerThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    socketOutputStream = socket.getOutputStream();

//                    File localFile = new File(localFileUrl);
//                    if(localFile.exists()){
//                        localFile.delete();
//                    }
//                    localFile.createNewFile();
//                    fileOutputStream = new FileOutputStream(localFile);


                    byte[] videoHeaderData = null;
                    byte[] audioHeaderData = null;
                    //  构造并发送HTTP头
                    StringBuilder sBuilder = new StringBuilder();
                    sBuilder.append("POST ");
                    sBuilder.append(url);
                    sBuilder.append(" HTTP/1.1");
                    sBuilder.append("\r\n");
                    sBuilder.append("Transfer-Encoding: chunked\r\n\r\n");
                    socketOutputStream.write(sBuilder.toString().getBytes());
                    socketOutputStream.flush();

                    isConsuming = true;
                    byte[] mediaData = null;
                    long pts = 0;
                    Object[] mediaDataArray = null;
                    int sendSize = 0;
                    int sendFrameNum = 0;
                    videoQueue.clear();
                    audioQueue.clear();
                    while (isConsuming) {
                        if (videoQueue.size() > 0) {
                            mediaDataArray = videoQueue.poll();
                            pts = (long)mediaDataArray[0];
                            mediaData = (byte[])mediaDataArray[1];
                            //TODO 增加发送HTTP头域信息及元数据信息


                            // 构造视频元数据
                            videoHeaderData = generateVideoHeader(pts);
                            sendSize += videoHeaderData.length + mediaData.length + 6;
                            if(++sendFrameNum == getFrameRate()*2){
                                EventBus.getDefault().post(new BitRateInfo(sendSize/2048));
                                sendFrameNum = 0;
                                sendSize = 0;
                                retryNum = 0;
                            }
                            socketOutputStream.write(Integer.toHexString(videoHeaderData.length+mediaData.length+6).getBytes());
                            socketOutputStream.write("\r\n".getBytes());
                            socketOutputStream.write(videoHeaderData);
                            socketOutputStream.write(ACCESS_UNIT_DELIMITER);
                            socketOutputStream.write(mediaData);
                            socketOutputStream.flush();

//                            fileOutputStream.write(mediaData);
//                            fileOutputStream.flush();
                        }
                        if (audioQueue.size() > 0) {
                            mediaDataArray = audioQueue.poll();
                            pts = (long)mediaDataArray[0];
                            mediaData = (byte[])mediaDataArray[1];
                            //TODO 增加发送HTTP头域信息及元数据信息

                            // 构造视频元数据
                            audioHeaderData = generateAudioHeader(pts);
                            sendSize += audioHeaderData.length + mediaData.length;
                            socketOutputStream.write(Integer.toHexString(audioHeaderData.length + mediaData.length).getBytes());
                            socketOutputStream.write("\r\n".getBytes());
                            socketOutputStream.write(audioHeaderData);
                            socketOutputStream.write(mediaData);
                            socketOutputStream.flush();

                        }
                    }
                    //  发送chunked结束标记
                    socketOutputStream.write(Integer.toHexString(0).getBytes());
                    socketOutputStream.write("\r\n".getBytes());
                    socketOutputStream.flush();
                    //  清除提示信息
                    EventBus.getDefault().post(new ConnectStatus(2));
                } catch (SocketException e){
                    e.printStackTrace();
                    if(isConsuming){
                        EventBus.getDefault().post(new ConnectStatus(-2));
                        reTryConnect();
                    }
                } catch (IOException e) {
                    if(isConsuming) {
                        e.printStackTrace();
                        EventBus.getDefault().post(new ConnectStatus(-2));
                        reTryConnect();
                    }
                }
            }
        });
        consumerThread.start();
    }

    private void reTryConnect() {
        retryNum++;
        StopConsume();
        if(retryNum < 3) {
            if(connectServer()==0){
                consumerThread.run();
            }else{
                EventBus.getDefault().post(new ConnectStatus(1));
            }
        }else{
            EventBus.getDefault().post(new ConnectStatus(1));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //  清除提示信息
            EventBus.getDefault().post(new ConnectStatus(2));
        }
    }


    /** byte 1字节  short 2字节 int 4字节 long 8字节
     *  header_size		16 bits(2+xx) short
        stream_type     8 bits(1:video   2:audio) byte
        stream_subtype  8bits(1:h264    1:AAC)  byte
        jj_upload_header_xx xxbits
     jj_upload_header_audio()
     {
        PTS_DTS_mark    2 bits  PTS、DTS标志。
        reserved_mark   14 bits 保留标志位，必须位0，扩展使用。
        if (PTS_DTS_mark=='11'){
            PTS         33 bits
            reserved    6 bits
            DTS         33 bits
        }else if (PTS_DTS_mark=='10'){
             PTS        33 bits
            reserved    7 bits
        }
     }
     */
    private byte[] generateAudioHeader(long pts) {

        ByteBuffer headBuffer = ByteBuffer.allocate(9);
        headBuffer.put(MEDIA_TYPE_AUDIO);
        headBuffer.put(MEDIA_TYPE_AUDIO_AAC);

        byte[] ptsHead = new byte[]{-0x80|0x20,0x00};
        headBuffer.put(ptsHead);

        //  构建pts+reserved 5个字节的内容
        byte[] ptsBytes = new byte[5];
        ByteBuffer ptsBuffer = ByteBuffer.allocate(8);
        ptsBuffer.putLong(((pts & 0x1ffffffffL) << 31));
        ptsBuffer.flip();
        ptsBuffer.get(ptsBytes, 0, ptsBytes.length);
        headBuffer.put(ptsBytes);
        return headBuffer.array();
    }

    /** byte 1字节  short 2字节 int 4字节 long 8字节
     *  stream_type     8 bits(1:video   2:audio) byte
        stream_subtype  8bits(1:h264    1:AAC)  byte
        jj_upload_header_xx xxbits

         jj_upload_header_video()
             {
                PTS_DTS_mark    2 bits  short   PTS、DTS标志。
                image_size_mark 1 bit   图像分辨率标志。
                reserved_mark	13 bits 保留标志位，必须位0，扩展使用。
             if (PTS_DTS_mark=='11'){
                 PTS        33 bits
                reserved    6 bits
                DTS         33 bits
             }else if (PTS_DTS_mark=='10'){
                 PTS        33 bits
                reserved    7 bits
             }
             if (image_size_mark=='1'){
                image_width     16 bits 图像宽度。
                image_height    16 bits 图像高度。
             }
         }
     */
    private byte[] generateVideoHeader(long pts) {
        ByteBuffer headBuffer = ByteBuffer.allocate(13);
        headBuffer.put(MEDIA_TYPE_VIDEO);
        headBuffer.put(MEDIA_TYPE_VIDEO_H264);

//        PTS_DTS_mark    2 bits  short   PTS、DTS标志。
//        image_size_mark 1 bit   图像分辨率标志。
//        reserved_mark	13 bits 保留标志位，必须位0，扩展使用。
        byte[] ptsHead = new byte[]{-0x80|0x20,0x00};
        headBuffer.put(ptsHead);

        //  构建pts+reserved 5个字节的内容
        byte[] ptsBytes = new byte[5];
        ByteBuffer ptsBuffer = ByteBuffer.allocate(8);
        ptsBuffer.putLong(((pts & 0x1ffffffffL) << 31));
        ptsBuffer.flip();
        ptsBuffer.get(ptsBytes, 0, ptsBytes.length);
        headBuffer.put(ptsBytes);
        headBuffer.putShort((short)width);
        headBuffer.putShort((short)height);
        return headBuffer.array();
    }


    private boolean parseUploadUrl(String uploadUrl) {

        try {
            String[] splitStr = uploadUrl.split(":");
            int urlSplitIndex = splitStr[2].indexOf("/");
            serverIp = splitStr[1].substring(2);
            serverPort = Integer.parseInt(splitStr[2].substring(0, urlSplitIndex));
            url = splitStr[2].substring(urlSplitIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
