package com.sfchain.core;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sfchain.core.plugin.ClassCompressionPlugin;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ClassCompressPluginTest {

    @Test
    public void testCompressObj(){
        ClassCompressionPlugin plugin = new ClassCompressionPlugin();
        Student student = new Student();
        System.out.println(JSON.toJSONString(student));
        Object outputJsonObj = plugin.output(student);
        System.out.println(outputJsonObj);
        Student student1 = plugin.decompress(outputJsonObj.toString(), Student.class);
        System.out.println(student1);
    }

    @Test
    public void testIdGenerate(){
        ClassCompressionPlugin.ParamsIdGenerator idGenerator = new ClassCompressionPlugin.ParamsIdGenerator();
        for (int i = 0; i < 1000; i++) {
            System.out.println(idGenerator.generateId());
        };
    }


    @Data
    @NoArgsConstructor
    public class Student{
        private String name = "Genius";
        private int age = 18;
        private Address address = new Address();
        private List<String> hobbies = List.of("Basketball", "Football");
        private List<Game> games = List.of(new Game());
    }

    @Data
    @NoArgsConstructor
    public class Address{
        private String province = "湖南";
        private String city = "长沙";
        private String street = "芙蓉区";
    }

    @Data
    @NoArgsConstructor
    public class Game{
        private String name = "王者荣耀";
        private int level = 30;
        private String company = "Tencent";
        private String type = "MOBA";
        private String serve = "QQ";
        private String rank = "最强王者";
        private int cloths =105;
        private int vip =10;
        private int heroNum =108;
        private String nickname = "SKT、FAKER";
    }
}
