package com.univscheduler.controller;

import com.univscheduler.MainApp;
import com.univscheduler.model.Utilisateur;
import com.univscheduler.model.AlertePersonnalisee;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class BaseController {

    protected Utilisateur currentUser;

    // ✅ Reference du chatbot
    protected Stage chatStage = null;

    public void initUser(Utilisateur user) {
        this.currentUser = user;
        onUserLoaded();
    }

    protected void onUserLoaded() {}

    // ── Deconnexion ───────────────────────────────────────────────
    protected void logout() {
        if (!AlertePersonnalisee.confirmerDeconnexion()) return;

        // ✅ Fermer le chatbot si ouvert
        if (chatStage != null && chatStage.isShowing()) {
            chatStage.close();
            chatStage = null;
        }

        try { com.univscheduler.model.Servicerappel.getInstance().arreter(); }
        catch (Exception ignored) {}

        MainApp.showLogin();
    }

    // ── Alertes ───────────────────────────────────────────────────
    protected void showInfo(String titre, String message) {
        AlertePersonnalisee.succes(titre, message);
    }
    protected void showError(String titre, String message) {
        AlertePersonnalisee.erreur(titre, message);
    }
    protected void showWarning(String titre, String message) {
        AlertePersonnalisee.avertissement(titre, message);
    }
    protected void showInfoBleu(String titre, String message) {
        AlertePersonnalisee.info(titre, message);
    }
    protected boolean confirmDelete(String item) {
        return AlertePersonnalisee.confirmerSuppression(item);
    }
    protected boolean confirmer(String titre, String message) {
        return AlertePersonnalisee.confirmer(titre, message);
    }

    // ── Chatbot ───────────────────────────────────────────────────
    @FXML
    protected void openChatbot() {
        // ✅ Toggle — fermer si deja ouvert
        if (chatStage != null && chatStage.isShowing()) {
            chatStage.close();
            chatStage = null;
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/univscheduler/fxml/chatbot.fxml"));
            Parent root = loader.load();

            ChatbotController ctrl = loader.getController();
            ctrl.initUser(currentUser);

            chatStage = new Stage();
            chatStage.initStyle(StageStyle.UNDECORATED);
            chatStage.initOwner(MainApp.primaryStage);
            chatStage.setScene(new Scene(root));
            chatStage.getScene().getStylesheets().add(
                    getClass().getResource(
                                    "/com/univscheduler/css/style.css")
                            .toExternalForm());
            ctrl.setChatStage(chatStage);

            chatStage.setX(MainApp.primaryStage.getX()
                    + MainApp.primaryStage.getWidth() - 420);
            chatStage.setY(MainApp.primaryStage.getY()
                    + MainApp.primaryStage.getHeight() - 570);

            // ✅ Nettoyer quand ferme via bouton X
            chatStage.setOnHidden(e -> chatStage = null);

            // ✅ Fermer si fenetre principale fermee
            MainApp.primaryStage.setOnCloseRequest(e -> {
                if (chatStage != null && chatStage.isShowing()) {
                    chatStage.close();
                    chatStage = null;
                }
            });

            chatStage.show();

        } catch (Exception e) { e.printStackTrace(); }
    }
}