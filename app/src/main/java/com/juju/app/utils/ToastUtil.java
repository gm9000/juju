package com.juju.app.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;


public class ToastUtil {
	private static Toast toast = null;
	private static int LENGTH_SHORT = Toast.LENGTH_SHORT;
	public static int LENGTH_LONG = Toast.LENGTH_LONG;
	

	/**
	 * 普通文本消息提示
	 * 
	 * @param context
	 * @param text
	 * @param duration
	 */
	public static void TextStringToast(Context context, CharSequence text,
			int duration) {
		// 创建一个Toast提示消息
		toast = Toast.makeText(context, text, duration);
		// 设置Toast提示消息在屏幕上的位置
		toast.setGravity(Gravity.CENTER, 0, 0);
		// 显示消息
		toast.show();
	}
	
	public static void TextIntToast(Context context, int text,
			int duration) {
		// 创建一个Toast提示消息
		toast = Toast.makeText(context, text, duration);
		// 设置Toast提示消息在屏幕上的位置
		toast.setGravity(Gravity.CENTER, 0, 0);
		// 显示消息
		toast.show();
	}


	/**
	 * 带图片消息提示
	 * 
	 * @param context
	 * @param ImageResourceId
	 * @param text
	 * @param duration
	 */
	public static void ImageToast(Context context, int ImageResourceId,
			CharSequence text, int duration) {
		// 创建一个Toast提示消息
		toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
		// 设置Toast提示消息在屏幕上的位置
		toast.setGravity(Gravity.CENTER, 0, 0);
		// 获取Toast提示消息里原有的View
		View toastView = toast.getView();
		// 创建一个ImageView
		ImageView img = new ImageView(context);
		img.setImageResource(ImageResourceId);
		// 创建一个LineLayout容器
		LinearLayout ll = new LinearLayout(context);
		// 向LinearLayout中添加ImageView和Toast原有的View
		ll.addView(img);
		ll.addView(toastView);
		// 将LineLayout容器设置为toast的View
		toast.setView(ll);
		// 显示消息
		toast.show();
	}
	
	public static void showShortToast(Context context,String text, int grayity) {
		toast(context,text,grayity,Toast.LENGTH_SHORT);
	}

	public static void toast(Context context,String text,int grayity,int time){
		Toast toast = Toast.makeText(context,text,time);
		toast.setGravity(grayity,0,100);
		toast.show();
	}

	public static void showLongToast(Context context, String pMsg) {
		Toast.makeText(context, pMsg, Toast.LENGTH_LONG).show();
	}

	public static void showSpeeker(Context context, CharSequence text, int duration) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View view = inflater.inflate(R.layout.tt_speeker_layout, null);
		TextView title = (TextView) view.findViewById(R.id.top_tip);
		title.setText(text);
		Toast toast = new Toast(context.getApplicationContext());
		toast.setGravity(
				Gravity.FILL_HORIZONTAL | Gravity.TOP,
				0,
				(int) context.getResources().getDimension(
						R.dimen.top_bar_default_height));
		toast.setDuration(duration);
		toast.setView(view);
		toast.show();
	}
}