module com.quickbite.quickbite {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Extra libraries
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // FXMLLoader uses reflection to create controllers and inject @FXML fields,
    // so packages holding controllers must be "opened" to javafx.fxml.
    opens com.quickbite.quickbite to javafx.fxml;
    opens com.quickbite.quickbite.controller to javafx.fxml;

    // The main package must be exported so JavaFX can start the Application class.
    exports com.quickbite.quickbite;
    exports com.quickbite.quickbite.controller;
}