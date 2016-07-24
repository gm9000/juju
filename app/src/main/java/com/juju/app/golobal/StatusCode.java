package com.juju.app.golobal;

import java.lang.reflect.Field;
import java.util.HashMap;

public class StatusCode {

	public static final int SUCCESS = 0;

	public static final int USER_NOT_EXIST = 11;
	public static final int GROUP_NOT_EXIST = 12;
	public static final int GROUP_USER_NOT_EXIST = 13;
	public static final int PARTY_NOT_EXIST = 14;
	public static final int PLAN_NOT_EXIST = 15;
	public static final int SERVER_NOT_EXIST = 16;

	public static final int TOKEN_NOT_EXIST = 17;
	public static final int LIVE_NOT_EXIST = 18;
	public static final int VOTE_NOT_EXIST = 19;
	public static final int LIVE_TOKEN_NOT_EXIST = 20;

	public static final int USER_HAS_EXIST = 21;
	public static final int PHONE_HAS_EXIST = 22;
	public static final int SERVER_HAS_EXIST = 23;
	public static final int VOTE_HAS_EXIST = 24;
	public static final int LIVE_HAS_EXIST = 25;


	public static final int PASSWORD_ERROR = 31;
	public static final int TOKEN_ERROR = 32;
	public static final int INVITE_CODE_ERROR = 33;
	public static final int ADMIN_QUIT_GROUP_ERROR = 34;


	public static final int NOT_PERMIT = 40;
	public static final int HAVE_CHECKED = 41;

	public static final int PARAM_ILLEGAL = 50;
	public static final int INVALID = 51;



	public static final int UNKNOWN_ERROR = 500;

	private static final HashMap<Integer,String> descMap = new HashMap<Integer,String>();

	static{
		Field[] fields = StatusCode.class.getFields();
		for(Field field:fields){
			if(field.getName().equals("descMap")){
				continue;
			}
			try {
				descMap.put(field.getInt(StatusCode.class), field.getName());
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String getDesc(int status) {
		return descMap.get(status);
	}

}
