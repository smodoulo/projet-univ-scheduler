package com.univscheduler.model;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.List;

/**
 * Alertes personnalisées UNIV-SCHEDULER
 * Palette teal — identique au dashboard HTML
 *   --teal-dark   #1a5f6e
 *   --teal-mid    #2a9cb0
 *   --teal-light  #4ecdc4
 *   --bg          #e8f4f4
 *   --green-soft  #3ecf8e
 *   --gold        #f0a500
 *   --red-soft    #e05c5c
 */
public class AlertePersonnalisee {

    // ── Constantes palette teal ────────────────────────────────────
    private static final String TEAL_DARK    = "#1a5f6e";
    private static final String TEAL_MID     = "#2a9cb0";
    private static final String TEAL_LIGHT   = "#4ecdc4";
    private static final String TEAL_BG      = "#e8f4f4";      // --bg
    private static final String TEAL_BG_SOFT = "#f0f9fa";      // très léger
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
    private static final String CARD_BG      = "#ffffff";
    private static final String ROW_ALT      = "#f0f9fa";
    private static final String BORDER_LIGHT = "#c0dde4";
    private static final String TEXT_MUTED   = "#9eb3bf";
    private static final String TEXT_SECOND  = "#6b8394";
    private static final String TEXT_THIRD   = "#4a7a87";

    // ════════════════════════════════════════════════════════════════
    public enum Type {
        //               couleur bande     fond carte    couleur texte     icône   titre défaut
        SUCCES  (TEAL_DARK,  TEAL_BG,   TEAL_DARK,  "✅", "Succès"),
        ERREUR  (RED_SOFT,   RED_BG,    RED_TXT,    "❌", "Erreur"),
        INFO    (TEAL_DARK,  TEAL_BG,   TEAL_DARK,  "ℹ",  "Information"),
        WARN    (GOLD,       GOLD_BG,   GOLD_TXT,   "⚠",  "Avertissement"),
        QUESTION(TEAL_DARK,  TEAL_BG,   TEAL_DARK,  "❓", "Confirmation");

        final String pri, fond, txt, icone, defTitre;
        Type(String pri,String fond,String txt,String icone,String defTitre){
            this.pri=pri; this.fond=fond; this.txt=txt; this.icone=icone; this.defTitre=defTitre;
        }
    }

    /** Couleur du bouton primaire selon le type */
    private static String couleurBtn(Type type) {
        switch (type) {
            case ERREUR:  return RED_SOFT;
            case WARN:    return GOLD;
            case SUCCES:  return GREEN_SOFT;
            default:      return TEAL_MID;     // INFO, QUESTION → teal-mid
        }
    }

    // ── API simple ────────────────────────────────────────────────
    public static void   succes(String t, String m)        { afficher(Type.SUCCES,   t, m, false); }
    public static void   info(String t, String m)          { afficher(Type.INFO,     t, m, false); }
    public static void   erreur(String t, String m)        { afficher(Type.ERREUR,   t, m, false); }
    public static void   avertissement(String t, String m) { afficher(Type.WARN,     t, m, false); }
    public static boolean confirmer(String t, String m)    { return afficher(Type.QUESTION, t, m, true); }

    public static boolean confirmerSuppression(String e) {
        return afficher(Type.ERREUR, "Confirmer la suppression",
                "Êtes-vous sûr(e) de vouloir supprimer\n" + e + " ?\n\nCette action est irréversible.", true);
    }
    public static boolean confirmerDeconnexion() {
        return afficher(Type.QUESTION, "Déconnexion",
                "Voulez-vous vous déconnecter\nde UNIV-SCHEDULER ?", true);
    }

    // ════════════════════════════════════════════════════════════════
    //  Demander motif annulation
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

        // ── Bande titre teal-dark ─────────────────────────────────
        HBox bandeTitre = bandeTitre("❌", "Annuler ce cours", fenetre);
        // ligne décorative rouge (action destructive)
        Region ligne = ligneDeco(RED_SOFT);

        // ── Corps ─────────────────────────────────────────────────
        VBox corps = new VBox(14);
        corps.setPadding(new Insets(20, 24, 14, 24));
        corps.setStyle("-fx-background-color:" + TEAL_BG + ";");

        // Grille infos cours
        String[][] infos = {
                {"Cours", nomCours},
                {"Jour",  jour + "  " + heureDebut + " – " + heureFin}
        };
        corps.getChildren().add(grilleInfos(infos));

        // Champ motif
        VBox blocMotif = new VBox(6);
        Label lblMotif = new Label("Motif d'annulation *");
        lblMotif.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_DARK + ";");

        javafx.scene.control.TextArea champMotif = new javafx.scene.control.TextArea();
        champMotif.setPromptText("Ex: Cours reporté, enseignant absent...");
        champMotif.setPrefRowCount(3);
        champMotif.setWrapText(true);
        champMotif.setStyle(
                "-fx-background-color:white;-fx-border-color:" + BORDER_LIGHT + ";" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";");

        Label lblErreur = new Label("⚠ Veuillez saisir un motif.");
        lblErreur.setStyle("-fx-text-fill:" + RED_SOFT + ";-fx-font-size:11px;");
        lblErreur.setVisible(false);
        blocMotif.getChildren().addAll(lblMotif, champMotif, lblErreur);
        corps.getChildren().add(blocMotif);

        // ── Boutons ───────────────────────────────────────────────
        HBox zoneBoutons = new HBox(12);
        zoneBoutons.setAlignment(Pos.CENTER_RIGHT);
        zoneBoutons.setPadding(new Insets(14, 24, 20, 24));
        zoneBoutons.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");

        Button btnAnnuler = creerBouton("Annuler", TEAL_BG, TEAL_DARK, BORDER_LIGHT);
        btnAnnuler.setOnAction(e -> fenetre.close());

        Button btnConfirmer = creerBouton("❌ Confirmer l'annulation", RED_SOFT, "white", "#c94040");
        btnConfirmer.setOnAction(e -> {
            String m = champMotif.getText().trim();
            if (m.isEmpty()) { lblErreur.setVisible(true); return; }
            motif[0] = m;
            fenetre.close();
        });
        zoneBoutons.getChildren().addAll(btnAnnuler, btnConfirmer);

        carte.getChildren().addAll(bandeTitre, ligne, corps, zoneBoutons);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
        return motif[0];
    }

    // ════════════════════════════════════════════════════════════════
    //  Détail signalement
    // ════════════════════════════════════════════════════════════════
    public static void afficherDetailSignalement(
            int idSignal, String titre,
            String[][] lignes,
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

        // ── Bande titre ───────────────────────────────────────────
        HBox bandeTitre = bandeTitreAvecId("📋", "Signalement #" + idSignal, fenetre);
        Region ligne = ligneDeco(TEAL_MID);

        // ── En-tête titre signalement + badge statut ──────────────
        HBox enTete = new HBox(12);
        enTete.setAlignment(Pos.CENTER_LEFT);
        enTete.setPadding(new Insets(16, 24, 12, 24));
        enTete.setStyle("-fx-background-color:" + TEAL_DARK + ";");

        Label lblTitreSignal = new Label(titre != null ? titre : "—");
        lblTitreSignal.setWrapText(true);
        lblTitreSignal.setMaxWidth(340);
        lblTitreSignal.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");

        if (couleurStatut != null && lignes != null) {
            String statut = "";
            for (String[] l : lignes) if (l[0].contains("Statut")) { statut = l[1]; break; }
            Label badge = new Label(statut);
            badge.setStyle(
                    "-fx-background-color:" + couleurStatut + ";-fx-text-fill:white;" +
                            "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-padding:3 10;-fx-background-radius:20;");
            enTete.getChildren().addAll(lblTitreSignal, badge);
        } else {
            enTete.getChildren().add(lblTitreSignal);
        }

        // ── Corps scrollable ──────────────────────────────────────
        VBox corps = new VBox(0);
        corps.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";");

        if (lignes != null) {
            VBox grille = new VBox(0);
            grille.setPadding(new Insets(16, 24, 8, 24));
            for (int i = 0; i < lignes.length; i++) {
                if (lignes[i][0].contains("Statut")) continue;
                HBox rangee = new HBox(0);
                rangee.setPadding(new Insets(10, 12, 10, 12));
                rangee.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : ROW_ALT) +
                        ";-fx-background-radius:6;");
                Label lblCle = new Label(lignes[i][0]);
                lblCle.setMinWidth(140);
                lblCle.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
                Label lblVal = new Label(lignes[i][1] != null ? lignes[i][1] : "—");
                lblVal.setWrapText(true); lblVal.setMaxWidth(280);
                lblVal.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEAL_DARK + ";-fx-font-weight:bold;");
                rangee.getChildren().addAll(lblCle, lblVal);
                grille.getChildren().add(rangee);
            }
            corps.getChildren().add(grille);
        }

        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:" + BORDER_LIGHT + ";");
        corps.getChildren().add(sep);

        // Description
        if (description != null && !description.isEmpty()) {
            VBox blocDesc = new VBox(6);
            blocDesc.setPadding(new Insets(14, 24, 10, 24));
            Label titDesc = new Label("📝 Description");
            titDesc.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_SECOND + ";");
            Label valDesc = new Label(description);
            valDesc.setWrapText(true); valDesc.setMaxWidth(420);
            valDesc.setStyle(
                    "-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";-fx-line-spacing:3;" +
                            "-fx-background-color:" + ROW_ALT + ";-fx-padding:10;-fx-background-radius:8;");
            blocDesc.getChildren().addAll(titDesc, valDesc);
            corps.getChildren().add(blocDesc);
        }

        // Réponse admin — fond teal pâle au lieu de bleu
        if (reponseAdmin != null && !reponseAdmin.isEmpty()) {
            VBox blocReponse = new VBox(6);
            blocReponse.setPadding(new Insets(10, 24, 10, 24));
            Label titRep = new Label("💬 Réponse de l'administration");
            titRep.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEAL_MID + ";");
            Label valRep = new Label(reponseAdmin);
            valRep.setWrapText(true); valRep.setMaxWidth(420);
            valRep.setStyle(
                    "-fx-font-size:13px;-fx-text-fill:" + TEAL_DARK + ";-fx-line-spacing:3;" +
                            "-fx-background-color:#c8edf2;-fx-padding:10;-fx-background-radius:8;");
            blocReponse.getChildren().addAll(titRep, valRep);
            corps.getChildren().add(blocReponse);
        }

        // Date résolution
        if (dateResolution != null && !dateResolution.isEmpty()) {
            Label lblResol = new Label("✅ Résolu le : " + dateResolution);
            lblResol.setPadding(new Insets(6, 24, 12, 24));
            lblResol.setStyle("-fx-font-size:12px;-fx-text-fill:" + GREEN_SOFT + ";-fx-font-weight:bold;");
            corps.getChildren().add(lblResol);
        }

        ScrollPane scroll = new ScrollPane(corps);
        scroll.setFitToWidth(true); scroll.setMaxHeight(320);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");

        HBox zoneBouton = zoneBoutonFermer(fenetre);
        carte.getChildren().addAll(bandeTitre, ligne, enTete, scroll, zoneBouton);
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
        HBox bandeTitre = new HBox(10);
        bandeTitre.setAlignment(Pos.CENTER_LEFT);
        bandeTitre.setPadding(new Insets(15, 20, 15, 20));
        bandeTitre.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:14 14 0 0;");

        Label lblLogo  = new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep   = new Label("|");  lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lblIcone = new Label("🔔"); lblIcone.setStyle("-fx-font-size:16px;");
        Label lblTitre = new Label("Mes Notifications");
        lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);

        long nbNonLus = notifs.stream().filter(n -> !n.isLu()).count();
        Label lblCompteur = new Label(notifs.size() + " notif" + (notifs.size() > 1 ? "s" : "")
                + (nbNonLus > 0 ? "  •  🆕 " + nbNonLus : ""));
        lblCompteur.setStyle(
                "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:rgba(255,255,255,0.9);" +
                        "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");

        Label lblApp = new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");

        Button btnX = btnFermerX();
        btnX.setOnAction(e -> fenetre.close());
        bandeTitre.getChildren().addAll(lblLogo, lblSep, lblIcone, lblTitre, esp, lblCompteur, lblApp, btnX);

        Region ligne = ligneDeco(TEAL_MID);

        // ── Sous-en-tête ──────────────────────────────────────────
        HBox sousEntete = new HBox(12);
        sousEntete.setAlignment(Pos.CENTER_LEFT);
        sousEntete.setPadding(new Insets(10, 20, 10, 20));
        sousEntete.setStyle("-fx-background-color:" + TEAL_BG + ";");

        Label lblResume = new Label("Toutes les notifications sont marquées comme lues.");
        lblResume.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_SECOND + ";");
        Region espSous = new Region(); HBox.setHgrow(espSous, Priority.ALWAYS);

        HBox legende = new HBox(8);
        legende.setAlignment(Pos.CENTER_RIGHT);
        legende.getChildren().addAll(
                creerPill("✅ Succès", GREEN_BG,  GREEN_TXT),
                creerPill("⚠️ Alerte", RED_BG,    RED_TXT),
                creerPill("🔔 Info",   TEAL_BG,   TEAL_DARK)   // remplace gris neutre
        );
        sousEntete.getChildren().addAll(lblResume, espSous, legende);

        // ── Liste notifications ───────────────────────────────────
        VBox listeBox = new VBox(8);
        listeBox.setPadding(new Insets(14, 16, 14, 16));
        listeBox.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";");

        if (notifs.isEmpty()) {
            VBox vide = new VBox(10);
            vide.setAlignment(Pos.CENTER);
            vide.setPadding(new Insets(48));
            Label icoVide = new Label("🔕"); icoVide.setStyle("-fx-font-size:32px;");
            Label msgVide = new Label("Aucune notification pour le moment.");
            msgVide.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");
            vide.getChildren().addAll(icoVide, msgVide);
            listeBox.getChildren().add(vide);
        } else {
            for (Notification n : notifs) {
                // ── Couleurs par type — teal pour INFO (au lieu de gris neutre)
                String bg, fg, bord, icone;
                switch (n.getType() != null ? n.getType() : "") {
                    case "INFO":
                        bg = GREEN_BG;  fg = GREEN_TXT;   bord = GREEN_BORDER; icone = "✅"; break;
                    case "ALERTE":
                        bg = RED_BG;    fg = RED_TXT;     bord = RED_BORDER;   icone = "⚠️"; break;
                    default:
                        bg = TEAL_BG;   fg = TEAL_DARK;   bord = BORDER_LIGHT; icone = "🔔"; break;
                }
                boolean nonLu = !n.isLu();

                HBox rangee = new HBox(10);
                rangee.setAlignment(Pos.CENTER_LEFT);
                rangee.setPadding(new Insets(11, 14, 11, 14));
                rangee.setStyle(
                        "-fx-background-color:" + bg + ";-fx-background-radius:10;" +
                                "-fx-border-color:" + bord + ";-fx-border-radius:10;" +
                                "-fx-border-width:" + (nonLu ? "1.5" : "0.8") + ";");

                if (nonLu) {
                    Region barreNonLu = new Region();
                    barreNonLu.setPrefWidth(3); barreNonLu.setPrefHeight(44);
                    barreNonLu.setMinHeight(Region.USE_PREF_SIZE);
                    barreNonLu.setStyle("-fx-background-color:" + fg + ";-fx-background-radius:3;");
                    rangee.getChildren().add(barreNonLu);
                }

                Label lblIco = new Label(icone);
                lblIco.setStyle("-fx-font-size:18px;-fx-min-width:22;");

                VBox textes = new VBox(4);
                HBox.setHgrow(textes, Priority.ALWAYS);

                if (nonLu) {
                    Label badgeNew = new Label("NOUVEAU");
                    badgeNew.setStyle(
                            "-fx-background-color:" + fg + ";-fx-text-fill:white;" +
                                    "-fx-font-size:9px;-fx-font-weight:bold;" +
                                    "-fx-padding:2 7;-fx-background-radius:20;");
                    textes.getChildren().add(badgeNew);
                }

                Label lblMsg = new Label(n.getMessage());
                lblMsg.setWrapText(true); lblMsg.setMaxWidth(400);
                lblMsg.setStyle("-fx-font-size:12px;-fx-text-fill:" + fg + ";" +
                        (nonLu ? "-fx-font-weight:bold;" : ""));
                textes.getChildren().add(lblMsg);

                String date = n.getDateEnvoi() != null ? n.getDateEnvoi().toLocalDate().toString() : "";
                Label lblDate = new Label("📅 " + date);
                lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:" + TEXT_MUTED + ";");
                textes.getChildren().add(lblDate);

                rangee.getChildren().addAll(lblIco, textes);
                listeBox.getChildren().add(rangee);
            }
        }

        ScrollPane scroll = new ScrollPane(listeBox);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(400);
        scroll.setPrefHeight(Math.min(notifs.size() * 82 + 28, 400));
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");

        HBox zoneBouton = zoneBoutonFermer(fenetre);
        carte.getChildren().addAll(bandeTitre, ligne, sousEntete, scroll, zoneBouton);
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

        // ── Bande titre ───────────────────────────────────────────
        HBox bandeTitre = new HBox(10); bandeTitre.setAlignment(Pos.CENTER_LEFT);
        bandeTitre.setPadding(new Insets(15, 20, 15, 20));
        bandeTitre.setStyle("-fx-background-color:" + type.pri + ";-fx-background-radius:14 14 0 0;");

        Label lblLogo  = new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep   = new Label("|");  lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.40);-fx-font-size:14px;");
        Label lblIcone = new Label(type.icone); lblIcone.setStyle("-fx-font-size:16px;");
        Label lblTitre = new Label(titre != null && !titre.isEmpty() ? titre : type.defTitre);
        lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label lblApp = new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.60);-fx-font-weight:bold;");

        Button btnX = btnFermerX();
        btnX.setOnAction(e -> { resultat[0] = false; fenetre.close(); });
        bandeTitre.getChildren().addAll(lblLogo, lblSep, lblIcone, lblTitre, esp, lblApp, btnX);

        // Ligne décorative couleur bouton
        Region ligne = ligneDeco(couleurBtn(type));

        // Corps
        HBox corps = new HBox(14);
        corps.setPadding(new Insets(22, 24, 10, 24));
        corps.setAlignment(Pos.TOP_LEFT);

        Region barre = new Region();
        barre.setPrefWidth(4); barre.setPrefHeight(55); barre.setMinHeight(Region.USE_PREF_SIZE);
        barre.setStyle("-fx-background-color:" + couleurBtn(type) + ";-fx-background-radius:4;");

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true); lblMsg.setMaxWidth(350);
        lblMsg.setTextAlignment(TextAlignment.LEFT);
        lblMsg.setStyle("-fx-font-size:13px;-fx-text-fill:" + type.txt + ";-fx-line-spacing:4;");
        corps.getChildren().addAll(barre, lblMsg);

        // Boutons
        HBox zoneBoutons = new HBox(12); zoneBoutons.setAlignment(Pos.CENTER_RIGHT);
        zoneBoutons.setPadding(new Insets(14, 24, 20, 24));
        zoneBoutons.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");

        String cp = couleurBtn(type);
        if (avecAnnuler) {
            Button btnAnnuler = creerBouton("Annuler", TEAL_BG, TEAL_DARK, BORDER_LIGHT);
            btnAnnuler.setOnAction(e -> { resultat[0] = false; fenetre.close(); });
            String labelOk = type == Type.ERREUR ? "🗑 Supprimer" : "✔ Confirmer";
            Button btnOk   = creerBouton(labelOk, cp, "white", assombrir(cp));
            btnOk.setOnAction(e -> { resultat[0] = true; fenetre.close(); });
            zoneBoutons.getChildren().addAll(btnAnnuler, btnOk);
        } else {
            Button btnOk = creerBouton("  OK  ", cp, "white", assombrir(cp));
            btnOk.setMinWidth(100);
            btnOk.setOnAction(e -> { resultat[0] = true; fenetre.close(); });
            zoneBoutons.getChildren().add(btnOk);
        }

        carte.getChildren().addAll(bandeTitre, ligne, corps, zoneBoutons);
        overlay.getChildren().add(carte);
        animerEtAfficher(carte, fenetre);
        return resultat[0];
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers partagés
    // ════════════════════════════════════════════════════════════════

    /** Bande titre standard (teal-dark) avec bouton X */
    private static HBox bandeTitre(String icone, String titre, Stage fenetre) {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 20, 14, 20));
        h.setStyle("-fx-background-color:" + TEAL_DARK + ";-fx-background-radius:14 14 0 0;");

        Label lblLogo  = new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep   = new Label("|");  lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lblIco   = new Label(icone); lblIco.setStyle("-fx-font-size:16px;");
        Label lblTitre = new Label(titre); lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label lblApp = new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");

        Button btnX = btnFermerX();
        btnX.setOnAction(e -> fenetre.close());
        h.getChildren().addAll(lblLogo, lblSep, lblIco, lblTitre, esp, lblApp, btnX);
        return h;
    }

    /** Variante avec id dans le titre */
    private static HBox bandeTitreAvecId(String icone, String titre, Stage fenetre) {
        return bandeTitre(icone, titre, fenetre);
    }

    /** Bouton ✕ standard */
    private static Button btnFermerX() {
        Button b = new Button("✕");
        String base = "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-background-radius:50;" +
                "-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace("0.15", "0.35")));
        b.setOnMouseExited (e -> b.setStyle(base.replace("0.35", "0.15")));
        return b;
    }

    /** Ligne décorative colorée sous la bande titre */
    private static Region ligneDeco(String couleur) {
        Region r = new Region(); r.setPrefHeight(3);
        r.setStyle("-fx-background-color:" + couleur + ";");
        return r;
    }

    /** Zone bouton "Fermer" en bas de fenêtre */
    private static HBox zoneBoutonFermer(Stage fenetre) {
        HBox z = new HBox(); z.setAlignment(Pos.CENTER_RIGHT);
        z.setPadding(new Insets(12, 24, 18, 24));
        z.setStyle("-fx-background-color:" + TEAL_BG_SOFT + ";-fx-background-radius:0 0 14 14;");
        Button btn = creerBouton("  Fermer  ", TEAL_DARK, "white", "#0f3d48");
        btn.setOnAction(e -> fenetre.close());
        z.getChildren().add(btn);
        return z;
    }

    /** Grille d'informations (clé / valeur) alternée */
    private static VBox grilleInfos(String[][] infos) {
        VBox grille = new VBox(0);
        grille.setStyle(
                "-fx-background-color:white;-fx-background-radius:10;" +
                        "-fx-border-color:" + BORDER_LIGHT + ";-fx-border-radius:10;-fx-border-width:1;");
        for (int i = 0; i < infos.length; i++) {
            HBox rangee = new HBox(0);
            rangee.setPadding(new Insets(10, 14, 10, 14));
            rangee.setStyle("-fx-background-color:" + (i % 2 == 0 ? "white" : ROW_ALT) + ";");
            Label cle = new Label(infos[i][0]);
            cle.setMinWidth(80);
            cle.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_SECOND + ";-fx-font-weight:bold;");
            Label val = new Label(infos[i][1]);
            val.setWrapText(true); val.setMaxWidth(280);
            val.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEAL_DARK + ";-fx-font-weight:bold;");
            rangee.getChildren().addAll(cle, val);
            grille.getChildren().add(rangee);
        }
        return grille;
    }

    /** Ombre commune à toutes les cartes */
    private static DropShadow ombreCarte() {
        DropShadow d = new DropShadow();
        d.setRadius(36); d.setOffsetY(10);
        d.setColor(Color.color(0, 0, 0, 0.30));
        return d;
    }

    /** Animation fade + slide vers le bas */
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

    /** Bouton stylé avec hover */
    private static Button creerBouton(String texte, String bg, String fg, String hover) {
        Button b = new Button(texte);
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-weight:bold;" +
                "-fx-padding:9 22;-fx-background-radius:10;-fx-cursor:hand;-fx-font-size:13px;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace(bg, hover)));
        b.setOnMouseExited (e -> b.setStyle(base));
        return b;
    }

    /** Pill légende notifications */
    private static Label creerPill(String texte, String bg, String fg) {
        Label l = new Label(texte);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-padding:3 9;-fx-background-radius:20;");
        return l;
    }

    /** Assombrit une couleur hex de ~18% */
    private static String assombrir(String hex) {
        try {
            Color c = Color.web(hex);
            return String.format("#%02x%02x%02x",
                    (int)(c.getRed()   * 0.82 * 255),
                    (int)(c.getGreen() * 0.82 * 255),
                    (int)(c.getBlue()  * 0.82 * 255));
        } catch (Exception e) { return hex; }
    }
}