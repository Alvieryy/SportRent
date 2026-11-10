package com.example.sportsrentfx.security;

import com.example.sportsrentfx.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

public final class AccessControl {

    private static boolean adminAuthenticated;
    private static long lastActivity;

    private static final long SESSION_TIMEOUT =
            Duration.ofMinutes(15).toNanos();

    private AccessControl() {}

    public static synchronized void logout() {
        adminAuthenticated = false;
        lastActivity = 0;
    }

    public static synchronized boolean isAdminSessionExpired() {
        return adminAuthenticated
                && System.nanoTime() - lastActivity >= SESSION_TIMEOUT;
    }

    public static synchronized void requireAdmin() {
        if (isAdminSessionExpired()) {
            logout();
        }

        if (!adminAuthenticated) {
            throw new SecurityException("Administrator access required.");
        }

        lastActivity = System.nanoTime();
    }

    public static synchronized boolean loginAdmin(
            String username,
            char[] password
    ) {
        // Failed authentication must never retain an older admin session.
        logout();

        if (username == null
                || username.isBlank()
                || username.length() > 100
                || password == null
                || password.length == 0
                || password.length > 128) {
            return false;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int adminId;
                int failedAttempts;
                boolean validPassword;

                String query = """
                        SELECT admin_id,
                               password_hash,
                               failed_attempts,
                               active,
                               (
                                   locked_until IS NOT NULL
                                   AND locked_until > CURRENT_TIMESTAMP
                               ) AS locked
                        FROM admin_accounts
                        WHERE username = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(query)) {

                    statement.setString(1, username.trim());

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()
                                || !result.getBoolean("active")
                                || result.getBoolean("locked")) {
                            connection.rollback();
                            return false;
                        }

                        adminId = result.getInt("admin_id");
                        failedAttempts = result.getInt("failed_attempts");

                        validPassword = PasswordHash.verify(
                                password,
                                result.getString("password_hash")
                        );
                    }
                }

                if (validPassword) {
                    String reset = """
                            UPDATE admin_accounts
                            SET failed_attempts = 0,
                                locked_until = NULL
                            WHERE admin_id = ?
                            """;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(reset)) {
                        statement.setInt(1, adminId);
                        statement.executeUpdate();
                    }

                } else {
                    // A completed lockout starts a new attempt window.
                    int nextAttempt =
                            failedAttempts >= 5 ? 1 : failedAttempts + 1;

                    String update = """
                            UPDATE admin_accounts
                            SET failed_attempts = ?,
                                locked_until =
                                    CASE
                                        WHEN ? >= 5
                                        THEN DATE_ADD(
                                            CURRENT_TIMESTAMP,
                                            INTERVAL 15 MINUTE
                                        )
                                        ELSE NULL
                                    END
                            WHERE admin_id = ?
                            """;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(update)) {
                        statement.setInt(1, nextAttempt);
                        statement.setInt(2, nextAttempt);
                        statement.setInt(3, adminId);
                        statement.executeUpdate();
                    }
                }

                connection.commit();

                if (validPassword) {
                    adminAuthenticated = true;
                    lastActivity = System.nanoTime();
                }

                return validPassword;

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                logout();
                return false;
            }

        } catch (SQLException exception) {
            logout();
            return false;
        }
    }
}