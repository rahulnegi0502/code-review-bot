// src/main/java/com/reviewbot/code_review_bot/TestCode.java
package com.reviewbot.code_review_bot;

public class TestCode {

    // SQL Injection vulnerability
    public String getUser(int id) {
        return "SELECT * FROM users WHERE id = " + id;
    }

    // Plain text password storage
    public void savePassword(String password) {
        String p = password;
        System.out.println("Password: " + p);
    }

    // Null pointer risk
    public int getLength(String str) {
        return str.length();
    }

    // Resource leak
    public void readFile(String path) throws Exception {
        java.io.FileInputStream fis =
                new java.io.FileInputStream(path);
        int data = fis.read();
    }
}