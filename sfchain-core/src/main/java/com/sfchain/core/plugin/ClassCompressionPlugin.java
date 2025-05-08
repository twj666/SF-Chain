package com.sfchain.core.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassCompressionPlugin {

    private final Map<Class<?>, Map<String, ClassParamsInfo>> clazzParamsMapping = new HashMap<>();

    public Object output(Object obj) {
        return compressJsonObj(obj);
    }

    private Object compressJsonObj(Object obj) {
        Object jsonObj = JSON.toJSON(obj);
        if(jsonObj instanceof JSONArray){
            ((JSONArray) jsonObj).forEach(this::compressJsonObj);
        }else if(jsonObj instanceof JSONObject){
            ParamsIdGenerator idGenerator = new ParamsIdGenerator();
            List<String> replaceKeys = new ArrayList<>();
            ((JSONObject) jsonObj).forEach((key, value) -> {
                replaceKeys.add(key);
                ((JSONObject) jsonObj).put(key, compressJsonObj(value));
            });
            replaceKeys.forEach(key -> {
                ((JSONObject) jsonObj).put(idGenerator.generateId(), ((JSONObject) jsonObj).get(key));
                ((JSONObject) jsonObj).remove(key);
            });
        }
        return jsonObj;
    }

    private static boolean isJdkClass(Class<?> clazz) {
        String name = clazz.getName();
        return name.startsWith("java.") || name.startsWith("javax.");
    }

    public static void main(String[] args) {

    }

    @Data
    @AllArgsConstructor
    class ClassParamsInfo{

        private String paramsName;

        private String description;
    }

    public static class ParamsIdGenerator{

        private final static String CHAR_STR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        private int index = 0;

        public String generateId(){
            return getCharSequence(index++);
        }

        private static String getCharSequence(int index) {
            if (index < 0) return "";
            StringBuilder sb = new StringBuilder();
            do {
                int remainder = index % 62;
                sb.insert(0, CHAR_STR.charAt(remainder));
                index = (index / 62) - 1;
            } while (index >= 0);
            return sb.toString();
        }
    }
}
