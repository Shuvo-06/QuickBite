package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.model.Coupon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO extends BaseDao<Coupon> {

    @Override
    public List<Coupon> findAll() throws SQLException {
        String sql = "SELECT id, code, discount_percent, start_date, end_date, is_active FROM coupons ORDER BY id DESC";
        List<Coupon> coupons = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                coupons.add(mapRow(rs));
            }
        }
        return coupons;
    }

    /** Looks up a coupon by its code (case-insensitive), for applying at checkout. */
    public Coupon findByCode(String code) throws SQLException {
        String sql = "SELECT id, code, discount_percent, start_date, end_date, is_active "
                + "FROM coupons WHERE UPPER(code) = UPPER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void insert(String code, int discountPercent, LocalDate start, LocalDate end) throws SQLException {
        String sql = "INSERT INTO coupons (code, discount_percent, start_date, end_date, is_active) "
                + "VALUES (?, ?, ?, ?, 1)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            ps.setInt(2, discountPercent);
            ps.setString(3, start.toString());
            ps.setString(4, end.toString());
            ps.executeUpdate();
        }
    }

    public void update(int id, int discountPercent, LocalDate start, LocalDate end) throws SQLException {
        String sql = "UPDATE coupons SET discount_percent = ?, start_date = ?, end_date = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, discountPercent);
            ps.setString(2, start.toString());
            ps.setString(3, end.toString());
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void setActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE coupons SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM coupons WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Coupon mapRow(ResultSet rs) throws SQLException {
        return new Coupon(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getInt("discount_percent"),
                LocalDate.parse(rs.getString("start_date")),
                LocalDate.parse(rs.getString("end_date")),
                rs.getInt("is_active") == 1);
    }
}
