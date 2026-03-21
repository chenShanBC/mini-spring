package com.wubai.summer.test.pojo;

import com.wubai.summer.annotation.Component;



@Component
public class User {
    private String name = "testUser";
    private int age = 20;
    @Override
    public String toString() { return "User{name='" + name + "', age=" + age + "}"; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}

