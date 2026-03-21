package com.univscheduler.controller;

import com.univscheduler.MainApp;
import com.univscheduler.model.Utilisateur;
import com.univscheduler.model.AlertePersonnalisee;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public abstract class BaseController {

    protected Utilisateur currentUser;

    public void initUser(Utilisateur user) {
        this.currentUser = user;
        onUserLoaded();
    }

    protected void onUserLoaded() {}

    // ── Déconnexion ───────────────────────────────────────────────
    protected void logout() {
        // ✅ Alerte personnalisée à la place de Alert JavaFX
        if (!AlertePersonnalisee.confirmerDeconnexion()) return;

        // Arrêter le service de rappel s'il tourne
        try { com.univscheduler.model.Servicerappel.getInstance().arreter(); }
        catch (Exception ignored) {}

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/univscheduler/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 650);
            scene.getStylesheets().add(
                    getClass().getResource("/com/univscheduler/css/style.css").toExternalForm());
            MainApp.primaryStage.setScene(scene);
            MainApp.primaryStage.setMaximized(false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Alertes ───────────────────────────────────────────────────

    /** Alerte succès — bande verte. */
    protected void showInfo(String titre, String message) {
        AlertePersonnalisee.succes(titre, message);
    }

    /** Alerte erreur — bande rouge. */
    protected void showError(String titre, String message) {
        AlertePersonnalisee.erreur(titre, message);
    }

    /** Alerte avertissement — bande orange. */
    protected void showWarning(String titre, String message) {
        AlertePersonnalisee.avertissement(titre, message);
    }

    /** Alerte information — bande bleue. */
    protected void showInfoBleu(String titre, String message) {
        AlertePersonnalisee.info(titre, message);
    }

    /**
     * Confirmation de suppression stylisée.
     * @param item ex : "ce cours", "cette réservation"
     * @return true si l'utilisateur confirme
     */
    protected boolean confirmDelete(String item) {
        return AlertePersonnalisee.confirmerSuppression(item);
    }

    /**
     * Confirmation générique.
     * @return true si l'utilisateur confirme
     */
    protected boolean confirmer(String titre, String message) {
        return AlertePersonnalisee.confirmer(titre, message);
    }
}