package com.univscheduler.controller;

import com.univscheduler.MainApp;
import com.univscheduler.dao.UtilisateurDAO;
import com.univscheduler.model.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setVisible(false);
        if (loginBtn != null) loginBtn.setOnAction(e -> handleLogin());
        if (passwordField != null) passwordField.setOnAction(e -> handleLogin());
    }

    @FXML private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs."); return;
        }
        Utilisateur user = utilisateurDAO.authentifier(email, password);
        if (user == null) { showError("Email ou mot de passe incorrect."); return; }
        navigateToDashboard(user);
    }

    @FXML private void handleQuickAdmin()        { quickLogin("admin@univ.fr",        "admin123"); }
    @FXML private void handleQuickGestionnaire() { quickLogin("marie.dupont@univ.fr", "gest123");  }
    @FXML private void handleQuickEnseignant()   { quickLogin("jean.martin@univ.fr",  "ens123");   }
    @FXML private void handleQuickEtudiant()     { quickLogin("paul.leroy@univ.fr",   "etu123");   }

    private void quickLogin(String email, String password) {
        emailField.setText(email);
        passwordField.setText(password);
        handleLogin();
    }

    private void navigateToDashboard(Utilisateur user) {
        String fxml;
        switch (user.getRole()) {
            case "ADMIN":        fxml = "/com/univscheduler/fxml/admin_dashboard.fxml";        break;
            case "GESTIONNAIRE": fxml = "/com/univscheduler/fxml/gestionnaire_dashboard.fxml"; break;
            case "ENSEIGNANT":   fxml = "/com/univscheduler/fxml/enseignant_dashboard.fxml";   break;
            case "ETUDIANT":     fxml = "/com/univscheduler/fxml/etudiant_dashboard.fxml";     break;
            default:             fxml = "/com/univscheduler/fxml/admin_dashboard.fxml";
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) { showError("FXML introuvable: " + fxml); return; }
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/com/univscheduler/css/style.css").toExternalForm());
            Object controller = loader.getController();
            if (controller instanceof BaseController) ((BaseController) controller).initUser(user);
            MainApp.primaryStage.setScene(scene);
            MainApp.primaryStage.setMaximized(true);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
            showError("Erreur: " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    }
}
