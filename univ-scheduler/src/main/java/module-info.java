module com.univscheduler {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.sql;
    requires java.mail;
    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    opens com.univscheduler to javafx.fxml;
    opens com.univscheduler.controller to javafx.fxml;
    opens com.univscheduler.model to javafx.base;

    exports com.univscheduler;
    exports com.univscheduler.model;
    exports com.univscheduler.controller;
    exports com.univscheduler.dao;
    exports com.univscheduler.service;
}
