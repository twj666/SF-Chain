package com.sfchain.core.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;

public class ClassCompressionPlugin {

    private final Map<Class<?>, List<ClassParamsInfo>> clazzParamsMapping = new HashMap<>();

    public Object output(Object obj) {
        buildMapping(obj.getClass());
        return compressJsonObj(obj);
    }

    public <T> T decompress(String jsonStr, Class<T> clazz) {
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        decompress0(jsonObject, clazz);
        return JSONObject.parseObject(jsonObject.toJSONString(), clazz);
    }

    private void decompress0(Object jsonObj, Class<?> clazz) {
        List<ClassParamsInfo> paramsInfoList = clazzParamsMapping.getOrDefault(clazz, Collections.emptyList());
        if(CollectionUtils.isEmpty(paramsInfoList)){
            return;
        }
        if(jsonObj instanceof JSONArray){
            ((JSONArray) jsonObj).forEach(this::compressJsonObj);
        }else if(jsonObj instanceof JSONObject){
            Map<String, ClassParamsInfo> paramsInfoMap = new HashMap<>();
            ((JSONObject) jsonObj).forEach((key, value) -> {
                ClassParamsInfo classParamsInfo = paramsInfoList.get(ParamsIdGenerator.idToIndex(key));
                paramsInfoMap.put(key, classParamsInfo);
            });

            paramsInfoMap.forEach((key, paramsInfo)->{
                ((JSONObject) jsonObj).put(paramsInfo.getParamsName(), ((JSONObject) jsonObj).get(key));
                ((JSONObject) jsonObj).remove(key);
            });
        }
        return;
    }

    private void buildMapping(Class<?> clazz){
        if(clazzParamsMapping.containsKey(clazz)){
           return;
        }
        List<ClassParamsInfo> paramsInfoList = new ArrayList<>();
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            String fieldName = field.getName();
            Class<? extends Field> fieldClass = field.getClass();
            ClassParamsInfo classParamsInfo = new ClassParamsInfo(fieldName ,fieldClass, fieldName);
            paramsInfoList.add(classParamsInfo);
            if(!isJdkClass(fieldClass)){
                buildMapping(fieldClass);
            };
        }
        Collections.sort(paramsInfoList);
        clazzParamsMapping.put(clazz, paramsInfoList);
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

    @Data
    @AllArgsConstructor
    class ClassParamsInfo implements Comparable<ClassParamsInfo> {

        private String paramsName;

        private Class<?> clazz;

        private String description;

        @Override
        public int compareTo(ClassParamsInfo o) {
            return paramsName.compareTo(o.paramsName);
        }
    }

    public static class ParamsIdGenerator{

        private final static String CHAR_STR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        private int index = 0;

        public String generateId(){
            return getCharSequence(index++);
        }

        public static int idToIndex(String id){
            int tempIndex = 0;
            for (int i = 0; i < id.length(); i++) {
                char ch = id.charAt(i);
                int index = CHAR_STR.indexOf(ch);
                if (index == -1) {
                    return -1;
                }
                tempIndex = tempIndex * 62 + index;
            }
            return tempIndex;
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
