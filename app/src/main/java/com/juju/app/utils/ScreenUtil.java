package com.juju.app.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ScreenUtil {
	/**
	 * 获得屏幕宽度
	 */
	public static int getScreenWidth(Activity context){
        DisplayMetrics  dm = new DisplayMetrics(); 
        context.getWindowManager().getDefaultDisplay().getMetrics(dm); 
		return dm.widthPixels;
	}
	/**
	 * 获得屏幕高度
	 */
	public static int getScreenHeight(Activity context){
		DisplayMetrics  dm = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/**
	 * 获得屏幕宽高比
	 */
	public static float getScreenRate(Activity context){
		DisplayMetrics  dm = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dm);
		Float width = Float.intBitsToFloat(dm.widthPixels);
		Float height = Float.intBitsToFloat(dm.heightPixels);
		return (height/width);
	}

	public static int ViewgetScreenWidth(Context context){
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		int width = wm.getDefaultDisplay().getWidth();
		return width;
	}
	
	/**
	 * dp 转 px
	 */
	public static int dip2px(Context context,float dip){
		float scale=context.getResources().getDisplayMetrics().density;
		return (int)(dip*scale+0.5f);
	}
	/**
	 * px 转 dp
	 */
	public static int px2dp(Context context,float px){
		float scale = context.getResources().getDisplayMetrics().density; 
		return (int)(px/scale+0.5f); 
	}
	/**
	 * 获取通知栏的高度
	 * @param context
	 */
	public static int getStatusBarHeight(Activity context){
		Rect rect = new Rect();  
		context.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);  
		return rect.top;  
	}
}
