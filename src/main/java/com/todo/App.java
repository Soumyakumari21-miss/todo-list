package com.todo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        // initialize DB (creates tasks.db and table/columns if needed)
        DatabaseHelper.initializeDatabase();

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- TO DO LIST ---");
            System.out.println("1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Mark Task as Completed");
            System.out.println("4. Delete Task");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception ignored) {
                System.out.println("Invalid input! Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    addTask(sc);
                case 2:
                    viewTasks();
                case 3:
                    markCompleted(sc);
                case 4:
                    deleteTask(sc);
                case 5: {
                    System.out.println("Exiting...");
                    sc.close();
                    return;
                }
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private static void addTask(Scanner sc) {
        System.out.print("Enter task name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Task cannot be empty.");
            return;
        }

        System.out.print("Enter due date (optional, e.g. 2023-11-30) or press Enter: ");
        String due = sc.nextLine().trim();
        if (due.isEmpty()) {
            due = null;
        }

        System.out.print("Enter priority (1=low,2=medium,3=high). Press Enter for default (1): ");
        String p = sc.nextLine().trim();
        int priority = 1;
        if (!p.isEmpty()) {
            try {
                priority = Integer.parseInt(p);
            } catch (NumberFormatException ignored) {
                priority = 1;
            }
        }

        String now = Instant.now().toString(); // ISO timestamp

        String sql = "INSERT INTO tasks (name, due_date, priority, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            if (due != null) {
                ps.setString(2, due);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            ps.setInt(3, priority);
            ps.setString(4, now);
            ps.setString(5, now);
            ps.executeUpdate();
            System.out.println("✅ Task added!");
        } catch (SQLException e) {
            System.out.println("Error adding task: " + e.getMessage());
        }
    }

    private static void viewTasks() {
        String sql = "SELECT id, name, completed, due_date, priority, created_at, updated_at FROM tasks ORDER BY id";
        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nYour Tasks:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                int id = rs.getInt("id");
                String name = rs.getString("name");
                boolean done = rs.getInt("completed") != 0;
                String due = rs.getString("due_date");
                int priority = rs.getInt("priority");
                String created = rs.getString("created_at");
                String updated = rs.getString("updated_at");

                String status = done ? "✅" : "❌";
                String dueText = (due == null) ? "" : (" | due: " + due);
                System.out.printf("%d. %s %s | priority:%d%s\n    created:%s updated:%s\n",
                        id, name, status, priority, dueText,
                        (created == null ? "-" : created),
                        (updated == null ? "-" : updated));
            }
            if (!any) {
                System.out.println("No tasks yet!");
            }
        } catch (SQLException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }

    private static void markCompleted(Scanner sc) {
        System.out.print("Enter task ID to toggle completed: ");
        String line = sc.nextLine().trim();
        int id;
        try {
            id = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid id.");
            return;
        }

        // First read current completed state
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement psSelect = conn.prepareStatement("SELECT completed FROM tasks WHERE id = ?")) {
            psSelect.setInt(1, id);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("⚠️ Task not found!");
                    return;
                }
                int current = rs.getInt("completed");
                int newVal = (current == 0) ? 1 : 0;
                String now = java.time.Instant.now().toString();

                try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE tasks SET completed = ?, updated_at = ? WHERE id = ?")) {
                    psUpdate.setInt(1, newVal);
                    psUpdate.setString(2, now);
                    psUpdate.setInt(3, id);
                    psUpdate.executeUpdate();
                    System.out.println(newVal == 1 ? "✅ Task marked as completed!" : "↩️ Task marked as not completed");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating task: " + e.getMessage());
        }
    }

    private static void deleteTask(Scanner sc) {
        System.out.print("Enter task ID to delete: ");
        String line = sc.nextLine().trim();
        int id;
        try {
            id = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid id.");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("🗑️ Task deleted!");
            } else {
                System.out.println("⚠️ Task not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting task: " + e.getMessage());
        }
    }
}
