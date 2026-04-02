package com.univscheduler.controller;

import com.univscheduler.model.Utilisateur;
import com.univscheduler.service.ChatbotService;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ChatbotController {

    @FXML private VBox       messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  inputField;
    @FXML private Button     btn1, btn2, btn3;

    private final ChatbotService service = new ChatbotService();
    private Stage chatStage;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        addBotMessage("👋 Bonjour ! Tapez 'aide' pour voir ce que je peux faire.");
    }

    public void initUser(Utilisateur user) {
        service.setUser(user);
        // Adapter les suggestions selon le rôle
        if (user != null) {
            switch (user.getRole()) {
                case "ETUDIANT":
                    btn1.setText("📅 Mon planning");
                    btn2.setText("🏛 Salles libres");
                    btn3.setText("🔔 Notifications");
                    break;
                case "ENSEIGNANT":
                    btn1.setText("📅 Mes cours");
                    btn2.setText("📋 Réservations");
                    btn3.setText("🔧 Signalements");
                    break;
                case "GESTIONNAIRE":
                    btn1.setText("📅 Cours du jour");
                    btn2.setText("⏳ En attente");
                    btn3.setText("📊 Statistiques");
                    break;
                case "ADMIN":
                    btn1.setText("👥 Utilisateurs");
                    btn2.setText("🏛 Salles");
                    btn3.setText("📊 Statistiques");
                    break;
            }
        }
        addBotMessage(service.repondre("bonjour"));
    }

    public void setChatStage(Stage stage) {
        this.chatStage = stage;
    }

    // ── Drag fenêtre ─────────────────────────────────────────────
    @FXML private void handleHeaderPressed(MouseEvent e) {
        if (chatStage != null) {
            xOffset = chatStage.getX() - e.getScreenX();
            yOffset = chatStage.getY() - e.getScreenY();
        }
    }

    @FXML private void handleHeaderDragged(MouseEvent e) {
        if (chatStage != null) {
            chatStage.setX(e.getScreenX() + xOffset);
            chatStage.setY(e.getScreenY() + yOffset);
        }
    }

    @FXML private void handleClose() {
        if (chatStage != null) chatStage.close();
    }

    @FXML private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();
        addUserMessage(text);
        showTyping();
        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> {
            removeTyping();
            addBotMessage(service.repondre(text));
        });
        pause.play();
    }

    @FXML private void suggest1() { sendSuggestion(btn1.getText().replaceAll("^\\S+\\s", "")); }
    @FXML private void suggest2() { sendSuggestion(btn2.getText().replaceAll("^\\S+\\s", "")); }
    @FXML private void suggest3() { sendSuggestion(btn3.getText().replaceAll("^\\S+\\s", "")); }

    private void sendSuggestion(String text) {
        addUserMessage(text);
        showTyping();
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            removeTyping();
            addBotMessage(service.repondre(text));
        });
        pause.play();
    }

    // ── Messages ─────────────────────────────────────────────────
    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setSpacing(6);

        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(270);
        label.getStyleClass().add("msg-user");

        row.getChildren().add(label);
        fadeIn(row);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size:15px; -fx-padding: 0 4 0 0;");

        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(270);
        label.getStyleClass().add("msg-bot");

        row.getChildren().addAll(avatar, label);
        fadeIn(row);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    // ── Typing indicator ─────────────────────────────────────────
    private HBox typingRow;

    private void showTyping() {
        typingRow = new HBox(8);
        typingRow.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size:15px;");
        Label dot = new Label("● ● ●");
        dot.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:11px;"
                + "-fx-background-color:#f1f5f9;"
                + "-fx-padding:10 16; -fx-background-radius:12 12 12 4;");
        typingRow.getChildren().addAll(avatar, dot);
        messagesBox.getChildren().add(typingRow);
        scrollToBottom();
    }

    private void removeTyping() {
        if (typingRow != null) {
            messagesBox.getChildren().remove(typingRow);
            typingRow = null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void fadeIn(javafx.scene.Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void scrollToBottom() {
        PauseTransition p = new PauseTransition(Duration.millis(120));
        p.setOnFinished(e -> scrollPane.setVvalue(1.0));
        p.play();
    }
}