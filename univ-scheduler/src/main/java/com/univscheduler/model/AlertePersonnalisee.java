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

public class AlertePersonnalisee {

    public enum Type {
        SUCCES  ("#1e293b","#eff6ff","#1e3a5f","✅","Succès"),
        ERREUR  ("#ef4444","#fef2f2","#7f1d1d","❌","Erreur"),
        INFO    ("#1e293b","#eff6ff","#1e3a5f","ℹ", "Information"),
        WARN    ("#f59e0b","#fffbeb","#78350f","⚠", "Avertissement"),
        QUESTION("#1e293b","#eff6ff","#1e3a5f","❓","Confirmation");

        final String pri,fond,txt,icone,defTitre;
        Type(String pri,String fond,String txt,String icone,String defTitre){
            this.pri=pri;this.fond=fond;this.txt=txt;this.icone=icone;this.defTitre=defTitre;
        }
    }

    private static String couleurBtn(Type type) {
        switch(type){case ERREUR:return "#ef4444";case WARN:return "#f59e0b";case SUCCES:return "#10b981";default:return "#3b82f6";}
    }

    // ── API simple ────────────────────────────────────────────────
    public static void succes(String t, String m)        { afficher(Type.SUCCES,  t,m,false); }
    public static void info(String t, String m)          { afficher(Type.INFO,    t,m,false); }
    public static void erreur(String t, String m)        { afficher(Type.ERREUR,  t,m,false); }
    public static void avertissement(String t, String m) { afficher(Type.WARN,    t,m,false); }
    public static boolean confirmer(String t, String m)  { return afficher(Type.QUESTION,t,m,true); }
    public static boolean confirmerSuppression(String e) {
        return afficher(Type.ERREUR,"Confirmer la suppression",
                "Êtes-vous sûr(e) de vouloir supprimer\n"+e+" ?\n\nCette action est irréversible.",true);
    }
    public static boolean confirmerDeconnexion() {
        return afficher(Type.QUESTION,"Déconnexion",
                "Voulez-vous vous déconnecter\nde UNIV-SCHEDULER ?",true);
    }

    // ── confirmer annulation d'un cours ──────────────────────────
    public static boolean confirmerAnnulationCours(
            String nomCours, String jour, String heureDebut, String heureFin) {

        final boolean[] resultat = {false};

        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(520, 340);

        VBox carte = new VBox(0);
        carte.setMaxWidth(440);
        DropShadow ombre = new DropShadow();
        ombre.setRadius(36); ombre.setOffsetY(10); ombre.setColor(Color.color(0,0,0,0.35));
        carte.setEffect(ombre);
        carte.setStyle("-fx-background-color:#eff6ff;-fx-background-radius:14;");

        // ── Bande titre ──────────────────────────────────────────
        HBox bandeTitre = new HBox(10);
        bandeTitre.setAlignment(Pos.CENTER_LEFT);
        bandeTitre.setPadding(new Insets(14,20,14,20));
        bandeTitre.setStyle("-fx-background-color:#1e293b;-fx-background-radius:14 14 0 0;");

        Label lblLogo  = new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep   = new Label("|");  lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lblIcone = new Label("❓"); lblIcone.setStyle("-fx-font-size:16px;");
        Label lblTitre = new Label("Confirmer l'annulation");
        lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label lblApp = new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-size:11px;"
                +"-fx-background-radius:50;-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;");
        btnX.setOnMouseEntered(e->btnX.setStyle(btnX.getStyle().replace("0.15","0.35")));
        btnX.setOnMouseExited (e->btnX.setStyle(btnX.getStyle().replace("0.35","0.15")));
        btnX.setOnAction(e->{ resultat[0]=false; fenetre.close(); });
        bandeTitre.getChildren().addAll(lblLogo,lblSep,lblIcone,lblTitre,esp,lblApp,btnX);

        // ── Ligne décorative bleue ────────────────────────────────
        Region ligne = new Region();
        ligne.setPrefHeight(3);
        ligne.setStyle("-fx-background-color:#3b82f6;");

        // ── Corps ─────────────────────────────────────────────────
        HBox corps = new HBox(14);
        corps.setPadding(new Insets(22,24,14,24));
        corps.setAlignment(Pos.TOP_LEFT);
        corps.setStyle("-fx-background-color:#eff6ff;");

        Region barre = new Region();
        barre.setPrefWidth(4); barre.setPrefHeight(90); barre.setMinHeight(Region.USE_PREF_SIZE);
        barre.setStyle("-fx-background-color:#3b82f6;-fx-background-radius:4;");

        VBox contenu = new VBox(12);

        Label lblQuestion = new Label("Annuler ce cours ?");
        lblQuestion.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1e3a5f;");

        VBox grille = new VBox(0);
        grille.setStyle("-fx-background-color:white;-fx-background-radius:8;"
                +"-fx-border-color:#e2e8f0;-fx-border-radius:8;-fx-border-width:0.5;");

        String[][] infos = {
                {"Cours", nomCours},
                {"Jour",  jour + "  " + heureDebut + " – " + heureFin}
        };
        for (int i = 0; i < infos.length; i++) {
            HBox rangee = new HBox(0);
            rangee.setPadding(new Insets(10,14,10,14));
            rangee.setStyle("-fx-background-color:"+(i%2==0?"white":"#f8fafc")+";");
            Label cle = new Label(infos[i][0]);
            cle.setMinWidth(80);
            cle.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;-fx-font-weight:bold;");
            Label val = new Label(infos[i][1]);
            val.setWrapText(true); val.setMaxWidth(280);
            val.setStyle("-fx-font-size:12px;-fx-text-fill:#1e293b;-fx-font-weight:bold;");
            rangee.getChildren().addAll(cle, val);
            grille.getChildren().add(rangee);
        }

        HBox note = new HBox(8);
        note.setAlignment(Pos.TOP_LEFT);
        note.setPadding(new Insets(10,14,10,14));
        note.setStyle("-fx-background-color:#dbeafe;-fx-background-radius:8;");
        Label iconeInfo = new Label("ℹ"); iconeInfo.setStyle("-fx-font-size:14px;");
        Label txtInfo   = new Label("Le gestionnaire sera notifié automatiquement.");
        txtInfo.setWrapText(true); txtInfo.setMaxWidth(290);
        txtInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#1e3a5f;-fx-line-spacing:3;");
        note.getChildren().addAll(iconeInfo, txtInfo);

        contenu.getChildren().addAll(lblQuestion, grille, note);
        corps.getChildren().addAll(barre, contenu);

        // ── Zone boutons ──────────────────────────────────────────
        HBox zoneBoutons = new HBox(12);
        zoneBoutons.setAlignment(Pos.CENTER_RIGHT);
        zoneBoutons.setPadding(new Insets(14,24,20,24));
        zoneBoutons.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:0 0 14 14;");

        Button btnAnnuler = creerBouton("Annuler","#e2e8f0","#1e293b","#cbd5e1");
        btnAnnuler.setOnAction(e->{ resultat[0]=false; fenetre.close(); });

        Button btnConfirmer = creerBouton("✔ Confirmer","#3b82f6","white","#2563eb");
        btnConfirmer.setOnAction(e->{ resultat[0]=true; fenetre.close(); });

        zoneBoutons.getChildren().addAll(btnAnnuler, btnConfirmer);

        carte.getChildren().addAll(bandeTitre, ligne, corps, zoneBoutons);
        overlay.getChildren().add(carte);

        animerEtAfficher(carte, fenetre);
        return resultat[0];
    }

    // ── dialogue détail signalement ───────────────────────────────
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
        DropShadow ombre = new DropShadow();
        ombre.setRadius(36); ombre.setOffsetY(10); ombre.setColor(Color.color(0,0,0,0.35));
        carte.setEffect(ombre);
        carte.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:14;");

        // ── Bande titre ──────────────────────────────────────────
        HBox bandeTitre = new HBox(10);
        bandeTitre.setAlignment(Pos.CENTER_LEFT);
        bandeTitre.setPadding(new Insets(15,20,15,20));
        bandeTitre.setStyle("-fx-background-color:#1e293b;-fx-background-radius:14 14 0 0;");

        Label lblLogo = new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep  = new Label("|");  lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lblIcone = new Label("📋"); lblIcone.setStyle("-fx-font-size:16px;");
        Label lblTitre = new Label("Signalement #" + idSignal);
        lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp,Priority.ALWAYS);
        Label lblApp = new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:50;-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;");
        btnX.setOnMouseEntered(e->btnX.setStyle(btnX.getStyle().replace("0.15","0.35")));
        btnX.setOnMouseExited(e ->btnX.setStyle(btnX.getStyle().replace("0.35","0.15")));
        btnX.setOnAction(e->fenetre.close());
        bandeTitre.getChildren().addAll(lblLogo,lblSep,lblIcone,lblTitre,esp,lblApp,btnX);

        // ── Ligne décorative ─────────────────────────────────────
        Region ligne = new Region();
        ligne.setPrefHeight(3);
        ligne.setStyle("-fx-background-color:#3b82f6;");

        // ── En-tête titre signalement ─────────────────────────────
        HBox enTete = new HBox(12);
        enTete.setAlignment(Pos.CENTER_LEFT);
        enTete.setPadding(new Insets(16,24,12,24));
        enTete.setStyle("-fx-background-color:#1e293b;");

        Label lblTitreSignal = new Label(titre != null ? titre : "—");
        lblTitreSignal.setWrapText(true);
        lblTitreSignal.setMaxWidth(380);
        lblTitreSignal.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");

        if (couleurStatut != null && lignes != null) {
            String statut = "";
            for (String[] l : lignes) if (l[0].contains("Statut")) { statut = l[1]; break; }
            Label badge = new Label(statut);
            badge.setStyle("-fx-background-color:"+couleurStatut+";-fx-text-fill:white;"
                    +"-fx-font-size:11px;-fx-font-weight:bold;"
                    +"-fx-padding:3 10;-fx-background-radius:20;");
            enTete.getChildren().addAll(lblTitreSignal, badge);
        } else {
            enTete.getChildren().add(lblTitreSignal);
        }

        // ── Corps scrollable ──────────────────────────────────────
        VBox corps = new VBox(0);
        corps.setStyle("-fx-background-color:#f8fafc;");

        if (lignes != null) {
            VBox grille = new VBox(0);
            grille.setPadding(new Insets(16,24,8,24));
            for (int i = 0; i < lignes.length; i++) {
                if (lignes[i][0].contains("Statut")) continue;
                HBox rangee = new HBox(0);
                rangee.setPadding(new Insets(10,12,10,12));
                rangee.setStyle("-fx-background-color:" + (i%2==0?"white":"#f1f5f9") + ";"
                        +"-fx-background-radius:6;");
                Label lblCle = new Label(lignes[i][0]);
                lblCle.setMinWidth(140);
                lblCle.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;-fx-font-weight:bold;");
                Label lblVal = new Label(lignes[i][1] != null ? lignes[i][1] : "—");
                lblVal.setWrapText(true); lblVal.setMaxWidth(280);
                lblVal.setStyle("-fx-font-size:12px;-fx-text-fill:#1e293b;-fx-font-weight:bold;");
                rangee.getChildren().addAll(lblCle, lblVal);
                grille.getChildren().add(rangee);
            }
            corps.getChildren().add(grille);
        }

        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#e2e8f0;");
        corps.getChildren().add(sep);

        if (description != null && !description.isEmpty()) {
            VBox blocDesc = new VBox(6);
            blocDesc.setPadding(new Insets(14,24,10,24));
            Label titDesc = new Label("📝 Description");
            titDesc.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
            Label valDesc = new Label(description);
            valDesc.setWrapText(true); valDesc.setMaxWidth(420);
            valDesc.setStyle("-fx-font-size:13px;-fx-text-fill:#1e293b;-fx-line-spacing:3;"
                    +"-fx-background-color:#f1f5f9;-fx-padding:10;-fx-background-radius:8;");
            blocDesc.getChildren().addAll(titDesc, valDesc);
            corps.getChildren().add(blocDesc);
        }

        if (reponseAdmin != null && !reponseAdmin.isEmpty()) {
            VBox blocReponse = new VBox(6);
            blocReponse.setPadding(new Insets(10,24,10,24));
            Label titRep = new Label("💬 Réponse de l'administration");
            titRep.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#3b82f6;");
            Label valRep = new Label(reponseAdmin);
            valRep.setWrapText(true); valRep.setMaxWidth(420);
            valRep.setStyle("-fx-font-size:13px;-fx-text-fill:#1e3a5f;-fx-line-spacing:3;"
                    +"-fx-background-color:#dbeafe;-fx-padding:10;-fx-background-radius:8;");
            blocReponse.getChildren().addAll(titRep, valRep);
            corps.getChildren().add(blocReponse);
        }

        if (dateResolution != null && !dateResolution.isEmpty()) {
            Label lblResol = new Label("✅ Résolu le : " + dateResolution);
            lblResol.setPadding(new Insets(6,24,12,24));
            lblResol.setStyle("-fx-font-size:12px;-fx-text-fill:#10b981;-fx-font-weight:bold;");
            corps.getChildren().add(lblResol);
        }

        ScrollPane scroll = new ScrollPane(corps);
        scroll.setFitToWidth(true); scroll.setMaxHeight(320);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");

        HBox zoneBouton = new HBox();
        zoneBouton.setAlignment(Pos.CENTER_RIGHT);
        zoneBouton.setPadding(new Insets(12,24,18,24));
        zoneBouton.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:0 0 14 14;");
        Button btnOk = creerBouton("  Fermer  ","#1e293b","white","#334155");
        btnOk.setOnAction(e->fenetre.close());
        zoneBouton.getChildren().add(btnOk);

        carte.getChildren().addAll(bandeTitre, ligne, enTete, scroll, zoneBouton);
        overlay.getChildren().add(carte);

        animerEtAfficher(carte, fenetre);
    }

    // ── ✅ NOUVEAU : dialogue notifications gestionnaire ──────────
    // Usage : AlertePersonnalisee.afficherNotifications(notifDAO.findByUtilisateur(userId));
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
        DropShadow ombre = new DropShadow();
        ombre.setRadius(36); ombre.setOffsetY(10); ombre.setColor(Color.color(0,0,0,0.35));
        carte.setEffect(ombre);
        carte.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:14;");

        // ── Bande titre ──────────────────────────────────────────
        HBox bandeTitre = new HBox(10);
        bandeTitre.setAlignment(Pos.CENTER_LEFT);
        bandeTitre.setPadding(new Insets(15,20,15,20));
        bandeTitre.setStyle("-fx-background-color:#1e293b;-fx-background-radius:14 14 0 0;");

        Label lblLogo  = new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep   = new Label("|");  lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:14px;");
        Label lblIcone = new Label("🔔"); lblIcone.setStyle("-fx-font-size:16px;");
        Label lblTitre = new Label("Mes Notifications");
        lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);

        long nbNonLus = notifs.stream().filter(n -> !n.isLu()).count();
        Label lblCompteur = new Label(notifs.size() + " notif" + (notifs.size() > 1 ? "s" : "")
                + (nbNonLus > 0 ? "  •  🆕 " + nbNonLus : ""));
        lblCompteur.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:rgba(255,255,255,0.9);"
                +"-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");

        Label lblApp = new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);-fx-font-weight:bold;");
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-size:11px;"
                +"-fx-background-radius:50;-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;");
        btnX.setOnMouseEntered(e->btnX.setStyle(btnX.getStyle().replace("0.15","0.35")));
        btnX.setOnMouseExited (e->btnX.setStyle(btnX.getStyle().replace("0.35","0.15")));
        btnX.setOnAction(e->fenetre.close());
        bandeTitre.getChildren().addAll(lblLogo,lblSep,lblIcone,lblTitre,esp,lblCompteur,lblApp,btnX);

        // ── Ligne décorative bleue ────────────────────────────────
        Region ligne = new Region();
        ligne.setPrefHeight(3);
        ligne.setStyle("-fx-background-color:#3b82f6;");

        // ── Sous-en-tête résumé + légende ─────────────────────────
        HBox sousEntete = new HBox(12);
        sousEntete.setAlignment(Pos.CENTER_LEFT);
        sousEntete.setPadding(new Insets(10,20,10,20));
        sousEntete.setStyle("-fx-background-color:#f1f5f9;");

        Label lblResume = new Label("Toutes les notifications sont marquées comme lues.");
        lblResume.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        Region espSous = new Region(); HBox.setHgrow(espSous, Priority.ALWAYS);

        HBox legende = new HBox(8);
        legende.setAlignment(Pos.CENTER_RIGHT);
        legende.getChildren().addAll(
                creerPill("✅ Succès","#dcfce7","#166534"),
                creerPill("⚠️ Alerte","#fee2e2","#991b1b"),
                creerPill("🔔 Info",  "#f1f5f9","#475569")
        );
        sousEntete.getChildren().addAll(lblResume, espSous, legende);

        // ── Liste des notifications ───────────────────────────────
        VBox listeBox = new VBox(8);
        listeBox.setPadding(new Insets(14,16,14,16));
        listeBox.setStyle("-fx-background-color:#f8fafc;");

        if (notifs.isEmpty()) {
            VBox vide = new VBox(10);
            vide.setAlignment(Pos.CENTER);
            vide.setPadding(new Insets(48));
            Label icoVide = new Label("🔕"); icoVide.setStyle("-fx-font-size:32px;");
            Label msgVide = new Label("Aucune notification pour le moment.");
            msgVide.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
            vide.getChildren().addAll(icoVide, msgVide);
            listeBox.getChildren().add(vide);
        } else {
            for (Notification n : notifs) {
                String bg, fg, bord, icone;
                switch (n.getType() != null ? n.getType() : "") {
                    case "INFO":   bg="#dcfce7"; fg="#166534"; bord="#86efac"; icone="✅"; break;
                    case "ALERTE": bg="#fee2e2"; fg="#991b1b"; bord="#fca5a5"; icone="⚠️"; break;
                    default:       bg="#f1f5f9"; fg="#475569"; bord="#cbd5e1"; icone="🔔"; break;
                }
                boolean nonLu = !n.isLu();

                HBox rangee = new HBox(10);
                rangee.setAlignment(Pos.CENTER_LEFT);
                rangee.setPadding(new Insets(11,14,11,14));
                rangee.setStyle("-fx-background-color:"+bg+";-fx-background-radius:10;"
                        +"-fx-border-color:"+bord+";-fx-border-radius:10;"
                        +"-fx-border-width:"+(nonLu?"1.5":"0.8")+";");

                // Barre latérale colorée si non-lu
                if (nonLu) {
                    Region barreNonLu = new Region();
                    barreNonLu.setPrefWidth(3); barreNonLu.setPrefHeight(44);
                    barreNonLu.setMinHeight(Region.USE_PREF_SIZE);
                    barreNonLu.setStyle("-fx-background-color:"+fg+";-fx-background-radius:3;");
                    rangee.getChildren().add(barreNonLu);
                }

                // Icône type
                Label lblIco = new Label(icone);
                lblIco.setStyle("-fx-font-size:18px;-fx-min-width:22;");

                // Zone texte principale
                VBox textes = new VBox(4);
                HBox.setHgrow(textes, Priority.ALWAYS);

                // Badge NOUVEAU si non-lu
                if (nonLu) {
                    Label badgeNew = new Label("NOUVEAU");
                    badgeNew.setStyle("-fx-background-color:"+fg+";-fx-text-fill:white;"
                            +"-fx-font-size:9px;-fx-font-weight:bold;"
                            +"-fx-padding:2 7;-fx-background-radius:20;");
                    textes.getChildren().add(badgeNew);
                }

                Label lblMsg = new Label(n.getMessage());
                lblMsg.setWrapText(true); lblMsg.setMaxWidth(400);
                lblMsg.setStyle("-fx-font-size:12px;-fx-text-fill:"+fg+";"
                        +(nonLu?"-fx-font-weight:bold;":""));
                textes.getChildren().add(lblMsg);

                String date = n.getDateEnvoi() != null ? n.getDateEnvoi().toLocalDate().toString() : "";
                Label lblDate = new Label("📅 " + date);
                lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:#94a3b8;");
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

        // ── Zone bouton ───────────────────────────────────────────
        HBox zoneBouton = new HBox();
        zoneBouton.setAlignment(Pos.CENTER_RIGHT);
        zoneBouton.setPadding(new Insets(12,24,18,24));
        zoneBouton.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:0 0 14 14;");
        Button btnFermer = creerBouton("  Fermer  ","#1e293b","white","#334155");
        btnFermer.setOnAction(e->fenetre.close());
        zoneBouton.getChildren().add(btnFermer);

        carte.getChildren().addAll(bandeTitre, ligne, sousEntete, scroll, zoneBouton);
        overlay.getChildren().add(carte);

        animerEtAfficher(carte, fenetre);
    }

    // ── Alerte standard ───────────────────────────────────────────
    private static boolean afficher(Type type, String titre, String message, boolean avecAnnuler) {
        final boolean[] resultat = {false};
        Stage fenetre = new Stage();
        fenetre.initModality(Modality.APPLICATION_MODAL);
        fenetre.initStyle(StageStyle.TRANSPARENT);
        fenetre.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setPrefSize(520,320);

        VBox carte = new VBox(0); carte.setMaxWidth(430);
        DropShadow ombre=new DropShadow(); ombre.setRadius(36);ombre.setOffsetY(10);ombre.setColor(Color.color(0,0,0,0.35));
        carte.setEffect(ombre);
        carte.setStyle("-fx-background-color:"+type.fond+";-fx-background-radius:14;");

        HBox bandeTitre=new HBox(10); bandeTitre.setAlignment(Pos.CENTER_LEFT);
        bandeTitre.setPadding(new Insets(15,20,15,20));
        bandeTitre.setStyle("-fx-background-color:"+type.pri+";-fx-background-radius:14 14 0 0;");
        Label lblLogo=new Label("🎓"); lblLogo.setStyle("-fx-font-size:16px;");
        Label lblSep=new Label("|"); lblSep.setStyle("-fx-text-fill:rgba(255,255,255,0.4);-fx-font-size:14px;");
        Label lblIcone=new Label(type.icone); lblIcone.setStyle("-fx-font-size:16px;");
        Label lblTitre=new Label(titre!=null&&!titre.isEmpty()?titre:type.defTitre);
        lblTitre.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Region esp=new Region(); HBox.setHgrow(esp,Priority.ALWAYS);
        Label lblApp=new Label("UNIV-SCHEDULER");
        lblApp.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.6);-fx-font-weight:bold;");
        Button btnX=new Button("✕");
        btnX.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:50;-fx-min-width:24;-fx-min-height:24;-fx-cursor:hand;-fx-padding:0;");
        btnX.setOnMouseEntered(e->btnX.setStyle(btnX.getStyle().replace("0.15","0.35")));
        btnX.setOnMouseExited(e ->btnX.setStyle(btnX.getStyle().replace("0.35","0.15")));
        btnX.setOnAction(e->{resultat[0]=false;fenetre.close();});
        bandeTitre.getChildren().addAll(lblLogo,lblSep,lblIcone,lblTitre,esp,lblApp,btnX);

        Region ligne=new Region(); ligne.setPrefHeight(3);
        ligne.setStyle("-fx-background-color:"+couleurBtn(type)+";");

        HBox corps=new HBox(14); corps.setPadding(new Insets(22,24,10,24)); corps.setAlignment(Pos.TOP_LEFT);
        Region barre=new Region(); barre.setPrefWidth(4);barre.setPrefHeight(55);barre.setMinHeight(Region.USE_PREF_SIZE);
        barre.setStyle("-fx-background-color:"+couleurBtn(type)+";-fx-background-radius:4;");
        Label lblMsg=new Label(message); lblMsg.setWrapText(true);lblMsg.setMaxWidth(350);
        lblMsg.setTextAlignment(TextAlignment.LEFT);
        lblMsg.setStyle("-fx-font-size:13px;-fx-text-fill:"+type.txt+";-fx-line-spacing:4;");
        corps.getChildren().addAll(barre,lblMsg);

        HBox zoneBoutons=new HBox(12); zoneBoutons.setAlignment(Pos.CENTER_RIGHT);
        zoneBoutons.setPadding(new Insets(14,24,20,24));
        String cp=couleurBtn(type);
        if(avecAnnuler){
            Button btnAnnuler=creerBouton("Annuler","#e2e8f0","#1e293b","#cbd5e1");
            btnAnnuler.setOnAction(e->{resultat[0]=false;fenetre.close();});
            String labelOk=type==Type.ERREUR?"🗑 Supprimer":"✔ Confirmer";
            Button btnOk=creerBouton(labelOk,cp,"white",assombrir(cp));
            btnOk.setOnAction(e->{resultat[0]=true;fenetre.close();});
            zoneBoutons.getChildren().addAll(btnAnnuler,btnOk);
        } else {
            Button btnOk=creerBouton("  OK  ",cp,"white",assombrir(cp));
            btnOk.setMinWidth(100);
            btnOk.setOnAction(e->{resultat[0]=true;fenetre.close();});
            zoneBoutons.getChildren().add(btnOk);
        }

        carte.getChildren().addAll(bandeTitre,ligne,corps,zoneBoutons);
        overlay.getChildren().add(carte);

        animerEtAfficher(carte, fenetre);
        return resultat[0];
    }

    // ── Helpers partagés ──────────────────────────────────────────
    private static void animerEtAfficher(VBox carte, Stage fenetre) {
        carte.setOpacity(0); carte.setTranslateY(-14);
        FadeTransition ft=new FadeTransition(Duration.millis(220),carte); ft.setFromValue(0);ft.setToValue(1);ft.play();
        TranslateTransition tt=new TranslateTransition(Duration.millis(220),carte); tt.setFromY(-14);tt.setToY(0);tt.play();
        Scene scene=new Scene((StackPane)carte.getParent()); scene.setFill(Color.TRANSPARENT);
        fenetre.setScene(scene); fenetre.showAndWait();
    }

    private static Button creerBouton(String texte,String bg,String fg,String hover){
        Button b=new Button(texte);
        String base="-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-weight:bold;"
                +"-fx-padding:9 22;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13px;";
        b.setStyle(base);
        b.setOnMouseEntered(e->b.setStyle(base.replace(bg,hover)));
        b.setOnMouseExited(e ->b.setStyle(base));
        return b;
    }

    private static Label creerPill(String texte, String bg, String fg) {
        Label l = new Label(texte);
        l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";"
                +"-fx-font-size:10px;-fx-font-weight:bold;"
                +"-fx-padding:3 9;-fx-background-radius:20;");
        return l;
    }

    private static String assombrir(String hex){
        try{Color c=Color.web(hex);return String.format("#%02x%02x%02x",
                (int)(c.getRed()*0.82*255),(int)(c.getGreen()*0.82*255),(int)(c.getBlue()*0.82*255));}
        catch(Exception e){return hex;}
    }
}