package com.example.demo.util;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

import java.util.*;

public class JsonUtil {


	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(String args) {
		Map<String, Object> argsMap = null;
		try {
			ObjectMapper om = new ObjectMapper();
	    	argsMap = om.readValue(args, Map.class);
		} catch(Exception ex) {
			argsMap = new HashMap<String, Object>();
		}
		
		return argsMap;
	}
	
	
	
	public static String toJsonString(Object obj) {
		if(obj == null) {
			return "";
		}
		ObjectMapper om = new ObjectMapper();
		try {
			return om.writeValueAsString(obj);
		} catch (Exception e) {

			return null;
		}
	}
	
	
	public static <T> T fromString(String jsonString, Class<T> c) {
		ObjectMapper om = new ObjectMapper();
		try {
		    //当字段不匹配时，保证转化其他字段能够正常转化
		    om.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return om.readValue(jsonString, c);
		} catch(Exception e) {

			return null;
		}
	}
	
	public static <T> List<T> toList(String jsonString, Class<T> c) {
		ObjectMapper om = new ObjectMapper();
		TypeFactory typeFactory = TypeFactory.defaultInstance();
		try {
		    //当字段不匹配时，保证转化其他字段能够正常转化
		    om.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return om.readValue(jsonString, typeFactory.constructCollectionType(List.class, c));
		} catch (Exception e) {

			return null;
		}
	}

	public static <T> List<T> toMapList(String jsonString, Class<Map> c) {
		ObjectMapper om = new ObjectMapper();
		TypeFactory typeFactory = TypeFactory.defaultInstance();
		try {
			//当字段不匹配时，保证转化其他字段能够正常转化
			om.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return om.readValue(jsonString, typeFactory.constructCollectionType(List.class, c));
		} catch (Exception e) {

			return null;
		}
	}


}
