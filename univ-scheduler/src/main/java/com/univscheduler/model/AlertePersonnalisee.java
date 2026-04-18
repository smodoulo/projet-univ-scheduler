package com.univscheduler.model;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.List;

/**
 * Alertes personnalisées UNIV-SCHEDULER
 * Icônes : FontIcon (Ikonli / Font Awesome Solid/Regular)
 * Couleurs : palette teal UNIV-SCHEDULER
 */
public class AlertePersonnalisee {

    // ── Palette teal ──────────────────────────────────────────────
    private static final String TEAL_DARK    = "#1a5f6e";
    private static final String TEAL_MID     = "#2a9cb0";
    private static final String TEAL_LIGHT   = "#4ecdc4";
    private static final String TEAL_BG      = "#e8f4f4";
    private static final String TEAL_BG_SOFT = "#f0f9fa";
    private static final String GREEN_SOFT   = "#3ecf8e";
    private static final String GREEN_BG     = "#dcfce7";
    private static final String GREEN_TXT    = "#166534";
    private static final String GREEN_BORDER = "#86efac";
    private static final String GOLD         = "#f0a500";
    private static final String GOLD_BG      = "#fffbeb";
    private static final String GOLD_TXT     = "#78350f";
    private static final String RED_SOFT     = "#e05c5c";
    private static final String RED_BG       = "#fee2e2";
    private static final String RED_TXT      = "#991b1b";
    private static final String RED_BORDER   = "#fca5a5";
    private static final String ROW_ALT      = "#f0f9fa";
    private static final String BORDER_LIGHT = "#c0dde4";
    private static final String TEXT_MUTED   = "#9eb3bf";
    private static final String TEXT_SECOND  = "#6b8394";

    // ── Couleurs icônes navbar (référence FXML étudiant) ──────────
    private static final String ICON_NAV    = "#a8d8e2";  // fas-bell, fas-graduation-cap navbar
    private static final String ICON_TAB    = "#2a9cb0";  // fas-list-alt, fas-calendar-alt tabs
    private static final String ICON_WHITE  = "white";    // icônes sur fonds colorés

    // ════════════════════════════════════════════════════════════════
    //  Helpers FontIcon — remplacent les émojis partout
    // ════════════════════════════════════════════════════════════════

    /** Crée un FontIcon Ikonli avec taille et couleur. */
    private static FontIcon fi(String literal, int size, String hexColor) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(hexColor));
        return icon;
    }

    /** FontIcon dans un StackPane centré (pour les cercles d'icône). */
    private static StackPane iconInCircle(String literal, int iconSize, String iconColor,
                                          double radius, String fillHex, String strokeHex) {
        Circle bg = new Circle(radius);
        bg.setFill(Color.web(fillHex));
        if (strokeHex != null) {
            bg.setStroke(Color.web(strokeHex));
            bg.setStrokeWidth(2.5);
        }
        FontIcon icon = fi(literal, iconSize, iconColor);
        StackPane sp = new StackPane(bg, icon);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    // ════════════════════════════════════════════════════════════════
    public enum Type {
        SUCCES  (TEAL_DARK, TEAL_BG,  TEAL_DARK, "fas-check-circle",   "Succès"),
        ERREUR  (RED_SOFT,  RED_BG,   RED_TXT,   "fas-times-circle",   "Erreur"),
        INFO    (TEAL_MID,  TEAL_BG,  TEAL_DARK, "fas-info-circle",    "Information"),
        WARN    (GOLD,      GOLD_BG,  GOLD_TXT,  "fas-exclamation-triangle", "Avertissement"),
        QUESTION(TEAL_DARK, TEAL_BG,  TEAL_DARK, "fas-question-circle","Confirmation");

        final String pri, fond, txt, iconLiteral, defTitre;
        Type(String p, String f, String t, String i, String d) {
            pri=p; fond=f; txt=t; iconLiteral=i; defTitre=d;
        }
    }

    private static String couleurBtn(Type type) {
        return switch (type) {
            case ERREUR -> RED_SOFT;
            case WARN   -> GOLD;
            case SUCCES -> GREEN_SOFT;
            default     -> TEAL_MID;
        };
    }

    // ── API simple ────────────────────────────────────────────────
    public static void    succes(String t, String m)        { afficher(Type.SUCCES,   t, m, false); }
    public static void    info(String t, String m)          { afficher(Type.INFO,     t, m, false); }
    public static void    erreur(String t, String m)        { afficher(Type.ERREUR,   t, m, false); }
    public static void    avertissement(String t, String m) { afficher(Type.WARN,     t, m, false); }
    public static boolean confirmer(String t, String m)     { return afficher(Type.QUESTION, t, m, true); }

    public static boolean confirmerSuppression(String e) {
        return afficher(Type.ERREUR, "Confirmer la suppression",
                "Êtes-vous sûr(e) de vouloir supprimer\n" + e + " ?\n\nCette action est irréversible.", true);
    }
    public static boolean confirmerDeconnexion() {
        return afficher(Type.QUESTION, "Déconnexion",
                "Voulez-vous vous déconnecter\nde UNIV-SCHEDULER ?", true);
    }

    // ════════════════════════════════════════════════════════════════
    //  CHATBOT INTÉGRÉ
    // ════════════════════════════════════════════════════════════════
    public static void ouvrirChatbot(String nomUtilisateur) {
        Stage fenetre = new Stage();
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);
        fenetre.setAlwaysOnTop(true);

        VBox root = new VBox(0);
        root.setPrefSize(380, 520);
        root.setMaxSize(380, 520);
        root.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:" + BORDER_LIGHT + ";" +
                        "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;" +
                        "-fx-effect:dropshadow(gaussian,rgba(26,95,110,0.22),32,0,0,8);");

        // ── En-tête ───────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:20 20 0 0;-fx-cursor:move;");

        // Avatar robot (FontIcon fas-robot dans cercle teal-light)
        StackPane avatarPane = iconInCircle("fas-robot", 15, TEAL_LIGHT, 18, "rgba(78,205,196,0.2)", TEAL_LIGHT);

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Assistant UNIV-SCHEDULER");
        titleLbl.setStyle("-fx-text-fill:#f0f9fa;-fx-font-size:13px;-fx-font-weight:bold;");
        HBox statusRow = new HBox(5); statusRow.setAlignment(Pos.CENTER_LEFT);
        Circle statusDot = new Circle(4, Color.web(GREEN_SOFT));
        statusDot.setEffect(new DropShadow(4, Color.web(GREEN_SOFT, 0.6)));
        Label statusLbl = new Label("En ligne");
        statusLbl.setStyle("-fx-text-fill:" + TEAL_LIGHT + ";-fx-font-size:10px;");
        statusRow.getChildren().addAll(statusDot, statusLbl);
        titleBox.getChildren().addAll(titleLbl, statusRow);

        Region espH = new Region(); HBox.setHgrow(espH, Priority.ALWAYS);

        // Bouton fermer (fas-times)
        Button btnClose = new Button();
        btnClose.setGraphic(fi("fas-times", 12, TEXT_MUTED));
        String closeBase = "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:4 8;-fx-background-radius:8;";
        btnClose.setStyle(closeBase);
        btnClose.setOnMouseEntered(e -> { btnClose.setStyle(closeBase.replace("transparent", RED_SOFT)); btnClose.setGraphic(fi("fas-times", 12, ICON_WHITE)); });
        btnClose.setOnMouseExited(e  -> { btnClose.setStyle(closeBase); btnClose.setGraphic(fi("fas-times", 12, TEXT_MUTED)); });
        btnClose.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setToValue(0); ft.setOnFinished(ev -> fenetre.close()); ft.play();
        });

        header.getChildren().addAll(avatarPane, titleBox, espH, btnClose);

        final double[] drag = {0, 0};
        header.setOnMousePressed(e  -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        header.setOnMouseDragged(e  -> { fenetre.setX(e.getScreenX() - drag[0]); fenetre.setY(e.getScreenY() - drag[1]); });

        // ── Zone messages ─────────────────────────────────────────
        VBox messagesBox = new VBox(10);
        messagesBox.setPadding(new Insets(14, 16, 14, 16));
        messagesBox.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";");

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true); scrollPane.setPrefHeight(280);
        scrollPane.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-border-width:0;-fx-background:" + TEAL_BG_SOFT + ";");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        ajouterMsgBot(messagesBox, scrollPane,
                "Bonjour " + nomUtilisateur + " !\nJe suis votre assistant UNIV-SCHEDULER. Comment puis-je vous aider ?");

        // ── Suggestions rapides ───────────────────────────────────
        HBox suggestions = new HBox(6);
        suggestions.setPadding(new Insets(8, 10, 8, 10));
        suggestions.setStyle("-fx-background-color:" + TEAL_BG + ";-fx-border-color:" + BORDER_LIGHT + ";-fx-border-width:1 0 0 0;");
        String[][] chips = {
                {"fas-book",      "Cours"},
                {"fas-door-open", "Salles"},
                {"fas-calendar",  "Planning"},
                {"fas-exclamation-triangle", "Conflits"}
        };
        for (String[] chip : chips) {
            Button btn = creerBtnSuggestIcon(chip[0], chip[1]);
            final String texte = chip[1];
            btn.setOnAction(e -> {
                ajouterMsgUser(messagesBox, scrollPane, texte);
                Platform.runLater(() -> repondreBot(messagesBox, scrollPane, texte, nomUtilisateur));
            });
            suggestions.getChildren().add(btn);
        }

        // ── Barre saisie ──────────────────────────────────────────
        HBox inputBar = new HBox(8);
        inputBar.setPadding(new Insets(10, 12, 10, 12));
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER_LIGHT + ";-fx-border-width:1 0 0 0;-fx-background-radius:0 0 20 20;");

        TextField inputField = new TextField();
        inputField.setPromptText("Posez votre question...");
        inputField.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-border-color:transparent;-fx-border-width:0;-fx-border-radius:20;-fx-background-radius:20;-fx-padding:10 16;-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Bouton envoyer (fas-paper-plane)
        Button btnSend = new Button();
        btnSend.setGraphic(fi("fas-paper-plane", 14, ICON_WHITE));
        String sendBase = "-fx-background-color:" + TEAL_MID + ";-fx-background-radius:50%;-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;-fx-cursor:hand;-fx-padding:0;";
        btnSend.setStyle(sendBase);
        btnSend.setOnMouseEntered(e -> btnSend.setStyle(sendBase.replace(TEAL_MID, TEAL_DARK)));
        btnSend.setOnMouseExited(e  -> btnSend.setStyle(sendBase.replace(TEAL_DARK, TEAL_MID)));

        Runnable envoyer = () -> {
            String txt = inputField.getText().trim();
            if (txt.isEmpty()) return;
            ajouterMsgUser(messagesBox, scrollPane, txt);
            inputField.clear();
            Platform.runLater(() -> repondreBot(messagesBox, scrollPane, txt, nomUtilisateur));
        };
        btnSend.setOnAction(e -> envoyer.run());
        inputField.setOnAction(e -> envoyer.run());
        inputBar.getChildren().addAll(inputField, btnSend);

        root.getChildren().addAll(header, scrollPane, suggestions, inputBar);

        StackPane scene = new StackPane(root);
        scene.setStyle("-fx-background-color:transparent;");
        Scene sc = new Scene(scene);
        sc.setFill(Color.TRANSPARENT);
        fenetre.setScene(sc);
        root.setOpacity(0);
        fenetre.show();
        FadeTransition ft = new FadeTransition(Duration.millis(220), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private static void ajouterMsgUser(VBox box, ScrollPane scroll, String texte) {
        HBox row = new HBox(); row.setAlignment(Pos.CENTER_RIGHT);
        Label msg = new Label(texte);
        msg.setWrapText(true); msg.setMaxWidth(260);
        msg.setStyle("-fx-background-color:" + TEAL_MID + ";-fx-text-fill:white;-fx-padding:10 14;-fx-background-radius:18 4 18 18;-fx-font-size:13px;");
        row.getChildren().add(msg);
        box.getChildren().add(row);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private static void ajouterMsgBot(VBox box, ScrollPane scroll, String texte) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        // Avatar bot : fas-robot
        Label avatar = new Label(); avatar.setGraphic(fi("fas-robot", 16, TEAL_MID));
        Label msg = new Label(texte);
        msg.setWrapText(true); msg.setMaxWidth(270);
        msg.setStyle("-fx-background-color:white;-fx-text-fill:" + TEAL_DARK + ";-fx-padding:10 14;-fx-background-radius:4 18 18 18;-fx-font-size:13px;-fx-border-color:" + BORDER_LIGHT + ";-fx-border-width:1;-fx-border-radius:4 18 18 18;");
        row.setOpacity(0);
        row.getChildren().addAll(avatar, msg);
        box.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setToValue(1); ft.play();
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private static void repondreBot(VBox box, ScrollPane scroll, String question, String nom) {
        HBox typing = new HBox(6); typing.setAlignment(Pos.CENTER_LEFT);
        Label dots = new Label("● ● ●"); dots.setStyle("-fx-text-fill:" + TEXT_MUTED + ";-fx-font-size:14px;");
        typing.getChildren().add(dots);
        box.getChildren().add(typing);
        Platform.runLater(() -> scroll.setVvalue(1.0));
        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> {
            box.getChildren().remove(typing);
            ajouterMsgBot(box, scroll, genererReponse(question.toLowerCase(), nom));
        });
        pause.play();
    }

    private static String genererReponse(String q, String nom) {
        if (q.contains("cours"))       return "Gérez les cours dans l'onglet \"Emploi du Temps\".\nAjout, modification, suppression et export disponibles.";
        if (q.contains("salle"))       return "La carte des salles est dans l'onglet \"Carte Salles\".\nTaux d'occupation en temps réel par bâtiment.";
        if (q.contains("planning") || q.contains("calendrier")) return "Le calendrier est dans l'onglet \"Vue Calendrier\".\nVue semaine ou mois disponible.";
        if (q.contains("conflit"))     return "Les conflits sont détectés automatiquement lors de la création d'un cours.\nConflit salle ou enseignant signalé en rouge.";
        if (q.contains("rapport") || q.contains("statistique")) return "Les rapports sont dans l'onglet \"Rapports\".\nHebdomadaire, mensuel, graphiques et export Excel.";
        if (q.contains("réservation") || q.contains("reserv")) return "Les réservations se valident dans l'onglet \"Réservations\".\nAcceptation ou refus avec notification automatique.";
        if (q.contains("bonjour") || q.contains("salut") || q.contains("hello")) return "Bonjour " + nom + " ! Comment puis-je vous aider aujourd'hui ?";
        if (q.contains("merci"))       return "Avec plaisir ! N'hésitez pas si vous avez d'autres questions.";
        return "Je n'ai pas bien compris votre question.\nEssayez : \"cours\", \"salles\", \"planning\", \"conflits\"...";
    }

    /** Bouton suggestion chatbot avec FontIcon + texte */
    private static Button creerBtnSuggestIcon(String iconLiteral, String texte) {
        Button b = new Button(texte);
        b.setGraphic(fi(iconLiteral, 11, TEAL_MID));
        String base = "-fx-background-color:white;-fx-text-fill:" + TEAL_MID + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 10;-fx-background-radius:20;-fx-border-color:#a8e6df;-fx-border-width:1;-fx-border-radius:20;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> { b.setStyle(base.replace("white", TEAL_MID).replace(TEAL_MID + ";", "white;")); b.setGraphic(fi(iconLiteral, 11, ICON_WHITE)); });
        b.setOnMouseExited(e  -> { b.setStyle(base); b.setGraphic(fi(iconLiteral, 11, TEAL_MID)); });
        return b;
    }

    // ════════════════════════════════════════════════════════════════
    //  Motif annulation
    // ════════════════════════════════════════════════════════════════
    public static String demanderMotifAnnulation(
            String nomCours, String jour, String heureDebut, String heureFin) {
        final String[] motif = {null};
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(520, 420);

        VBox carte = new VBox(0);
        carte.setMaxWidth(440);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:" + TEAL_BG + ";-fx-background-radius:14;");

        HBox bandeTitre = bandeTitre("fas-times-circle", "Annuler ce cours", fenetre, RED_SOFT);
        Region ligne = ligneDeco(RED_SOFT);

        VBox corps = new VBox(14);
        corps.setPadding(new Insets(20, 24, 14, 24));
        corps.setStyle("-fx-background-color:" + TEAL_BG + ";");
        corps.getChildren().add(grilleInfos(new String[][]{
                {"Cours", nomCours},
                {"Jour",  jour + "  " + heureDebut + " - " + heureFin}
        }));

        VBox blocMotif = new VBox(6);
        Label lblMotif = new Label("Motif d'annulation *");
        lblMotif.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_DARK + ";");
        TextArea champMotif = new TextArea();
        champMotif.setPromptText("Ex: Cours reporté, enseignant absent...");
        champMotif.setPrefRowCount(3); champMotif.setWrapText(true);
        champMotif.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER_LIGHT + ";-fx-border-radius:8;-fx-background-radius:8;-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";");

        HBox errRow = new HBox(5); errRow.setAlignment(Pos.CENTER_LEFT);
        errRow.setVisible(false);
        errRow.getChildren().addAll(fi("fas-exclamation-triangle", 11, RED_SOFT),
                labelStyled("Veuillez saisir un motif.", "-fx-text-fill:" + RED_SOFT + ";-fx-font-size:11px;"));
        blocMotif.getChildren().addAll(lblMotif, champMotif, errRow);
        corps.getChildren().add(blocMotif);

        HBox boutons = new HBox(12); boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setPadding(new Insets(14, 24, 20, 24));
        boutons.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");

        Button btnAnnuler = creerBoutonIcon("fas-arrow-left", "Retour", TEAL_BG, TEAL_DARK, BORDER_LIGHT);
        btnAnnuler.setOnAction(e -> fenetre.close());
        Button btnConfirmer = creerBoutonIcon("fas-times-circle", "Confirmer l'annulation", RED_SOFT, ICON_WHITE, "#c94040");
        btnConfirmer.setOnAction(e -> {
            String m = champMotif.getText().trim();
            if (m.isEmpty()) { errRow.setVisible(true); return; }
            motif[0] = m; fenetre.close();
        });
        boutons.getChildren().addAll(btnAnnuler, btnConfirmer);

        carte.getChildren().addAll(bandeTitre, ligne, corps, boutons);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
        return motif[0];
    }

    // ════════════════════════════════════════════════════════════════
    //  Détail signalement
    // ════════════════════════════════════════════════════════════════
    public static void afficherDetailSignalement(
            int idSignal, String titre, String[][] lignes,
            String description, String reponseAdmin, String dateResolution,
            String couleurStatut) {

        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(600, 500);

        VBox carte = new VBox(0);
        carte.setMaxWidth(500);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:14;");

        String titreBande = idSignal > 0 ? "Signalement #" + idSignal : (titre != null ? titre : "Détail");
        HBox bandeTitre = bandeTitre("fas-clipboard-list", titreBande, fenetre, TEAL_MID);
        Region ligne = ligneDeco(TEAL_MID);

        HBox enTete = new HBox(12); enTete.setAlignment(Pos.CENTER_LEFT);
        enTete.setPadding(new Insets(16, 24, 12, 24));
        enTete.setStyle("-fx-background-color:" + TEAL_DARK + ";");
        Label lblTitreSignal = new Label(titre != null ? titre : "—");
        lblTitreSignal.setWrapText(true); lblTitreSignal.setMaxWidth(340);
        lblTitreSignal.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
        if (couleurStatut != null && lignes != null) {
            String statut = "";
            for (String[] l : lignes) if (l[0].contains("Statut")) { statut = l[1]; break; }
            Label badge = new Label(statut.isEmpty() ? "Info" : statut);
            badge.setStyle("-fx-background-color:" + couleurStatut + ";-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
            enTete.getChildren().addAll(lblTitreSignal, badge);
        } else {
            enTete.getChildren().add(lblTitreSignal);
        }

        VBox corps = new VBox(0);
        corps.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";");

        if (lignes != null) {
            VBox grille = new VBox(0); grille.setPadding(new Insets(16, 24, 8, 24));
            for (int i = 0; i < lignes.length; i++) {
                if (lignes[i][0].contains("Statut")) continue;
                HBox rangee = new HBox(0); rangee.setPadding(new Insets(10, 12, 10, 12));
                rangee.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : ROW_ALT) + ";-fx-background-radius:6;");
                Label cle = new Label(lignes[i][0]); cle.setMinWidth(140);
                cle.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
                Label val = new Label(lignes[i][1] != null ? lignes[i][1] : "—");
                val.setWrapText(true); val.setMaxWidth(280);
                val.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEAL_DARK + ";-fx-font-weight:bold;");
                rangee.getChildren().addAll(cle, val); grille.getChildren().add(rangee);
            }
            corps.getChildren().add(grille);
        }

        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:" + BORDER_LIGHT + ";");
        corps.getChildren().add(sep);

        if (description != null && !description.isEmpty()) {
            VBox bloc = new VBox(6); bloc.setPadding(new Insets(14, 24, 10, 24));
            HBox titRow = new HBox(6, fi("fas-file-alt", 12, TEXT_SECOND),
                    labelStyled("Description", "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_SECOND + ";"));
            titRow.setAlignment(Pos.CENTER_LEFT);
            Label val = new Label(description); val.setWrapText(true); val.setMaxWidth(420);
            val.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";-fx-line-spacing:3;-fx-background-color:" + ROW_ALT + ";-fx-padding:10;-fx-background-radius:8;");
            bloc.getChildren().addAll(titRow, val); corps.getChildren().add(bloc);
        }
        if (reponseAdmin != null && !reponseAdmin.isEmpty()) {
            VBox bloc = new VBox(6); bloc.setPadding(new Insets(10, 24, 10, 24));
            HBox titRow = new HBox(6, fi("fas-comment-dots", 12, TEAL_MID),
                    labelStyled("Réponse de l'administration", "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_MID + ";"));
            titRow.setAlignment(Pos.CENTER_LEFT);
            Label val = new Label(reponseAdmin); val.setWrapText(true); val.setMaxWidth(420);
            val.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";-fx-line-spacing:3;-fx-background-color:#c8edf2;-fx-padding:10;-fx-background-radius:8;");
            bloc.getChildren().addAll(titRow, val); corps.getChildren().add(bloc);
        }
        if (dateResolution != null && !dateResolution.isEmpty()) {
            HBox row = new HBox(6, fi("fas-check-circle", 13, GREEN_SOFT),
                    labelStyled("Résolu le : " + dateResolution, "-fx-font-size:12px;-fx-text-fill:" + GREEN_SOFT + ";-fx-font-weight:bold;"));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 24, 12, 24));
            corps.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(corps);
        scroll.setFitToWidth(true); scroll.setMaxHeight(320);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");

        carte.getChildren().addAll(bandeTitre, ligne, enTete, scroll, zoneBoutonFermer(fenetre));
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    // ════════════════════════════════════════════════════════════════
    //  Notifications
    // ════════════════════════════════════════════════════════════════
    public static void afficherNotifications(List<Notification> notifs) {
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(640, 560);

        VBox carte = new VBox(0);
        carte.setMaxWidth(560);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:14;");

        // ── Bande titre ───────────────────────────────────────────
        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:14 14 0 0;");

        // fas-graduation-cap (couleur ICON_NAV comme dans le FXML étudiant)
        bande.getChildren().add(fi("fas-graduation-cap", 16, ICON_NAV));
        Label lSep = labelStyled("|", "-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        // fas-bell (couleur ICON_NAV comme navbar étudiant)
        bande.getChildren().addAll(lSep, fi("fas-bell", 15, ICON_NAV));
        Label lTit = labelStyled("Mes Notifications", "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        long nbNL = notifs.stream().filter(n -> !n.isLu()).count();
        Label lComp = new Label(notifs.size() + " notif" + (notifs.size() > 1 ? "s" : "") + (nbNL > 0 ? "  •  " + nbNL + " new" : ""));
        lComp.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:rgba(255,255,255,0.9);-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
        Label lApp = labelStyled("UNIV-SCHEDULER", "-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(lTit, esp, lComp, lApp, btnX);

        Region ligne = ligneDeco(TEAL_MID);

        // ── Sous-en-tête légende ───────────────────────────────────
        HBox sousEn = new HBox(12); sousEn.setAlignment(Pos.CENTER_LEFT);
        sousEn.setPadding(new Insets(10, 20, 10, 20));
        sousEn.setStyle("-fx-background-color:" + TEAL_BG + ";");
        Label lRes = labelStyled("Toutes les notifications sont marquées comme lues.", "-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";");
        Region espS = new Region(); HBox.setHgrow(espS, Priority.ALWAYS);
        HBox leg = new HBox(8); leg.setAlignment(Pos.CENTER_RIGHT);
        leg.getChildren().addAll(
                creerPillIcon("fas-check-circle",  "Succès", GREEN_BG, GREEN_TXT),
                creerPillIcon("fas-exclamation-triangle", "Alerte", RED_BG, RED_TXT),
                creerPillIcon("fas-bell", "Info", TEAL_BG, TEAL_DARK)
        );
        sousEn.getChildren().addAll(lRes, espS, leg);

        // ── Liste notifs ──────────────────────────────────────────
        VBox listeBox = new VBox(8); listeBox.setPadding(new Insets(14, 16, 14, 16));
        listeBox.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";");

        if (notifs.isEmpty()) {
            VBox vide = new VBox(10); vide.setAlignment(Pos.CENTER); vide.setPadding(new Insets(48));
            vide.getChildren().addAll(
                    fi("fas-bell-slash", 32, TEXT_MUTED),
                    labelStyled("Aucune notification pour le moment.", "-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";")
            );
            listeBox.getChildren().add(vide);
        } else {
            for (Notification n : notifs) {
                String bg, fg, bord, iconLiteral;
                switch (n.getType() != null ? n.getType() : "") {
                    case "INFO":   bg = GREEN_BG; fg = GREEN_TXT; bord = GREEN_BORDER; iconLiteral = "fas-check-circle";       break;
                    case "ALERTE": bg = RED_BG;   fg = RED_TXT;   bord = RED_BORDER;   iconLiteral = "fas-exclamation-triangle"; break;
                    default:       bg = TEAL_BG;  fg = TEAL_DARK; bord = BORDER_LIGHT; iconLiteral = "fas-bell";               break;
                }
                boolean nonLu = !n.isLu();
                HBox rangee = new HBox(10); rangee.setAlignment(Pos.CENTER_LEFT);
                rangee.setPadding(new Insets(11, 14, 11, 14));
                rangee.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10;-fx-border-color:" + bord + ";-fx-border-radius:10;-fx-border-width:" + (nonLu ? "1.5" : "0.8") + ";");
                if (nonLu) {
                    Region b = new Region(); b.setPrefWidth(3); b.setPrefHeight(44); b.setMinHeight(Region.USE_PREF_SIZE);
                    b.setStyle("-fx-background-color:" + fg + ";-fx-background-radius:3;");
                    rangee.getChildren().add(b);
                }
                // Icône FontIcon selon le type
                FontIcon icoN = fi(iconLiteral, 18, fg);
                VBox textes = new VBox(4); HBox.setHgrow(textes, Priority.ALWAYS);
                if (nonLu) {
                    Label bn = new Label("NOUVEAU");
                    bn.setStyle("-fx-background-color:" + fg + ";-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 7;-fx-background-radius:20;");
                    textes.getChildren().add(bn);
                }
                Label lMsg = new Label(n.getMessage()); lMsg.setWrapText(true); lMsg.setMaxWidth(400);
                lMsg.setStyle("-fx-font-size:12px;-fx-text-fill:" + fg + ";" + (nonLu ? "-fx-font-weight:bold;" : ""));
                String date = n.getDateEnvoi() != null ? n.getDateEnvoi().toLocalDate().toString() : "";
                HBox dateRow = new HBox(4, fi("fas-calendar-alt", 10, TEXT_MUTED),
                        labelStyled(date, "-fx-font-size:10px;-fx-text-fill:" + TEXT_MUTED + ";"));
                dateRow.setAlignment(Pos.CENTER_LEFT);
                textes.getChildren().addAll(lMsg, dateRow);
                rangee.getChildren().addAll(icoN, textes);
                listeBox.getChildren().add(rangee);
            }
        }

        ScrollPane scroll = new ScrollPane(listeBox);
        scroll.setFitToWidth(true); scroll.setMaxHeight(400);
        scroll.setPrefHeight(Math.min(notifs.size() * 82 + 28, 400));
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");

        carte.getChildren().addAll(bande, ligne, sousEn, scroll, zoneBoutonFermer(fenetre));
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    // ════════════════════════════════════════════════════════════════
    //  Alerte standard
    // ════════════════════════════════════════════════════════════════
    private static boolean afficher(Type type, String titre, String message, boolean avecAnnuler) {
        final boolean[] resultat = {false};
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(520, 320);

        VBox carte = new VBox(0); carte.setMaxWidth(430);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:" + type.fond + ";-fx-background-radius:14;");

        HBox bande = bandeTitre(type.iconLiteral,
                titre != null && !titre.isEmpty() ? titre : type.defTitre, fenetre, couleurBtn(type));
        ((Button) bande.getChildren().get(bande.getChildren().size() - 1))
                .setOnAction(e -> { resultat[0] = false; fenetre.close(); });

        Region ligne = ligneDeco(couleurBtn(type));

        HBox corps = new HBox(14); corps.setPadding(new Insets(22, 24, 10, 24)); corps.setAlignment(Pos.TOP_LEFT);
        Region barre = new Region(); barre.setPrefWidth(4); barre.setPrefHeight(55); barre.setMinHeight(Region.USE_PREF_SIZE);
        barre.setStyle("-fx-background-color:" + couleurBtn(type) + ";-fx-background-radius:4;");
        Label lblMsg = new Label(message); lblMsg.setWrapText(true); lblMsg.setMaxWidth(350);
        lblMsg.setTextAlignment(TextAlignment.LEFT);
        lblMsg.setStyle("-fx-font-size:13px;-fx-text-fill:" + type.txt + ";-fx-line-spacing:4;");
        corps.getChildren().addAll(barre, lblMsg);

        HBox boutons = new HBox(12); boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setPadding(new Insets(14, 24, 20, 24));
        boutons.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");
        String cp = couleurBtn(type);
        if (avecAnnuler) {
            Button bAnn = creerBoutonIcon("fas-arrow-left", "Annuler", TEAL_BG, TEAL_DARK, BORDER_LIGHT);
            bAnn.setOnAction(e -> { resultat[0] = false; fenetre.close(); });
            String okLabel = type == Type.ERREUR ? "Supprimer" : "Confirmer";
            String okIcon  = type == Type.ERREUR ? "fas-trash-alt" : "fas-check";
            Button bOk = creerBoutonIcon(okIcon, okLabel, cp, ICON_WHITE, assombrir(cp));
            bOk.setOnAction(e -> { resultat[0] = true; fenetre.close(); });
            boutons.getChildren().addAll(bAnn, bOk);
        } else {
            Button bOk = creerBoutonIcon("fas-check", "OK", cp, ICON_WHITE, assombrir(cp));
            bOk.setMinWidth(100);
            bOk.setOnAction(e -> { resultat[0] = true; fenetre.close(); });
            boutons.getChildren().add(bOk);
        }

        carte.getChildren().addAll(bande, ligne, corps, boutons);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
        return resultat[0];
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers partagés
    // ════════════════════════════════════════════════════════════════

    /** Bande de titre commune — icône FontIcon + texte + bouton X */
    private static HBox bandeTitre(String iconLiteral, String titre, Stage fenetre, String iconColor) {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 20, 14, 20));
        h.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:14 14 0 0;");

        // fas-graduation-cap (comme navbar FXML étudiant, couleur ICON_NAV)
        h.getChildren().add(fi("fas-graduation-cap", 16, ICON_NAV));
        h.getChildren().add(labelStyled("|", "-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;"));
        // Icône contexte (couleur passée en paramètre — pour la lisibilité sur fond dark)
        h.getChildren().add(fi(iconLiteral, 15, ICON_NAV));

        Label lTit = labelStyled(titre, "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label lApp = labelStyled("UNIV-SCHEDULER", "-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        h.getChildren().addAll(lTit, esp, lApp, btnX);
        return h;
    }

    /** Bouton fermer X (fas-times, style rond translucide) */
    private static Button btnFermerX() {
        Button b = new Button();
        b.setGraphic(fi("fas-times", 10, ICON_WHITE));
        String base = "-fx-background-color:rgba(255,255,255,0.15);-fx-background-radius:50;-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> { b.setStyle(base.replace("0.15", "0.35")); });
        b.setOnMouseExited (e -> { b.setStyle(base.replace("0.35", "0.15")); });
        return b;
    }

    private static Region ligneDeco(String couleur) {
        Region r = new Region(); r.setPrefHeight(3);
        r.setStyle("-fx-background-color:" + couleur + ";"); return r;
    }

    /** Zone bouton fermer en bas de carte */
    private static HBox zoneBoutonFermer(Stage fenetre) {
        HBox z = new HBox(); z.setAlignment(Pos.CENTER_RIGHT);
        z.setPadding(new Insets(12, 24, 18, 24));
        z.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");
        Button btn = creerBoutonIcon("fas-times", "Fermer", TEAL_DARK, ICON_WHITE, "#0f3d48");
        btn.setOnAction(e -> fenetre.close()); z.getChildren().add(btn); return z;
    }

    /** Grille d'informations (clé / valeur) */
    private static VBox grilleInfos(String[][] infos) {
        VBox g = new VBox(0);
        g.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-border-color:" + BORDER_LIGHT + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < infos.length; i++) {
            HBox r = new HBox(0); r.setPadding(new Insets(10, 14, 10, 14));
            r.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : ROW_ALT) + ";");
            Label c = new Label(infos[i][0]); c.setMinWidth(80);
            c.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
            Label v = new Label(infos[i][1]); v.setWrapText(true); v.setMaxWidth(280);
            v.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEAL_DARK + ";-fx-font-weight:bold;");
            r.getChildren().addAll(c, v); g.getChildren().add(r);
        }
        return g;
    }

    private static DropShadow ombreCarte() {
        DropShadow d = new DropShadow();
        d.setRadius(36); d.setOffsetY(10); d.setColor(Color.color(0, 0, 0, 0.30)); return d;
    }

    private static void animerEtAfficher(VBox carte, Stage fenetre) {
        carte.setOpacity(0); carte.setTranslateY(-14);
        FadeTransition ft = new FadeTransition(Duration.millis(220), carte);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), carte);
        tt.setFromY(-14); tt.setToY(0); tt.play();
        Scene scene = new Scene((StackPane) carte.getParent());
        scene.setFill(Color.TRANSPARENT);
        fenetre.setScene(scene);
        fenetre.showAndWait();
    }

    /** Bouton stylé avec FontIcon + texte */
    private static Button creerBoutonIcon(String iconLiteral, String texte, String bg, String fg, String hover) {
        Button b = new Button(texte);
        b.setGraphic(fi(iconLiteral, 12, fg));
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-weight:bold;-fx-padding:9 22;-fx-background-radius:10;-fx-cursor:hand;-fx-font-size:13px;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> { b.setStyle(base.replace(bg, hover)); b.setGraphic(fi(iconLiteral, 12, ICON_WHITE)); });
        b.setOnMouseExited (e -> { b.setStyle(base); b.setGraphic(fi(iconLiteral, 12, fg)); });
        return b;
    }

    /** Pill (badge) avec FontIcon */
    private static HBox creerPillIcon(String iconLiteral, String texte, String bg, String fg) {
        HBox h = new HBox(4, fi(iconLiteral, 10, fg), labelStyled(texte, "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + fg + ";"));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color:" + bg + ";-fx-padding:3 9;-fx-background-radius:20;");
        return h;
    }

    private static Label labelStyled(String text, String style) {
        Label l = new Label(text); l.setStyle(style); return l;
    }

    private static String assombrir(String hex) {
        try {
            Color c = Color.web(hex);
            return String.format("#%02x%02x%02x",
                    (int)(c.getRed()*0.82*255), (int)(c.getGreen()*0.82*255), (int)(c.getBlue()*0.82*255));
        } catch (Exception e) { return hex; }
    }

    // ════════════════════════════════════════════════════════════════
    //  ALERTES EXAMENS & DEVOIRS
    // ════════════════════════════════════════════════════════════════

    /** ✅ Alerte succès après soumission d'un examen/devoir */
    public static void examenSoumisAvecSucces(
            String type, String titre, String classeNom,
            String matiereNom, String dateExamen, String heure,
            String salleNumero, int dureeMinutes) {

        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(560, 500);

        VBox carte = new VBox(0);
        carte.setMaxWidth(480);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:white;-fx-background-radius:18;");

        // Bande
        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:18 18 0 0;");
        bande.getChildren().add(fi("fas-graduation-cap", 16, ICON_NAV));
        bande.getChildren().add(labelStyled("|", "-fx-text-fill:rgba(255,255,255,0.30);"));
        String typeIconLiteral = "EXAMEN".equals(type) ? "fas-clipboard-list" : "DEVOIR".equals(type) ? "fas-pencil-alt" : "fas-pen";
        bande.getChildren().add(fi(typeIconLiteral, 13, TEAL_LIGHT));
        Label lTypeLabel = labelStyled(type, "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_LIGHT + ";");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label lApp = labelStyled("UNIV-SCHEDULER", "-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.50);-fx-font-weight:bold;");
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(lTypeLabel, esp, lApp, btnX);

        // Ligne verte
        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + GREEN_SOFT + ";");

        // Zone succès
        VBox zoneSucces = new VBox(8); zoneSucces.setAlignment(Pos.CENTER);
        zoneSucces.setPadding(new Insets(22, 24, 16, 24));
        zoneSucces.setStyle("-fx-background-color:#f0fdf4;");

        // Cercle check (fas-check-circle dans cercle vert)
        StackPane cercle = iconInCircle("fas-check-circle", 28, GREEN_SOFT, 34, GREEN_BG, GREEN_SOFT);
        cercle.setScaleX(0.3); cercle.setScaleY(0.3);
        javafx.animation.ScaleTransition pop = new javafx.animation.ScaleTransition(Duration.millis(320), cercle);
        pop.setFromX(0.3); pop.setFromY(0.3); pop.setToX(1.0); pop.setToY(1.0);
        pop.setInterpolator(javafx.animation.Interpolator.SPLINE(0.175, 0.885, 0.320, 1.275));
        pop.play();

        Label titreSucces = labelStyled("Demande soumise avec succès !", "-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_TXT + ";");
        Label sousTitre = new Label("Le gestionnaire a été notifié et traitera votre demande.");
        sousTitre.setWrapText(true); sousTitre.setTextAlignment(TextAlignment.CENTER);
        sousTitre.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";");
        zoneSucces.getChildren().addAll(cercle, titreSucces, sousTitre);

        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:" + BORDER_LIGHT + ";");

        // Récapitulatif
        VBox recap = new VBox(0); recap.setPadding(new Insets(14, 24, 10, 24));
        HBox titRecapRow = new HBox(6, fi("fas-list-alt", 12, TEXT_SECOND),
                labelStyled("Récapitulatif de la demande", "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_SECOND + ";"));
        titRecapRow.setAlignment(Pos.CENTER_LEFT);
        titRecapRow.setPadding(new Insets(0, 0, 8, 0));

        String salle = (salleNumero != null && !salleNumero.isBlank()) ? salleNumero : "Devoir à la maison";
        String[][] lignes = {
                {"Type",    type    != null ? type    : "—"},
                {"Titre",   titre   != null ? titre   : "—"},
                {"Classe",  classeNom  != null ? classeNom  : "—"},
                {"Matière", matiereNom != null ? matiereNom : "—"},
                {"Date",    (dateExamen != null ? dateExamen : "—") + " à " + (heure != null ? heure : "—")},
                {"Durée",   dureeMinutes + " min"},
                {"Salle",   salle},
        };
        VBox grille = new VBox(0);
        grille.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-border-color:" + BORDER_LIGHT + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < lignes.length; i++) {
            HBox rangee = new HBox(0); rangee.setPadding(new Insets(9, 14, 9, 14));
            rangee.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : ROW_ALT) + ";"
                    + (i == 0 ? "-fx-background-radius:10 10 0 0;" : "")
                    + (i == lignes.length - 1 ? "-fx-background-radius:0 0 10 10;" : ""));
            Label cle = new Label(lignes[i][0]); cle.setMinWidth(100);
            cle.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
            Label val = new Label(lignes[i][1]); val.setWrapText(true); val.setMaxWidth(290);
            val.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEAL_DARK + ";-fx-font-weight:bold;");
            rangee.getChildren().addAll(cle, val); grille.getChildren().add(rangee);
        }
        recap.getChildren().addAll(titRecapRow, grille);

        // Info badge
        HBox infoBadge = new HBox(8); infoBadge.setAlignment(Pos.CENTER_LEFT);
        infoBadge.setPadding(new Insets(10, 24, 4, 24));
        infoBadge.getChildren().addAll(
                fi("fas-bell", 14, TEAL_MID),
                new Label("Vous recevrez une notification dès validation par le gestionnaire.") {{
                    setWrapText(true); setMaxWidth(380);
                    setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-style:italic;");
                }}
        );

        // Bouton fermer
        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:0 0 18 18;");
        Button btnFermer = creerBoutonIcon("fas-check", "Parfait !", GREEN_SOFT, ICON_WHITE, "#2aaf72");
        btnFermer.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnFermer);

        carte.getChildren().addAll(bande, ligne, zoneSucces, sep, recap, infoBadge, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    /** ⚠️ Alerte champs manquants */
    public static void examenChampsManquants(List<String> champsManquants) {
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(480, 380);

        VBox carte = new VBox(0);
        carte.setMaxWidth(420);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:white;-fx-background-radius:18;");

        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:18 18 0 0;");
        bande.getChildren().addAll(
                fi("fas-graduation-cap", 16, ICON_NAV),
                labelStyled("|", "-fx-text-fill:rgba(255,255,255,0.30);"),
                fi("fas-exclamation-triangle", 14, GOLD),
                labelStyled("Champs manquants", "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;")
        );
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(esp, btnX);

        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + GOLD + ";");

        VBox zoneWarn = new VBox(8); zoneWarn.setAlignment(Pos.CENTER);
        zoneWarn.setPadding(new Insets(20, 24, 14, 24));
        zoneWarn.setStyle("-fx-background-color:" + GOLD_BG + ";");

        StackPane cercle = iconInCircle("fas-exclamation-triangle", 22, GOLD, 30, GOLD_BG, GOLD);
        Label titWarn = labelStyled("Formulaire incomplet", "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + GOLD_TXT + ";");
        Label sousWarn = labelStyled("Veuillez remplir les champs obligatoires (*) :", "-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";");
        zoneWarn.getChildren().addAll(cercle, titWarn, sousWarn);

        VBox listeCh = new VBox(6); listeCh.setPadding(new Insets(14, 24, 14, 24));
        for (String champ : champsManquants) {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color:" + RED_BG + ";-fx-background-radius:8;-fx-border-color:" + RED_BORDER + ";-fx-border-radius:8;-fx-border-width:1;");
            row.getChildren().addAll(fi("fas-circle", 8, RED_SOFT), labelStyled(champ, "-fx-font-size:13px;-fx-text-fill:" + RED_TXT + ";-fx-font-weight:bold;"));
            listeCh.getChildren().add(row);
        }

        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 18 18;");
        Button btnOk = creerBoutonIcon("fas-edit", "Corriger les champs", TEAL_MID, ICON_WHITE, TEAL_DARK);
        btnOk.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnOk);

        carte.getChildren().addAll(bande, ligne, zoneWarn, listeCh, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    /** 🔴 Alerte conflit de salle */
    public static void examenConflitSalle(String salleNumero, String dateExamen, String heure) {
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(500, 380);

        VBox carte = new VBox(0);
        carte.setMaxWidth(430);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:white;-fx-background-radius:18;");

        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:18 18 0 0;");
        bande.getChildren().addAll(
                fi("fas-graduation-cap", 16, ICON_NAV),
                labelStyled("|", "-fx-text-fill:rgba(255,255,255,0.30);"),
                fi("fas-times-circle", 14, RED_SOFT),
                labelStyled("Conflit de Salle", "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;")
        );
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(esp, btnX);

        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + RED_SOFT + ";");

        VBox zoneErr = new VBox(10); zoneErr.setAlignment(Pos.CENTER);
        zoneErr.setPadding(new Insets(22, 24, 16, 24));
        zoneErr.setStyle("-fx-background-color:" + RED_BG + ";");

        StackPane cercle = iconInCircle("fas-times-circle", 26, RED_SOFT, 32, RED_BG, RED_SOFT);
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), cercle);
        shake.setFromX(-6); shake.setToX(6); shake.setCycleCount(6); shake.setAutoReverse(true); shake.play();

        Label titErr = labelStyled("Salle déjà occupée !", "-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + RED_TXT + ";");
        Label sousErr = labelStyled("La salle est indisponible pour ce créneau.", "-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";");
        zoneErr.getChildren().addAll(cercle, titErr, sousErr);

        VBox details = new VBox(0); details.setPadding(new Insets(14, 24, 14, 24));
        String[][] infos = {
                {"Salle", salleNumero != null ? salleNumero : "—"},
                {"Date",  (dateExamen != null ? dateExamen : "—") + " à " + (heure != null ? heure : "—")},
        };
        VBox grille = new VBox(0);
        grille.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-border-color:" + RED_BORDER + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < infos.length; i++) {
            HBox row = new HBox(0); row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : "#fff5f5") + ";"
                    + (i == 0 ? "-fx-background-radius:10 10 0 0;" : "-fx-background-radius:0 0 10 10;"));
            Label c = new Label(infos[i][0]); c.setMinWidth(80);
            c.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
            Label v = new Label(infos[i][1]);
            v.setStyle("-fx-font-size:12px;-fx-text-fill:" + RED_TXT + ";-fx-font-weight:bold;");
            row.getChildren().addAll(c, v); grille.getChildren().add(row);
        }
        HBox conseil = new HBox(8); conseil.setAlignment(Pos.CENTER_LEFT);
        conseil.setPadding(new Insets(10, 0, 0, 0));
        conseil.getChildren().addAll(fi("fas-lightbulb", 14, GOLD),
                labelStyled("Choisissez une autre salle ou une autre date/heure.", "-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-style:italic;"));
        details.getChildren().addAll(grille, conseil);

        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 18 18;");
        Button btnOk = creerBoutonIcon("fas-edit", "Modifier le formulaire", RED_SOFT, ICON_WHITE, "#c44040");
        btnOk.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnOk);

        carte.getChildren().addAll(bande, ligne, zoneErr, details, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    /** 📅 Alerte date passée */
    public static void examenDatePassee(String dateChoisie) {
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(460, 320);

        VBox carte = new VBox(0);
        carte.setMaxWidth(400);
        carte.setEffect(ombreCarte());
        carte.setStyle("-fx-background-color:white;-fx-background-radius:18;");

        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:18 18 0 0;");
        bande.getChildren().addAll(
                fi("fas-graduation-cap", 16, ICON_NAV),
                labelStyled("|", "-fx-text-fill:rgba(255,255,255,0.30);"),
                fi("fas-calendar-times", 14, GOLD),
                labelStyled("Date invalide", "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;")
        );
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(esp, btnX);

        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + GOLD + ";");

        VBox corps = new VBox(14); corps.setAlignment(Pos.CENTER);
        corps.setPadding(new Insets(28, 28, 20, 28));
        corps.setStyle("-fx-background-color:" + GOLD_BG + ";");

        corps.getChildren().addAll(
                fi("fas-calendar-times", 40, GOLD),
                labelStyled("Date dans le passé", "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + GOLD_TXT + ";"),
                new Label("La date sélectionnée (" + (dateChoisie != null ? dateChoisie : "—")
                        + ") est déjà passée.\nVeuillez choisir une date future.") {{
                    setWrapText(true); setTextAlignment(TextAlignment.CENTER);
                    setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_SECOND + ";");
                }}
        );

        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 18 18;");
        Button btnOk = creerBoutonIcon("fas-calendar-alt", "Choisir une autre date", GOLD, ICON_WHITE, "#c87f00");
        btnOk.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnOk);

        carte.getChildren().addAll(bande, ligne, corps, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }
}