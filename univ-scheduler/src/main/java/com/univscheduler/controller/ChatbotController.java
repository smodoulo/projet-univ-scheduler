package com.univscheduler.controller;

import com.univscheduler.model.Utilisateur;
import com.univscheduler.service.ChatbotService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.concurrent.Executors;

/**
 * Contrôleur du chatbot UNIV-SCHEDULER.
 *
 * Chargé par BaseController.openChatbot() via chatbot.fxml.
 * Reçoit l'utilisateur via initUser(user).
 * Les suggestions s'adaptent automatiquement au rôle.
 */
public class ChatbotController {

    // ── FXML — lié à chatbot.fxml ─────────────────────────────────
    @FXML private ScrollPane scrollPane;
    @FXML private VBox       messagesBox;
    @FXML private TextField  inputField;
    @FXML private Button     btn1, btn2, btn3;

    // ── État ──────────────────────────────────────────────────────
    private Stage            chatStage;
    private double           dragX, dragY;
    private final ChatbotService service = new ChatbotService();

    // ── Couleurs palette UNIV-SCHEDULER ───────────────────────────
    private static final String BOT_BG  = "#e8f4f4";
    private static final String BOT_FG  = "#1a5f6e";
    private static final String USER_BG = "#1a5f6e";

    // ════════════════════════════════════════════════════════════════
    //  Appelé par BaseController.openChatbot() après chargement FXML
    // ════════════════════════════════════════════════════════════════

    public void setChatStage(Stage stage) {
        this.chatStage = stage;
    }

    /**
     * Point d'entrée principal.
     * Reçoit l'utilisateur connecté → configure le service et les suggestions.
     */
    public void initUser(Utilisateur user) {
        service.setUser(user);
        configurerSuggestions(user);

        // Message de bienvenue personnalisé depuis ChatbotService
        String bienvenue = service.repondre("bonjour");
        Platform.runLater(() -> ajouterMessageBot(bienvenue));
    }

    // ════════════════════════════════════════════════════════════════
    //  Suggestions adaptées au rôle
    // ════════════════════════════════════════════════════════════════

    private void configurerSuggestions(Utilisateur user) {
        if (user == null) return;

        switch (user.getRole()) {

            case "ETUDIANT":
                // Fonctionnalités étudiant : EDT, salles libres, notifications
                configBtn(btn1, "fas-list-alt",   "Mon EDT",   "mon emploi du temps");
                configBtn(btn2, "fas-university", "Salles",    "salles libres");
                configBtn(btn3, "fas-bell",       "Notifs",    "mes notifications");
                break;

            case "ENSEIGNANT":
                // Fonctionnalités enseignant : cours, réservations, signalements
                configBtn(btn1, "fas-calendar-alt",         "Mes cours",    "mes cours");
                configBtn(btn2, "fas-bookmark",             "Réservations", "mes reservations");
                configBtn(btn3, "fas-exclamation-triangle", "Signalements", "mes signalements");
                break;

            case "GESTIONNAIRE":
                // Fonctionnalités gestionnaire : cours du jour, à valider, stats
                configBtn(btn1, "fas-calendar-alt", "Cours du jour",  "cours du jour");
                configBtn(btn2, "fas-bookmark",     "À valider",      "reservations en attente");
                configBtn(btn3, "fas-chart-line",   "Statistiques",   "statistiques");
                break;

            case "ADMIN":
                // Fonctionnalités admin : utilisateurs, salles, stats globales
                configBtn(btn1, "fas-users",     "Utilisateurs",  "utilisateurs");
                configBtn(btn2, "fas-building",  "Salles",        "salles");
                configBtn(btn3, "fas-chart-bar", "Statistiques",  "statistiques globales");
                break;

            default:
                configBtn(btn1, "fas-calendar-alt", "Planning",  "planning");
                configBtn(btn2, "fas-university",   "Salles",    "salles libres");
                configBtn(btn3, "fas-bell",         "Notifs",    "notifications");
        }
    }

    /**
     * Configure un bouton suggestion : icône FA5 + label + commande stockée en userData.
     */
    private void configBtn(Button btn, String iconLiteral, String label, String commande) {
        if (btn == null) return;
        try {
            FontIcon fi = new FontIcon();
            fi.setIconLiteral(iconLiteral);
            fi.setIconSize(12);
            btn.setGraphic(fi);
        } catch (Exception ignored) {
            // Si l'icône échoue, le bouton reste sans icône mais fonctionnel
        }
        btn.setText(label);
        btn.setUserData(commande); // commande envoyée au clic
    }

    // ════════════════════════════════════════════════════════════════
    //  Actions FXML — liées à chatbot.fxml
    // ════════════════════════════════════════════════════════════════

    @FXML
    private void handleSend() {
        String texte = inputField.getText().trim();
        if (texte.isEmpty()) return;
        inputField.clear();
        traiterEnvoi(texte);
    }

    // Les 3 boutons suggestion appellent leur commande stockée en userData
    @FXML private void suggest1() { envoyerSuggestion(btn1); }
    @FXML private void suggest2() { envoyerSuggestion(btn2); }
    @FXML private void suggest3() { envoyerSuggestion(btn3); }

    private void envoyerSuggestion(Button btn) {
        if (btn == null) return;
        String cmd = btn.getUserData() != null
                ? btn.getUserData().toString()
                : btn.getText();
        traiterEnvoi(cmd);
    }

    @FXML
    private void handleClose() {
        if (chatStage != null) chatStage.close();
    }

    @FXML
    private void handleHeaderPressed(javafx.scene.input.MouseEvent e) {
        dragX = e.getScreenX() - (chatStage != null ? chatStage.getX() : 0);
        dragY = e.getScreenY() - (chatStage != null ? chatStage.getY() : 0);
    }

    @FXML
    private void handleHeaderDragged(javafx.scene.input.MouseEvent e) {
        if (chatStage != null) {
            chatStage.setX(e.getScreenX() - dragX);
            chatStage.setY(e.getScreenY() - dragY);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Logique d'envoi + réponse en background
    // ════════════════════════════════════════════════════════════════

    private void traiterEnvoi(String texte) {
        // 1. Afficher le message utilisateur
        ajouterMessageUser(texte);

        // 2. Indicateur "en cours"
        HBox typingRow = ajouterTypingIndicator();

        // 3. Appel service en background (évite de bloquer le thread UI)
        Executors.newSingleThreadExecutor().submit(() -> {
            String reponse;
            try {
                Thread.sleep(350);
                reponse = service.repondre(texte);
            } catch (Exception e) {
                reponse = "Une erreur est survenue. Veuillez réessayer.";
            }
            final String rep = reponse;
            Platform.runLater(() -> {
                messagesBox.getChildren().remove(typingRow);
                ajouterMessageBot(rep);
            });
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  Construction des bulles de message
    // ════════════════════════════════════════════════════════════════

    private void ajouterMessageUser(String texte) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(0, 4, 0, 52));

        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(255);
        bubble.setStyle(
                "-fx-background-color:" + USER_BG + ";"
                        + "-fx-text-fill:white;"
                        + "-fx-padding:10 14;-fx-background-radius:18 4 18 18;"
                        + "-fx-font-size:12.5px;");

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void ajouterMessageBot(String texte) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(0, 52, 0, 4));

        // Avatar robot
        StackPane avatar = new StackPane();
        Circle cercle = new Circle(14, Color.web("#2a9cb0"));
        try {
            FontIcon ic = new FontIcon();
            ic.setIconLiteral("fas-robot");
            ic.setIconSize(12);
            ic.setIconColor(Color.WHITE);
            avatar.getChildren().addAll(cercle, ic);
        } catch (Exception e) {
            avatar.getChildren().add(cercle);
        }
        avatar.setMinSize(28, 28);
        avatar.setMaxSize(28, 28);

        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(270);
        bubble.setStyle(
                "-fx-background-color:" + BOT_BG + ";"
                        + "-fx-text-fill:" + BOT_FG + ";"
                        + "-fx-padding:10 14;-fx-background-radius:4 18 18 18;"
                        + "-fx-font-size:12px;-fx-font-family:'Courier New';");

        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private HBox ajouterTypingIndicator() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(0, 52, 0, 4));

        Label typing = new Label("⋯");
        typing.setStyle(
                "-fx-background-color:" + BOT_BG + ";"
                        + "-fx-text-fill:#2a9cb0;-fx-font-size:22px;"
                        + "-fx-padding:4 14;-fx-background-radius:4 18 18 18;");

        row.getChildren().add(typing);
        messagesBox.getChildren().add(row);
        scrollToBottom();
        return row;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}