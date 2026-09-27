package com.quickbite.quickbite.model;

/**
 * A registered customer account, stored in the users table.
 * NOTE: passwords are stored as plain text, which is acceptable for this university prototype
 * but would need hashing (e.g. BCrypt) before any real-world use.
 */
public class User {
    private final int id;
    private final String username;
    private final String password;

    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
