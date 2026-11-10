module com.example.sportsrentfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.sportsrentfx to javafx.fxml;
    exports com.example.sportsrentfx;
}