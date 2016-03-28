package com.juju.app.media.util;

/**
 * Created by Administrator on 2016/3/14 0014.
 */
public class YuvProcess {

    public static byte[] rotateYUV240SP(byte[] src,int width,int height)
    {
        byte[] des = new byte[width*height*3/2];
        int wh = width * height;
        //旋转Y
        int k = 0;
        for(int i=0;i<width;i++) {
            for(int j=height-1;j>=0;j--)
            {
                des[k] = src[width*j + i];
                k++;
            }
        }
        // System.arraycopy(src, width*height, des, width*height, src.length-width*height);

        for(int i=0;i<width;i+=2) {
            for(int j=height/2-1;j>=0;j--)
            {
                des[k] = src[wh+ width*j + i];
                des[k+1]=src[wh + width*j + i+1];
                k+=2;
            }
        }


        return des;
    }
}
