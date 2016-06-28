package com.juju.app.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import com.juju.app.utils.PaintUtil;
import com.juju.app.view.edge.Edge;
import com.juju.app.view.imagezoom.ImageViewTouch;
import com.juju.app.view.imagezoom.graphics.FastBitmapDrawable;

/**
 * Created by Administrator on 2016/4/8 0008.
 */
public class CircleCopperImageView extends ImageViewTouch{

    private static final String LOG = "CircleCopperImageView";

    // The Paint used to darken the surrounding areas outside the crop area.
    private Paint mSurroundingAreaOverlayPaint;


    private int cutTop;
    private int cutEdgeSize;
    private float originRatio = 0.0f;


    public CircleCopperImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        seftInit(context, attrs);
    }

    private void seftInit(@NonNull Context context, @Nullable AttributeSet attrs) {
        final Resources resources = context.getResources();
        mSurroundingAreaOverlayPaint = PaintUtil.newSurroundingAreaOverlayPaint(resources);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale  = getScale();
        Log.d(LOG,"scale:"+scale);
        if(originRatio==0.0f && getScale()==1.0f){
            originRatio = (float)((FastBitmapDrawable) getDrawable()).getBitmap().getWidth()/canvas.getWidth();
        }
        drawCircleShape(canvas);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        originRatio = 0.0f;
    }



    /** 绘制裁剪框
    *
            * @param canvas
    */
    private void drawCircleShape(Canvas canvas) {
        cutEdgeSize = canvas.getWidth();
        cutTop = (canvas.getHeight()- canvas.getWidth()) / 2;
        canvas.save();
        Path LPath=new Path();
        LPath.lineTo(cutEdgeSize / 2, 0);
        LPath.lineTo(cutEdgeSize / 2, cutTop);

        LPath.cubicTo(-cutEdgeSize / 6, cutTop, -cutEdgeSize / 6, cutTop + cutEdgeSize, cutEdgeSize / 2, cutTop + cutEdgeSize);//三阶贝塞尔曲线
        LPath.lineTo(cutEdgeSize / 2, canvas.getHeight());
        LPath.lineTo(0, canvas.getHeight());
        LPath.close();

        Path RPath= new Path();
        RPath.moveTo(cutEdgeSize, 0);
        RPath.lineTo(cutEdgeSize / 2, 0);
        RPath.lineTo(cutEdgeSize / 2, cutTop);
        RPath.cubicTo(cutEdgeSize * 7 / 6, cutTop, cutEdgeSize * 7 / 6, cutTop + cutEdgeSize, cutEdgeSize / 2, cutTop + cutEdgeSize);
        RPath.lineTo(cutEdgeSize / 2, canvas.getHeight());
        RPath.lineTo(cutEdgeSize, canvas.getHeight());
        RPath.close();

        canvas.drawPath(LPath, mSurroundingAreaOverlayPaint);
        canvas.drawPath(RPath, mSurroundingAreaOverlayPaint);
        canvas.restore();

    }

    public boolean isFillCircle(){
        return getBitmapRect().top<=cutTop&&getBitmapRect().left<=0;
    }

    /**
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage() {

        // Implementation reference: http://stackoverflow.com/a/26930938/1068656


        float curRadio = originRatio/getScale();

        final Drawable drawable = getDrawable();
        if (drawable == null || !(drawable instanceof FastBitmapDrawable)) {
            return null;
        }



        // Get the original bitmap object.
        final Bitmap originalBitmap = ((FastBitmapDrawable) drawable).getBitmap();

        int originWidth = originalBitmap.getWidth();
        int originHeight = originalBitmap.getHeight();



        // Calculate the top-left corner of the crop window relative to the ~original~ bitmap size.
        float cropX = -getBitmapRect().left*curRadio;
        if(cropX > originWidth){
            cropX = originWidth;
        }
        float cropY = (cutTop-getBitmapRect().top)*curRadio;
        if(cropY > originHeight){
            cropY = originHeight;
        }

        // Calculate the crop window size relative to the ~original~ bitmap size.
        // Make sure the right and bottom edges are not outside the ImageView bounds (this is just to address rounding discrepancies).
        float cropWidth = cutEdgeSize*curRadio;
        if(cropWidth > originWidth-cropX){
            cropWidth = (float)originWidth-cropX;
        }
        float cropHeight = cutEdgeSize*curRadio;
        if(cropHeight > originHeight-cropY){
            cropHeight = (float)originHeight-cropY;
        }

        // Crop the subset from the original Bitmap.
        return Bitmap.createBitmap(originalBitmap,
                (int) cropX,
                (int) cropY,
                (int) cropWidth,
                (int) cropHeight);
    }

}
