package com.juju.app.utils;

import android.content.ClipboardManager;
import android.content.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	private static final Pattern emailer = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	private static final String URLSTR = "[http|https]+[://]+[0-9A-Za-z:/[-]_#[?][=][.][&]]*";
	private static final String URLSTR2 = "网页链接";
	private static final Pattern URL = Pattern.compile(URLSTR);
	private static final Pattern URL2 = Pattern.compile(URLSTR2);

	/**
	 * 判断是不是手机号
	 * @param mobiles
	 * @return
	 */
	public static boolean isMobileNO(String mobiles){
		String regex = "^(((86|\\+86|0|)1[34578][0-9]{9})|(\\d{3,4}-(\\d{7,8})))$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(mobiles);
		return m.matches();
	}
	
	public static boolean isEmail(String email)
	{
		if ((email == null) || (email.trim().length() == 0))
			return false;
		return emailer.matcher(email).matches();
	}
	
	public static boolean notEmpty(Object obj){
		if(obj!=null && !obj.toString().trim().equals("")){
			return true;
		}
		return false;
	}
	
	public static boolean empty(Object obj){
		if(obj ==null || obj.toString().trim().equals("")){
			return true;
		}
		return false;
	}
	

	/**
	 * 复制到黏贴板
	 */
	public static void copy(Context context,String str){
		ClipboardManager clip = (ClipboardManager)context.
				getSystemService(Context.CLIPBOARD_SERVICE);
		clip.setText(str);
	}
	/**
	 * 黏贴
	 */
	public static String paste(Context context){
		ClipboardManager clip = (ClipboardManager)context.
				getSystemService(Context.CLIPBOARD_SERVICE);
		return clip.getText().toString();
	}

	public static String formatMobileNO(String mobiles) {
		if(isMobileNO(mobiles)) {
			String first = mobiles.substring(0, 3);
			String middle = mobiles.substring(3, 7);
			String end = mobiles.substring(7, 11);
			String target = first+" "+middle+" "+end;
			return target;
		} else {
			return mobiles;
		}
	}

	public static boolean isBlank(String str) {
		return org.apache.commons.lang.StringUtils.isBlank(str);
	}

	public static boolean isNotBlank(String str) {
		return org.apache.commons.lang.StringUtils.isNotBlank(str);
	}
}
