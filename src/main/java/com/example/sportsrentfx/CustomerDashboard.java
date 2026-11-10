package com.example.sportsrentfx;

import com.example.sportsrentfx.dao.CustomerDAO;
import com.example.sportsrentfx.dao.EquipmentDAO;
import com.example.sportsrentfx.dao.RentalDAO;
import com.example.sportsrentfx.model.Customer;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public class CustomerDashboard {

    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private static final RentalDAO rentalDAO = new RentalDAO();

    private static final String CARD_STYLE =
            "-fx-background-color: #ffffff;"
                    + "-fx-background-radius: 12px;"
                    + "-fx-border-color: #e2e8f0;"
                    + "-fx-border-radius: 12px;"
                    + "-fx-effect: dropshadow(gaussian,"
                    + "rgba(15,23,42,0.04), 12, 0, 0, 4);";

    private static final String PANEL_STYLE =
            "-fx-background-color: #f8fafc;"
                    + "-fx-background-radius: 10px;"
                    + "-fx-border-color: #e2e8f0;"
                    + "-fx-border-radius: 10px;";

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #0f172a;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
        );
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-text-fill: #0f172a;"
                        + "-fx-border-color: #cbd5e1;"
                        + "-fx-border-radius: 8px;"
        );
        return button;
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 17px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0f172a;"
        );
        return label;
    }

    private static Label description(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #475569;"
        );
        return label;
    }

    private static void addColumns(
            TableView<String[]> table,
            String[] headers,
            int[] indices,
            double[] widths,
            int firstMoneyColumn,
            int lastMoneyColumn
    ) {
        for (int i = 0; i < headers.length; i++) {
            final int index = indices[i];
            final boolean moneyColumn =
                    i >= firstMoneyColumn && i <= lastMoneyColumn;

            TableColumn<String[], String> column =
                    new TableColumn<>(headers[i]);

            column.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            moneyColumn
                                    ? "₱" + data.getValue()[index]
                                    : data.getValue()[index]
                    )
            );

            column.setPrefWidth(widths[i]);
            table.getColumns().add(column);
        }
    }

    public static Scene getScene(Stage stage) {
        HelloApplication.showCustomerAuthScreen(stage);
        return stage.getScene();
    }

    public static Scene getScene(Stage stage, Customer customer) {
        if (customer == null) {
            return getScene(stage);
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f1f5f9;");

        HBox topBar = new HBox(14);
        topBar.setPadding(new Insets(14, 24, 14, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #0f172a;");

        Label brand = new Label("SPORTRENT");
        brand.setStyle(
                "-fx-background-color: #334155;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 4px 10px;"
                        + "-fx-background-radius: 6px;"
        );

        Label header = new Label("Customer Workspace");
        header.setStyle(
                "-fx-text-fill: #f8fafc;"
                        + "-fx-font-size: 16px;"
                        + "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userBadge = new Label(
                customer.getFullName()
                        + "  •  ID #" + customer.getCustomerId()
        );
        userBadge.setStyle(
                "-fx-background-color: #1e293b;"
                        + "-fx-text-fill: #f8fafc;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 6px 14px;"
                        + "-fx-background-radius: 20px;"
                        + "-fx-border-color: #334155;"
                        + "-fx-border-radius: 20px;"
        );

        Button signOut = secondaryButton("Sign Out");
        signOut.setOnAction(
                event -> HelloApplication.showMainScreen(stage)
        );

        topBar.getChildren().addAll(
                brand, header, spacer, userBadge, signOut
        );

        Label activeValue = new Label("0 Items Out");
        Label balanceValue = new Label("₱0.00");
        Label pointsValue = new Label(
                customer.getRewardPoints() + " pts"
        );

        VBox activeCard = createKpiCard(
                "ACTIVE BORROWED GEAR", activeValue
        );
        VBox balanceCard = createKpiCard(
                "OUTSTANDING BALANCE", balanceValue
        );
        VBox pointsCard = createKpiCard(
                "MY REWARD POINTS", pointsValue
        );

        Label pointsTitle =
                (Label) pointsCard.getChildren().remove(0);

        Button pointsInfo = new Button("ⓘ");
        pointsInfo.setAccessibleText("Reward points information");
        pointsInfo.setTooltip(new Tooltip("About reward points"));
        pointsInfo.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-border-color: transparent;"
                        + "-fx-text-fill: #64748b;"
                        + "-fx-font-size: 15px;"
                        + "-fx-padding: 0 4px;"
                        + "-fx-cursor: hand;"
        );

        pointsInfo.setOnAction(event -> {
            Alert notice = new Alert(Alert.AlertType.INFORMATION);
            notice.initOwner(stage);
            notice.setTitle("Reward Points");
            notice.setHeaderText(null);
            notice.setContentText(
                    "Earn 10 points on every on time return\n\n"
                            + "Returned equipment must be undamaged.\n"
                            + "10 points = ₱1 discount.\n\n"
                            + "Vouchers depend on the equipment's daily rate:\n"
                            + "Below ₱100: 5% off, maximum ₱5\n"
                            + "₱100–₱499.99: 10% off, maximum ₱30\n"
                            + "₱500 and above: 15% off, maximum ₱100\n\n"
                            + "Discounts also depend on your available points.\n"
                            + "One redemption per rental.\n"
                            + "Full settlement is required when using a voucher."
            );
            notice.showAndWait();
        });

        HBox pointsHeader = new HBox(6, pointsTitle, pointsInfo);
        pointsHeader.setAlignment(Pos.CENTER_LEFT);
        pointsCard.getChildren().add(0, pointsHeader);

        HBox kpiStrip = new HBox(
                16, activeCard, balanceCard, pointsCard
        );
        kpiStrip.setPadding(new Insets(14, 24, 10, 24));
        kpiStrip.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(activeCard, Priority.ALWAYS);
        HBox.setHgrow(balanceCard, Priority.ALWAYS);
        HBox.setHgrow(pointsCard, Priority.ALWAYS);

        root.setTop(new VBox(0, topBar, kpiStrip));

        TabPane tabs = new TabPane();

        Runnable[] catalogRefresh = new Runnable[1];
        Runnable[] borrowRefresh = new Runnable[1];
        Runnable[] paymentRefresh = new Runnable[1];
        Runnable[] returnRefresh = new Runnable[1];
        Runnable[] profileRefresh = new Runnable[1];

        Runnable refreshKpis = () -> {
            List<String[]> active =
                    rentalDAO.getCustomerActiveBorrowedItems(
                            customer.getCustomerId()
                    );

            activeValue.setText(
                    active.size()
                            + (active.size() == 1
                            ? " Item Out"
                            : " Items Out")
            );

            BigDecimal unpaid = BigDecimal.ZERO;

            for (String[] rental :
                    rentalDAO.getCustomerRentalsWithBalance(
                            customer.getCustomerId()
                    )) {
                try {
                    unpaid = unpaid.add(new BigDecimal(rental[6]));
                } catch (NumberFormatException ignored) {
                }
            }

            balanceValue.setText(
                    "₱" + unpaid.setScale(2, RoundingMode.HALF_UP)
            );

            Customer fresh = customerDAO.getCustomerById(
                    customer.getCustomerId()
            );

            if (fresh != null) {
                customer.setRewardPoints(fresh.getRewardPoints());
            }

            pointsValue.setText(customer.getRewardPoints() + " pts");
        };

        Runnable refreshAll = () -> {
            if (catalogRefresh[0] != null) catalogRefresh[0].run();
            if (borrowRefresh[0] != null) borrowRefresh[0].run();
            if (paymentRefresh[0] != null) paymentRefresh[0].run();
            if (returnRefresh[0] != null) returnRefresh[0].run();
            if (profileRefresh[0] != null) profileRefresh[0].run();
            refreshKpis.run();
        };

        Tab catalogTab = new Tab(
                "Browse Catalog",
                createEquipmentCatalogView(catalogRefresh)
        );

        Tab paymentTab = new Tab(
                "Pay Rental Fee & My Rentals",
                createPayRentalFeeView(
                        customer, refreshAll, paymentRefresh
                )
        );

        Tab returnTab = new Tab(
                "Return Equipment",
                createCustomerReturnView(
                        customer, refreshAll, returnRefresh
                )
        );

        Tab borrowTab = new Tab(
                "Borrow Gear",
                createBorrowView(
                        customer, tabs, paymentTab,
                        refreshAll, borrowRefresh
                )
        );

        Tab profileTab = new Tab(
                "My Profile & Points",
                createProfileAndPointsView(
                        customer, userBadge,
                        refreshAll, profileRefresh
                )
        );

        for (Tab tab : List.of(
                catalogTab, borrowTab, paymentTab,
                returnTab, profileTab
        )) {
            tab.setClosable(false);
        }

        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldTab, newTab) -> {
                    refreshKpis.run();

                    if (newTab == catalogTab
                            && catalogRefresh[0] != null) {
                        catalogRefresh[0].run();

                    } else if (newTab == borrowTab
                            && borrowRefresh[0] != null) {
                        borrowRefresh[0].run();

                    } else if (newTab == paymentTab
                            && paymentRefresh[0] != null) {
                        paymentRefresh[0].run();

                    } else if (newTab == returnTab
                            && returnRefresh[0] != null) {
                        returnRefresh[0].run();

                    } else if (newTab == profileTab
                            && profileRefresh[0] != null) {
                        profileRefresh[0].run();
                    }
                });

        tabs.getTabs().addAll(
                catalogTab, borrowTab, paymentTab,
                returnTab, profileTab
        );
        tabs.setStyle(CARD_STYLE);

        StackPane body = new StackPane(tabs);
        body.setPadding(new Insets(0, 24, 20, 24));
        root.setCenter(body);

        refreshKpis.run();

        Scene scene = new Scene(root, 980, 680);
        HelloApplication.applyModernTheme(scene);
        return scene;
    }

    private static VBox createKpiCard(String text, Label value) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(CARD_STYLE);

        Label title = new Label(text);
        title.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #475569;"
        );

        value.setStyle(
                "-fx-font-size: 20px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0f172a;"
        );

        card.getChildren().addAll(title, value);
        return card;
    }

    private static VBox createEquipmentCatalogView(
            Runnable[] refreshRef
    ) {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20, 24, 20, 24));

        VBox titleBox = new VBox(
                2,
                heading("Sports Equipment Catalog"),
                description(
                        "Live inventory availability and daily rental rates"
                )
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = secondaryButton("↻  Refresh Catalog");

        HBox header = new HBox(12, titleBox, spacer, refresh);
        header.setAlignment(Pos.CENTER_LEFT);

        TableView<String[]> table = new TableView<>();
        table.setPlaceholder(new Label("No sports equipment found."));
        VBox.setVgrow(table, Priority.ALWAYS);

        addColumns(
                table,
                new String[]{
                        "Equipment Name", "Category", "Daily Rate (₱)",
                        "Total Stock", "Available Now"
                },
                new int[]{0, 1, 2, 3, 6},
                new double[]{260, 160, 160, 140, 150},
                2, 2
        );

        Runnable load = () -> table.getItems().setAll(
                equipmentDAO.getEquipmentCatalogWithPrices()
        );

        refreshRef[0] = load;
        refresh.setOnAction(event -> load.run());
        load.run();

        layout.getChildren().addAll(header, table);
        return layout;
    }

    private static VBox createBorrowView(
            Customer customer,
            TabPane tabs,
            Tab paymentTab,
            Runnable onSuccess,
            Runnable[] refreshRef
    ) {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(24, 28, 24, 28));

        VBox titleBox = new VBox(
                3,
                heading("Borrow Sports Equipment"),
                description(
                        "Select available gear and duration to preview "
                                + "your rental charge before confirming."
                )
        );

        ComboBox<String> equipment = new ComboBox<>();
        equipment.setMaxWidth(Double.MAX_VALUE);
        equipment.setPrefHeight(38);

        TextField quantityField = new TextField("1");
        quantityField.setPrefHeight(38);

        TextField daysField = new TextField("1");
        daysField.setPrefHeight(38);

        VBox quantityBox = new VBox(
                5, new Label("Quantity to Borrow"), quantityField
        );
        VBox daysBox = new VBox(
                5, new Label("Duration (Days)"), daysField
        );

        HBox.setHgrow(quantityBox, Priority.ALWAYS);
        HBox.setHgrow(daysBox, Priority.ALWAYS);

        HBox numbers = new HBox(14, quantityBox, daysBox);

        VBox form = new VBox(
                14,
                heading("1. Rental Selection"),
                new VBox(
                        5,
                        new Label("Select Available Equipment"),
                        equipment
                ),
                numbers
        );
        form.setPadding(new Insets(20));
        form.setPrefWidth(440);
        form.setStyle(PANEL_STYLE);

        Label rateLabel = description(
                "Daily Rate: ₱0.00 • Available Now: 0"
        );
        Label formulaLabel = description(
                "1 unit × ₱0.00/day × 1 day"
        );
        Label totalLabel = heading("₱0.00");
        totalLabel.setStyle(
                "-fx-font-size: 26px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0f172a;"
        );

        Button borrow = primaryButton("Confirm & Borrow Equipment");
        borrow.setMaxWidth(Double.MAX_VALUE);
        borrow.setPrefHeight(42);

        Button pay = secondaryButton("Proceed to Pay Rental Fee  →");
        pay.setMaxWidth(Double.MAX_VALUE);
        pay.setOnAction(
                event -> tabs.getSelectionModel().select(paymentTab)
        );

        VBox summary = new VBox(
                14,
                heading("2. Live Billing Summary"),
                rateLabel,
                formulaLabel,
                totalLabel,
                borrow,
                pay
        );
        summary.setPadding(new Insets(20));
        summary.setPrefWidth(420);
        summary.setStyle(PANEL_STYLE);
        HBox.setHgrow(summary, Priority.ALWAYS);

        HBox content = new HBox(24, form, summary);
        content.setAlignment(Pos.TOP_LEFT);

        Label status = HelloApplication.createNoticeLabel();

        Runnable preview = () -> {
            String name = equipment.getValue();

            if (name == null || name.isBlank()) {
                rateLabel.setText("No available equipment selected");
                formulaLabel.setText("—");
                totalLabel.setText("₱0.00");
                return;
            }

            double rate = rentalDAO.getEquipmentDailyPrice(name);
            int stock = rentalDAO.getAvailableCountForEquipment(name);

            rateLabel.setText(String.format(
                    "Daily Rate: ₱%.2f / day • Available Now: %d",
                    rate,
                    stock
            ));

            try {
                int quantity = Integer.parseInt(
                        quantityField.getText().trim()
                );
                int days = Integer.parseInt(
                        daysField.getText().trim()
                );

                if (quantity <= 0 || days <= 0) {
                    formulaLabel.setText(
                            "Enter positive quantity and days"
                    );
                    totalLabel.setText("₱0.00");
                    return;
                }

                formulaLabel.setText(String.format(
                        "%d unit(s) × ₱%.2f/day × %d day(s)",
                        quantity,
                        rate,
                        days
                ));
                totalLabel.setText(String.format(
                        "₱%.2f",
                        rate * quantity * days
                ));

            } catch (NumberFormatException exception) {
                formulaLabel.setText("Enter valid whole numbers");
                totalLabel.setText("₱0.00");
            }
        };

        Runnable load = () -> {
            String previous = equipment.getValue();

            equipment.getItems().setAll(
                    rentalDAO.getAvailableEquipmentNames()
            );

            if (previous != null
                    && equipment.getItems().contains(previous)) {
                equipment.setValue(previous);

            } else if (!equipment.getItems().isEmpty()) {
                equipment.getSelectionModel().selectFirst();

            } else {
                equipment.setValue(null);
            }

            preview.run();
        };

        refreshRef[0] = load;

        equipment.setOnAction(event -> preview.run());
        quantityField.textProperty().addListener(
                (observable, oldValue, newValue) -> preview.run()
        );
        daysField.textProperty().addListener(
                (observable, oldValue, newValue) -> preview.run()
        );

        borrow.setOnAction(event -> {
            try {
                String name = equipment.getValue();

                if (name == null || name.isBlank()) {
                    HelloApplication.showNotice(
                            status,
                            "Select available equipment."
                    );
                    return;
                }

                int quantity = Integer.parseInt(
                        quantityField.getText().trim()
                );
                int days = Integer.parseInt(
                        daysField.getText().trim()
                );

                if (quantity <= 0 || days <= 0) {
                    HelloApplication.showNotice(
                            status,
                            "Quantity and days must be at least 1."
                    );
                    return;
                }

                int stock = rentalDAO.getAvailableCountForEquipment(name);

                if (quantity > stock) {
                    HelloApplication.showNotice(
                            status,
                            "Only " + stock + " unit(s) are available."
                    );
                    return;
                }

                double total =
                        rentalDAO.getEquipmentDailyPrice(name)
                                * quantity * days;

                Alert confirm = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        String.format(
                                "Borrow %d × %s for %d day(s)?"
                                        + "\n\nTotal Rental Charge: ₱%.2f",
                                quantity, name, days, total
                        ),
                        ButtonType.YES,
                        ButtonType.NO
                );
                confirm.setTitle("Confirm Rental Checkout");
                confirm.setHeaderText("Review Rental Billing Summary");

                if (confirm.showAndWait().orElse(ButtonType.NO)
                        != ButtonType.YES) {
                    return;
                }

                int rentalId = rentalDAO.borrowEquipmentQuantity(
                        customer.getCustomerId(),
                        name,
                        quantity,
                        days
                );

                if (rentalId != -1) {
                    quantityField.setText("1");
                    daysField.setText("1");
                    onSuccess.run();

                    HelloApplication.showNotice(
                            status,
                            String.format(
                                    "Rental #%d confirmed! Total Fee: ₱%.2f. "
                                            + "Open 'Pay Rental Fee & My Rentals' to pay.",
                                    rentalId,
                                    total
                            )
                    );
                } else {
                    HelloApplication.showNotice(
                            status,
                            "Rental failed. Refresh availability and try again."
                    );
                }
            } catch (NumberFormatException exception) {
                HelloApplication.showNotice(
                        status,
                        "Enter valid whole numbers for Quantity and Days."
                );
            }
        });

        load.run();
        layout.getChildren().addAll(titleBox, content, status);
        return layout;
    }

    private static VBox createPayRentalFeeView(
            Customer customer,
            Runnable onSuccess,
            Runnable[] refreshRef
    ) {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(18, 24, 18, 24));

        VBox titleBox = new VBox(
                2,
                heading("My Rentals & Pay Rental Fee"),
                description(
                        "Select a rental to pay its balance or make a partial payment."
                )
        );

        TableView<String[]> rentals = new TableView<>();
        rentals.setPlaceholder(
                new Label("You have no rental transactions yet.")
        );
        rentals.setPrefHeight(170);

        addColumns(
                rentals,
                new String[]{
                        "Rental #",
                        "Equipment (Serial #)",
                        "Date Rented",
                        "Due Date",
                        "Net Fee",
                        "Paid",
                        "Balance",
                        "Payment Status",
                        "Rental Status",
                        "Points Used"
                },
                new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                new double[]{
                        70, 185, 130, 130, 88,
                        85, 88, 110, 100, 95
                },
                4, 6
        );

        TextField selectedRental = new TextField();
        selectedRental.setEditable(false);
        selectedRental.setPrefWidth(80);

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.setPrefWidth(115);

        ComboBox<String> method = new ComboBox<>();
        method.getItems().addAll(
                rentalDAO.getAllowedPaymentMethods()
        );
        method.getSelectionModel().selectFirst();
        method.setPrefWidth(135);

        Button pay = primaryButton("Pay Rental Fee");
        pay.setDisable(true);

        Button refresh = secondaryButton("↻  Refresh");

        FlowPane fields = new FlowPane();
        fields.setHgap(12);
        fields.setVgap(10);
        fields.setAlignment(Pos.CENTER_LEFT);

        fields.getChildren().addAll(
                new Label("Rental #:"),
                selectedRental,
                new Label("Amount (₱):"),
                amountField,
                new Label("Method:"),
                method,
                pay,
                refresh
        );

        CheckBox usePoints = new CheckBox(
                "Apply equipment voucher using points"
        );
        usePoints.setDisable(true);
        usePoints.setWrapText(true);

        Label rules = description(
                "10 points = ₱1 • Voucher depends on the equipment's daily rate "
                        + "• Full settlement required"
        );

        Label voucherDetails = description(
                "Select a rental to view its voucher."
        );

        Label previewLabel = description("");
        previewLabel.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0f172a;"
        );

        VBox voucherBox = new VBox(
                8,
                usePoints,
                voucherDetails,
                rules,
                previewLabel
        );
        voucherBox.setPadding(new Insets(12));
        voucherBox.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-border-color: #cbd5e1;"
                        + "-fx-border-radius: 8px;"
        );

        VBox paymentCard = new VBox(12, fields, voucherBox);
        paymentCard.setPadding(new Insets(14, 16, 14, 16));
        paymentCard.setStyle(PANEL_STYLE);

        Label status = HelloApplication.createNoticeLabel();

        TableView<String[]> receipts = new TableView<>();
        receipts.setPlaceholder(
                new Label("No payments recorded yet.")
        );
        receipts.setPrefHeight(125);

        addColumns(
                receipts,
                new String[]{
                        "Receipt #",
                        "Rental #",
                        "Amount Paid",
                        "Payment Method",
                        "Payment Timestamp"
                },
                new int[]{0, 1, 2, 3, 4},
                new double[]{95, 95, 140, 160, 220},
                -1, -1
        );

        int[] availablePoints = {customer.getRewardPoints()};
        int[] pointsToUse = {0};

        Runnable preview = () -> {
            String[] selected =
                    rentals.getSelectionModel().getSelectedItem();

            pointsToUse[0] = 0;

            if (selected == null) {
                selectedRental.clear();
                amountField.clear();
                amountField.setEditable(true);
                usePoints.setDisable(true);
                usePoints.setText(
                        "Apply equipment voucher using points"
                );
                voucherDetails.setText(
                        "Select a rental to view its voucher."
                );
                pay.setDisable(true);
                previewLabel.setText(
                        "Available points: " + availablePoints[0]
                );
                return;
            }

            selectedRental.setText(selected[0]);

            try {
                BigDecimal balance = new BigDecimal(selected[6])
                        .setScale(2, RoundingMode.HALF_UP);

                int previouslyUsed = Integer.parseInt(selected[9]);
                BigDecimal dailyRate = new BigDecimal(selected[10]);

                String voucherDescription =
                        RentalDAO.getVoucherDescription(dailyRate);

                usePoints.setText(
                        "Apply equipment voucher: " + voucherDescription
                );

                voucherDetails.setText(
                        "Equipment daily rate: ₱"
                                + dailyRate.setScale(2, RoundingMode.HALF_UP)
                                + " • Available points: "
                                + availablePoints[0]
                );

                if (balance.signum() <= 0) {
                    amountField.setText("0.00");
                    amountField.setEditable(false);
                    usePoints.setDisable(true);
                    pay.setDisable(true);

                    if (previouslyUsed > 0) {
                        BigDecimal usedDiscount =
                                BigDecimal.valueOf(previouslyUsed)
                                        .divide(BigDecimal.TEN)
                                        .setScale(2);

                        previewLabel.setText(
                                "Fully paid • Used " + previouslyUsed
                                        + " points • Saved ₱" + usedDiscount
                        );
                    } else {
                        previewLabel.setText("This rental is fully paid.");
                    }

                    return;
                }

                int eligible = previouslyUsed > 0
                        ? 0
                        : RentalDAO.calculateRedeemablePoints(
                        availablePoints[0],
                        balance,
                        dailyRate
                );

                usePoints.setDisable(eligible == 0);
                pay.setDisable(false);

                if (usePoints.isSelected() && eligible > 0) {
                    pointsToUse[0] = eligible;

                    BigDecimal discount = BigDecimal.valueOf(eligible)
                            .divide(BigDecimal.TEN)
                            .setScale(2);

                    BigDecimal due = balance.subtract(discount);

                    amountField.setText(due.toPlainString());
                    amountField.setEditable(false);

                    previewLabel.setText(
                            "Use " + eligible + " points"
                                    + " • Discount: ₱" + discount
                                    + " • Pay: ₱" + due
                                    + " • Points left: "
                                    + (availablePoints[0] - eligible)
                    );

                } else {
                    amountField.setText(balance.toPlainString());
                    amountField.setEditable(true);

                    if (previouslyUsed > 0) {
                        previewLabel.setText(
                                "A voucher has already been used on this rental."
                        );

                    } else if (eligible > 0) {
                        BigDecimal discount =
                                BigDecimal.valueOf(eligible)
                                        .divide(BigDecimal.TEN)
                                        .setScale(2);

                        previewLabel.setText(
                                "Check the box to use " + eligible
                                        + " points and save ₱" + discount + "."
                        );

                    } else if (dailyRate.signum() <= 0) {
                        previewLabel.setText(
                                "No equipment voucher is available for this rental."
                        );

                    } else if (availablePoints[0] < 10) {
                        previewLabel.setText(
                                "At least 10 reward points are needed."
                        );

                    } else {
                        previewLabel.setText(
                                "This voucher currently gives less than ₱1 off."
                        );
                    }
                }

            } catch (NumberFormatException exception) {
                amountField.clear();
                usePoints.setDisable(true);
                pay.setDisable(true);
                previewLabel.setText(
                        "Unable to read the balance. Please refresh."
                );
            }
        };

        usePoints.setOnAction(event -> preview.run());

        rentals.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldSelection, selection) -> {
                    usePoints.setSelected(false);
                    preview.run();
                });

        Runnable load = () -> {
            usePoints.setSelected(false);

            Customer fresh = customerDAO.getCustomerById(
                    customer.getCustomerId()
            );

            availablePoints[0] = fresh == null
                    ? 0
                    : Math.max(0, fresh.getRewardPoints());

            if (fresh != null) {
                customer.setRewardPoints(fresh.getRewardPoints());
            }

            rentals.getItems().setAll(
                    rentalDAO.getCustomerRentalsWithBalance(
                            customer.getCustomerId()
                    )
            );

            receipts.getItems().setAll(
                    rentalDAO.getCustomerPaymentHistory(
                            customer.getCustomerId()
                    )
            );

            if (!rentals.getItems().isEmpty()) {
                rentals.getSelectionModel().selectFirst();
            }

            preview.run();
        };

        refreshRef[0] = load;
        refresh.setOnAction(event -> load.run());

        pay.setOnAction(event -> {
            String[] selected =
                    rentals.getSelectionModel().getSelectedItem();

            if (selected == null) {
                return;
            }

            try {
                int rentalId = Integer.parseInt(selected[0]);

                BigDecimal amount = new BigDecimal(
                        amountField.getText().trim()
                ).setScale(2, RoundingMode.UNNECESSARY);

                if (amount.signum() <= 0) {
                    HelloApplication.showNotice(
                            status,
                            "Enter an amount greater than ₱0.00."
                    );
                    return;
                }

                String paymentMethod = method.getValue();

                if (paymentMethod == null || paymentMethod.isBlank()) {
                    HelloApplication.showNotice(
                            status,
                            "Please select a payment method."
                    );
                    return;
                }

                int redeemed = pointsToUse[0];

                BigDecimal discount = BigDecimal.valueOf(redeemed)
                        .divide(BigDecimal.TEN)
                        .setScale(2);

                String message = "Rental #" + rentalId
                        + "\nPayment method: " + paymentMethod
                        + "\nAmount to pay: ₱" + amount;

                if (redeemed > 0) {
                    BigDecimal dailyRate = new BigDecimal(selected[10]);

                    message +=
                            "\n\nVoucher: "
                                    + RentalDAO.getVoucherDescription(dailyRate)
                                    + "\nPoints to use: " + redeemed
                                    + "\nDiscount: ₱" + discount
                                    + "\nPoints remaining: "
                                    + (availablePoints[0] - redeemed);
                }

                Alert confirm = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        message,
                        ButtonType.YES,
                        ButtonType.NO
                );
                confirm.setTitle("Confirm Payment");
                confirm.setHeaderText("Review your payment");

                if (layout.getScene() != null
                        && layout.getScene().getWindow() != null) {
                    confirm.initOwner(layout.getScene().getWindow());
                }

                Optional<ButtonType> answer = confirm.showAndWait();

                if (answer.isEmpty() || answer.get() != ButtonType.YES) {
                    return;
                }

                pay.setDisable(true);

                try {
                    String error = rentalDAO.payRentalFee(
                            customer.getCustomerId(),
                            rentalId,
                            amount,
                            paymentMethod,
                            redeemed
                    );

                    if (error == null) {
                        usePoints.setSelected(false);
                        onSuccess.run();

                        String success = "Payment of ₱" + amount
                                + " recorded for Rental #" + rentalId + ".";

                        if (redeemed > 0) {
                            success += " Voucher applied: used " + redeemed
                                    + " points and saved ₱" + discount + ".";
                        }

                        HelloApplication.showNotice(status, success);

                    } else {
                        load.run();
                        HelloApplication.showNotice(status, error);
                    }

                } finally {
                    preview.run();
                }

            } catch (NumberFormatException | ArithmeticException exception) {
                HelloApplication.showNotice(
                        status,
                        "Enter a positive amount with at most two decimal places."
                );
            }
        });

        load.run();

        layout.getChildren().addAll(
                titleBox,
                rentals,
                paymentCard,
                status,
                heading("My Payment Receipts History"),
                receipts
        );

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return wrapper;
    }

    private static VBox createCustomerReturnView(
            Customer customer,
            Runnable onSuccess,
            Runnable[] refreshRef
    ) {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20, 24, 20, 24));

        VBox titleBox = new VBox(
                2,
                heading("Return My Borrowed Equipment"),
                description(
                        "Select an active borrowed item below to return it."
                )
        );

        TableView<String[]> table = new TableView<>();
        table.setPlaceholder(
                new Label("You have no active borrowed equipment to return.")
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        addColumns(
                table,
                new String[]{
                        "Rental #",
                        "Equipment Name",
                        "Category",
                        "Serial #",
                        "Date Rented",
                        "Due Date",
                        "Rental Status",
                        "Payment Status"
                },
                new int[]{0, 1, 2, 3, 4, 5, 6, 7},
                new double[]{75, 175, 105, 95, 135, 135, 120, 145},
                -1, -1
        );

        TextField serialField = new TextField();
        serialField.setPromptText("Select item above");
        serialField.setPrefWidth(140);

        CheckBox damaged = new CheckBox(
                "Report item as Damaged / Needs Maintenance"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button returnButton = primaryButton("Return Selected Item");
        Button refresh = secondaryButton("↻  Refresh");

        HBox actions = new HBox(
                14,
                new Label("Serial #:"),
                serialField,
                damaged,
                spacer,
                returnButton,
                refresh
        );
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(12, 16, 12, 16));
        actions.setStyle(PANEL_STYLE);

        Label status = HelloApplication.createNoticeLabel();

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, previous, selected) -> {
                    if (selected != null) {
                        serialField.setText(selected[3]);
                    }
                });

        Runnable load = () -> {
            table.getItems().setAll(
                    rentalDAO.getCustomerActiveBorrowedItems(
                            customer.getCustomerId()
                    )
            );

            if (table.getItems().isEmpty()) {
                serialField.clear();
            } else {
                table.getSelectionModel().selectFirst();
            }
        };

        refreshRef[0] = load;
        refresh.setOnAction(event -> load.run());

        returnButton.setOnAction(event -> {
            String serial = serialField.getText().trim();

            if (serial.isEmpty()) {
                HelloApplication.showNotice(
                        status,
                        "Select an item or enter its Serial Number."
                );
                return;
            }

            boolean isDamaged = damaged.isSelected();

            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Return item " + serial + "?\n\n"
                            + (isDamaged
                            ? "Condition: Damaged / Needs Maintenance"
                            : "Condition: Serviceable"),
                    ButtonType.YES,
                    ButtonType.NO
            );
            confirm.setTitle("Confirm Equipment Return");
            confirm.setHeaderText(null);

            if (confirm.showAndWait().orElse(ButtonType.NO)
                    != ButtonType.YES) {
                return;
            }

            returnButton.setDisable(true);

            try {
                boolean success = rentalDAO.returnEquipmentWithCondition(
                        customer.getCustomerId(),
                        serial,
                        isDamaged
                );

                if (success) {
                    damaged.setSelected(false);
                    onSuccess.run();

                    HelloApplication.showNotice(
                            status,
                            "Item " + serial + " returned successfully."
                                    + (isDamaged
                                    ? " It has been marked unavailable."
                                    : " Your reward points have been refreshed.")
                    );

                } else {
                    HelloApplication.showNotice(
                            status,
                            "Unable to return this item. Refresh and verify that "
                                    + "it is still borrowed under your account. "
                                    + "For damaged items, the database must support "
                                    + "a damaged or maintenance status."
                    );
                }

            } finally {
                returnButton.setDisable(false);
            }
        });

        load.run();
        layout.getChildren().addAll(titleBox, table, actions, status);
        return layout;
    }

    private static VBox createProfileAndPointsView(
            Customer customer,
            Label badge,
            Runnable onSuccess,
            Runnable[] refreshRef
    ) {
        VBox layout = new VBox(18);
        layout.setPadding(new Insets(28));
        layout.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.setPadding(new Insets(26, 34, 26, 34));
        card.setMaxWidth(500);
        card.setStyle(PANEL_STYLE);

        Label points = new Label();
        points.setMaxWidth(Double.MAX_VALUE);
        points.setStyle(
                "-fx-background-color: #f1f5f9;"
                        + "-fx-border-color: #cbd5e1 #cbd5e1 #cbd5e1 #0f172a;"
                        + "-fx-border-width: 1px 1px 1px 4px;"
                        + "-fx-text-fill: #0f172a;"
                        + "-fx-font-size: 13.5px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 10px 16px;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-border-radius: 8px;"
        );

        TextField id = new TextField(
                String.valueOf(customer.getCustomerId())
        );
        id.setEditable(false);
        id.setStyle("-fx-background-color: #e2e8f0;");

        TextField firstName = new TextField(customer.getFirstName());
        TextField lastName = new TextField(customer.getLastName());
        TextField contact = new TextField(customer.getContactNumber());
        TextField email = new TextField(customer.getEmail());
        email.setPrefWidth(260);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(10);
        form.addRow(0, new Label("Customer ID:"), id);
        form.addRow(1, new Label("First Name:"), firstName);
        form.addRow(2, new Label("Last Name:"), lastName);
        form.addRow(3, new Label("Contact Number:"), contact);
        form.addRow(4, new Label("Email Address:"), email);

        Button save = primaryButton("Save Profile Changes");
        Button refresh = secondaryButton("↻  Refresh Points");

        HBox actions = new HBox(10, save, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label status = HelloApplication.createNoticeLabel();

        Runnable load = () -> {
            Customer fresh = customerDAO.getCustomerById(
                    customer.getCustomerId()
            );

            if (fresh != null) {
                customer.setFirstName(fresh.getFirstName());
                customer.setLastName(fresh.getLastName());
                customer.setContactNumber(fresh.getContactNumber());
                customer.setEmail(fresh.getEmail());
                customer.setRewardPoints(fresh.getRewardPoints());
            }

            points.setText(
                    "REWARD POINTS  —  "
                            + customer.getRewardPoints() + " pts"
            );

            badge.setText(
                    customer.getFullName()
                            + "  •  ID #" + customer.getCustomerId()
            );
        };

        refreshRef[0] = load;
        refresh.setOnAction(event -> load.run());

        save.setOnAction(event -> {
            String first = firstName.getText().trim();
            String last = lastName.getText().trim();
            String phone = contact.getText().trim();
            String mail = email.getText().trim();

            if (first.isEmpty() || last.isEmpty() || phone.isEmpty()) {
                HelloApplication.showNotice(
                        status,
                        "First Name, Last Name, and Contact Number cannot be empty."
                );
                return;
            }

            Customer updated = new Customer(
                    customer.getCustomerId(),
                    first,
                    last,
                    phone,
                    mail,
                    customer.getRewardPoints()
            );

            if (customerDAO.updateCustomer(updated)) {
                onSuccess.run();

                HelloApplication.showNotice(
                        status,
                        "Your profile details have been saved."
                );
            } else {
                HelloApplication.showNotice(
                        status,
                        "Failed to update profile."
                );
            }
        });

        load.run();

        card.getChildren().addAll(
                heading("My Profile & Loyalty Rewards"),
                points,
                description(
                        "Redeem points in the payment tab. "
                                + "10 points = ₱1. Equipment vouchers offer "
                                + "5%, 10%, or 15% off, subject to voucher limits."
                ),
                form,
                actions,
                status
        );

        layout.getChildren().add(card);
        return layout;
    }
}