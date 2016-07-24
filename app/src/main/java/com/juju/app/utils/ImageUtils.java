package com.juju.app.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.juju.app.helper.PhotoHelper;

import java.io.File;

/**
 * Created by Administrator on 2016/5/23 0023.
 */
public class ImageUtils {

    private static Logger logger = Logger.getLogger(ImageUtils.class);


    public static Bitmap drawableToBitmap(Drawable drawable) {



        Bitmap bitmap = Bitmap.createBitmap(

                drawable.getIntrinsicWidth(),

                drawable.getIntrinsicHeight(),

                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888

                        : Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);

        //canvas.setBitmap(bitmap);

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

        drawable.draw(canvas);

        return bitmap;

    }


    public static Bitmap getBigBitmapForDisplay(String imagePath,
                                                Context context) {
        if (null == imagePath || !new File(imagePath).exists())
            return null;
        try {
            int degeree = PhotoHelper.readPictureDegree(imagePath);
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null)
                return null;
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
            float scale = bitmap.getWidth() / (float) dm.widthPixels;
            Bitmap newBitMap = null;
            if (scale > 1) {
                newBitMap = zoomBitmap(bitmap, (int) (bitmap.getWidth() / scale), (int) (bitmap.getHeight() / scale));
                bitmap.recycle();
                Bitmap resultBitmap = PhotoHelper.rotaingImageView(degeree, newBitMap);
                return resultBitmap;
            }
            Bitmap resultBitmap = PhotoHelper.rotaingImageView(degeree, bitmap);
            return resultBitmap;
        } catch (Exception e) {
            logger.e(e.getMessage());
            return null;
        }
    }

    private static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        if (null == bitmap) {
            return null;
        }
        try {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Matrix matrix = new Matrix();
            float scaleWidth = ((float) width / w);
            float scaleHeight = ((float) height / h);
            matrix.postScale(scaleWidth, scaleHeight);
            Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
            return newbmp;
        } catch (Exception e) {
            logger.e(e.getMessage());
            return null;
        }
    }



}
