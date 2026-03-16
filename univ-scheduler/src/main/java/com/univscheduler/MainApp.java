package com.univscheduler;

import com.univscheduler.dao.DatabaseManager;
import com.univscheduler.service.AlerteService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        DatabaseManager.getInstance().initDatabase();
        AlerteService.start(); // Start background reminder service

        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/univscheduler/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 650);
        scene.getStylesheets().add(MainApp.class.getResource("/com/univscheduler/css/style.css").toExternalForm());
        stage.setTitle("UNIV-SCHEDULER v2.1 – Gestion des Salles et Emplois du Temps");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        AlerteService.stop(); // Clean shutdown
    }

    public static void main(String[] args) { launch(); }
}
