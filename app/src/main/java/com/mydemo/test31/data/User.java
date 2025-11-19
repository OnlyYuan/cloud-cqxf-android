package com.mydemo.test31.data;

/**
 * 用户表
 */
public class User {

    private int id;
    private String name;
    private String password;

    private String pocUserName;

    private String pocPassword;

    public User() {

    }

    public User(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public User(String name, String password, String pocUserName, String pocPassword) {
        this.name = name;
        this.password = password;
        this.pocUserName = pocUserName;
        this.pocPassword = pocPassword;
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPocUserName() {
        return pocUserName;
    }

    public void setPocUserName(String pocUserName) {
        this.pocUserName = pocUserName;
    }

    public String getPocPassword() {
        return pocPassword;
    }

    public void setPocPassword(String pocPassword) {
        this.pocPassword = pocPassword;
    }
}