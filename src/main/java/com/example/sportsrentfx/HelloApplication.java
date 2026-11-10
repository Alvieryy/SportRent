package com.example.sportsrentfx;

import com.example.sportsrentfx.dao.CustomerDAO;
import com.example.sportsrentfx.model.Customer;
import com.example.sportsrentfx.security.AccessControl;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventDispatcher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public class HelloApplication extends Application {

    private static final CustomerDAO customerDAO = new CustomerDAO();

    private static final String BACKGROUND_PATH =
            "/com/example/sportsrentfx/images/sports-background.jpg";

    private static Image backgroundImage;

    private Timeline sessionTimer;

    private static final String MODERN_CSS = """
            .root {
                -fx-font-family: "Segoe UI";
                -fx-font-size: 14px;
                -fx-background-color: #f1f5f9;
            }

            .label {
                -fx-text-fill: #172b23;
            }

            .button {
                -fx-background-color: #e2e8f0;
                -fx-text-fill: #172b23;
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-padding: 10 18;
                -fx-cursor: hand;
            }

            .button:hover {
                -fx-background-color: #d4deea;
            }

            .button:focused {
                -fx-border-color: #3f9470;
                -fx-border-width: 2;
            }

            .button:disabled {
                -fx-opacity: 0.55;
            }

            .text-field, .password-field {
                -fx-background-color: #f8fafc;
                -fx-text-fill: #1e293b;
                -fx-prompt-text-fill: #8794a6;
                -fx-background-radius: 12;
                -fx-border-radius: 12;
                -fx-border-color: #dce3e5;
                -fx-border-width: 1;
                -fx-padding: 13 15;
                -fx-pref-height: 48;
                -fx-font-size: 14px;
            }

            .text-field:focused, .password-field:focused {
                -fx-background-color: white;
                -fx-border-color: #23734f;
                -fx-effect: dropshadow(
                    gaussian,
                    rgba(15, 89, 56, 0.16),
                    8,
                    0,
                    0,
                    0
                );
            }

            .combo-box {
                -fx-background-color: white;
                -fx-background-radius: 8;
                -fx-border-color: #cbd5e1;
                -fx-border-radius: 8;
                -fx-padding: 5 8;
            }

            .auth-root {
                -fx-background-color: #101815;
            }

            .auth-overlay {
                -fx-background-color: linear-gradient(
                    to bottom right,
                    rgba(0, 42, 30, 0.46),
                    rgba(4, 12, 10, 0.24),
                    rgba(0, 24, 17, 0.52)
                );
            }

            .brand-title {
                -fx-text-fill: white;
                -fx-font-size: 23px;
                -fx-font-weight: bold;
            }

            .brand-caption {
                -fx-text-fill: #d9e8e2;
                -fx-font-size: 12px;
            }

            .brand-symbol {
                -fx-background-color: rgba(255, 255, 255, 0.14);
                -fx-text-fill: white;
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-background-radius: 14;
                -fx-alignment: center;
            }

            .header-button {
                -fx-background-color: rgba(255, 255, 255, 0.10);
                -fx-text-fill: white;
                -fx-border-color: rgba(255, 255, 255, 0.45);
                -fx-border-width: 1;
                -fx-border-radius: 12;
                -fx-background-radius: 12;
                -fx-padding: 11 18;
            }

            .header-button:hover {
                -fx-background-color: rgba(255, 255, 255, 0.22);
            }

            .header-button:focused {
                -fx-border-color: white;
                -fx-border-width: 2;
            }

            .auth-card {
                -fx-background-color: rgba(238, 245, 242, 0.88);
                -fx-background-radius: 24;
                -fx-border-color: rgba(255, 255, 255, 0.80);
                -fx-border-radius: 24;
                -fx-border-width: 1;
                -fx-effect: dropshadow(
                    gaussian,
                    rgba(0, 0, 0, 0.25),
                    32,
                    0,
                    0,
                    12
                );
            }

            .auth-emblem {
                -fx-background-color: #0c482c;
                -fx-text-fill: white;
                -fx-font-size: 25px;
                -fx-font-weight: bold;
                -fx-background-radius: 18;
                -fx-alignment: center;
            }

            .auth-title {
                -fx-text-fill: #0c482c;
                -fx-font-size: 32px;
                -fx-font-weight: bold;
            }

            .field-label {
                -fx-text-fill: #344454;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
            }

            .primary-button {
                -fx-background-color: #0c482c;
                -fx-text-fill: white;
                -fx-background-radius: 12;
                -fx-border-radius: 12;
                -fx-border-color: transparent;
                -fx-border-width: 2;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-padding: 13 20;
                -fx-min-height: 48;
            }

            .primary-button:hover {
                -fx-background-color: #14633e;
            }

            .primary-button:pressed {
                -fx-background-color: #08361f;
            }

            .primary-button:focused {
                -fx-border-color: #7fc9a3;
                -fx-effect: dropshadow(
                    gaussian,
                    rgba(12, 72, 44, 0.25),
                    10,
                    0,
                    0,
                    0
                );
            }

            .auth-link {
                -fx-text-fill: #0c482c;
                -fx-font-weight: bold;
                -fx-border-color: transparent;
                -fx-padding: 5;
                -fx-cursor: hand;
            }

            .auth-link:hover {
                -fx-underline: true;
            }

            .auth-link:focused {
                -fx-border-color: #23734f;
                -fx-border-radius: 4;
            }

            .muted-text {
                -fx-text-fill: #5f6e7c;
                -fx-font-size: 13px;
            }

            .footer-text {
                -fx-text-fill: #e2ebe6;
                -fx-font-size: 12px;
            }

            .auth-scroll,
            .auth-scroll > .viewport {
                -fx-background-color: transparent;
            }

            .auth-scroll {
                -fx-padding: 0;
                -fx-background-insets: 0;
                -fx-border-width: 0;
            }

            .auth-scroll .scroll-bar:vertical {
                -fx-background-color: transparent;
                -fx-pref-width: 10;
            }

            .auth-scroll .scroll-bar:vertical .track {
                -fx-background-color: transparent;
            }

            .auth-scroll .scroll-bar:vertical .thumb {
                -fx-background-color: rgba(255, 255, 255, 0.35);
                -fx-background-radius: 8;
            }

            .login-dialog {
                -fx-background-color: #edf3ef;
                -fx-padding: 18;
            }

            .tab-pane > .tab-header-area {
                -fx-padding: 8 16 0 16;
            }

            .tab-pane > .tab-header-area > .tab-header-background {
                -fx-background-color: white;
            }

            .tab-pane .tab {
                -fx-background-color: transparent;
                -fx-padding: 10 16;
                -fx-cursor: hand;
            }

            .tab-pane .tab .tab-label {
                -fx-text-fill: #64748b;
                -fx-font-weight: bold;
            }

            .tab-pane .tab:selected {
                -fx-background-color: #e7f0ea;
                -fx-background-radius: 8 8 0 0;
            }

            .tab-pane .tab:selected .tab-label {
                -fx-text-fill: #0c482c;
            }

            .table-view {
                -fx-background-color: white;
                -fx-background-radius: 10;
                -fx-border-color: #e2e8f0;
                -fx-border-radius: 10;
                -fx-padding: 0;
            }

            .table-view .column-header-background {
                -fx-background-color: #eef3f0;
            }

            .table-view .column-header {
                -fx-background-color: transparent;
                -fx-size: 40px;
                -fx-border-color: transparent;
            }

            .table-view .column-header .label {
                -fx-text-fill: #172b23;
                -fx-font-weight: bold;
                -fx-alignment: CENTER_LEFT;
                -fx-padding: 0 8;
            }

            .table-row-cell {
                -fx-background-color: white;
                -fx-cell-size: 38px;
            }

            .table-row-cell:odd {
                -fx-background-color: #f8faf9;
            }

            .table-row-cell:hover {
                -fx-background-color: #edf3ef;
            }

            .table-row-cell:selected {
                -fx-background-color: #d8e9df;
            }

            .table-row-cell:selected .text {
                -fx-fill: #172b23;
            }

            .table-cell {
                -fx-text-fill: #172b23;
                -fx-alignment: CENTER_LEFT;
                -fx-padding: 0 10;
                -fx-border-color: transparent;
            }

            .check-box .box {
                -fx-background-color: white;
                -fx-border-color: #94a3b8;
                -fx-background-radius: 4;
                -fx-border-radius: 4;
            }

            .check-box:selected .box {
                -fx-background-color: #0c482c;
                -fx-border-color: #0c482c;
            }

            .check-box:selected .mark {
                -fx-background-color: white;
            }
            """;

    public static void applyModernTheme(Scene scene) {
        if (scene == null) {
            return;
        }

        String handlerKey = "sportrent.securityHandlerInstalled";

        if (!Boolean.TRUE.equals(scene.getProperties().get(handlerKey))) {
            EventDispatcher originalDispatcher = scene.getEventDispatcher();

            scene.setEventDispatcher((event, tail) -> {
                try {
                    return originalDispatcher.dispatchEvent(event, tail);
                } catch (SecurityException denied) {
                    AccessControl.logout();

                    if (scene.getWindow() instanceof Stage window) {
                        showMainScreen(window);

                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.initOwner(window);
                        alert.setTitle("Access Denied");
                        alert.setHeaderText(null);
                        alert.setContentText(
                                "Access denied or your session has expired. "
                                        + "Please sign in again."
                        );
                        alert.show();
                    }

                    return null;
                }
            });

            scene.getProperties().put(handlerKey, true);
        }

        String encoded = Base64.getEncoder().encodeToString(
                MODERN_CSS.getBytes(StandardCharsets.UTF_8)
        );

        String stylesheet = "data:text/css;base64," + encoded;

        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    public static Label createNoticeLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setVisible(false);
        label.setManaged(false);

        return label;
    }

    public static void showNotice(Label label, String message) {
        if (label == null) {
            return;
        }

        if (message == null || message.isBlank()) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
            return;
        }

        label.setText(
                message.trim().replaceFirst("^[✓ⓘ!\\s]+", "")
        );

        label.setVisible(true);
        label.setManaged(true);

        label.setStyle(
                "-fx-background-color: #fff3df;"
                        + "-fx-text-fill: #7c4615;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #edd6ad;"
                        + "-fx-border-radius: 10;"
                        + "-fx-padding: 12 14;"
                        + "-fx-font-size: 13px;"
        );
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("SportRent — Sports Equipment Rental System");
        stage.setMinWidth(640);
        stage.setMinHeight(540);

        AccessControl.logout();
        showMainScreen(stage);
        stage.show();

        sessionTimer = new Timeline(
                new KeyFrame(Duration.seconds(10), event -> {
                    if (AccessControl.isAdminSessionExpired()) {
                        AccessControl.logout();
                        showMainScreen(stage);

                        Alert alert = new Alert(
                                Alert.AlertType.INFORMATION
                        );

                        alert.initOwner(stage);
                        alert.setTitle("Session Expired");
                        alert.setHeaderText(null);
                        alert.setContentText(
                                "Your administrator session has expired. "
                                        + "Please sign in again."
                        );

                        alert.show();
                    }
                })
        );

        sessionTimer.setCycleCount(Timeline.INDEFINITE);
        sessionTimer.play();

        stage.setOnHidden(event -> {
            if (sessionTimer != null) {
                sessionTimer.stop();
            }

            AccessControl.logout();
        });
    }

    @Override
    public void stop() {
        if (sessionTimer != null) {
            sessionTimer.stop();
        }

        AccessControl.logout();
    }

    public static void showMainScreen(Stage stage) {
        showCustomerAuthScreen(stage);
    }

    public static void showCustomerAuthScreen(Stage stage) {
        showAuthenticationScreen(stage, false);
    }

    private static void showRegistrationScreen(Stage stage) {
        showAuthenticationScreen(stage, true);
    }

    private static Image getBackgroundImage() {
        if (backgroundImage == null) {
            URL resource = HelloApplication.class.getResource(
                    BACKGROUND_PATH
            );

            if (resource != null) {
                Image image = new Image(resource.toExternalForm());

                if (!image.isError()) {
                    backgroundImage = image;
                }
            }
        }

        return backgroundImage;
    }

    private static StackPane createBackground() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        Region picture = new Region();
        picture.setMouseTransparent(true);
        picture.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        Image image = getBackgroundImage();

        if (image != null) {
            BackgroundSize size = new BackgroundSize(
                    BackgroundSize.AUTO,
                    BackgroundSize.AUTO,
                    false,
                    false,
                    false,
                    true
            );

            BackgroundImage background = new BackgroundImage(
                    image,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    size
            );

            picture.setBackground(new Background(background));
        } else {
            picture.setStyle(
                    "-fx-background-color: linear-gradient("
                            + "to bottom right, #173e2d, #0a1010);"
            );
        }

        Region overlay = new Region();
        overlay.getStyleClass().add("auth-overlay");
        overlay.setMouseTransparent(true);
        overlay.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        root.getChildren().addAll(picture, overlay);

        return root;
    }

    private static HBox createHeader(Stage stage) {
        Label symbol = new Label("S");
        symbol.getStyleClass().add("brand-symbol");
        symbol.setMinSize(46, 46);
        symbol.setPrefSize(46, 46);

        Label brand = new Label("SportRent");
        brand.getStyleClass().add("brand-title");

        Label caption = new Label(
                "Sports Equipment Rental Management System"
        );

        caption.getStyleClass().add("brand-caption");
        caption.setWrapText(true);

        VBox brandText = new VBox(3, brand, caption);

        HBox identity = new HBox(12, symbol, brandText);
        identity.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button adminButton = new Button("Admin Login");
        adminButton.getStyleClass().add("header-button");
        adminButton.setMinWidth(122);
        adminButton.setOnAction(
                event -> showAdminLoginDialog(stage)
        );

        HBox header = new HBox(
                16,
                identity,
                spacer,
                adminButton
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 34, 20, 34));

        return header;
    }

    private static VBox createCard(String heading) {
        Label emblem = new Label("S");
        emblem.getStyleClass().add("auth-emblem");
        emblem.setMinSize(58, 58);
        emblem.setPrefSize(58, 58);
        emblem.setMaxSize(58, 58);

        Label title = new Label(heading);
        title.getStyleClass().add("auth-title");

        VBox titleGroup = new VBox(20, emblem, title);
        titleGroup.setAlignment(Pos.CENTER);
        titleGroup.setPadding(new Insets(0, 0, 8, 0));

        VBox card = new VBox(22);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(36));
        card.setMaxWidth(450);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.TOP_CENTER);
        card.getChildren().add(titleGroup);

        return card;
    }

    private static VBox fieldGroup(
            String text,
            TextField field
    ) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        label.setLabelFor(field);

        field.setMaxWidth(Double.MAX_VALUE);

        VBox group = new VBox(8, label, field);
        group.setFillWidth(true);
        group.setMaxWidth(Double.MAX_VALUE);

        return group;
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setMaxWidth(Double.MAX_VALUE);

        return button;
    }

    private static void submitOnEnter(
            Button button,
            TextField... fields
    ) {
        for (TextField field : fields) {
            field.setOnAction(event -> {
                if (!button.isDisabled()) {
                    button.fire();
                }

                event.consume();
            });
        }
    }

    private static void showAuthenticationScreen(
            Stage stage,
            boolean registration
    ) {
        AccessControl.logout();

        StackPane root = createBackground();

        BorderPane layout = new BorderPane();
        layout.setTop(createHeader(stage));

        VBox card = registration
                ? createRegistrationCard(stage)
                : createLoginCard(stage);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(20, 24, 30, 24));

        ScrollPane scroll = new ScrollPane(center);
        scroll.getStyleClass().add("auth-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scroll.viewportBoundsProperty().addListener(
                (observable, oldBounds, bounds) ->
                        center.setMinHeight(bounds.getHeight())
        );

        layout.setCenter(scroll);

        Label footer = new Label(
                "SportRent · Sports Equipment Rental Management System"
        );

        footer.getStyleClass().add("footer-text");
        footer.setWrapText(true);

        HBox footerBox = new HBox(footer);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setPadding(new Insets(14, 24, 22, 24));

        layout.setBottom(footerBox);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 1120, 800);
        applyModernTheme(scene);
        stage.setScene(scene);

        Platform.runLater(() -> {
            if (card.getUserData() instanceof TextField firstField) {
                firstField.requestFocus();
            }
        });
    }

    private static VBox createLoginCard(Stage stage) {
        VBox card = createCard("LOG IN");

        TextField identifierField = new TextField();
        identifierField.setPromptText(
                "Customer ID, email or full name"
        );

        TextField contactField = new TextField();
        contactField.setPromptText(
                "Enter your registered contact number"
        );

        VBox identifierGroup = fieldGroup(
                "Customer ID, Email or Full Name",
                identifierField
        );

        VBox contactGroup = fieldGroup(
                "Contact Number",
                contactField
        );

        Label status = createNoticeLabel();

        Button loginButton = primaryButton("Sign in");
        loginButton.setDefaultButton(true);

        submitOnEnter(
                loginButton,
                identifierField,
                contactField
        );

        loginButton.setOnAction(event -> {
            showNotice(status, null);

            String identifier = identifierField.getText().trim();
            String contact = contactField.getText().trim();

            if (identifier.isEmpty()) {
                showNotice(
                        status,
                        "Enter your Customer ID, email or full name."
                );

                identifierField.requestFocus();
                return;
            }

            if (contact.isEmpty()) {
                showNotice(
                        status,
                        "Enter your registered contact number."
                );

                contactField.requestFocus();
                return;
            }

            AccessControl.logout();

            try {
                Customer customer = customerDAO.authenticateCustomer(
                        identifier,
                        contact
                );

                if (customer == null) {
                    showNotice(
                            status,
                            "Your details do not match an account. Please try again."
                    );

                    contactField.requestFocus();
                    return;
                }

                Scene customerScene = CustomerDashboard.getScene(
                        stage,
                        customer
                );

                applyModernTheme(customerScene);
                stage.setScene(customerScene);
            } catch (RuntimeException failure) {
                showNotice(
                        status,
                        "Unable to sign in. Please try again."
                );
            }
        });

        Separator separator = new Separator();
        separator.setOpacity(0.55);

        Label prompt = new Label("Don't have an account?");
        prompt.getStyleClass().add("muted-text");

        Hyperlink registerLink = new Hyperlink(
                "Create an account"
        );

        registerLink.getStyleClass().add("auth-link");
        registerLink.setOnAction(
                event -> showRegistrationScreen(stage)
        );

        FlowPane registrationRow = new FlowPane(
                3,
                4,
                prompt,
                registerLink
        );

        registrationRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                identifierGroup,
                contactGroup,
                status,
                loginButton,
                separator,
                registrationRow
        );

        card.setUserData(identifierField);

        return card;
    }

    private static VBox createRegistrationCard(Stage stage) {
        VBox card = createCard("CREATE ACCOUNT");

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First name");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last name");

        TextField contactField = new TextField();
        contactField.setPromptText("e.g. 09171234567");

        TextField emailField = new TextField();
        emailField.setPromptText("you@example.com");

        VBox firstNameGroup = fieldGroup(
                "First Name",
                firstNameField
        );

        VBox lastNameGroup = fieldGroup(
                "Last Name",
                lastNameField
        );

        VBox contactGroup = fieldGroup(
                "Contact Number",
                contactField
        );

        VBox emailGroup = fieldGroup(
                "Email Address (optional)",
                emailField
        );

        Label status = createNoticeLabel();

        Button registerButton = primaryButton(
                "Create account"
        );

        registerButton.setDefaultButton(true);

        submitOnEnter(
                registerButton,
                firstNameField,
                lastNameField,
                contactField,
                emailField
        );

        registerButton.setOnAction(event -> {
            showNotice(status, null);

            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String contact = contactField.getText().trim();
            String email = emailField.getText().trim();

            if (firstName.isEmpty()) {
                showNotice(status, "Enter your first name.");
                firstNameField.requestFocus();
                return;
            }

            if (lastName.isEmpty()) {
                showNotice(status, "Enter your last name.");
                lastNameField.requestFocus();
                return;
            }

            String contactDigits = contact.replaceAll("[^0-9]", "");

            if (!contact.matches("^\\+?[0-9\\s-]+$")
                    || contactDigits.length() < 7
                    || contactDigits.length() > 15) {

                showNotice(
                        status,
                        "Enter a valid contact number containing 7 to 15 digits."
                );

                contactField.requestFocus();
                return;
            }

            if (!email.isEmpty()
                    && !email.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            )) {

                showNotice(status, "Enter a valid email address.");
                emailField.requestFocus();
                return;
            }

            try {
                Customer existing = customerDAO.findPotentialDuplicate(
                        firstName,
                        lastName,
                        contact,
                        email
                );

                if (existing != null) {
                    Alert duplicateAlert = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "An account with matching details already exists.\n\n"
                                    + "Would you like to create a separate account?",
                            ButtonType.YES,
                            ButtonType.NO
                    );

                    duplicateAlert.initOwner(stage);
                    duplicateAlert.setTitle("Existing Account");
                    duplicateAlert.setHeaderText(
                            "Check your account details"
                    );

                    Optional<ButtonType> result =
                            duplicateAlert.showAndWait();

                    if (result.isEmpty()
                            || result.get() != ButtonType.YES) {

                        showNotice(
                                status,
                                "Use Sign in below to access your existing account."
                        );

                        return;
                    }
                }

                AccessControl.logout();

                Customer customer = new Customer(
                        0,
                        firstName,
                        lastName,
                        contact,
                        email,
                        0
                );

                int customerId = customerDAO.addCustomerReturnId(
                        customer
                );

                if (customerId == -1) {
                    showNotice(
                            status,
                            "Unable to create your account. Please try again."
                    );

                    return;
                }

                customer.setCustomerId(customerId);

                Alert success = new Alert(
                        Alert.AlertType.INFORMATION
                );

                success.initOwner(stage);
                success.setTitle("Account Created");
                success.setHeaderText(
                        "Welcome to SportRent, "
                                + customer.getFullName() + "!"
                );

                success.setContentText(
                        "Your Customer ID is #" + customerId + ".\n\n"
                                + "Use your Customer ID, email or full name "
                                + "and registered contact number to sign in."
                );

                success.showAndWait();

                Scene customerScene = CustomerDashboard.getScene(
                        stage,
                        customer
                );

                applyModernTheme(customerScene);
                stage.setScene(customerScene);
            } catch (RuntimeException failure) {
                showNotice(
                        status,
                        "Unable to create your account. Please try again."
                );
            }
        });

        Label prompt = new Label("Already have an account?");
        prompt.getStyleClass().add("muted-text");

        Hyperlink loginLink = new Hyperlink("Sign in");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnAction(
                event -> showCustomerAuthScreen(stage)
        );

        FlowPane loginRow = new FlowPane(
                3,
                4,
                prompt,
                loginLink
        );

        loginRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                firstNameGroup,
                lastNameGroup,
                contactGroup,
                emailGroup,
                status,
                registerButton,
                loginRow
        );

        card.setUserData(firstNameField);

        return card;
    }

    private static void showAdminLoginDialog(Stage stage) {
        AccessControl.logout();

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("SportRent Admin");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("login-dialog");
        pane.setPrefWidth(430);

        Label title = new Label("ADMIN LOGIN");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 26px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        Label errorLabel = createNoticeLabel();

        VBox content = new VBox(
                20,
                title,
                fieldGroup("Username", usernameField),
                fieldGroup("Password", passwordField),
                errorLabel
        );

        content.setPadding(new Insets(12, 10, 20, 10));
        pane.setContent(content);

        ButtonType signInType = new ButtonType(
                "Sign in",
                ButtonBar.ButtonData.OK_DONE
        );

        pane.getButtonTypes().addAll(
                signInType,
                ButtonType.CANCEL
        );

        Button signInButton = (Button) pane.lookupButton(
                signInType
        );

        signInButton.getStyleClass().add("primary-button");
        signInButton.setDefaultButton(true);

        submitOnEnter(
                signInButton,
                usernameField,
                passwordField
        );

        signInButton.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    showNotice(errorLabel, null);

                    if (usernameField.getText().isBlank()) {
                        showNotice(
                                errorLabel,
                                "Enter your username."
                        );

                        usernameField.requestFocus();
                        event.consume();
                        return;
                    }

                    if (passwordField.getText().isEmpty()) {
                        showNotice(
                                errorLabel,
                                "Enter your password."
                        );

                        passwordField.requestFocus();
                        event.consume();
                        return;
                    }

                    char[] password = passwordField
                            .getText()
                            .toCharArray();

                    passwordField.clear();

                    try {
                        boolean authenticated = AccessControl.loginAdmin(
                                usernameField.getText().trim(),
                                password
                        );

                        if (!authenticated) {
                            showNotice(
                                    errorLabel,
                                    "Sign-in failed. Check your credentials "
                                            + "or try again later."
                            );

                            passwordField.requestFocus();
                            event.consume();
                        }
                    } catch (RuntimeException failure) {
                        AccessControl.logout();

                        showNotice(
                                errorLabel,
                                "Unable to sign in. Please try again."
                        );

                        event.consume();
                    } finally {
                        Arrays.fill(password, '\0');
                    }
                }
        );

        dialog.setResultConverter(
                button -> button == signInType
        );

        dialog.setOnShown(event -> {
            applyModernTheme(pane.getScene());
            Platform.runLater(usernameField::requestFocus);
        });

        boolean signedIn = dialog.showAndWait().orElse(false);

        passwordField.clear();

        if (signedIn) {
            AccessControl.requireAdmin();

            Scene adminScene = AdminDashboard.getScene(stage);
            applyModernTheme(adminScene);
            stage.setScene(adminScene);
        } else {
            AccessControl.logout();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}