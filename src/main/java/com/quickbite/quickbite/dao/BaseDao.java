package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Common base for QuickBite's DAOs. Demonstrates an abstract class: it requires every subclass
 * to provide findAll() (each decides what "all" means for its table) while sharing one real
 * implementation, connection(), so every DAO opens connections the same way instead of repeating
 * "DatabaseManager.getConnection()" as a magic call scattered across the codebase.
 *
 * @param <T> the model type this DAO loads rows into (Restaurant, Order, Coupon, User, ...)
 */
public abstract class BaseDao<T> {

    /** Every DAO must be able to load all of its rows. Each subclass decides what "all" means. */
    public abstract List<T> findAll() throws SQLException;

    /** Shared helper so every DAO opens connections the same way, with the same PRAGMAs applied. */
    protected Connection connection() throws SQLException {
        return DatabaseManager.getConnection();
    }
}