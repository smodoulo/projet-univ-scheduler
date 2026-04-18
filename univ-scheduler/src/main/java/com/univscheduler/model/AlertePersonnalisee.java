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
import java.util.List;

/**
 * Alertes personnalisées UNIV-SCHEDULER — palette teal
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

    // ════════════════════════════════════════════════════════════════
    public enum Type {
        SUCCES  (TEAL_DARK, TEAL_BG,  TEAL_DARK, "✅", "Succès"),
        ERREUR  (RED_SOFT,  RED_BG,   RED_TXT,   "❌", "Erreur"),
        INFO    (TEAL_DARK, TEAL_BG,  TEAL_DARK, "ℹ",  "Information"),
        WARN    (GOLD,      GOLD_BG,  GOLD_TXT,  "⚠",  "Avertissement"),
        QUESTION(TEAL_DARK, TEAL_BG,  TEAL_DARK, "❓", "Confirmation");

        final String pri, fond, txt, icone, defTitre;
        Type(String p, String f, String t, String i, String d) {
            pri=p; fond=f; txt=t; icone=i; defTitre=d;
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
    //  ✅ CHATBOT INTEGRE
    // ════════════════════════════════════════════════════════════════

    /**
     * Ouvre la fenêtre chatbot flottante.
     * Usage : AlertePersonnalisee.ouvrirChatbot(currentUser.getNomComplet());
     */
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

        // ── En-tête teal-dark ─────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:20 20 0 0;-fx-cursor:move;");

        StackPane avatarPane = new StackPane();
        Circle avatarBg = new Circle(18);
        avatarBg.setFill(Color.web(TEAL_LIGHT, 0.20));
        avatarBg.setStroke(Color.web(TEAL_LIGHT));
        avatarBg.setStrokeWidth(1.5);
        Label avatarLbl = new Label("🤖"); avatarLbl.setStyle("-fx-font-size:15px;");
        avatarPane.getChildren().addAll(avatarBg, avatarLbl);

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Assistant UNIV-SCHEDULER");
        titleLbl.setStyle("-fx-text-fill:#f0f9fa;-fx-font-size:13px;-fx-font-weight:bold;");
        HBox statusRow = new HBox(5);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Circle statusDot = new Circle(4);
        statusDot.setFill(Color.web(GREEN_SOFT));
        statusDot.setEffect(new DropShadow(4, Color.web(GREEN_SOFT, 0.6)));
        Label statusLbl = new Label("En ligne");
        statusLbl.setStyle("-fx-text-fill:" + TEAL_LIGHT + ";-fx-font-size:10px;");
        statusRow.getChildren().addAll(statusDot, statusLbl);
        titleBox.getChildren().addAll(titleLbl, statusRow);

        Region espH = new Region(); HBox.setHgrow(espH, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + TEXT_MUTED + ";" +
                        "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:4 8;-fx-background-radius:8;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnClose.getStyle()
                .replace("transparent", RED_SOFT).replace(TEXT_MUTED, "white")));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(btnClose.getStyle()
                .replace(RED_SOFT, "transparent").replace("white", TEXT_MUTED)));
        btnClose.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setToValue(0); ft.setOnFinished(ev -> fenetre.close()); ft.play();
        });

        header.getChildren().addAll(avatarPane, titleBox, espH, btnClose);

        // Drag
        final double[] drag = {0, 0};
        header.setOnMousePressed(e  -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        header.setOnMouseDragged(e  -> {
            fenetre.setX(e.getScreenX() - drag[0]);
            fenetre.setY(e.getScreenY() - drag[1]);
        });

        // ── Zone messages ─────────────────────────────────────────
        VBox messagesBox = new VBox(10);
        messagesBox.setPadding(new Insets(14, 16, 14, 16));
        messagesBox.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";");

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true); scrollPane.setPrefHeight(280);
        scrollPane.setStyle("-fx-background-color:" + TEAL_BG_SOFT +
                ";-fx-border-width:0;-fx-background:" + TEAL_BG_SOFT + ";");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        ajouterMsgBot(messagesBox, scrollPane,
                "Bonjour " + nomUtilisateur + " !\nJe suis votre assistant UNIV-SCHEDULER. Comment puis-je vous aider ?");

        // ── Suggestions rapides ───────────────────────────────────
        HBox suggestions = new HBox(6);
        suggestions.setPadding(new Insets(8, 10, 8, 10));
        suggestions.setStyle("-fx-background-color:" + TEAL_BG +
                ";-fx-border-color:" + BORDER_LIGHT + ";-fx-border-width:1 0 0 0;");
        for (String s : new String[]{"📋 Cours", "🏫 Salles", "📅 Planning", "⚠ Conflits"}) {
            Button btn = creerBtnSuggest(s);
            btn.setOnAction(e -> {
                ajouterMsgUser(messagesBox, scrollPane, s);
                Platform.runLater(() -> repondreBot(messagesBox, scrollPane, s, nomUtilisateur));
            });
            suggestions.getChildren().add(btn);
        }

        // ── Barre saisie ──────────────────────────────────────────
        HBox inputBar = new HBox(8);
        inputBar.setPadding(new Insets(10, 12, 10, 12));
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER_LIGHT +
                ";-fx-border-width:1 0 0 0;-fx-background-radius:0 0 20 20;");

        TextField inputField = new TextField();
        inputField.setPromptText("Posez votre question...");
        inputField.setStyle(
                "-fx-background-color:" + TEAL_BG_SOFT +
                        ";-fx-border-color:transparent;-fx-border-width:0;" +
                        "-fx-border-radius:20;-fx-background-radius:20;-fx-padding:10 16;" +
                        "-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button btnSend = new Button("➤");
        btnSend.setStyle(
                "-fx-background-color:" + TEAL_MID +
                        ";-fx-text-fill:white;-fx-font-size:14px;-fx-background-radius:50%;" +
                        "-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;" +
                        "-fx-cursor:hand;-fx-padding:0;");
        btnSend.setOnMouseEntered(e -> btnSend.setStyle(btnSend.getStyle().replace(TEAL_MID, TEAL_DARK)));
        btnSend.setOnMouseExited(e  -> btnSend.setStyle(btnSend.getStyle().replace(TEAL_DARK, TEAL_MID)));

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
        msg.setStyle(
                "-fx-background-color:" + TEAL_MID +
                        ";-fx-text-fill:white;-fx-padding:10 14;-fx-background-radius:18 4 18 18;-fx-font-size:13px;");
        row.getChildren().add(msg);
        box.getChildren().add(row);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private static void ajouterMsgBot(VBox box, ScrollPane scroll, String texte) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖"); avatar.setStyle("-fx-font-size:16px;");
        Label msg = new Label(texte);
        msg.setWrapText(true); msg.setMaxWidth(270);
        msg.setStyle(
                "-fx-background-color:white;-fx-text-fill:" + TEAL_DARK +
                        ";-fx-padding:10 14;-fx-background-radius:4 18 18 18;-fx-font-size:13px;" +
                        "-fx-border-color:" + BORDER_LIGHT + ";-fx-border-width:1;-fx-border-radius:4 18 18 18;");
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
        if (q.contains("cours") || q.contains("📋"))
            return "📚 Gérez les cours dans l'onglet \"Emploi du Temps\".\nAjout, modification, suppression et export disponibles.";
        if (q.contains("salle") || q.contains("🏫"))
            return "🏫 La carte des salles est dans l'onglet \"Carte Salles\".\nTaux d'occupation en temps réel par bâtiment.";
        if (q.contains("planning") || q.contains("calendrier") || q.contains("📅"))
            return "📅 Le calendrier est dans l'onglet \"Vue Calendrier\".\nVue semaine ou mois disponible.";
        if (q.contains("conflit") || q.contains("⚠"))
            return "⚠ Les conflits sont détectés automatiquement lors de la création d'un cours.\nConflit salle ou enseignant signalé en rouge.";
        if (q.contains("rapport") || q.contains("statistique"))
            return "📈 Les rapports sont dans l'onglet \"Rapports\".\nHebdomadaire, mensuel, graphiques et export Excel.";
        if (q.contains("réservation") || q.contains("reserv"))
            return "📌 Les réservations se valident dans l'onglet \"Réservations\".\nAcceptation ou refus avec notification automatique.";
        if (q.contains("bonjour") || q.contains("salut") || q.contains("hello"))
            return "Bonjour " + nom + " ! Comment puis-je vous aider aujourd'hui ?";
        if (q.contains("merci"))
            return "Avec plaisir ! N'hésitez pas si vous avez d'autres questions.";
        return "Je n'ai pas bien compris votre question.\nEssayez : \"cours\", \"salles\", \"planning\", \"conflits\"...";
    }

    private static Button creerBtnSuggest(String texte) {
        Button b = new Button(texte);
        String base =
                "-fx-background-color:white;-fx-text-fill:" + TEAL_MID +
                        ";-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 10;-fx-background-radius:20;" +
                        "-fx-border-color:#a8e6df;-fx-border-width:1;-fx-border-radius:20;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace("white", TEAL_MID).replace(TEAL_MID + ";", "white;")));
        b.setOnMouseExited(e  -> b.setStyle(base));
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

        HBox bandeTitre = bandeTitre("❌", "Annuler ce cours", fenetre);
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
        champMotif.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER_LIGHT +
                ";-fx-border-radius:8;-fx-background-radius:8;-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";");
        Label lblErreur = new Label("⚠ Veuillez saisir un motif.");
        lblErreur.setStyle("-fx-text-fill:" + RED_SOFT + ";-fx-font-size:11px;");
        lblErreur.setVisible(false);
        blocMotif.getChildren().addAll(lblMotif, champMotif, lblErreur);
        corps.getChildren().add(blocMotif);

        HBox boutons = new HBox(12); boutons.setAlignment(Pos.CENTER_RIGHT);
        boutons.setPadding(new Insets(14, 24, 20, 24));
        boutons.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");
        Button btnAnnuler = creerBouton("Annuler", TEAL_BG, TEAL_DARK, BORDER_LIGHT);
        btnAnnuler.setOnAction(e -> fenetre.close());
        Button btnConfirmer = creerBouton("❌ Confirmer l'annulation", RED_SOFT, "white", "#c94040");
        btnConfirmer.setOnAction(e -> {
            String m = champMotif.getText().trim();
            if (m.isEmpty()) { lblErreur.setVisible(true); return; }
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
        HBox bandeTitre = bandeTitre("📋", titreBande, fenetre);
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
            badge.setStyle("-fx-background-color:" + couleurStatut + ";-fx-text-fill:white;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
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
            Label tit = new Label("📝 Description"); tit.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_SECOND + ";");
            Label val = new Label(description); val.setWrapText(true); val.setMaxWidth(420);
            val.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";-fx-line-spacing:3;-fx-background-color:" + ROW_ALT + ";-fx-padding:10;-fx-background-radius:8;");
            bloc.getChildren().addAll(tit, val); corps.getChildren().add(bloc);
        }
        if (reponseAdmin != null && !reponseAdmin.isEmpty()) {
            VBox bloc = new VBox(6); bloc.setPadding(new Insets(10, 24, 10, 24));
            Label tit = new Label("💬 Réponse de l'administration"); tit.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_MID + ";");
            Label val = new Label(reponseAdmin); val.setWrapText(true); val.setMaxWidth(420);
            val.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";-fx-line-spacing:3;-fx-background-color:#c8edf2;-fx-padding:10;-fx-background-radius:8;");
            bloc.getChildren().addAll(tit, val); corps.getChildren().add(bloc);
        }
        if (dateResolution != null && !dateResolution.isEmpty()) {
            Label l = new Label("✅ Résolu le : " + dateResolution);
            l.setPadding(new Insets(6, 24, 12, 24));
            l.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREEN_SOFT + ";-fx-font-weight:bold;");
            corps.getChildren().add(l);
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

        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:14 14 0 0;");
        Label lLogo=new Label("🎓"); lLogo.setStyle("-fx-font-size:16px;");
        Label lSep=new Label("|"); lSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lIco=new Label("🔔"); lIco.setStyle("-fx-font-size:16px;");
        Label lTit=new Label("Mes Notifications"); lTit.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp=new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        long nbNL=notifs.stream().filter(n->!n.isLu()).count();
        Label lComp=new Label(notifs.size()+" notif"+(notifs.size()>1?"s":"")+(nbNL>0?"  •  🆕 "+nbNL:""));
        lComp.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:rgba(255,255,255,0.9);-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
        Label lApp=new Label("UNIV-SCHEDULER"); lApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX=btnFermerX(); btnX.setOnAction(e->fenetre.close());
        bande.getChildren().addAll(lLogo,lSep,lIco,lTit,esp,lComp,lApp,btnX);

        Region ligne=ligneDeco(TEAL_MID);

        HBox sousEn=new HBox(12); sousEn.setAlignment(Pos.CENTER_LEFT);
        sousEn.setPadding(new Insets(10,20,10,20));
        sousEn.setStyle("-fx-background-color:"+TEAL_BG+";");
        Label lRes=new Label("Toutes les notifications sont marquées comme lues.");
        lRes.setStyle("-fx-font-size:11px;-fx-text-fill:"+TEXT_SECOND+";");
        Region espS=new Region(); HBox.setHgrow(espS,Priority.ALWAYS);
        HBox leg=new HBox(8); leg.setAlignment(Pos.CENTER_RIGHT);
        leg.getChildren().addAll(creerPill("✅ Succès",GREEN_BG,GREEN_TXT),creerPill("⚠️ Alerte",RED_BG,RED_TXT),creerPill("🔔 Info",TEAL_BG,TEAL_DARK));
        sousEn.getChildren().addAll(lRes,espS,leg);

        VBox listeBox=new VBox(8); listeBox.setPadding(new Insets(14,16,14,16));
        listeBox.setStyle("-fx-background-color:"+TEAL_BG_SOFT+";");

        if(notifs.isEmpty()){
            VBox vide=new VBox(10); vide.setAlignment(Pos.CENTER); vide.setPadding(new Insets(48));
            Label iv=new Label("🔕"); iv.setStyle("-fx-font-size:32px;");
            Label mv=new Label("Aucune notification pour le moment."); mv.setStyle("-fx-font-size:13px;-fx-text-fill:"+TEXT_MUTED+";");
            vide.getChildren().addAll(iv,mv); listeBox.getChildren().add(vide);
        } else {
            for(Notification n:notifs){
                String bg,fg,bord,icone;
                switch(n.getType()!=null?n.getType():""){
                    case"INFO":  bg=GREEN_BG; fg=GREEN_TXT; bord=GREEN_BORDER; icone="✅"; break;
                    case"ALERTE":bg=RED_BG;   fg=RED_TXT;   bord=RED_BORDER;   icone="⚠️"; break;
                    default:     bg=TEAL_BG;  fg=TEAL_DARK; bord=BORDER_LIGHT; icone="🔔"; break;
                }
                boolean nonLu=!n.isLu();
                HBox rangee=new HBox(10); rangee.setAlignment(Pos.CENTER_LEFT);
                rangee.setPadding(new Insets(11,14,11,14));
                rangee.setStyle("-fx-background-color:"+bg+";-fx-background-radius:10;-fx-border-color:"+bord+";-fx-border-radius:10;-fx-border-width:"+(nonLu?"1.5":"0.8")+";");
                if(nonLu){Region b=new Region();b.setPrefWidth(3);b.setPrefHeight(44);b.setMinHeight(Region.USE_PREF_SIZE);b.setStyle("-fx-background-color:"+fg+";-fx-background-radius:3;");rangee.getChildren().add(b);}
                Label lIcN=new Label(icone); lIcN.setStyle("-fx-font-size:18px;-fx-min-width:22;");
                VBox textes=new VBox(4); HBox.setHgrow(textes,Priority.ALWAYS);
                if(nonLu){Label bn=new Label("NOUVEAU");bn.setStyle("-fx-background-color:"+fg+";-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 7;-fx-background-radius:20;");textes.getChildren().add(bn);}
                Label lMsg=new Label(n.getMessage()); lMsg.setWrapText(true); lMsg.setMaxWidth(400);
                lMsg.setStyle("-fx-font-size:12px;-fx-text-fill:"+fg+";"+(nonLu?"-fx-font-weight:bold;":""));
                String date=n.getDateEnvoi()!=null?n.getDateEnvoi().toLocalDate().toString():"";
                Label lDate=new Label("📅 "+date); lDate.setStyle("-fx-font-size:10px;-fx-text-fill:"+TEXT_MUTED+";");
                textes.getChildren().addAll(lMsg,lDate);
                rangee.getChildren().addAll(lIcN,textes); listeBox.getChildren().add(rangee);
            }
        }

        ScrollPane scroll=new ScrollPane(listeBox);
        scroll.setFitToWidth(true); scroll.setMaxHeight(400);
        scroll.setPrefHeight(Math.min(notifs.size()*82+28,400));
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");

        carte.getChildren().addAll(bande,ligne,sousEn,scroll,zoneBoutonFermer(fenetre));
        overlay.getChildren().add(carte);
        animerEtAfficher(carte,fenetre);
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

        HBox bande = bandeTitre(type.icone, titre != null && !titre.isEmpty() ? titre : type.defTitre, fenetre);
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
            Button bAnn = creerBouton("Annuler", TEAL_BG, TEAL_DARK, BORDER_LIGHT);
            bAnn.setOnAction(e -> { resultat[0] = false; fenetre.close(); });
            Button bOk = creerBouton(type == Type.ERREUR ? "🗑 Supprimer" : "✔ Confirmer", cp, "white", assombrir(cp));
            bOk.setOnAction(e -> { resultat[0] = true; fenetre.close(); });
            boutons.getChildren().addAll(bAnn, bOk);
        } else {
            Button bOk = creerBouton("  OK  ", cp, "white", assombrir(cp));
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

    private static HBox bandeTitre(String icone, String titre, Stage fenetre) {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 20, 14, 20));
        h.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:14 14 0 0;");
        Label lLogo=new Label("🎓"); lLogo.setStyle("-fx-font-size:16px;");
        Label lSep=new Label("|");   lSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lIco=new Label(icone); lIco.setStyle("-fx-font-size:16px;");
        Label lTit=new Label(titre); lTit.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp=new Region(); HBox.setHgrow(esp,Priority.ALWAYS);
        Label lApp=new Label("UNIV-SCHEDULER"); lApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX=btnFermerX(); btnX.setOnAction(e->fenetre.close());
        h.getChildren().addAll(lLogo,lSep,lIco,lTit,esp,lApp,btnX);
        return h;
    }

    private static Button btnFermerX() {
        Button b = new Button("✕");
        String base = "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-background-radius:50;-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace("0.15", "0.35")));
        b.setOnMouseExited (e -> b.setStyle(base.replace("0.35", "0.15")));
        return b;
    }

    private static Region ligneDeco(String couleur) {
        Region r = new Region(); r.setPrefHeight(3);
        r.setStyle("-fx-background-color:" + couleur + ";"); return r;
    }

    private static HBox zoneBoutonFermer(Stage fenetre) {
        HBox z = new HBox(); z.setAlignment(Pos.CENTER_RIGHT);
        z.setPadding(new Insets(12, 24, 18, 24));
        z.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");
        Button btn = creerBouton("  Fermer  ", TEAL_DARK, "white", "#0f3d48");
        btn.setOnAction(e -> fenetre.close()); z.getChildren().add(btn); return z;
    }

    private static VBox grilleInfos(String[][] infos) {
        VBox g = new VBox(0);
        g.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-border-color:" + BORDER_LIGHT + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < infos.length; i++) {
            HBox r = new HBox(0); r.setPadding(new Insets(10, 14, 10, 14));
            r.setStyle("-fx-background-color:" + (i%2==0?"white":ROW_ALT) + ";");
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

    private static Button creerBouton(String texte, String bg, String fg, String hover) {
        Button b = new Button(texte);
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-font-weight:bold;-fx-padding:9 22;-fx-background-radius:10;-fx-cursor:hand;-fx-font-size:13px;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace(bg, hover)));
        b.setOnMouseExited (e -> b.setStyle(base)); return b;
    }

    private static Label creerPill(String texte, String bg, String fg) {
        Label l = new Label(texte);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 9;-fx-background-radius:20;");
        return l;
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

    /**
     * ✅ Alerte SUCCÈS après soumission d'un examen/devoir.
     * Affiche les détails complets de ce qui a été envoyé.
     */
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

        // ── Bande teal-dark ───────────────────────────────────────
        HBox bande = new HBox(10); bande.setAlignment(Pos.CENTER_LEFT);
        bande.setPadding(new Insets(15, 20, 15, 20));
        bande.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:18 18 0 0;");
        Label lLogo = new Label("🎓"); lLogo.setStyle("-fx-font-size:16px;");
        Label lSep  = new Label("|"); lSep.setStyle("-fx-text-fill:rgba(255,255,255,0.30);");
        String typeIcon = "EXAMEN".equals(type) ? "📋" : "DEVOIR".equals(type) ? "📝" : "✍️";
        Label lIco  = new Label(typeIcon + " " + type); lIco.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_LIGHT + ";");
        Region esp  = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label lApp  = new Label("UNIV-SCHEDULER"); lApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.50);-fx-font-weight:bold;");
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(lLogo, lSep, lIco, esp, lApp, btnX);

        // ── Ligne verte ───────────────────────────────────────────
        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + GREEN_SOFT + ";");

        // ── Zone succès ───────────────────────────────────────────
        VBox zoneSucces = new VBox(6);
        zoneSucces.setAlignment(Pos.CENTER);
        zoneSucces.setPadding(new Insets(22, 24, 16, 24));
        zoneSucces.setStyle("-fx-background-color:#f0fdf4;");

        // Cercle vert animé
        StackPane cercle = new StackPane();
        Circle bg = new Circle(34); bg.setFill(Color.web(GREEN_BG));
        bg.setStroke(Color.web(GREEN_SOFT)); bg.setStrokeWidth(2.5);
        Label check = new Label("✅"); check.setStyle("-fx-font-size:28px;");
        cercle.getChildren().addAll(bg, check);

        // Animation pop-in
        cercle.setScaleX(0.3); cercle.setScaleY(0.3);
        javafx.animation.ScaleTransition pop = new javafx.animation.ScaleTransition(
                Duration.millis(320), cercle);
        pop.setFromX(0.3); pop.setFromY(0.3);
        pop.setToX(1.0);   pop.setToY(1.0);
        pop.setInterpolator(javafx.animation.Interpolator.SPLINE(0.175, 0.885, 0.320, 1.275));
        pop.play();

        Label titreSucces = new Label("Demande soumise avec succès !");
        titreSucces.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + GREEN_TXT + ";");
        Label sousTitre = new Label("Le gestionnaire a été notifié et traitera votre demande.");
        sousTitre.setWrapText(true); sousTitre.setTextAlignment(TextAlignment.CENTER);
        sousTitre.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";");
        zoneSucces.getChildren().addAll(cercle, titreSucces, sousTitre);

        // ── Séparateur ────────────────────────────────────────────
        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:" + BORDER_LIGHT + ";");

        // ── Récapitulatif ─────────────────────────────────────────
        VBox recap = new VBox(0);
        recap.setPadding(new Insets(14, 24, 10, 24));
        Label titRecap = new Label("Récapitulatif de la demande");
        titRecap.setPadding(new Insets(0, 0, 8, 0));
        titRecap.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_SECOND + ";letter-spacing:0.05em;");

        String salle = (salleNumero != null && !salleNumero.isBlank()) ? salleNumero : "Devoir à la maison";
        String duree = dureeMinutes + " min";

        String[][] lignes = {
                {"📋 Type",    typeIcon + " " + (type != null ? type : "—")},
                {"📌 Titre",   titre    != null ? titre   : "—"},
                {"🎓 Classe",  classeNom != null ? classeNom : "—"},
                {"📚 Matière", matiereNom != null ? matiereNom : "—"},
                {"📅 Date",    (dateExamen != null ? dateExamen : "—") + " à " + (heure != null ? heure : "—")},
                {"⏱ Durée",   duree},
                {"🏫 Salle",   salle},
        };

        VBox grille = new VBox(0);
        grille.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                + "-fx-border-color:" + BORDER_LIGHT + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < lignes.length; i++) {
            HBox rangee = new HBox(0); rangee.setPadding(new Insets(9, 14, 9, 14));
            rangee.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : ROW_ALT) + ";"
                    + (i == 0 ? "-fx-background-radius:10 10 0 0;" : "")
                    + (i == lignes.length - 1 ? "-fx-background-radius:0 0 10 10;" : ""));
            Label cle = new Label(lignes[i][0]); cle.setMinWidth(100);
            cle.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
            Label val = new Label(lignes[i][1]); val.setWrapText(true); val.setMaxWidth(290);
            val.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEAL_DARK + ";-fx-font-weight:bold;");
            rangee.getChildren().addAll(cle, val);
            grille.getChildren().add(rangee);
        }
        recap.getChildren().addAll(titRecap, grille);

        // ── Info badge ────────────────────────────────────────────
        HBox infoBadge = new HBox(8); infoBadge.setAlignment(Pos.CENTER_LEFT);
        infoBadge.setPadding(new Insets(10, 24, 4, 24));
        Label infoIco = new Label("🔔"); infoIco.setStyle("-fx-font-size:14px;");
        Label infoTxt = new Label("Vous recevrez une notification dès que le gestionnaire aura traité votre demande.");
        infoTxt.setWrapText(true); infoTxt.setMaxWidth(380);
        infoTxt.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-style:italic;");
        infoBadge.getChildren().addAll(infoIco, infoTxt);

        // ── Bouton fermer ─────────────────────────────────────────
        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:0 0 18 18;");
        Button btnFermer = creerBouton("  Parfait ! ✓  ", GREEN_SOFT, "white", "#2aaf72");
        btnFermer.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnFermer);

        carte.getChildren().addAll(bande, ligne, zoneSucces, sep, recap, infoBadge, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    /**
     * ❌ Alerte VALIDATION MANQUANTE — liste les champs vides.
     */
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
        Label lLogo = new Label("🎓"); lLogo.setStyle("-fx-font-size:16px;");
        Label lSep  = new Label("|"); lSep.setStyle("-fx-text-fill:rgba(255,255,255,0.30);");
        Label lIco  = new Label("⚠️ Champs manquants"); lIco.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp  = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(lLogo, lSep, lIco, esp, btnX);

        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + GOLD + ";");

        // Zone icône + message
        VBox zoneWarn = new VBox(8); zoneWarn.setAlignment(Pos.CENTER);
        zoneWarn.setPadding(new Insets(20, 24, 14, 24));
        zoneWarn.setStyle("-fx-background-color:" + GOLD_BG + ";");

        StackPane cercle = new StackPane();
        Circle bg = new Circle(30); bg.setFill(Color.web(GOLD_BG));
        bg.setStroke(Color.web(GOLD)); bg.setStrokeWidth(2.5);
        Label icoWarn = new Label("⚠️"); icoWarn.setStyle("-fx-font-size:24px;");
        cercle.getChildren().addAll(bg, icoWarn);

        Label titWarn = new Label("Formulaire incomplet");
        titWarn.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + GOLD_TXT + ";");
        Label sousWarn = new Label("Veuillez remplir les champs obligatoires (*) :");
        sousWarn.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";");
        zoneWarn.getChildren().addAll(cercle, titWarn, sousWarn);

        // Liste des champs manquants
        VBox listeCh = new VBox(6); listeCh.setPadding(new Insets(14, 24, 14, 24));
        for (String champ : champsManquants) {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color:" + RED_BG + ";-fx-background-radius:8;"
                    + "-fx-border-color:" + RED_BORDER + ";-fx-border-radius:8;-fx-border-width:1;");
            Label dot = new Label("●"); dot.setStyle("-fx-text-fill:" + RED_SOFT + ";-fx-font-size:10px;");
            Label lbl = new Label(champ); lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + RED_TXT + ";-fx-font-weight:bold;");
            row.getChildren().addAll(dot, lbl);
            listeCh.getChildren().add(row);
        }

        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 18 18;");
        Button btnOk = creerBouton("  Corriger les champs  ", TEAL_MID, "white", TEAL_DARK);
        btnOk.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnOk);

        carte.getChildren().addAll(bande, ligne, zoneWarn, listeCh, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    /**
     * 🔴 Alerte CONFLIT SALLE détecté.
     */
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
        Label lLogo = new Label("🎓"); lLogo.setStyle("-fx-font-size:16px;");
        Label lSep  = new Label("|"); lSep.setStyle("-fx-text-fill:rgba(255,255,255,0.30);");
        Label lIco  = new Label("🔴 Conflit de Salle"); lIco.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp  = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(lLogo, lSep, lIco, esp, btnX);

        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + RED_SOFT + ";");

        VBox zoneErr = new VBox(10); zoneErr.setAlignment(Pos.CENTER);
        zoneErr.setPadding(new Insets(22, 24, 16, 24));
        zoneErr.setStyle("-fx-background-color:" + RED_BG + ";");

        StackPane cercle = new StackPane();
        Circle bg = new Circle(32); bg.setFill(Color.web(RED_BG));
        bg.setStroke(Color.web(RED_SOFT)); bg.setStrokeWidth(2.5);

        // Shake animation
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), cercle);
        shake.setFromX(-6); shake.setToX(6); shake.setCycleCount(6);
        shake.setAutoReverse(true); shake.play();

        Label icoErr = new Label("❌"); icoErr.setStyle("-fx-font-size:26px;");
        cercle.getChildren().addAll(bg, icoErr);

        Label titErr = new Label("Salle déjà occupée !");
        titErr.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + RED_TXT + ";");
        Label sousErr = new Label("La salle est indisponible pour ce créneau.");
        sousErr.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";");
        zoneErr.getChildren().addAll(cercle, titErr, sousErr);

        // Infos conflit
        VBox details = new VBox(0); details.setPadding(new Insets(14, 24, 14, 24));
        String[][] infos = {
                {"🏫 Salle", salleNumero != null ? salleNumero : "—"},
                {"📅 Date",  (dateExamen != null ? dateExamen : "—") + " à " + (heure != null ? heure : "—")},
        };
        VBox grille = new VBox(0);
        grille.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                + "-fx-border-color:" + RED_BORDER + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < infos.length; i++) {
            HBox row = new HBox(0); row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : "#fff5f5") + ";"
                    + (i == 0 ? "-fx-background-radius:10 10 0 0;" : "-fx-background-radius:0 0 10 10;"));
            Label c = new Label(infos[i][0]); c.setMinWidth(80);
            c.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
            Label v = new Label(infos[i][1]); v.setStyle("-fx-font-size:12px;-fx-text-fill:" + RED_TXT + ";-fx-font-weight:bold;");
            row.getChildren().addAll(c, v); grille.getChildren().add(row);
        }

        HBox conseil = new HBox(8); conseil.setAlignment(Pos.CENTER_LEFT);
        conseil.setPadding(new Insets(10, 0, 0, 0));
        Label conseilIco = new Label("💡"); conseilIco.setStyle("-fx-font-size:14px;");
        Label conseilTxt = new Label("Choisissez une autre salle ou une autre date/heure.");
        conseilTxt.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-style:italic;");
        conseil.getChildren().addAll(conseilIco, conseilTxt);

        details.getChildren().addAll(grille, conseil);

        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 18 18;");
        Button btnOk = creerBouton("  Modifier le formulaire  ", RED_SOFT, "white", "#c44040");
        btnOk.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnOk);

        carte.getChildren().addAll(bande, ligne, zoneErr, details, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }

    /**
     * 📅 Alerte DATE PASSÉE.
     */
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
        Label lLogo = new Label("🎓"); lLogo.setStyle("-fx-font-size:16px;");
        Label lSep  = new Label("|"); lSep.setStyle("-fx-text-fill:rgba(255,255,255,0.30);");
        Label lIco  = new Label("📅 Date invalide"); lIco.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp  = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Button btnX = btnFermerX(); btnX.setOnAction(e -> fenetre.close());
        bande.getChildren().addAll(lLogo, lSep, lIco, esp, btnX);

        Region ligne = new Region(); ligne.setPrefHeight(4);
        ligne.setStyle("-fx-background-color:" + GOLD + ";");

        VBox corps = new VBox(14); corps.setAlignment(Pos.CENTER);
        corps.setPadding(new Insets(28, 28, 20, 28));
        corps.setStyle("-fx-background-color:" + GOLD_BG + ";");

        Label ico = new Label("📅"); ico.setStyle("-fx-font-size:40px;");
        Label tit = new Label("Date dans le passé");
        tit.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + GOLD_TXT + ";");
        Label msg = new Label("La date sélectionnée (" + (dateChoisie != null ? dateChoisie : "—")
                + ") est déjà passée.\nVeuillez choisir une date future.");
        msg.setWrapText(true); msg.setTextAlignment(TextAlignment.CENTER);
        msg.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_SECOND + ";");
        corps.getChildren().addAll(ico, tit, msg);

        HBox zoneFermer = new HBox(); zoneFermer.setAlignment(Pos.CENTER_RIGHT);
        zoneFermer.setPadding(new Insets(12, 24, 18, 24));
        zoneFermer.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 18 18;");
        Button btnOk = creerBouton("  Choisir une autre date  ", GOLD, "white", "#c87f00");
        btnOk.setOnAction(e -> fenetre.close());
        zoneFermer.getChildren().add(btnOk);

        carte.getChildren().addAll(bande, ligne, corps, zoneFermer);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
    }
}