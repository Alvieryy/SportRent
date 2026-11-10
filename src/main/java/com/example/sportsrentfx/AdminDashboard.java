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
import com.example.sportsrentfx.security.AccessControl;

import java.util.List;

/**
 * Modern Minimalist Staff & Admin Console.
 * Uses a uniform, high-contrast dark-slate font color (#0f172a) across all headings,
 * KPI cards, tables, and Notice Banners.
 */
public class AdminDashboard {

    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private static final RentalDAO rentalDAO = new RentalDAO();

    private static final String CARD_STYLE =
            "-fx-background-color: #ffffff; " +
                    "-fx-background-radius: 12px; " +
                    "-fx-border-color: #e2e8f0; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.04), 12, 0, 0, 4);";

    public static Scene getScene(Stage stage) {

        AccessControl.requireAdmin();
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f1f5f9;");

        // 1. Modern Top Header Bar
        HBox topBar = new HBox(14);
        topBar.setPadding(new Insets(14, 24, 14, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #0f172a;");

        Label brandPill = new Label("SPORTRENT");
        brandPill.setStyle(
                "-fx-background-color: #334155; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 4px 10px; " +
                        "-fx-background-radius: 6px;"
        );

        Label headerTitle = new Label("Staff & Admin Console");
        headerTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label adminBadge = new Label("Administrator Session");
        adminBadge.setStyle(
                "-fx-background-color: #1e293b; " +
                        "-fx-text-fill: #f8fafc; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6px 14px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: #334155; " +
                        "-fx-border-radius: 20px;"
        );

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.setStyle(
                "-fx-background-color: #f8fafc; " +
                        "-fx-text-fill: #0f172a; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 6px 14px;"
        );
        logoutBtn.setOnAction(event -> {
            AccessControl.logout();
            HelloApplication.showMainScreen(stage);
        });

        topBar.getChildren().addAll(brandPill, headerTitle, spacer, adminBadge, logoutBtn);

        // 2. Minimalist Admin KPI Strip (Uniform #0f172a Font Color)
        HBox kpiStrip = new HBox(16);
        kpiStrip.setPadding(new Insets(14, 24, 10, 24));
        kpiStrip.setAlignment(Pos.CENTER_LEFT);

        Label totalUnitsVal = new Label("0 Units");
        VBox kpi1 = createKpiCard("TOTAL INVENTORY STOCK", totalUnitsVal);

        Label availUnitsVal = new Label("0 Available");
        VBox kpi2 = createKpiCard("AVAILABLE FOR CHECKOUT", availUnitsVal);

        Label rentedOutVal = new Label("0 Rented Out");
        VBox kpi3 = createKpiCard("CURRENTLY RENTED OUT", rentedOutVal);

        Label totalCustomersVal = new Label("0 Accounts");
        VBox kpi4 = createKpiCard("REGISTERED CUSTOMERS", totalCustomersVal);

        HBox.setHgrow(kpi1, Priority.ALWAYS);
        HBox.setHgrow(kpi2, Priority.ALWAYS);
        HBox.setHgrow(kpi3, Priority.ALWAYS);
        HBox.setHgrow(kpi4, Priority.ALWAYS);
        kpiStrip.getChildren().addAll(kpi1, kpi2, kpi3, kpi4);

        Runnable refreshAdminKpis = () -> {
            List<String[]> catalog = equipmentDAO.getEquipmentCatalogWithPrices();
            int tot = 0, rented = 0, avail = 0;
            for (String[] row : catalog) {
                try {
                    tot += Integer.parseInt(row[3]);
                    rented += Integer.parseInt(row[4]);
                    avail += Integer.parseInt(row[6]);
                } catch (NumberFormatException ignored) {}
            }
            totalUnitsVal.setText(tot + " Units");
            availUnitsVal.setText(avail + " Available");
            rentedOutVal.setText(rented + " Rented Out");
            totalCustomersVal.setText(customerDAO.getAllCustomers().size() + " Accounts");
        };

        VBox topContainer = new VBox(0, topBar, kpiStrip);
        root.setTop(topContainer);

        // 3. Admin Tabs inside Elevated White Container
        TabPane tabPane = new TabPane();

        Tab equipTab = new Tab("Equipment Inventory", createEquipmentView(refreshAdminKpis));
        equipTab.setClosable(false);

        Tab customerTab = new Tab("Customer Directory", createCustomerView(refreshAdminKpis));
        customerTab.setClosable(false);

        Tab historyTab = new Tab("Rental History", createTransactionHistoryView());
        historyTab.setClosable(false);

        Tab paymentReportTab = new Tab("Payments & Collections", createPaymentsReportView());
        paymentReportTab.setClosable(false);

        Tab returnTab = new Tab("Return Desk", createReturnDeskView(refreshAdminKpis));
        returnTab.setClosable(false);

        Tab addEquipTab = new Tab("Add Stock", createAdminEquipmentView(refreshAdminKpis));
        addEquipTab.setClosable(false);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshAdminKpis.run());
        tabPane.getTabs().addAll(equipTab, customerTab, historyTab, paymentReportTab, returnTab, addEquipTab);

        StackPane bodyWrapper = new StackPane(tabPane);
        bodyWrapper.setPadding(new Insets(0, 24, 20, 24));
        tabPane.setStyle(CARD_STYLE);
        root.setCenter(bodyWrapper);

        refreshAdminKpis.run();

        Scene scene = new Scene(root, 1020, 680);
        HelloApplication.applyModernTheme(scene);
        return scene;
    }

    private static VBox createKpiCard(String labelText, Label valueLabel) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(CARD_STYLE);

        Label title = new Label(labelText);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        card.getChildren().addAll(title, valueLabel);
        return card;
    }

    private static VBox createEquipmentView(Runnable refreshKpis) {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20, 24, 20, 24));

        HBox headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Real-Time Equipment Inventory");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Available Quantity = Total Quantity − Rented Out − Damaged/Unavailable");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻  Refresh Inventory");
        refreshBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1;");
        headerBar.getChildren().addAll(titleBox, spacer, refreshBtn);

        TableView<String[]> table = new TableView<>();
        VBox.setVgrow(table, Priority.ALWAYS);

        String[] headers = {"Equipment Name", "Category", "Daily Rate (₱)", "Total Stock", "Rented Out", "Damaged/Unavail", "Available Now"};
        double[] widths = {210, 125, 120, 115, 110, 130, 120};
        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            final boolean isPrice = (i == 2);
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(data -> new SimpleStringProperty(
                    isPrice ? "₱" + data.getValue()[colIndex] : data.getValue()[colIndex]
            ));
            col.setPrefWidth(widths[i]);
            table.getColumns().add(col);
        }

        refreshBtn.setOnAction(e -> {
            table.getItems().clear();
            table.getItems().addAll(equipmentDAO.getEquipmentCatalogWithPrices());
            if (refreshKpis != null) refreshKpis.run();
        });
        refreshBtn.fire();

        layout.getChildren().addAll(headerBar, table);
        return layout;
    }

    private static VBox createCustomerView(Runnable refreshKpis) {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20, 24, 20, 24));

        VBox titleBox = new VBox(2);
        Label title = new Label("Customer Directory & Records");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Search customer accounts or remove unreferenced duplicate profiles (customers with rental history are protected)");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        titleBox.getChildren().addAll(title, sub);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name, Contact Number, or Email...");
        searchField.setPrefWidth(290);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white;");

        Button resetBtn = new Button("Show All");
        resetBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("Delete Unreferenced Customer");
        deleteBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1; -fx-font-weight: bold;");

        searchRow.getChildren().addAll(searchField, searchBtn, resetBtn, spacer, deleteBtn);

        TableView<Customer> table = new TableView<>();
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Customer, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCustomerId())));
        colId.setPrefWidth(65);

        TableColumn<Customer, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFirstName() + " " + c.getValue().getLastName()));
        colName.setPrefWidth(220);

        TableColumn<Customer, String> colContact = new TableColumn<>("Contact Number");
        colContact.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContactNumber()));
        colContact.setPrefWidth(160);

        TableColumn<Customer, String> colEmail = new TableColumn<>("Email Address");
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmail.setPrefWidth(230);

        TableColumn<Customer, String> colPoints = new TableColumn<>("Reward Points");
        colPoints.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRewardPoints() + " pts"));
        colPoints.setPrefWidth(120);

        table.getColumns().addAll(colId, colName, colContact, colEmail, colPoints);

        Label customerNotice = HelloApplication.createNoticeLabel();

        Runnable loadAll = () -> {
            table.getItems().clear();
            table.getItems().addAll(customerDAO.getAllCustomers());
            if (refreshKpis != null) refreshKpis.run();
        };

        searchBtn.setOnAction(e -> {
            String kw = searchField.getText().trim();
            if (!kw.isEmpty()) {
                table.getItems().clear();
                table.getItems().addAll(customerDAO.searchCustomers(kw));
            } else {
                loadAll.run();
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            loadAll.run();
        });

        deleteBtn.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                HelloApplication.showNotice(customerNotice, "Please select a customer from the table first.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete customer record for " + selected.getFullName() + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait();
            if (confirm.getResult() == ButtonType.YES) {
                boolean ok = customerDAO.deleteCustomer(selected.getCustomerId());
                if (ok) {
                    HelloApplication.showNotice(customerNotice, "Deleted unreferenced customer #" + selected.getCustomerId() + " (" + selected.getFullName() + ").");
                    loadAll.run();
                } else {
                    HelloApplication.showNotice(customerNotice, "Cannot delete " + selected.getFullName() + " because they have existing rental history.");
                }
            }
        });

        loadAll.run();
        layout.getChildren().addAll(titleBox, searchRow, table, customerNotice);
        return layout;
    }

    private static VBox createTransactionHistoryView() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20, 24, 20, 24));

        HBox headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Rental & Borrowing Transaction History");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("Complete audit trail of borrowed items, due dates, and return timestamps");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻  Refresh History");
        refreshBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1;");
        headerBar.getChildren().addAll(titleBox, spacer, refreshBtn);

        TableView<String[]> table = new TableView<>();
        VBox.setVgrow(table, Priority.ALWAYS);

        String[] headers = {"Rental #", "Customer Name", "Equipment", "Serial #", "Date Rented", "Due Date", "Return Status"};
        double[] widths = {80, 165, 175, 100, 145, 145, 150};
        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[colIndex]));
            col.setPrefWidth(widths[i]);
            table.getColumns().add(col);
        }

        refreshBtn.setOnAction(e -> {
            table.getItems().clear();
            table.getItems().addAll(rentalDAO.getTransactionHistoryReport());
        });
        refreshBtn.fire();

        layout.getChildren().addAll(headerBar, table);
        return layout;
    }

    private static VBox createPaymentsReportView() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20, 24, 20, 24));

        HBox headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Customer Rental Fee Payments & Collections");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label sub = new Label("All recorded rental fee payments, methods, and remaining rental balances");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻  Refresh Collections");
        refreshBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1;");
        headerBar.getChildren().addAll(titleBox, spacer, refreshBtn);

        TableView<String[]> table = new TableView<>();
        table.setPlaceholder(new Label("No rental fee payments recorded yet."));
        VBox.setVgrow(table, Priority.ALWAYS);

        String[] headers = {"Receipt #", "Payment Date", "Customer Name", "Rental #", "Amount Paid", "Method", "Rental Charge", "Remaining Balance"};
        double[] widths = {85, 150, 165, 80, 115, 115, 115, 130};
        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[colIndex]));
            col.setPrefWidth(widths[i]);
            table.getColumns().add(col);
        }

        refreshBtn.setOnAction(e -> {
            table.getItems().clear();
            table.getItems().addAll(rentalDAO.getAllPaymentsReport());
        });
        refreshBtn.fire();

        layout.getChildren().addAll(headerBar, table);
        return layout;
    }

    private static VBox createReturnDeskView(Runnable refreshKpis) {
        VBox layout = new VBox(18);
        layout.setPadding(new Insets(28));
        layout.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(16);
        card.setPadding(new Insets(26, 32, 26, 32));
        card.setMaxWidth(520);
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");

        Label returnTitle = new Label("Counter Return Desk");
        returnTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label sub = new Label("Process an equipment return on behalf of a customer and inspect item condition.");
        sub.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");

        GridPane returnForm = new GridPane();
        returnForm.setHgap(14);
        returnForm.setVgap(12);

        TextField returnCustIdField = new TextField();
        returnCustIdField.setPromptText("e.g. 11");
        returnCustIdField.setPrefWidth(260);

        TextField returnSerialField = new TextField();
        returnSerialField.setPromptText("e.g. MO-002 or BSK-001");
        returnSerialField.setPrefWidth(260);

        CheckBox damagedCheck = new CheckBox("Item returned Damaged / Needs Maintenance");

        returnForm.addRow(0, new Label("Customer ID:"), returnCustIdField);
        returnForm.addRow(1, new Label("Item Serial #:"), returnSerialField);
        returnForm.addRow(2, new Label("Condition:"), damagedCheck);

        Button returnBtn = new Button("Process Equipment Return");
        returnBtn.setMaxWidth(Double.MAX_VALUE);
        returnBtn.setPrefHeight(40);
        returnBtn.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-font-weight: bold;");

        Label returnStatus = HelloApplication.createNoticeLabel();

        returnBtn.setOnAction(e -> {
            try {
                int cId = Integer.parseInt(returnCustIdField.getText().trim());
                String serial = returnSerialField.getText().trim();
                boolean isDamaged = damagedCheck.isSelected();

                boolean success = rentalDAO.returnEquipmentWithCondition(cId, serial, isDamaged);
                if (success) {
                    if (isDamaged) {
                        HelloApplication.showNotice(returnStatus, "Return recorded: Item " + serial + " marked for maintenance.");
                    } else {
                        HelloApplication.showNotice(returnStatus, "Item " + serial + " returned to Available stock! +10 Points awarded to Customer #" + cId + ".");
                    }
                    returnCustIdField.clear();
                    returnSerialField.clear();
                    damagedCheck.setSelected(false);
                    if (refreshKpis != null) refreshKpis.run();
                } else {
                    HelloApplication.showNotice(returnStatus, "Active unreturned rental not found for Customer #" + cId + " and Serial '" + serial + "'.");
                }
            } catch (NumberFormatException ex) {
                HelloApplication.showNotice(returnStatus, "Please enter a valid numeric Customer ID.");
            }
        });

        card.getChildren().addAll(returnTitle, sub, returnForm, returnBtn, returnStatus);
        layout.getChildren().add(card);
        return layout;
    }

    private static VBox createAdminEquipmentView(Runnable refreshKpis) {
        VBox layout = new VBox(18);
        layout.setPadding(new Insets(28));
        layout.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(16);
        card.setPadding(new Insets(26, 32, 26, 32));
        card.setMaxWidth(560);
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");

        Label title = new Label("Add Sports Equipment to Inventory");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label sub = new Label("Register a new equipment model or add additional serialized units to stock.");
        sub.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Molten Basketball GG7X");
        nameField.setPrefWidth(280);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Basketball", "Volleyball", "Badminton", "Soccer", "Tennis", "Table Tennis", "Other");
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setPrefWidth(280);

        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 50.00");

        TextField quantityField = new TextField();
        quantityField.setPromptText("e.g. 5");

        TextField prefixField = new TextField();
        prefixField.setPromptText("e.g. BSK");

        form.addRow(0, new Label("Equipment Name:"), nameField);
        form.addRow(1, new Label("Category:"), categoryBox);
        form.addRow(2, new Label("Daily Rate (₱):"), priceField);
        form.addRow(3, new Label("Quantity to Add:"), quantityField);
        form.addRow(4, new Label("Serial Prefix:"), prefixField);

        Button saveBtn = new Button("Add Equipment Units to Stock");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setPrefHeight(40);
        saveBtn.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-font-weight: bold;");

        Label statusLabel = HelloApplication.createNoticeLabel();

        saveBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                String category = categoryBox.getValue();
                double price = Double.parseDouble(priceField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim());
                String prefix = prefixField.getText().trim();

                if (name.isEmpty() || prefix.isEmpty() || quantity <= 0 || price <= 0) {
                    HelloApplication.showNotice(statusLabel, "Please fill out all fields with positive values.");
                    return;
                }

                boolean success = equipmentDAO.addEquipmentWithItems(name, category, price, quantity, prefix);

                if (success) {
                    HelloApplication.showNotice(statusLabel, "Successfully added " + quantity + " unit(s) of " + name + " to stock!");
                    nameField.clear();
                    priceField.clear();
                    quantityField.clear();
                    prefixField.clear();
                    if (refreshKpis != null) refreshKpis.run();
                } else {
                    HelloApplication.showNotice(statusLabel, "Error adding equipment. Check database connection.");
                }
            } catch (NumberFormatException ex) {
                HelloApplication.showNotice(statusLabel, "Daily Rate and Quantity must be valid numbers.");
            }
        });

        card.getChildren().addAll(title, sub, form, saveBtn, statusLabel);
        layout.getChildren().add(card);
        return layout;
    }
}