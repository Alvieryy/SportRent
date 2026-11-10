package com.example.sportsrentfx.dao;

import com.example.sportsrentfx.security.AccessControl;
import com.example.sportsrentfx.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentDAO {

    public List<String[]> getEquipmentAvailability() {
        List<String[]> rows = new ArrayList<>();

        String query = "SELECT * FROM Equipment_Inventory_Report_View";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("equipment_name"),
                        rs.getString("category"),
                        String.valueOf(rs.getInt("total_quantity")),
                        String.valueOf(rs.getInt("available_quantity"))
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    public List<String[]> getEquipmentCatalogWithPrices() {
        List<String[]> rows = new ArrayList<>();

        String query =
                "SELECT em.name AS equipment_name, em.category, "
                        + "em.rental_price_per_day, "
                        + "COUNT(ti.item_id) AS total_quantity, "
                        + "SUM(CASE WHEN ti.status = 'Rented' "
                        + "THEN 1 ELSE 0 END) AS rented_quantity, "
                        + "SUM(CASE WHEN ti.status NOT IN ('Available', 'Rented') "
                        + "THEN 1 ELSE 0 END) AS unavailable_quantity, "
                        + "SUM(CASE WHEN ti.status = 'Available' "
                        + "THEN 1 ELSE 0 END) AS available_quantity "
                        + "FROM Equipment_Models em "
                        + "LEFT JOIN Tracked_Items ti ON em.model_id = ti.model_id "
                        + "GROUP BY em.model_id, em.name, em.category, "
                        + "em.rental_price_per_day "
                        + "ORDER BY em.name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("equipment_name"),
                        rs.getString("category"),
                        String.format(
                                "%.2f",
                                rs.getDouble("rental_price_per_day")
                        ),
                        String.valueOf(rs.getInt("total_quantity")),
                        String.valueOf(rs.getInt("rented_quantity")),
                        String.valueOf(rs.getInt("unavailable_quantity")),
                        String.valueOf(rs.getInt("available_quantity"))
                });
            }

        } catch (SQLException e) {
            for (String[] basic : getEquipmentAvailability()) {
                rows.add(new String[]{
                        basic[0],
                        basic[1],
                        "50.00",
                        basic[2],
                        "0",
                        "0",
                        basic[3]
                });
            }
        }

        return rows;
    }

    public boolean addEquipmentWithItems(
            String name,
            String category,
            double price,
            int quantity,
            String prefix
    ) {
        AccessControl.requireAdmin();

        if (name == null
                || name.trim().isEmpty()
                || price <= 0
                || quantity <= 0
                || prefix == null
                || prefix.trim().isEmpty()) {
            return false;
        }

        String findExistingModel =
                "SELECT model_id FROM Equipment_Models "
                        + "WHERE LOWER(name) = LOWER(?) LIMIT 1";

        String insertModel =
                "INSERT INTO Equipment_Models "
                        + "(name, category, rental_price_per_day) VALUES (?, ?, ?)";

        String countPrefix =
                "SELECT COUNT(*) FROM Tracked_Items WHERE serial_number LIKE ?";

        String insertItem =
                "INSERT INTO Tracked_Items "
                        + "(model_id, serial_number, status) "
                        + "VALUES (?, ?, 'Available')";

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int modelId = -1;

            try (PreparedStatement findStmt =
                         conn.prepareStatement(findExistingModel)) {

                findStmt.setString(1, name.trim());

                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        modelId = rs.getInt("model_id");
                    }
                }
            }

            if (modelId == -1) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        insertModel,
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    stmt.setString(1, name.trim());
                    stmt.setString(2, category);
                    stmt.setDouble(3, price);
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            modelId = rs.getInt(1);
                        }
                    }
                }
            }

            if (modelId == -1) {
                conn.rollback();
                return false;
            }

            String cleanPrefix = prefix.toUpperCase().trim();
            int startOffset = 0;

            try (PreparedStatement cntStmt =
                         conn.prepareStatement(countPrefix)) {

                cntStmt.setString(1, cleanPrefix + "-%");

                try (ResultSet rs = cntStmt.executeQuery()) {
                    if (rs.next()) {
                        startOffset = rs.getInt(1);
                    }
                }
            }

            try (PreparedStatement itemStmt =
                         conn.prepareStatement(insertItem)) {

                for (int i = 1; i <= quantity; i++) {
                    String serialNumber = cleanPrefix
                            + "-"
                            + String.format("%03d", startOffset + i);

                    itemStmt.setInt(1, modelId);
                    itemStmt.setString(2, serialNumber);
                    itemStmt.addBatch();
                }

                itemStmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();
            return false;

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean updateEquipmentRate(
            String equipmentName,
            double newPricePerDay
    ) {
        AccessControl.requireAdmin();

        if (equipmentName == null
                || equipmentName.trim().isEmpty()
                || newPricePerDay <= 0) {
            return false;
        }

        String sql = "UPDATE Equipment_Models "
                + "SET rental_price_per_day = ? WHERE name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPricePerDay);
            stmt.setString(2, equipmentName.trim());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}