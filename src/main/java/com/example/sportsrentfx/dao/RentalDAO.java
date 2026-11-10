package com.example.sportsrentfx.dao;

import com.example.sportsrentfx.db.DatabaseConnection;
import com.example.sportsrentfx.security.AccessControl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RentalDAO {

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private List<String> getEnumValues(
            Connection conn,
            String tableName,
            String columnName
    ) {
        List<String> values = new ArrayList<>();

        String sql = """
                SELECT COLUMN_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND LOWER(TABLE_NAME) = LOWER(?)
                  AND LOWER(COLUMN_NAME) = LOWER(?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("COLUMN_TYPE");

                    if (type != null
                            && type.toLowerCase().startsWith("enum(")) {
                        String inner = type.substring(5, type.length() - 1);

                        for (String part : inner.split(",")) {
                            String value = part.trim().replace("'", "");

                            if (!value.isEmpty()) {
                                values.add(value);
                            }
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return values;
    }

    private String findAllowedStatus(
            List<String> allowedValues,
            String... candidates
    ) {
        for (String candidate : candidates) {
            for (String allowed : allowedValues) {
                if (allowed.equalsIgnoreCase(candidate)) {
                    return allowed;
                }
            }
        }

        return null;
    }

    public List<String> getAllowedPaymentMethods() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<String> methods = getEnumValues(
                    conn,
                    "payments",
                    "payment_method"
            );

            if (!methods.isEmpty()) {
                return methods;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return Arrays.asList("Cash", "GCash", "Card", "Bank Transfer");
    }

    public List<String> getAvailableEquipmentNames() {
        List<String> names = new ArrayList<>();

        String sql = """
                SELECT DISTINCT em.name
                FROM Equipment_Models em
                JOIN Tracked_Items ti ON em.model_id = ti.model_id
                WHERE ti.status = 'Available'
                ORDER BY em.name ASC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return names;
    }

    public double getEquipmentDailyPrice(String equipmentName) {
        if (equipmentName == null || equipmentName.isBlank()) {
            return 0.0;
        }

        String sql = """
                SELECT rental_price_per_day
                FROM Equipment_Models
                WHERE name = ?
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, equipmentName.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("rental_price_per_day");
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return 0.0;
    }

    public int getAvailableCountForEquipment(String equipmentName) {
        if (equipmentName == null || equipmentName.isBlank()) {
            return 0;
        }

        String sql = """
                SELECT COUNT(ti.item_id) AS available_count
                FROM Tracked_Items ti
                JOIN Equipment_Models em ON ti.model_id = em.model_id
                WHERE em.name = ?
                  AND ti.status = 'Available'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, equipmentName.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("available_count");
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return 0;
    }

    public boolean borrowEquipment(
            int customerId,
            String equipmentName,
            int days
    ) {
        return borrowEquipmentQuantity(
                customerId,
                equipmentName,
                1,
                days
        ) != -1;
    }

    public int borrowEquipmentQuantity(
            int customerId,
            String equipmentName,
            int quantity,
            int days
    ) {
        if (customerId <= 0
                || equipmentName == null
                || equipmentName.isBlank()
                || quantity <= 0
                || days <= 0) {
            return -1;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                String itemSql = """
                        SELECT ti.item_id, em.rental_price_per_day
                        FROM Tracked_Items ti
                        JOIN Equipment_Models em
                            ON ti.model_id = em.model_id
                        WHERE em.name = ?
                          AND ti.status = 'Available'
                        ORDER BY ti.item_id ASC
                        LIMIT ? FOR UPDATE
                        """;

                List<Integer> itemIds = new ArrayList<>();
                BigDecimal rate = BigDecimal.ZERO;

                try (PreparedStatement stmt =
                             conn.prepareStatement(itemSql)) {
                    stmt.setString(1, equipmentName.trim());
                    stmt.setInt(2, quantity);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            itemIds.add(rs.getInt("item_id"));
                            rate = rs.getBigDecimal("rental_price_per_day");
                        }
                    }
                }

                if (itemIds.size() < quantity
                        || rate == null
                        || rate.signum() <= 0) {
                    conn.rollback();
                    return -1;
                }

                BigDecimal total = rate
                        .multiply(BigDecimal.valueOf(quantity))
                        .multiply(BigDecimal.valueOf(days))
                        .setScale(2, RoundingMode.HALF_UP);

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime due = now.plusDays(days);

                String rentalSql = """
                        INSERT INTO Rentals (
                            customer_id,
                            rental_date,
                            due_date,
                            total_amount,
                            points_redeemed,
                            status
                        )
                        VALUES (?, ?, ?, ?, 0, 'Active')
                        """;

                int rentalId = -1;

                try (PreparedStatement stmt = conn.prepareStatement(
                        rentalSql,
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    stmt.setInt(1, customerId);
                    stmt.setTimestamp(2, Timestamp.valueOf(now));
                    stmt.setTimestamp(3, Timestamp.valueOf(due));
                    stmt.setBigDecimal(4, total);
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            rentalId = rs.getInt(1);
                        }
                    }
                }

                if (rentalId == -1) {
                    conn.rollback();
                    return -1;
                }

                String detailSql = """
                        INSERT INTO Rental_Details (rental_id, item_id)
                        VALUES (?, ?)
                        """;

                String updateSql = """
                        UPDATE Tracked_Items
                        SET status = 'Rented'
                        WHERE item_id = ?
                        """;

                try (PreparedStatement detailStmt =
                             conn.prepareStatement(detailSql);
                     PreparedStatement itemStmt =
                             conn.prepareStatement(updateSql)) {

                    for (int itemId : itemIds) {
                        detailStmt.setInt(1, rentalId);
                        detailStmt.setInt(2, itemId);
                        detailStmt.addBatch();

                        itemStmt.setInt(1, itemId);
                        itemStmt.addBatch();
                    }

                    detailStmt.executeBatch();
                    itemStmt.executeBatch();
                }

                conn.commit();
                return rentalId;

            } catch (SQLException | RuntimeException exception) {
                rollback(conn);
                exception.printStackTrace();
                return -1;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }
    }

    // Voucher tiers are based on the equipment's current daily rental price.

    public static BigDecimal getVoucherPercentage(BigDecimal dailyRate) {
        if (dailyRate == null || dailyRate.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        if (dailyRate.compareTo(new BigDecimal("500")) >= 0) {
            return new BigDecimal("0.15");
        }

        if (dailyRate.compareTo(new BigDecimal("100")) >= 0) {
            return new BigDecimal("0.10");
        }

        return new BigDecimal("0.05");
    }

    public static BigDecimal getVoucherMaximum(BigDecimal dailyRate) {
        if (dailyRate == null || dailyRate.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        if (dailyRate.compareTo(new BigDecimal("500")) >= 0) {
            return new BigDecimal("100");
        }

        if (dailyRate.compareTo(new BigDecimal("100")) >= 0) {
            return new BigDecimal("30");
        }

        return new BigDecimal("5");
    }

    public static String getVoucherDescription(BigDecimal dailyRate) {
        if (dailyRate == null || dailyRate.signum() <= 0) {
            return "No voucher available";
        }

        int percentage = getVoucherPercentage(dailyRate)
                .multiply(new BigDecimal("100"))
                .intValueExact();

        return percentage + "% off • Maximum ₱"
                + getVoucherMaximum(dailyRate).toPlainString();
    }

    public static int calculateRedeemablePoints(
            int availablePoints,
            BigDecimal balance,
            BigDecimal dailyRate
    ) {
        if (availablePoints < 10
                || balance == null
                || balance.signum() <= 0
                || dailyRate == null
                || dailyRate.signum() <= 0) {
            return 0;
        }

        BigDecimal percentageDiscount = balance.multiply(
                getVoucherPercentage(dailyRate)
        );

        BigDecimal walletDiscount = BigDecimal.valueOf(
                availablePoints / 10
        );

        BigDecimal finalDiscount = percentageDiscount
                .min(getVoucherMaximum(dailyRate))
                .min(walletDiscount)
                .setScale(0, RoundingMode.DOWN);

        return finalDiscount.intValueExact() * 10;
    }

    private BigDecimal getRentalVoucherDailyRate(
            Connection conn,
            int rentalId
    ) throws SQLException {
        // For mixed equipment rentals, use the lowest daily rate.
        String sql = """
                SELECT MIN(em.rental_price_per_day) AS voucher_daily_rate
                FROM Rental_Details rd
                JOIN Tracked_Items ti ON rd.item_id = ti.item_id
                JOIN Equipment_Models em ON ti.model_id = em.model_id
                WHERE rd.rental_id = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rentalId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal rate = rs.getBigDecimal("voucher_daily_rate");
                    return rate == null ? BigDecimal.ZERO : rate;
                }
            }
        }

        return BigDecimal.ZERO;
    }

    public String payRentalFee(
            int customerId,
            int rentalId,
            BigDecimal amount,
            String paymentMethod,
            int requestedPoints
    ) {
        if (customerId <= 0 || rentalId <= 0) {
            return "Please select a valid rental.";
        }

        if (amount == null || amount.signum() <= 0) {
            return "Enter a payment amount greater than ₱0.00.";
        }

        final BigDecimal paymentAmount;

        try {
            paymentAmount = amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            return "Payment amounts can have at most two decimal places.";
        }

        if (requestedPoints < 0 || requestedPoints % 10 != 0) {
            return "Points must be redeemed in groups of 10.";
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "Please select a payment method.";
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                BigDecimal totalCharge;
                int previousPoints;

                String rentalSql = """
                        SELECT customer_id,
                               total_amount,
                               COALESCE(points_redeemed, 0) AS points_redeemed
                        FROM Rentals
                        WHERE rental_id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(rentalSql)) {
                    stmt.setInt(1, rentalId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()
                                || rs.getInt("customer_id") != customerId) {
                            conn.rollback();
                            return "Rental not found under your account.";
                        }

                        totalCharge = rs.getBigDecimal("total_amount");
                        previousPoints = rs.getInt("points_redeemed");

                        if (totalCharge == null) {
                            conn.rollback();
                            return "This rental has an invalid charge.";
                        }

                        totalCharge = money(totalCharge);
                    }
                }

                // Lock the wallet to prevent spending the same points twice.
                int availablePoints;

                String customerSql = """
                        SELECT COALESCE(reward_points, 0) AS reward_points
                        FROM Customers
                        WHERE customer_id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(customerSql)) {
                    stmt.setInt(1, customerId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return "Customer account not found.";
                        }

                        availablePoints = rs.getInt("reward_points");
                    }
                }

                BigDecimal paidSoFar = BigDecimal.ZERO.setScale(2);

                String paidSql = """
                        SELECT COALESCE(SUM(amount_paid), 0.00) AS paid
                        FROM Payments
                        WHERE rental_id = ?
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(paidSql)) {
                    stmt.setInt(1, rentalId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            paidSoFar = money(rs.getBigDecimal("paid"));
                        }
                    }
                }

                BigDecimal balance = totalCharge.subtract(paidSoFar);

                if (balance.signum() <= 0) {
                    conn.rollback();
                    return "This rental is already fully paid.";
                }

                BigDecimal discount = BigDecimal.ZERO.setScale(2);

                if (requestedPoints > 0) {
                    if (previousPoints > 0) {
                        conn.rollback();
                        return "A voucher has already been used on this rental.";
                    }

                    BigDecimal dailyRate = getRentalVoucherDailyRate(
                            conn,
                            rentalId
                    );

                    int allowedPoints = calculateRedeemablePoints(
                            availablePoints,
                            balance,
                            dailyRate
                    );

                    if (requestedPoints > allowedPoints) {
                        conn.rollback();
                        return "Your points, balance, or voucher changed. "
                                + "Refresh and try again.";
                    }

                    discount = BigDecimal.valueOf(requestedPoints)
                            .divide(BigDecimal.TEN)
                            .setScale(2);

                    BigDecimal requiredPayment =
                            balance.subtract(discount);

                    if (paymentAmount.compareTo(requiredPayment) != 0) {
                        conn.rollback();
                        return "Using a voucher requires full settlement of ₱"
                                + requiredPayment.toPlainString()
                                + ". Please refresh the payment form.";
                    }

                } else if (paymentAmount.compareTo(balance) > 0) {
                    conn.rollback();
                    return "Payment exceeds the remaining balance of ₱"
                            + balance.toPlainString() + ".";
                }

                if (requestedPoints > 0) {
                    String deductSql = """
                            UPDATE Customers
                            SET reward_points = reward_points - ?
                            WHERE customer_id = ?
                              AND reward_points >= ?
                            """;

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(deductSql)) {
                        stmt.setInt(1, requestedPoints);
                        stmt.setInt(2, customerId);
                        stmt.setInt(3, requestedPoints);

                        if (stmt.executeUpdate() != 1) {
                            conn.rollback();
                            return "Insufficient points. Please refresh.";
                        }
                    }

                    // total_amount stores the net charge after the discount.
                    String discountSql = """
                            UPDATE Rentals
                            SET total_amount = total_amount - ?,
                                points_redeemed = ?
                            WHERE rental_id = ?
                            """;

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(discountSql)) {
                        stmt.setBigDecimal(1, discount);
                        stmt.setInt(2, requestedPoints);
                        stmt.setInt(3, rentalId);

                        if (stmt.executeUpdate() != 1) {
                            conn.rollback();
                            return "Unable to apply the voucher.";
                        }
                    }
                }

                // Only actual money paid is recorded as payment revenue.
                String paymentSql = """
                        INSERT INTO Payments (
                            rental_id,
                            payment_date,
                            amount_paid,
                            payment_method
                        )
                        VALUES (?, ?, ?, ?)
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(paymentSql)) {
                    stmt.setInt(1, rentalId);
                    stmt.setTimestamp(
                            2,
                            Timestamp.valueOf(LocalDateTime.now())
                    );
                    stmt.setBigDecimal(3, paymentAmount);
                    stmt.setString(4, paymentMethod.trim());

                    if (stmt.executeUpdate() != 1) {
                        conn.rollback();
                        return "Unable to record payment.";
                    }
                }

                conn.commit();
                return null;

            } catch (SQLException | RuntimeException exception) {
                rollback(conn);
                exception.printStackTrace();

                return "Payment could not be completed. "
                        + "Refresh your payment history before trying again.";
            }

        } catch (SQLException exception) {
            exception.printStackTrace();

            return "Database operation failed. "
                    + "Refresh your payment history before trying again.";
        }
    }

    /*
     * Row indices:
     * 0 rental ID
     * 1 equipment summary
     * 2 rental date
     * 3 due date
     * 4 net rental total
     * 5 amount paid
     * 6 balance
     * 7 payment status
     * 8 rental status
     * 9 points redeemed
     * 10 voucher daily rate
     */
    public List<String[]> getCustomerRentalsWithBalance(int customerId) {
        List<String[]> rows = new ArrayList<>();

        String sql = """
                SELECT r.rental_id,
                       COALESCE(items.equip_summary, 'Sports Equipment')
                           AS equip_summary,
                       r.rental_date,
                       r.due_date,
                       r.total_amount,
                       COALESCE(pay.paid_amount, 0.00) AS paid_amount,
                       r.status,
                       COALESCE(r.points_redeemed, 0) AS points_redeemed,
                       COALESCE(items.voucher_daily_rate, 0.00)
                           AS voucher_daily_rate,
                       COALESCE(items.unreturned_count, 0)
                           AS unreturned_count
                FROM Rentals r
                LEFT JOIN (
                    SELECT rd.rental_id,
                           MIN(em.rental_price_per_day) AS voucher_daily_rate,
                           GROUP_CONCAT(
                               CONCAT(em.name, ' (', ti.serial_number, ')')
                               SEPARATOR ', '
                           ) AS equip_summary,
                           SUM(
                               CASE WHEN rd.return_date IS NULL
                               THEN 1 ELSE 0 END
                           ) AS unreturned_count
                    FROM Rental_Details rd
                    JOIN Tracked_Items ti ON rd.item_id = ti.item_id
                    JOIN Equipment_Models em ON ti.model_id = em.model_id
                    GROUP BY rd.rental_id
                ) items ON r.rental_id = items.rental_id
                LEFT JOIN (
                    SELECT rental_id, SUM(amount_paid) AS paid_amount
                    FROM Payments
                    GROUP BY rental_id
                ) pay ON r.rental_id = pay.rental_id
                WHERE r.customer_id = ?
                ORDER BY r.rental_id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal total = money(
                            rs.getBigDecimal("total_amount")
                    );

                    BigDecimal paid = money(
                            rs.getBigDecimal("paid_amount")
                    );

                    BigDecimal balance = total.subtract(paid)
                            .max(BigDecimal.ZERO)
                            .setScale(2);

                    String paymentStatus;

                    if (paid.compareTo(total) >= 0) {
                        paymentStatus = "Paid";
                    } else if (paid.signum() > 0) {
                        paymentStatus = "Partially Paid";
                    } else {
                        paymentStatus = "Unpaid";
                    }

                    Timestamp due = rs.getTimestamp("due_date");
                    String rentalStatus = rs.getString("status");

                    if (rs.getInt("unreturned_count") == 0) {
                        rentalStatus = "Returned";

                    } else if (due != null
                            && LocalDateTime.now().isAfter(
                            due.toLocalDateTime())) {
                        rentalStatus = "Overdue";
                    }

                    rows.add(new String[]{
                            String.valueOf(rs.getInt("rental_id")),
                            rs.getString("equip_summary"),
                            String.valueOf(rs.getTimestamp("rental_date")),
                            String.valueOf(due),
                            total.toPlainString(),
                            paid.toPlainString(),
                            balance.toPlainString(),
                            paymentStatus,
                            rentalStatus,
                            String.valueOf(rs.getInt("points_redeemed")),
                            money(
                                    rs.getBigDecimal("voucher_daily_rate")
                            ).toPlainString()
                    });
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return rows;
    }

    public List<String[]> getCustomerActiveBorrowedItems(int customerId) {
        List<String[]> rows = new ArrayList<>();

        String sql = """
                SELECT r.rental_id,
                       em.name AS equipment_name,
                       em.category,
                       ti.serial_number,
                       r.rental_date,
                       r.due_date,
                       r.total_amount,
                       COALESCE(pay.paid_amount, 0.00) AS paid_amount
                FROM Rental_Details rd
                JOIN Rentals r ON rd.rental_id = r.rental_id
                JOIN Tracked_Items ti ON rd.item_id = ti.item_id
                JOIN Equipment_Models em ON ti.model_id = em.model_id
                LEFT JOIN (
                    SELECT rental_id, SUM(amount_paid) AS paid_amount
                    FROM Payments
                    GROUP BY rental_id
                ) pay ON r.rental_id = pay.rental_id
                WHERE r.customer_id = ?
                  AND rd.return_date IS NULL
                ORDER BY r.due_date ASC, r.rental_id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp due = rs.getTimestamp("due_date");

                    String rentalStatus = due != null
                            && LocalDateTime.now().isAfter(
                            due.toLocalDateTime())
                            ? "Overdue"
                            : "Active (Borrowed)";

                    BigDecimal total = money(
                            rs.getBigDecimal("total_amount")
                    );

                    BigDecimal paid = money(
                            rs.getBigDecimal("paid_amount")
                    );

                    BigDecimal balance = total.subtract(paid)
                            .max(BigDecimal.ZERO)
                            .setScale(2);

                    String paymentStatus;

                    if (paid.compareTo(total) >= 0) {
                        paymentStatus = "Paid";

                    } else if (paid.signum() > 0) {
                        paymentStatus = "Partially Paid (₱"
                                + balance.toPlainString() + " due)";

                    } else {
                        paymentStatus = "Unpaid (₱"
                                + balance.toPlainString() + " due)";
                    }

                    rows.add(new String[]{
                            String.valueOf(rs.getInt("rental_id")),
                            rs.getString("equipment_name"),
                            rs.getString("category"),
                            rs.getString("serial_number"),
                            String.valueOf(rs.getTimestamp("rental_date")),
                            String.valueOf(due),
                            rentalStatus,
                            paymentStatus
                    });
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return rows;
    }

    public List<String[]> getCustomerPaymentHistory(int customerId) {
        List<String[]> rows = new ArrayList<>();

        String sql = """
                SELECT p.payment_id,
                       p.rental_id,
                       p.amount_paid,
                       p.payment_method,
                       p.payment_date
                FROM Payments p
                JOIN Rentals r ON p.rental_id = r.rental_id
                WHERE r.customer_id = ?
                ORDER BY p.payment_id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            String.valueOf(rs.getInt("payment_id")),
                            String.valueOf(rs.getInt("rental_id")),
                            "₱" + money(rs.getBigDecimal("amount_paid")),
                            rs.getString("payment_method"),
                            String.valueOf(rs.getTimestamp("payment_date"))
                    });
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return rows;
    }

    public List<String[]> getAllPaymentsReport() {
        AccessControl.requireAdmin();

        List<String[]> rows = new ArrayList<>();

        String sql = """
                SELECT p.payment_id,
                       p.payment_date,
                       CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
                       p.rental_id,
                       p.amount_paid,
                       p.payment_method,
                       r.total_amount,
                       GREATEST(
                           0.00,
                           r.total_amount - COALESCE(tot.total_paid, 0.00)
                       ) AS remaining_balance
                FROM Payments p
                JOIN Rentals r ON p.rental_id = r.rental_id
                JOIN Customers c ON r.customer_id = c.customer_id
                LEFT JOIN (
                    SELECT rental_id, SUM(amount_paid) AS total_paid
                    FROM Payments
                    GROUP BY rental_id
                ) tot ON r.rental_id = tot.rental_id
                ORDER BY p.payment_id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rows.add(new String[]{
                        String.valueOf(rs.getInt("payment_id")),
                        String.valueOf(rs.getTimestamp("payment_date")),
                        rs.getString("customer_name"),
                        String.valueOf(rs.getInt("rental_id")),
                        "₱" + money(rs.getBigDecimal("amount_paid")),
                        rs.getString("payment_method"),
                        "₱" + money(rs.getBigDecimal("total_amount")),
                        "₱" + money(rs.getBigDecimal("remaining_balance"))
                });
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return rows;
    }

    public boolean returnEquipment(int customerId, String serialNumber) {
        return returnEquipmentWithCondition(
                customerId,
                serialNumber,
                false
        );
    }

    public boolean returnEquipmentWithCondition(
            int customerId,
            String serialNumber,
            boolean isDamaged
    ) {
        if (customerId <= 0
                || serialNumber == null
                || serialNumber.isBlank()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int rentalId;
                int detailId;
                int itemId;

                String findSql = """
                        SELECT rd.rental_id,
                               rd.rental_detail_id,
                               ti.item_id
                        FROM Rental_Details rd
                        JOIN Rentals r ON rd.rental_id = r.rental_id
                        JOIN Tracked_Items ti ON rd.item_id = ti.item_id
                        WHERE r.customer_id = ?
                          AND ti.serial_number = ?
                          AND rd.return_date IS NULL
                        LIMIT 1
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(findSql)) {
                    stmt.setInt(1, customerId);
                    stmt.setString(2, serialNumber.trim());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }

                        rentalId = rs.getInt("rental_id");
                        detailId = rs.getInt("rental_detail_id");
                        itemId = rs.getInt("item_id");
                    }
                }

                Timestamp dueDate;

                String lockRentalSql = """
                        SELECT due_date
                        FROM Rentals
                        WHERE rental_id = ?
                          AND customer_id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(lockRentalSql)) {
                    stmt.setInt(1, rentalId);
                    stmt.setInt(2, customerId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }

                        dueDate = rs.getTimestamp("due_date");
                    }
                }

                // Use the same lock order as payment processing.
                try (PreparedStatement stmt = conn.prepareStatement("""
                        SELECT customer_id
                        FROM Customers
                        WHERE customer_id = ?
                        FOR UPDATE
                        """)) {
                    stmt.setInt(1, customerId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                LocalDateTime returnedAt = LocalDateTime.now();

                // Only one return can update this row and award points.
                String returnSql = """
                        UPDATE Rental_Details
                        SET return_date = ?
                        WHERE rental_detail_id = ?
                          AND rental_id = ?
                          AND return_date IS NULL
                        """;

                try (PreparedStatement stmt =
                             conn.prepareStatement(returnSql)) {
                    stmt.setTimestamp(1, Timestamp.valueOf(returnedAt));
                    stmt.setInt(2, detailId);
                    stmt.setInt(3, rentalId);

                    if (stmt.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                String itemStatus = "Available";

                if (isDamaged) {
                    itemStatus = findAllowedStatus(
                            getEnumValues(conn, "tracked_items", "status"),
                            "Damaged",
                            "Maintenance",
                            "Under Maintenance",
                            "Unavailable",
                            "Defective"
                    );

                    if (itemStatus == null) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement("""
                        UPDATE Tracked_Items
                        SET status = ?
                        WHERE item_id = ?
                        """)) {
                    stmt.setString(1, itemStatus);
                    stmt.setInt(2, itemId);
                    stmt.executeUpdate();
                }

                boolean onTime = dueDate != null
                        && !returnedAt.isAfter(dueDate.toLocalDateTime());

                if (!isDamaged && onTime) {
                    try (PreparedStatement stmt = conn.prepareStatement("""
                            UPDATE Customers
                            SET reward_points =
                                COALESCE(reward_points, 0) + 10
                            WHERE customer_id = ?
                            """)) {
                        stmt.setInt(1, customerId);
                        stmt.executeUpdate();
                    }
                }

                // Lock/read current detail rows, including concurrent returns.
                boolean hasUnreturned = false;

                try (PreparedStatement stmt = conn.prepareStatement("""
                        SELECT rental_detail_id, return_date
                        FROM Rental_Details
                        WHERE rental_id = ?
                        FOR UPDATE
                        """)) {
                    stmt.setInt(1, rentalId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            if (rs.getTimestamp("return_date") == null) {
                                hasUnreturned = true;
                            }
                        }
                    }
                }

                if (!hasUnreturned) {
                    String finishedStatus = findAllowedStatus(
                            getEnumValues(conn, "rentals", "status"),
                            "Returned",
                            "Completed",
                            "Closed"
                    );

                    if (finishedStatus != null) {
                        try (PreparedStatement stmt = conn.prepareStatement("""
                                UPDATE Rentals
                                SET status = ?
                                WHERE rental_id = ?
                                """)) {
                            stmt.setString(1, finishedStatus);
                            stmt.setInt(2, rentalId);
                            stmt.executeUpdate();
                        }
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException | RuntimeException exception) {
                rollback(conn);
                exception.printStackTrace();
                return false;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public List<String[]> getTransactionHistoryReport() {
        AccessControl.requireAdmin();

        List<String[]> rows = new ArrayList<>();

        String sql = """
                SELECT r.rental_id,
                       CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
                       em.name AS equipment_name,
                       ti.serial_number,
                       r.rental_date,
                       r.due_date,
                       IFNULL(rd.return_date, 'Active / Not Returned')
                           AS return_status
                FROM Rentals r
                JOIN Customers c ON r.customer_id = c.customer_id
                JOIN Rental_Details rd ON r.rental_id = rd.rental_id
                JOIN Tracked_Items ti ON rd.item_id = ti.item_id
                JOIN Equipment_Models em ON ti.model_id = em.model_id
                ORDER BY r.rental_id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rows.add(new String[]{
                        String.valueOf(rs.getInt("rental_id")),
                        rs.getString("customer_name"),
                        rs.getString("equipment_name"),
                        rs.getString("serial_number"),
                        String.valueOf(rs.getTimestamp("rental_date")),
                        String.valueOf(rs.getTimestamp("due_date")),
                        rs.getString("return_status")
                });
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return rows;
    }
}