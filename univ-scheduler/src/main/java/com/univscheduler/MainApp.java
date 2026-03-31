package com.univscheduler;

import com.univscheduler.dao.DatabaseManager;
import com.univscheduler.service.AlerteService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainApp extends Application {
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        DatabaseManager.getInstance().initDatabase();
        AlerteService.start();

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        showLogin();
        stage.show();
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/univscheduler/fxml/login.fxml")
            );
            Scene scene = new Scene(loader.load(), 1100, 680);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/univscheduler/css/style.css").toExternalForm()
            );
            primaryStage.setScene(scene);
            Platform.runLater(() -> {
                primaryStage.setMaximized(false);
                primaryStage.setWidth(1100);
                primaryStage.setHeight(680);
                primaryStage.centerOnScreen();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        AlerteService.stop();
    }

    public static void main(String[] args) { launch(); }
}