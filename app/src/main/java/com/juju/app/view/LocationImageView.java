package com.juju.app.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.test.suitebuilder.annotation.Suppress;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.juju.app.R;
import com.juju.app.utils.ScreenUtil;

import org.xutils.image.AsyncDrawable;


/**
 * 圆形位置ImageView，
 * <p/>
 * 设置颜色在xml布局文件中由自定义属性配置参数指定
 */
public class LocationImageView extends ImageView {

    private int mCircleThickness = 2;
    private int mArrowHeight = 4;
    private int mColor = 0xFF00a4e8;
    // 控件默认长、宽
    private int defaultWidth = 50;
    private int defaultHeight = 50;

    public LocationImageView(Context context) {
        super(context);
        init(context);
    }
    public LocationImageView(Context context,boolean self) {
        super(context);
        init(context);
        if(self) {
            mColor = 0xFFFF0000;
            mCircleThickness = 3;
        }
    }

    @SuppressLint("NewApi")
    private void init(Context context){
        defaultWidth = ScreenUtil.dip2px(context,defaultWidth);
        defaultHeight = ScreenUtil.dip2px(context,defaultHeight);
        defaultWidth = defaultWidth>120?120:defaultWidth;
        defaultHeight = defaultHeight>120?120:defaultHeight;
        setLayoutParams(new ViewGroup.LayoutParams(defaultWidth, defaultHeight));
        setAlpha(0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        if(getWidth() == 0 || getHeight() == 0) {
            return;
        }
        this.measure(0, 0);
        if(drawable.getClass() == NinePatchDrawable.class)
            return;

        Bitmap b = null;


        if(drawable instanceof AsyncDrawable){
            b = Bitmap
                    .createBitmap(
                            getWidth(),
                            getHeight(),
                            drawable.getOpacity() != PixelFormat.OPAQUE ? Config.ARGB_8888
                                    : Config.RGB_565);
            Canvas canvas1 = new Canvas(b);
            drawable.setBounds(0, 0, getWidth(),
                    getHeight());
            drawable.draw(canvas1);
        }else if(drawable instanceof BitmapDrawable){
            b =  ((BitmapDrawable)drawable).getBitmap() ;
        }

        Bitmap bitmap = b.copy(Config.ARGB_8888, true);
        if(bitmap == null){
            bitmap = b;
        }

        int radius = (defaultWidth < defaultHeight ? defaultWidth : defaultHeight) / 2 - mCircleThickness - mArrowHeight;
        drawCircleBorder(canvas, radius + mCircleThickness / 2, mColor);

        if(mArrowHeight>0) {
            drawLocateArrow(canvas,mColor);
        }

        Bitmap roundBitmap = getCroppedRoundBitmap(bitmap, radius);
        canvas.drawBitmap(roundBitmap, defaultWidth / 2 - radius, defaultHeight / 2 - radius - mArrowHeight, null);


    }




    /**
     *
     * @param bmp
     * @param radius 半径
     * @return
     */
    public Bitmap getCroppedRoundBitmap(Bitmap bmp, int radius) {
        Bitmap scaledSrcBmp;
        int diameter = radius * 2;
        // 为了防止宽高不相等，造成圆形图片变形，因此截取长方形中处于中间位置最大的正方形图片
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        int squareWidth = 0, squareHeight = 0;
        int x = 0, y = 0;
        Bitmap squareBitmap;

        if(bmpHeight > bmpWidth) {// 高大于宽
            squareWidth = squareHeight = bmpWidth;
            x = 0;
            y = (bmpHeight - bmpWidth) / 2;
            // 截取正方形图片
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);
        } else if (bmpHeight < bmpWidth) {
            // 宽大于高
            squareWidth = squareHeight = bmpHeight;
            x = (bmpWidth - bmpHeight) / 2;
            y = 0;
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);
        } else {
            squareBitmap = bmp;
        }

        if(squareBitmap.getWidth() != diameter || squareBitmap.getHeight() != diameter) {
            scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, diameter, diameter, true);
        } else {
            scaledSrcBmp = squareBitmap;
        }

        Bitmap output = Bitmap.createBitmap(scaledSrcBmp.getWidth(),scaledSrcBmp.getHeight(), Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, scaledSrcBmp.getWidth(), scaledSrcBmp.getHeight());
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(scaledSrcBmp.getWidth() / 2, scaledSrcBmp.getHeight() / 2, scaledSrcBmp.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(scaledSrcBmp, rect, rect, paint);

        return  output;
    }



    /**
     * 背景色
     */

    private void drawBackgroundColor(Canvas canvas,int color) {

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0,0,defaultWidth,defaultHeight,paint);
    }

    /**
     * 边缘画圆
     */

    private void drawCircleBorder(Canvas canvas, int radius, int color) {

        Paint paint = new Paint();
        /* 去锯齿 */
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setColor(color);
        /* 设置paint的　style　为STROKE：空心 */
        paint.setStyle(Paint.Style.STROKE);
        /* 设置paint的外框宽度 */
        paint.setStrokeWidth(mCircleThickness);
        canvas.drawCircle(defaultWidth / 2, defaultHeight / 2 - mArrowHeight, radius, paint);
    }

    private void drawLocateArrow(Canvas canvas, int color) {


        Path arrawPath = new Path();
        arrawPath.moveTo(mArrowHeight, defaultHeight/2);
        arrawPath.lineTo(defaultWidth/2, defaultHeight);
        arrawPath.lineTo(defaultWidth-mArrowHeight, defaultHeight/2);
        arrawPath.close();//封闭

        Paint paint = new Paint();
        /* 去锯齿 */
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawPath(arrawPath,paint);

    }



}
