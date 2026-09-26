module com.quickbite.quickbite {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Extra libraries
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // JDBC API (java.sql.*) and the SQLite driver
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // Networking (Phase 8: calling the external API) and JSON parsing
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    // FXMLLoader uses reflection to create controllers and inject @FXML fields,
    // so packages holding controllers must be "opened" to javafx.fxml.
    opens com.quickbite.quickbite to javafx.fxml;
    opens com.quickbite.quickbite.controller to javafx.fxml;

    // Jackson uses reflection to fill in the fields of our JSON model classes.
    opens com.quickbite.quickbite.api to com.fasterxml.jackson.databind;

    // The main package must be exported so JavaFX can start the Application class.
    exports com.quickbite.quickbite;
}
