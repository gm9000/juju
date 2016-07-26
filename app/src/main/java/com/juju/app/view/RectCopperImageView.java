package com.juju.app.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.juju.app.utils.PaintUtil;
import com.juju.app.view.imagezoom.ImageViewTouch;
import com.juju.app.view.imagezoom.graphics.FastBitmapDrawable;

/**
 * Created by Administrator on 2016/4/8 0008.
 */
public class RectCopperImageView extends ImageViewTouch{

    private static final String LOG = "RectCopperImageView";

    // The Paint used to darken the surrounding areas outside the crop area.
    private Paint mSurroundingAreaOverlayPaint;


    private int cutTop;
    private int cutWidth;
    private int cutHeight;
    private float originRatio = 0.0f;
    private float originScale = 1.0f;


    public RectCopperImageView(Context context, AttributeSet attrs) {
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
        if(originRatio==0.0f){
            originScale = scale;
            originRatio = (float)((FastBitmapDrawable) getDrawable()).getBitmap().getWidth()/canvas.getWidth();
        }
        drawRectShape(canvas);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        originRatio = 0.0f;
        super.setImageBitmap(bm);
    }



    /** 绘制裁剪框
    *
            * @param canvas
    */
    private void drawRectShape(Canvas canvas) {
        cutWidth = canvas.getWidth();
        cutHeight = cutWidth * 9 / 16;
        cutTop = (canvas.getHeight()-cutHeight) / 2;
        canvas.drawRect(0,0,cutWidth,cutTop,mSurroundingAreaOverlayPaint);
        canvas.drawRect(0,cutTop+cutHeight,cutWidth,canvas.getHeight(),mSurroundingAreaOverlayPaint);
        canvas.restore();

    }

    public boolean isFillRect(){
        return getBitmapRect().top<=cutTop&&getBitmapRect().left<=0;
    }

    /**
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage() {

        // Implementation reference: http://stackoverflow.com/a/26930938/1068656


        float curRadio = originRatio*originScale/getScale();

        final Drawable drawable = getDrawable();
        if (drawable == null || !(drawable instanceof FastBitmapDrawable)) {
            return null;
        }



        // Get the original bitmap object.
        final Bitmap originalBitmap = ((FastBitmapDrawable) drawable).getBitmap();

        int originWidth = originalBitmap.getWidth();
        int originHeight = originalBitmap.getHeight();



        // Calculate the top-left corner of the crop window relative to the ~original~ bitmap size.
        float cropX = -getBitmapRect().left*curRadio/originScale;
        if(cropX > originWidth){
            cropX = originWidth;
        }
        float cropY = (cutTop-getBitmapRect().top)*curRadio/originScale;
        if(cropY > originHeight){
            cropY = originHeight;
        }

        // Calculate the crop window size relative to the ~original~ bitmap size.
        // Make sure the right and bottom edges are not outside the ImageView bounds (this is just to address rounding discrepancies).
        float cropWidth = cutWidth*curRadio/originScale;
        if(cropWidth > originWidth-cropX){
            cropWidth = (float)originWidth-cropX;
        }
        float cropHeight = cutHeight*curRadio/originScale;
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
