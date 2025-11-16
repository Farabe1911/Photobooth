module com.example.photobooth {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    // The webcam-capture library does not provide a stable module name across
    // versions; compile it on the classpath instead of requiring a module.
    requires javafx.swing;

    exports com.example.photobooth;
}