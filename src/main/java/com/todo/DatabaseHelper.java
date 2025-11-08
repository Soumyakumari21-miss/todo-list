package com.todo;

import java.sql.*;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:tasks.db";

    // Call at application start
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

            // Create table if not exists (basic columns)
            String sqlCreate = "CREATE TABLE IF NOT EXISTS tasks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "completed INTEGER DEFAULT 0,"
                    + "due_date TEXT,"
                    + "priority INTEGER DEFAULT 1,"
                    + "created_at TEXT,"
                    + "updated_at TEXT"
                    + ")";
            stmt.execute(sqlCreate);

            // Try to add each new column safely (if it already exists, an exception is thrown; we ignore it)
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN due_date TEXT");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN priority INTEGER DEFAULT 1");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN created_at TEXT");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN updated_at TEXT");
            } catch (SQLException ignored) {
            }

        } catch (SQLException e) {
            System.out.println("⚠️ Error initializing database: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
