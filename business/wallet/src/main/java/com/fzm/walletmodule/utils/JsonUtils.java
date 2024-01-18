package com.fzm.walletmodule.utils;


import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {
	private final static String TAG = JsonUtils.class.getSimpleName();
	
	public static String toJson(Object obj) {
		try {
			return JSON.toJSONString(obj);
		} catch (Exception e) {
			Log.w(TAG, "Exception in toJson from object", e);
		}
		return null;
	}
	
	public static <T> T toObject(String jsonString, Class<T> cls) {
		T t = null;
		try {
			t = JSON.parseObject(jsonString, cls);
		} catch (Exception e) {
			Log.w(TAG, "Exception in toObject with cls:" + cls.getSimpleName(), e);
		}
		return t;
	}


	public static <T> T toObject(JSONObject jsonObj, Class<T> cls) {
		return toObject(jsonObj.toString(), cls);
	}

	public static <T> List<T> toObjectList(JSONObject jsonObj, Class<T> cls) {
		return toObjectList(jsonObj.toString(), cls);
	}
	
	public static <T> List<T> toObjectList(String jsonString, Class<T> cls) {
		List<T> list = null;
		try {
			list = JSON.parseArray(jsonString, cls);
		} catch (Exception e) {
			Log.w(TAG, "Exception in toObjectList with cls:" + cls.getSimpleName(), e);
		}
		if (null == list) {
			list = new ArrayList<T>();
		}
		return list;
	}
}


