package com.juju.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * The CustomVideoView is to make videoView view length-width based on the parameters you set to decide.
 *
 * @author peter.
 */
public class CustomerVideoView extends VideoView {
    private int mVideoWidth;
    private int mVideoHeight;

    public CustomerVideoView(Context context) {
        super(context);
    }

    public CustomerVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomerVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    /* The following code is to make videoView view length-width
        based on the parameters you set to decide. */
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}