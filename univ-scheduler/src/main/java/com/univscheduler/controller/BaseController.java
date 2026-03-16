package com.univscheduler.controller;

import com.univscheduler.MainApp;
import com.univscheduler.model.Utilisateur;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public abstract class BaseController {
    protected Utilisateur currentUser;

    public void initUser(Utilisateur user) {
        this.currentUser = user;
        onUserLoaded();
    }

    protected void onUserLoaded() {}

    protected void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vous déconnecter ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/univscheduler/fxml/login.fxml"));
                Scene scene = new Scene(loader.load(), 1000, 650);
                scene.getStylesheets().add(getClass().getResource("/com/univscheduler/css/style.css").toExternalForm());
                MainApp.primaryStage.setScene(scene);
                MainApp.primaryStage.setMaximized(false);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    protected void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    protected void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    protected boolean confirmDelete(String item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression"); alert.setHeaderText("Supprimer " + item + " ?");
        alert.setContentText("Cette action est irréversible.");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
