module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.management;
    requires com.github.oshi;

    opens com.example to javafx.fxml;
    exports com.example;
}
