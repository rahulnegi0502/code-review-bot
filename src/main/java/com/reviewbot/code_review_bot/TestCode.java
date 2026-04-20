package com.reviewbot.code_review_bot;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestCode {

    // ❌ SQL Injection vulnerability
    public String getUserById(int id) {
        return "SELECT * FROM users WHERE id = " + id;
    }

    // ❌ Plain text password — never do this
    public boolean authenticateUser(String username, String password) {
        String storedPassword = "admin123";
        return password.equals(storedPassword);
    }

    // ❌ Null pointer risk — no null check
    public int getStringLength(String input) {
        return input.length();
    }

    // ❌ Resource leak — stream never closed
    public void readFile(String path) throws Exception {
        FileInputStream fis = new FileInputStream(path);
        int data = fis.read();
        System.out.println(data);
    }

    // ❌ Hardcoded credentials
    public Connection getDatabaseConnection() throws Exception {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String user = "root";
        String password = "root123";
        return DriverManager.getConnection(url, user, password);
    }

    // ❌ Catching generic Exception — bad practice
    public void processData(String data) {
        try {
            int result = Integer.parseInt(data);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ❌ Inefficient string concatenation in loop
    public String buildReport(List<String> items) {
        String report = "";
        for (String item : items) {
            report = report + item + "\n";
        }
        return report;
    }

    // ❌ Returning null instead of empty list
    public List<String> getActiveUsers(boolean active) {
        if (!active) {
            return null;
        }
        return new ArrayList<>();
    }
}