package com.juju.app.media.jni;

/**
 * Created by JanzLee on 2016/3/11 0011.
 */
public class NativeLiveProcess {
    static{
        System.loadLibrary("NativeLiveProcess");
    }

    /**
     *
     * @param initParam
     * Json Format
     * {"mediaServerIp":"192.168.1.123","mediaServerPort":8080}
     * @return int init status
     *          0   success
     *          1   can't connect to meidia server
     *          2   param format error
     *          3   unknown error
     */
    public native int init(String initParam);

    /**
     *
     * @return  int unInit status code
     *            0 success
     *            1 failure
     */
    public native int unInit();

    /**
     * @param mediaParam
     * Json Format
     * {"width":1280,"height":720,"frameRate":25,"videBitRate":8500000,"sampleRate":10000,"audioBitRate":8000000}
     * @return int  media channel no,need set it to input param when call stop method
     */
    public native int start(String mediaParam);

    /**
     *
     * @param channelId
     * @return int  stop status code
     *          0   success
     *          1   failure
     */
    public native int stop(int channelId);

    public native void inputVideoData(byte[] h264Frame);

    public native void inputAudioData(byte[] aacFrame);

}
