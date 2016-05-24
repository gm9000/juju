package com.juju.app.media.encoder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;


public class MediaRecorderEncoder extends MediaEncoder {
    private final static String TAG = "MeidaRecorderEncoder";
    private MediaRecorder mediaRecorder;

    private Camera mCamera;

    private int mSocketId;
    private LocalServerSocket mLss = null;
    protected LocalSocket mReceiver, mSender = null;
    private SurfaceHolder mSurfaceHolder;
    private Context context;

    private final byte[] head = new byte[]{0x00,0x00,0x00,0x01};

    private String fd = null;


    public MediaRecorderEncoder(int width, int height, int framerate, ArrayBlockingQueue<byte[]> inputQueue, ArrayBlockingQueue<Object[]> outputQueue, int queueSize) {

        super(width, height, framerate, inputQueue, outputQueue, queueSize);
    }

    @Override
    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    @Override
    public void setSurfaceHolder(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void prepare() {
        fd = Environment.getExternalStorageDirectory()+"/juju_h264.3gp";
        InitMediaSharePreference();
        getSPSAndPPS();
        try {
            createSockets();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initializeRecorder();
    }

    @SuppressLint("NewApi")
    private void initializeRecorder() {
        mCamera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
//        mediaRecorder.setInputSurface(mSurfaceHolder.getSurface());
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mediaRecorder.setOrientationHint(90);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mediaRecorder.setVideoSize(m_height, m_width);
//        CamcorderProfile cProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        mediaRecorder.setProfile(cProfile);
//        mediaRecorder.setVideoFrameRate(m_framerate);
        mediaRecorder.setVideoEncodingBitRate(m_width * m_height * 5);
        mediaRecorder.setMaxDuration(0);
        mediaRecorder.setMaxFileSize(0);
        if (SPS == null) {
            File file = new File(fd);
            if(!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mediaRecorder.setOutputFile(fd);
        } else {
            mediaRecorder.setOutputFile(mSender.getFileDescriptor());
        }

    }

    @Override
    public void startStreamEncode() {
        final int MAXFRAMEBUFFER = 204800;//20K
        final byte[] h264frame = new byte[MAXFRAMEBUFFER];
        Thread EncoderThread = new Thread(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                isEncoding = true;
                try {
                    mediaRecorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaRecorder.start();


                if (SPS == null) {
                    Log.e(TAG, "Rlease MediaRecorder and get SPS and PPS");
                    try {
                        Thread.sleep(2000);
                        //释放MediaRecorder资源
                        releaseMediaRecorder();
                        //从已采集的视频数据中获取SPS和PPS
                        findSPSAndPPS();

                        //找到后重新初始化MediaRecorder
                        initializeRecorder();
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        isEncoding = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                DataInputStream dataInput = null;
                try {
                    dataInput = new DataInputStream(mReceiver.getInputStream());
                    dataInput.read(h264frame, 0, StartMdatPlace);

                    //先读取ftpy box and mdat box, 目的是skip ftpy and mdat data,(decisbe by phone)

                    int h264length = 0;
                    long startNanoTime = 0;

                    while (isEncoding) {

                        h264length = dataInput.readInt();
                        readSize(h264frame, h264length, dataInput);

                        dealH264Data(h264frame,h264length,computePresentationTime(startNanoTime));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        });
        EncoderThread.start();
    }

    private void dealH264Data(byte[] wholeFrameData,int frameLength,long pts) {
        byte[] sendData = null;
        int index = 0;
        boolean isKeyFrame = false;
        if((wholeFrameData[0]&0x1F)==5){
            isKeyFrame = true;
            sendData = new byte[3*head.length+SPS.length+PPS.length+frameLength];
            System.arraycopy(head, 0, sendData, index, head.length);
            index += head.length;
            System.arraycopy(SPS, 0, sendData, index, SPS.length);
            index += SPS.length;
            System.arraycopy(head, 0, sendData, index, head.length);
            index += head.length;
            System.arraycopy(PPS, 0, sendData, index, PPS.length);
            index += PPS.length;
        }else{
            sendData = new byte[head.length+frameLength];
        }

        System.arraycopy(head, 0, sendData, index, head.length);
        index += head.length;
        System.arraycopy(wholeFrameData, 0, sendData, index, frameLength);

        try {
            if(m_outputQueue.size()>=m_queueSize){
                if(isKeyFrame) {
                    System.out.println("-----------------------------skip queue h264 frame:" + m_outputQueue.poll().length);
                    m_outputQueue.put(new Object[]{pts,sendData});
                }else {
                    System.out.println("-----------------------------skip current h264 frame:" + sendData.length);
//                    m_outputQueue.put(m_outputQueue.poll());
                }
            }else{
                m_outputQueue.put(new Object[]{pts,sendData});
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void stopStreamEncode() {
        if(isEncoding) {
            isEncoding = false;
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        }
        closeSockets();
    }

    @Override
    @SuppressLint("NewApi")
    public void stop() {
        stopStreamEncode();
    }


    private void createSockets() throws IOException {

        final String LOCAL_ADDR = "cn.julema.streaming-";

        for (int i = 0; i < 10; i++) {
            try {
                mSocketId = new Random().nextInt();
                mLss = new LocalServerSocket(LOCAL_ADDR + mSocketId);
                break;
            } catch (IOException e1) {
            }
        }

        mReceiver = new LocalSocket();
        mReceiver.connect(new LocalSocketAddress(LOCAL_ADDR + mSocketId));
        mReceiver.setReceiveBufferSize(500000);
        mSender = mLss.accept();
        mSender.setSendBufferSize(500000);
    }

    private void closeSockets() {
        try {
            mSender.close();
            mSender = null;
            mReceiver.close();
            mReceiver = null;
            mLss.close();
            mLss = null;
        } catch (Exception ignore) {
        }
    }


    private void readSize(byte[] h264frame, int h264length, DataInputStream dataInput) throws IOException, InterruptedException {
        int read = 0;
        int temp = 0;
        while (read < h264length) {
            temp = dataInput.read(h264frame, read, h264length - read);
            Log.e(TAG, String.format("h264frame %d,%d,%d", h264length, read, h264length - read));
            if (temp == -1) {
                Log.e(TAG, "no data get wait for data coming.....");
                Thread.sleep(200);
                continue;
            }
            read += temp;
        }
    }

    //初始化，记录mdat开始位置的参数
    SharedPreferences sharedPreferences;
    private final String mediaShare = "media";

    private void InitMediaSharePreference() {
        sharedPreferences = context.getSharedPreferences(mediaShare, context.MODE_PRIVATE);
    }

    private byte[] SPS;
    private byte[] PPS;
    private int StartMdatPlace = 0;

    //得到序列参数集SPS和图像参数集PPS,如果已经存储在本地
    private void getSPSAndPPS() {
        StartMdatPlace = sharedPreferences.getInt(
                String.format("mdata_%d%d.mdat", m_width, m_height), -1);

        if (StartMdatPlace != -1) {
            byte[] temp = new byte[100];
            try {
                FileInputStream file_in = context.openFileInput(String.format("%d%d.sps", m_width, m_height));

                int index = 0;
                int read = 0;
                while (true) {
                    read = file_in.read(temp, index, 10);
                    if (read == -1) break;
                    else index += read;
                }
                Log.e(TAG, "sps length:" + index);
                SPS = new byte[index];
                System.arraycopy(temp, 0, SPS, 0, index);

                file_in.close();

                index = 0;
                //read PPS
                file_in = context.openFileInput(String.format("%d%d.pps", m_width, m_height));
                while (true) {
                    read = file_in.read(temp, index, 10);
                    if (read == -1) break;
                    else index += read;
                }
                Log.e(TAG, "pps length:" + index);
                PPS = new byte[index];
                System.arraycopy(temp, 0, PPS, 0, index);
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        } else {
            SPS = null;
            PPS = null;
        }
    }

    private void findSPSAndPPS() throws Exception {
        File file = new File(fd);
        FileInputStream fileInput = new FileInputStream(file);

        int length = (int) file.length();
        byte[] data = new byte[length];

        fileInput.read(data);

        final byte[] mdat = new byte[]{0x6D, 0x64, 0x61, 0x74};
        final byte[] avcc = new byte[]{0x61, 0x76, 0x63, 0x43};

        for (int i = 0; i < length; i++) {
            if (data[i] == mdat[0] && data[i + 1] == mdat[1] && data[i + 2] == mdat[2] && data[i + 3] == mdat[3]) {
                StartMdatPlace = i + 4;//find mdat
                break;
            }
        }
        Log.e(TAG, "StartMdatPlace:" + StartMdatPlace);
        //记录到xml文件里
        String mdatStr = String.format("mdata_%d%d.mdat", m_width, m_height);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(mdatStr, StartMdatPlace);
        editor.commit();

        for (int i = 0; i < length; i++) {
            if (data[i] == avcc[0] && data[i + 1] == avcc[1] && data[i + 2] == avcc[2] && data[i + 3] == avcc[3]) {
                int sps_start = i + 3 + 7;//其中i+3指到avcc的c，再加7跳过6位AVCDecoderConfigurationRecord参数

                //sps length and sps data
                byte[] sps_3gp = new byte[2];//sps length
                sps_3gp[1] = data[sps_start];
                sps_3gp[0] = data[sps_start + 1];
                int sps_length = bytes2short(sps_3gp);
                Log.e(TAG, "sps_length :" + sps_length);

                sps_start += 2;//skip length
                SPS = new byte[sps_length];
                System.arraycopy(data, sps_start, SPS, 0, sps_length);
                //save sps
                FileOutputStream file_out = context.openFileOutput(
                        String.format("%d%d.sps", m_width, m_height),
                        context.MODE_PRIVATE);
                file_out.write(SPS);
                file_out.close();

                //pps length and pps data
                int pps_start = sps_start + sps_length + 1;
                byte[] pps_3gp = new byte[2];
                pps_3gp[1] = data[pps_start];
                pps_3gp[0] = data[pps_start + 1];
                int pps_length = bytes2short(pps_3gp);
                Log.e(TAG, "PPS LENGTH:" + pps_length);

                pps_start += 2;

                PPS = new byte[pps_length];
                System.arraycopy(data, pps_start, PPS, 0, pps_length);


                //Save PPS
                file_out = context.openFileOutput(
                        String.format("%d%d.pps", m_width, m_height),
                        context.MODE_PRIVATE);
                file_out.write(PPS);
                file_out.close();
                break;
            }
        }

    }

    //计算长度
    private short bytes2short(byte[] b) {
        short mask = 0xff;
        short temp = 0;
        short res = 0;
        for (int i = 0; i < 2; i++) {
            res <<= 8;
            temp = (short) (b[1 - i] & mask);
            res |= temp;
        }
        return res;
    }

    //释放MediaRecorder资源
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            if (isEncoding) {
                mediaRecorder.stop();
                isEncoding = false;
                mCamera.lock();
            }
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

}
