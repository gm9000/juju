package com.juju.app.utils;

import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 *    
 * 项目名称：jlmApp
 * 类名称：JacksonUtil   
 * 类描述：Jaskson辅助类
 * 创建人：gm   
 * 日期：2016年2月15日 下午4:54:00
 * 版本：V1.0.0
 *
 */
public class JacksonUtil {

	public static ObjectMapper objectMapper;

	/**
	 * 
	 * 方法名： turnString2Obj 
	 * 方法描述： 将json转对象
	 * 参数说明：
	 * 返回类型： T 
	 *
	 */
	public static <T> T turnString2Obj(String jsonStr, Class<T> valueType) {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}
		try {
			return objectMapper.readValue(jsonStr, valueType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * 
	 * 方法名： turnString2Obj 
	 * 方法描述： 输入流转对象
	 * 参数说明：
	 * 返回类型： T 
	 *
	 */
	public static <T> T turnString2Obj(InputStream is, Class<T> valueType) {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}
		try {
			return objectMapper.readValue(is, valueType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * 方法名： turnObj2String 
	 * 方法描述： 对象转Json
	 * 参数说明：
	 * 返回类型： String 
	 *
	 */
	public static String turnObj2String(Object object) {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 
	 * 方法名： tunString2Obj 
	 * 方法描述： json(数组)转List
	 * 参数说明：
	 * 返回类型： T 
	 *
	 */
	public static <T> T tunString2Obj(String jsonStr, TypeReference<T> valueTypeRef) {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}

		try {
			return objectMapper.readValue(jsonStr, valueTypeRef);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
