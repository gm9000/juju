package com.juju.app.utils;

import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;

/**
 * Created by JanzLee on 2016/5/10 0010.
 */
public class CameraUtil {
    private static boolean checkCameraFacing(final int facing) {
        if (getSdkVersion() < Build.VERSION_CODES.GINGERBREAD) {
            return false;
        }
        final int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (facing == info.facing) {
                return true;
            }
        }
        return false;
    }
    public static boolean hasBackFacingCamera() {
        final int CAMERA_FACING_BACK = 0;
        return checkCameraFacing(CAMERA_FACING_BACK);
    }
    public static boolean hasFrontFacingCamera() {
        final int CAMERA_FACING_BACK = 1;
        return checkCameraFacing(CAMERA_FACING_BACK);
    }
    public static int getSdkVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    /**
     * 获得设备型号
     *
     * @return
     */
    public static String getDeviceModel() {
        return StringUtils.trim(Build.MODEL);
    }

    /** >=2.3 */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /** >=3.0 LEVEL:11 */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /** >=4.0 14 */
    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /** >= 4.2 17 */
    public static boolean hasJellyBeanMr1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * 检测当前设备是否是特定的设备
     *
     * @param devices
     * @return
     */
    public static boolean isDevice(String... devices) {
        String model = CameraUtil.getDeviceModel();
        if (devices != null && model != null) {
            for (String device : devices) {
                if (model.indexOf(device) != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获得设备的固件版本号
     */
    public static String getReleaseVersion() {
        return StringUtils.makeSafe(Build.VERSION.RELEASE);
    }

    /** 判断是否支持闪光灯 */
    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) //判断设备是否支持闪光灯
                        return true;
                }
            }
        }
        return false;
    }


}
