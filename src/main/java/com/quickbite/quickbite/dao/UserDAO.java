package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class UserDAO extends BaseDao<User> {

    /** Every registered account. Not shown anywhere in the UI yet, but ready for an admin "Users" tab. */
    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, password FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password")));
            }
        }
        return users;
    }

    /** Returns the matching account, or null if the username doesn't exist. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
                }
                return null;
            }
        }
    }

    /** True if this username is already registered. */
    public boolean exists(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    /** Creates a new account. Call exists() first to give the user a friendly "already taken" message. */
    public User register(String username, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getInt(1), username, password);
            }
        }
    }
}
