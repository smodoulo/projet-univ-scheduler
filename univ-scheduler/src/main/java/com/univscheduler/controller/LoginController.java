package com.univscheduler.controller;

import com.univscheduler.MainApp;
import com.univscheduler.dao.UtilisateurDAO;
import com.univscheduler.model.Utilisateur;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.io.PrintWriter;
import java.io.StringWriter;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginBtn;
    @FXML private Label         roleTitle;
    @FXML private Label         roleSubtitle;
    @FXML private Label         roleBadge;
    @FXML private VBox          rightPanel;
    @FXML private VBox          logoPane;

    private String selectedRole = null;
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        if (errorLabel    != null) errorLabel.setVisible(false);
        if (loginBtn      != null) loginBtn.setOnAction(e -> handleLogin());
        if (passwordField != null) passwordField.setOnAction(e -> handleLogin());
    }

    // ── Fenêtre ───────────────────────────────────────────────────
    @FXML private void handleClose()    { MainApp.primaryStage.close(); }
    @FXML private void handleMinimize() { MainApp.primaryStage.setIconified(true); }
    @FXML private void handleMaximize() {
        MainApp.primaryStage.setMaximized(!MainApp.primaryStage.isMaximized());
    }
    @FXML private void handleBarPressed(MouseEvent e) {
        xOffset = MainApp.primaryStage.getX() - e.getScreenX();
        yOffset = MainApp.primaryStage.getY() - e.getScreenY();
    }
    @FXML private void handleBarDragged(MouseEvent e) {
        MainApp.primaryStage.setX(e.getScreenX() + xOffset);
        MainApp.primaryStage.setY(e.getScreenY() + yOffset);
    }

    // ── Boutons rôles ─────────────────────────────────────────────
    @FXML private void handleQuickAdmin()        { selectRole("ADMIN",        "👑 Administrateur", "#7c3aed"); }
    @FXML private void handleQuickGestionnaire() { selectRole("GESTIONNAIRE", "📋 Gestionnaire",   "#0891b2"); }
    @FXML private void handleQuickEnseignant()   { selectRole("ENSEIGNANT",   "👨‍🏫 Enseignant",    "#059669"); }
    @FXML private void handleQuickEtudiant()     { selectRole("ETUDIANT",     "🎓 Étudiant",       "#d97706"); }

    private void selectRole(String role, String label, String color) {
        this.selectedRole = role;

        if (roleTitle    != null) roleTitle.setText("Connexion");
        if (roleSubtitle != null) roleSubtitle.setText("Espace " + label + " — entrez vos identifiants");
        if (roleBadge    != null) {
            roleBadge.setText(label);
            roleBadge.setStyle(
                    "-fx-background-color:" + color + "22;" +
                            "-fx-text-fill:"        + color + ";" +
                            "-fx-border-color:"     + color + "55;" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:20;" +
                            "-fx-background-radius:20;" +
                            "-fx-padding:4 16;" +
                            "-fx-font-size:12px;" +
                            "-fx-font-weight:bold;"
            );
            roleBadge.setVisible(true);
        }

        emailField.clear();
        passwordField.clear();
        if (errorLabel != null) errorLabel.setVisible(false);

        showRightPanel();
    }

    private void showRightPanel() {
        if (rightPanel == null) return;

        // Cacher les features
        if (logoPane != null) {
            logoPane.setVisible(false);
            logoPane.setManaged(false);
        }

        if (!rightPanel.isVisible()) {
            rightPanel.setVisible(true);
            rightPanel.setManaged(true);
            rightPanel.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(450), rightPanel);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setOnFinished(e -> emailField.requestFocus());
            fade.play();
        } else {
            FadeTransition fade = new FadeTransition(Duration.millis(250), rightPanel);
            fade.setFromValue(0.4);
            fade.setToValue(1);
            fade.setOnFinished(e -> emailField.requestFocus());
            fade.play();
        }
    }

    // ── Connexion ─────────────────────────────────────────────────
    @FXML private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs."); return;
        }

        Utilisateur user = utilisateurDAO.authentifier(email, password);
        if (user == null) {
            showError("Email ou mot de passe incorrect."); return;
        }

        if (selectedRole != null && !user.getRole().equalsIgnoreCase(selectedRole)) {
            showError("⛔ Ce compte n'est pas un compte " + getRoleLabel(selectedRole) + ".");
            return;
        }

        navigateToDashboard(user);
    }

    private String getRoleLabel(String role) {
        switch (role) {
            case "ADMIN":        return "Administrateur";
            case "GESTIONNAIRE": return "Gestionnaire";
            case "ENSEIGNANT":   return "Enseignant";
            case "ETUDIANT":     return "Étudiant";
            default:             return role;
        }
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
            if (loader.getLocation() == null) { showError("FXML introuvable : " + fxml); return; }
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/com/univscheduler/css/style.css").toExternalForm()
            );
            Object controller = loader.getController();
            if (controller instanceof BaseController)
                ((BaseController) controller).initUser(user);
            MainApp.primaryStage.setScene(scene);
            MainApp.primaryStage.setMaximized(true);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            showError("Erreur : " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    }
}