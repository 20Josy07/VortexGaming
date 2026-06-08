package com.localcode.vortexgaming.models;

public class UserModel {
    public String name;
    public String email;
    public String password;
    public String dob;

    public UserModel(String name, String email, String password, String dob) {
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.dob      = dob;
    }
}
