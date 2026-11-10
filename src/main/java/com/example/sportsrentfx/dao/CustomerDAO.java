package com.example.sportsrentfx.dao;

import com.example.sportsrentfx.security.AccessControl;
import com.example.sportsrentfx.db.DatabaseConnection;
import com.example.sportsrentfx.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public int addCustomerReturnId(Customer customer) {
        String query = "INSERT INTO Customers "
                + "(first_name, last_name, contact_number, email, reward_points) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setString(3, customer.getContactNumber());
            stmt.setString(4, customer.getEmail());
            stmt.setInt(5, customer.getRewardPoints());

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public Customer authenticateCustomer(
            String identifier,
            String contactNumber
    ) {
        if (identifier == null
                || identifier.trim().isEmpty()
                || contactNumber == null
                || contactNumber.trim().isEmpty()) {
            return null;
        }

        String cleanId = identifier.trim();
        String cleanContact = contactNumber.trim();

        String query = "SELECT * FROM Customers WHERE contact_number = ? "
                + "AND (CAST(customer_id AS CHAR) = ? "
                + "OR LOWER(email) = LOWER(?) "
                + "OR LOWER(first_name) = LOWER(?) "
                + "OR LOWER(CONCAT(first_name, ' ', last_name)) = LOWER(?)) "
                + "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, cleanContact);
            stmt.setString(2, cleanId);
            stmt.setString(3, cleanId);
            stmt.setString(4, cleanId);
            stmt.setString(5, cleanId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Customer getCustomerById(int customerId) {
        String query = "SELECT * FROM Customers WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Customer findPotentialDuplicate(
            String firstName,
            String lastName,
            String contactNumber,
            String email
    ) {
        boolean hasEmail = email != null && !email.trim().isEmpty();

        String query = "SELECT * FROM Customers WHERE "
                + "(LOWER(first_name) = LOWER(?) "
                + "AND LOWER(last_name) = LOWER(?) AND contact_number = ?) "
                + (hasEmail ? "OR LOWER(email) = LOWER(?) " : "")
                + "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, firstName.trim());
            stmt.setString(2, lastName.trim());
            stmt.setString(3, contactNumber.trim());

            if (hasEmail) {
                stmt.setString(4, email.trim());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateCustomer(Customer customer) {
        String query = "UPDATE Customers "
                + "SET first_name = ?, last_name = ?, "
                + "contact_number = ?, email = ? "
                + "WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setString(3, customer.getContactNumber());
            stmt.setString(4, customer.getEmail());
            stmt.setInt(5, customer.getCustomerId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Customer> getAllCustomers() {
        AccessControl.requireAdmin();

        List<Customer> list = new ArrayList<>();

        String query = "SELECT * FROM Customers ORDER BY customer_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                list.add(mapCustomer(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean deleteCustomer(int customerId) {
        AccessControl.requireAdmin();

        String checkHistory =
                "SELECT COUNT(*) FROM Rentals WHERE customer_id = ?";

        String deleteQuery =
                "DELETE FROM Customers WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement checkStmt =
                         conn.prepareStatement(checkHistory)) {

                checkStmt.setInt(1, customerId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false;
                    }
                }
            }

            try (PreparedStatement stmt =
                         conn.prepareStatement(deleteQuery)) {

                stmt.setInt(1, customerId);
                return stmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Customer> searchCustomers(String keyword) {
        AccessControl.requireAdmin();

        List<Customer> list = new ArrayList<>();

        String query = "SELECT * FROM Customers "
                + "WHERE first_name LIKE ? OR last_name LIKE ? "
                + "OR contact_number LIKE ? OR email LIKE ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String pattern = "%" + keyword + "%";

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCustomer(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("contact_number"),
                rs.getString("email"),
                rs.getInt("reward_points")
        );
    }
}