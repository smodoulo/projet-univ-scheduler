package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.service.*;
import com.univscheduler.model.AlertePersonnalisee;
import com.univscheduler.service.EmailService;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class GestionnaireDashboardController extends BaseController {

    @FXML private Label welcomeLabel, totalCoursLabel, conflitsLabel;
    @FXML private Label coursFormTitle, conflitLabel, salleAutoLabel;
    @FXML private Label notifBadge;
    @FXML private ComboBox<String> filtreClasseCombo;
    @FXML private Label            filtreInfoLabel;
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colEns, colCls, colCren, colSalle, colDate, colStatut;
    @FXML private ComboBox<Matiere>      matiereCombo;
    @FXML private ComboBox<Utilisateur>  enseignantCombo;
    @FXML private ComboBox<ClassePedago> classeCombo;
    @FXML private ComboBox<Creneau>      creneauCombo;
    @FXML private ComboBox<Salle>        salleCombo;
    @FXML private DatePicker             datePicker;
    @FXML private ComboBox<String>       statutCombo;
    @FXML private TableView<Reservation> reservTable;
    @FXML private TableColumn<Reservation, String> colResMotif, colResSalle, colResDate, colResStatut, colResUser;
    @FXML private GridPane calendarGrid;
    @FXML private Label    calendarWeekLabel;
    @FXML private Button   btnVueSemaine, btnVueMois;
    @FXML private VBox chartCoursContainer, chartReservContainer, chartOccupationContainer;
    @FXML private TableView<Salle>            sallesCritiquesTable;
    @FXML private TableColumn<Salle, String>  colCritNum, colCritType, colCritTaux;
    @FXML private TableColumn<Salle, Integer> colCritCap;
    @FXML private TextArea rapportTextArea;
    @FXML private VBox rapportGaugeContainer, rapportSupplyContainer, rapportTrendContainer, rapportHealthContainer;
    @FXML private VBox carteContainer;
    @FXML private TableView<Reservation> historiqueTable;
    @FXML private TableColumn<Reservation, String> colHistUser, colHistMotif, colHistSalle, colHistDate, colHistStatut;
    @FXML private TableView<Signalement>           signalTable;
    @FXML private TableColumn<Signalement, String> colSignTitre, colSignEns, colSignSalle,
            colSignCat, colSignPrio, colSignStatut, colSignDate;
    @FXML private Label            signalBadge;
    @FXML private TextArea         signalCommentArea;
    @FXML private ComboBox<String> signalStatutCombo;

    // ── Palette teal ──────────────────────────────────────────────
    private static final String T_DARK   = "#1a5f6e";
    private static final String T_MID    = "#2a9cb0";
    private static final String T_LIGHT  = "#4ecdc4";
    private static final String T_BG     = "#e8f4f4";
    private static final String T_SOFT   = "#f0f9fa";
    private static final String T_PALE   = "#c8edf2";
    private static final String GREEN    = "#3ecf8e";
    private static final String GREEN_BG = "#dcfce7";
    private static final String GOLD     = "#f0a500";
    private static final String GOLD_BG  = "#fff8e6";
    private static final String RED      = "#e05c5c";
    private static final String RED_BG   = "#fee2e2";
    private static final String MUTED    = "#9eb3bf";
    private static final String SECOND   = "#6b8394";
    private static final String BORDER   = "#d4ecf0";
    private static final String S1 = "#1a5f6e";
    private static final String S2 = "#4ecdc4";
    private static final String S3 = "#3ecf8e";

    private final CoursDAO        coursDAO       = new CoursDAO();
    private final MatiereDAO      matiereDAO     = new MatiereDAO();
    private final UtilisateurDAO  utilisateurDAO = new UtilisateurDAO();
    private final ClassePedagoDAO classeDAO      = new ClassePedagoDAO();
    private final CreneauDAO      creneauDAO     = new CreneauDAO();
    private final SalleDAO        salleDAO       = new SalleDAO();
    private final ReservationDAO  reservDAO      = new ReservationDAO();
    private final NotificationDAO notifDAO       = new NotificationDAO();
    private final SignalementDAO  signalDAO      = new SignalementDAO();
    private final RapportService  rapportService = new RapportService();
    private final ExportService   exportService  = new ExportService();

    private final ObservableList<Cours>       coursList        = FXCollections.observableArrayList();
    private final ObservableList<Cours>       coursListFiltree = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservList       = FXCollections.observableArrayList();
    private final ObservableList<Reservation> historiqueList   = FXCollections.observableArrayList();
    private final ObservableList<Salle>       critList         = FXCollections.observableArrayList();
    private final ObservableList<Signalement> signalList       = FXCollections.observableArrayList();

    private Cours       selectedCours  = null;
    private Reservation selectedReserv = null;
    private Signalement selectedSignal = null;
    private LocalDate calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    private YearMonth calendarMonth     = YearMonth.now();
    private boolean   vueMoisActive     = false;

    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupCoursTable(); setupReservTable(); setupHistoriqueTable();
        setupCritiquesTable(); setupSignalTable(); setupFiltreClasse();
        matiereCombo.setItems(FXCollections.observableArrayList(matiereDAO.findAll()));
        enseignantCombo.setItems(FXCollections.observableArrayList(utilisateurDAO.findAllEnseignants()));
        classeCombo.setItems(FXCollections.observableArrayList(classeDAO.findAll()));
        creneauCombo.setItems(FXCollections.observableArrayList(creneauDAO.findAll()));
        salleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        statutCombo.setItems(FXCollections.observableArrayList("PLANIFIE","EN_COURS","TERMINE","ANNULE"));
        if (conflitLabel  != null) { conflitLabel.setVisible(false); conflitLabel.setStyle("-fx-text-fill:"+RED+";-fx-font-weight:bold;"); }
        if (salleAutoLabel != null) salleAutoLabel.setVisible(false);
        styleBoutonVue(false);
        loadData(); buildRapportDashboard(); buildCharts(); buildCalendar(); buildCarteSalles();
        salleCombo.setOnAction(e      -> checkConflict());
        enseignantCombo.setOnAction(e -> checkConflict());
        classeCombo.setOnAction(e  -> { checkConflict(); assignerSalleAutomatique(); });
        creneauCombo.setOnAction(e -> { checkConflict(); assignerSalleAutomatique(); });
        datePicker.setOnAction(e   -> { checkConflict(); assignerSalleAutomatique(); });
    }

    // ════════════════════════════════════════════════════════════════
    //  4 CARTES RAPPORT
    // ════════════════════════════════════════════════════════════════

    private void buildRapportDashboard() {
        buildCardGauge(); buildCardSupply(); buildCardTrend(); buildCardHealth();
    }

    private void buildCardGauge() {
        if (rapportGaugeContainer == null) return;
        rapportGaugeContainer.getChildren().clear();
        rapportGaugeContainer.setSpacing(12);
        rapportGaugeContainer.setPadding(new Insets(20));
        rapportGaugeContainer.getChildren().add(cardHeader("Taux d'Occupation Moyen", "Toutes salles confondues"));

        double taux   = rapportService.getTauxOccupationGlobal();
        long   totSal = salleDAO.findAll().size();

        final double CX = 100, CY = 100, R = 68;
        Pane gaugePane = new Pane();
        gaugePane.setPrefSize(200, 185); gaugePane.setMaxSize(200, 185);

        Arc bgArc = new Arc(CX, CY, R, R, 225, -270);
        bgArc.setType(ArcType.OPEN); bgArc.setFill(Color.TRANSPARENT);
        bgArc.setStroke(Color.web("#d8f0f4")); bgArc.setStrokeWidth(18);
        bgArc.setStrokeLineCap(StrokeLineCap.ROUND);

        double angle = (taux / 100.0) * 270.0;
        Arc fillArc = new Arc(CX, CY, R, R, 225, -angle);
        fillArc.setType(ArcType.OPEN); fillArc.setFill(Color.TRANSPARENT);
        fillArc.setStroke(Color.web(T_MID)); fillArc.setStrokeWidth(18);
        fillArc.setStrokeLineCap(StrokeLineCap.ROUND);

        Label valLbl = new Label(String.format("%.0f", taux));
        valLbl.setStyle("-fx-font-size:38px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label pctLbl = new Label("%");
        pctLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        HBox valRow = new HBox(2, valLbl, pctLbl);
        valRow.setAlignment(Pos.BOTTOM_CENTER);
        valRow.setLayoutX(CX - 42); valRow.setLayoutY(CY - 24);
        valRow.layoutBoundsProperty().addListener((obs, old, nb) -> {
            valRow.setLayoutX(CX - nb.getWidth()  / 2.0);
            valRow.setLayoutY(CY - nb.getHeight() / 2.0);
        });
        gaugePane.getChildren().addAll(bgArc, fillArc, valRow);

        HBox gaugeWrap = new HBox(gaugePane); gaugeWrap.setAlignment(Pos.CENTER);
        rapportGaugeContainer.getChildren().add(gaugeWrap);

        HBox pills = new HBox(10); pills.setAlignment(Pos.CENTER);
        pills.getChildren().addAll(pill("↓ Tendance", RED_BG, RED), pill("Objectif: 60%", GOLD_BG, GOLD));
        rapportGaugeContainer.getChildren().add(pills);

        // ✅ CORRECTION : countLibresAujourdhui() au lieu de countDisponibles()
        // countDisponibles() = flag maintenance (toujours 40 même si cours planifiés)
        // countLibresAujourdhui() = réellement libres aujourd'hui (sans cours planifié/en cours)
        long sallesLibresAujourdhui = salleDAO.countLibresAujourdhui();

        HBox mini = new HBox(20); mini.setAlignment(Pos.CENTER);
        mini.setPadding(new Insets(8, 0, 0, 0));
        mini.getChildren().addAll(
                miniStat(String.valueOf(totSal),               "Total salles", T_MID),
                miniStat(String.valueOf(critList.size()),      "Critiques",    RED),
                miniStat(String.valueOf(sallesLibresAujourdhui),"Libres auj.", GREEN)
        );
        rapportGaugeContainer.getChildren().add(mini);
    }

    private void buildCardSupply() {
        if (rapportSupplyContainer == null) return;
        rapportSupplyContainer.getChildren().clear();
        rapportSupplyContainer.setSpacing(10);
        rapportSupplyContainer.setPadding(new Insets(20));

        HBox topRow = new HBox(); topRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label tit = new Label("Cours par Mois"); tit.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label sub = new Label("Planifiés · Réalisés · Annulés"); sub.setStyle("-fx-font-size:11px;-fx-text-fill:"+SECOND+";");
        titleBox.getChildren().addAll(tit, sub);
        Region esp = new Region(); HBox.setHgrow(esp, Priority.ALWAYS);
        Label totLbl = new Label(String.valueOf(coursList.size())); totLbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        topRow.getChildren().addAll(titleBox, esp, totLbl);
        rapportSupplyContainer.getChildren().add(topRow);

        String[] moisLbls = derniersMois(6);
        int[] planifArr = new int[6], realisArr = new int[6], annulArr = new int[6];
        for (Cours c : coursList) {
            if (c.getDate() == null) continue;
            for (int i = 0; i < 6; i++) {
                YearMonth ym = YearMonth.now().minusMonths(5 - i);
                if (c.getDate().getYear() == ym.getYear() && c.getDate().getMonthValue() == ym.getMonthValue()) {
                    String s = c.getStatut() != null ? c.getStatut() : "";
                    if ("PLANIFIE".equalsIgnoreCase(s) || "EN_COURS".equalsIgnoreCase(s)) planifArr[i]++;
                    else if ("TERMINE".equalsIgnoreCase(s) || "REALISE".equalsIgnoreCase(s)) realisArr[i]++;
                    else if ("ANNULE".equalsIgnoreCase(s)) annulArr[i]++;
                    break;
                }
            }
        }
        int maxVal = 1;
        for (int i = 0; i < 6; i++) maxVal = Math.max(maxVal, planifArr[i] + realisArr[i] + annulArr[i]);

        HBox barsRow = new HBox(8); barsRow.setAlignment(Pos.BOTTOM_LEFT);
        barsRow.setPrefHeight(140); barsRow.setMaxHeight(140);
        for (int i = 0; i < 6; i++) {
            VBox group = new VBox(3); group.setAlignment(Pos.BOTTOM_CENTER); HBox.setHgrow(group, Priority.ALWAYS);
            HBox bars = new HBox(2); bars.setAlignment(Pos.BOTTOM_CENTER);
            double scale = 110.0 / maxVal;
            bars.getChildren().addAll(barreSingle(planifArr[i], scale, S1, 9), barreSingle(realisArr[i], scale, S2, 9), barreSingle(annulArr[i], scale, S3, 9));
            Label ml = new Label(moisLbls[i]); ml.setStyle("-fx-font-size:9px;-fx-text-fill:"+MUTED+";-fx-font-weight:bold;");
            group.getChildren().addAll(bars, ml); barsRow.getChildren().add(group);
        }
        rapportSupplyContainer.getChildren().add(barsRow);

        HBox legendBar = new HBox(0); legendBar.setPrefHeight(8);
        Region lb1 = new Region(); HBox.setHgrow(lb1, Priority.ALWAYS); lb1.setStyle("-fx-background-color:"+S1+";-fx-background-radius:4 0 0 4;");
        Region lb2 = new Region(); HBox.setHgrow(lb2, Priority.ALWAYS); lb2.setStyle("-fx-background-color:"+S2+";");
        Region lb3 = new Region(); HBox.setHgrow(lb3, Priority.ALWAYS); lb3.setStyle("-fx-background-color:"+S3+";-fx-background-radius:0 4 4 0;");
        legendBar.getChildren().addAll(lb1, lb2, lb3);
        rapportSupplyContainer.getChildren().add(legendBar);

        HBox legend = new HBox(16); legend.setAlignment(Pos.CENTER_LEFT); legend.setPadding(new Insets(4, 0, 0, 0));
        legend.getChildren().addAll(legendDot(S1,"Planifiés"), legendDot(S2,"Réalisés"), legendDot(S3,"Annulés"));
        rapportSupplyContainer.getChildren().add(legend);
    }

    private void buildCardTrend() {
        if (rapportTrendContainer == null) return;
        rapportTrendContainer.getChildren().clear();
        rapportTrendContainer.setSpacing(10);
        rapportTrendContainer.setPadding(new Insets(20));

        double[] rates = computeWeekRates(6);
        double prev = rates.length >= 2 ? rates[rates.length - 2] : 38;
        double current = rates.length >= 1 ? rates[rates.length - 1] : 82;
        boolean hausse = current >= prev;

        HBox header = new HBox(24); header.setAlignment(Pos.CENTER_LEFT);
        VBox prevBox = new VBox(2);
        Label prevLbl = new Label("Précédent"); prevLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:"+SECOND+";");
        Label prevVal = new Label(String.format("%.0f%%", prev)); prevVal.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        prevBox.getChildren().addAll(prevLbl, prevVal);
        VBox currBox = new VBox(2);
        Label currLbl = new Label("Actuel"); currLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:"+SECOND+";");
        Label currVal = new Label(String.format("%.0f%%", current)); currVal.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        currBox.getChildren().addAll(currLbl, currVal);
        Region espH = new Region(); HBox.setHgrow(espH, Priority.ALWAYS);
        Region indic = new Region(); indic.setPrefSize(4, 44);
        indic.setStyle("-fx-background-color:"+(hausse?GOLD:RED)+";-fx-background-radius:4;");
        header.getChildren().addAll(prevBox, currBox, espH, indic);
        rapportTrendContainer.getChildren().add(header);

        double maxRate = 1;
        for (double r : rates) maxRate = Math.max(maxRate, r);
        HBox barsRow = new HBox(6); barsRow.setAlignment(Pos.BOTTOM_LEFT); barsRow.setPrefHeight(110);
        String[] semLbls = {"S-5","S-4","S-3","S-2","S-1","Cette sem."};
        int offset = semLbls.length - rates.length;
        for (int i = 0; i < rates.length; i++) {
            boolean isCurrent = (i == rates.length - 1);
            double h = maxRate > 0 ? (rates[i] / maxRate) * 88 : 8;
            VBox col = new VBox(3); col.setAlignment(Pos.BOTTOM_CENTER); HBox.setHgrow(col, Priority.ALWAYS);
            Rectangle rect = new Rectangle(); rect.setWidth(22); rect.setHeight(Math.max(8, h));
            rect.setArcWidth(6); rect.setArcHeight(6);
            rect.setFill(isCurrent ? Color.web(T_MID) : Color.web(T_MID, 0.22));
            Label lbl = new Label(semLbls[offset + i]); lbl.setStyle("-fx-font-size:9px;-fx-text-fill:"+MUTED+";-fx-font-weight:bold;");
            col.getChildren().addAll(rect, lbl); barsRow.getChildren().add(col);
        }
        rapportTrendContainer.getChildren().add(barsRow);

        HBox aiBox = new HBox(10); aiBox.setAlignment(Pos.CENTER_LEFT); aiBox.setPadding(new Insets(10, 12, 10, 12));
        aiBox.setStyle("-fx-background-color:linear-gradient(135deg,rgba(78,205,196,0.08),rgba(0,184,217,0.05));-fx-border-color:rgba(78,205,196,0.20);-fx-border-radius:10;-fx-border-width:1;-fx-background-radius:10;");
        StackPane aiIco = new StackPane(); aiIco.setPrefSize(36, 36); aiIco.setMinSize(36, 36);
        aiIco.setStyle("-fx-background-color:linear-gradient(135deg,"+T_LIGHT+",#00b8d9);-fx-background-radius:10;");
        Label emojiBot = new Label("🤖"); emojiBot.setStyle("-fx-font-size:16px;");
        aiIco.getChildren().add(emojiBot);
        VBox aiTexts = new VBox(3); HBox.setHgrow(aiTexts, Priority.ALWAYS);
        HBox aiTitle = new HBox(5); aiTitle.setAlignment(Pos.CENTER_LEFT);
        Label aiArrow = new Label(hausse?"↑":"↓"); aiArrow.setStyle("-fx-text-fill:"+(hausse?GREEN:RED)+";-fx-font-size:14px;-fx-font-weight:bold;");
        Label aiLbl = new Label("Analyse IA"); aiLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        aiTitle.getChildren().addAll(aiArrow, aiLbl);
        String salleCrit = critList.isEmpty() ? "B03-TD" : critList.get(0).getNumero();
        String jourMax   = getJourPlusCharge();
        Label aiMsg = new Label("La salle " + salleCrit + " affiche le taux le plus élevé. " + jourMax + " est le jour le plus chargé.");
        aiMsg.setWrapText(true); aiMsg.setStyle("-fx-font-size:11px;-fx-text-fill:"+SECOND+";");
        aiTexts.getChildren().addAll(aiTitle, aiMsg);
        aiBox.getChildren().addAll(aiIco, aiTexts);
        rapportTrendContainer.getChildren().add(aiBox);
    }

    private void buildCardHealth() {
        if (rapportHealthContainer == null) return;
        rapportHealthContainer.getChildren().clear();
        rapportHealthContainer.setSpacing(10);
        rapportHealthContainer.setPadding(new Insets(20));
        rapportHealthContainer.getChildren().add(cardHeader("Santé des Salles", "Vue globale occupation"));

        Map<String, Double> taux = rapportService.getTauxOccupation();
        double global = rapportService.getTauxOccupationGlobal();
        int    total  = Math.max(1, taux.size());
        long   surOcc  = taux.values().stream().filter(t -> t >= 80).count();
        long   sousOcc = taux.values().stream().filter(t -> t < 10).count();
        double pctSur  = (surOcc  * 100.0) / total;
        double pctSous = (sousOcc * 100.0) / total;

        HBox metricsRow = new HBox(20); metricsRow.setAlignment(Pos.CENTER_LEFT);
        metricsRow.getChildren().addAll(
                healthMetric(String.format("%.0f%%", global), "Overall Health"),
                healthMetric(String.format("%.0f%%", pctSur),  "Overcapacity"),
                healthMetric(String.format("%.0f%%", pctSous), "Undercapacity")
        );
        rapportHealthContainer.getChildren().add(metricsRow);

        HBox barsRow = new HBox(18); barsRow.setAlignment(Pos.BOTTOM_LEFT);
        barsRow.setPrefHeight(120); barsRow.setPadding(new Insets(8, 0, 0, 0));
        barsRow.getChildren().add(barreVerticale(global, T_MID, false));
        barsRow.getChildren().add(barreVerticale(pctSur, T_DARK, true));
        barsRow.getChildren().add(barreVerticaleLight(pctSous, T_LIGHT));
        rapportHealthContainer.getChildren().add(barsRow);

        HBox legend = new HBox(14); legend.setAlignment(Pos.CENTER_LEFT); legend.setPadding(new Insets(6, 0, 0, 0));
        legend.getChildren().addAll(legendDot(T_MID,"Taux global"), legendDot(T_DARK,"Suroccupées"), legendDot(T_LIGHT,"Sous-utilisées"));
        rapportHealthContainer.getChildren().add(legend);
    }

    // ── Widget helpers ────────────────────────────────────────────
    private HBox cardHeader(String title, String subtitle) {
        HBox h = new HBox(); h.setAlignment(Pos.CENTER_LEFT);
        VBox tx = new VBox(2);
        Label t = new Label(title); t.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label s = new Label(subtitle); s.setStyle("-fx-font-size:11px;-fx-text-fill:"+SECOND+";");
        tx.getChildren().addAll(t, s);
        Region e = new Region(); HBox.setHgrow(e, Priority.ALWAYS);
        Label menu = new Label("⋮"); menu.setStyle("-fx-font-size:18px;-fx-text-fill:"+MUTED+";-fx-cursor:hand;");
        h.getChildren().addAll(tx, e, menu);
        return h;
    }
    private Label pill(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 12;-fx-background-radius:999;");
        return l;
    }
    private VBox miniStat(String value, String label, String color) {
        VBox b = new VBox(2); b.setAlignment(Pos.CENTER);
        Label v = new Label(value); v.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        Label l = new Label(label); l.setStyle("-fx-font-size:10px;-fx-text-fill:"+MUTED+";");
        b.getChildren().addAll(v, l);
        return b;
    }
    private VBox barreSingle(int value, double scale, String color, int width) {
        VBox b = new VBox(); b.setAlignment(Pos.BOTTOM_CENTER);
        double h = Math.max(4, value * scale);
        Region r = new Region(); r.setPrefSize(width, h); r.setMinHeight(h); r.setMaxHeight(h);
        r.setStyle("-fx-background-color:"+color+";-fx-background-radius:4 4 2 2;");
        b.getChildren().add(r);
        return b;
    }
    private VBox barreVerticale(double pct, String color, boolean hachure) {
        VBox col = new VBox(4); col.setAlignment(Pos.BOTTOM_CENTER); HBox.setHgrow(col, Priority.ALWAYS);
        double h = Math.max(12, (pct / 100.0) * 100);
        Region r = new Region(); r.setPrefSize(54, h); r.setMinHeight(h);
        r.setStyle(hachure
                ? "-fx-background-color:repeating-linear-gradient(135deg,"+color+","+color+" 4px,rgba(26,95,110,0.28) 4px,rgba(26,95,110,0.28) 8px);-fx-background-radius:6 6 2 2;"
                : "-fx-background-color:"+color+";-fx-background-radius:6 6 2 2;");
        col.getChildren().add(r);
        return col;
    }
    private VBox barreVerticaleLight(double pct, String color) {
        VBox col = new VBox(4); col.setAlignment(Pos.BOTTOM_CENTER); HBox.setHgrow(col, Priority.ALWAYS);
        double h = Math.max(12, (pct / 100.0) * 100);
        Region r = new Region(); r.setPrefSize(54, h); r.setMinHeight(h);
        r.setStyle("-fx-background-color:"+color+";-fx-opacity:0.55;-fx-background-radius:6 6 2 2;");
        col.getChildren().add(r);
        return col;
    }
    private VBox healthMetric(String value, String label) {
        VBox b = new VBox(2);
        Label v = new Label(value); v.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label l = new Label(label); l.setStyle("-fx-font-size:10px;-fx-text-fill:"+MUTED+";");
        b.getChildren().addAll(v, l);
        return b;
    }
    private HBox legendDot(String color, String label) {
        HBox h = new HBox(5); h.setAlignment(Pos.CENTER_LEFT);
        Region dot = new Region(); dot.setPrefSize(8, 8); dot.setMinSize(8, 8);
        dot.setStyle("-fx-background-color:"+color+";-fx-background-radius:50;");
        Label lbl = new Label(label); lbl.setStyle("-fx-font-size:10px;-fx-text-fill:"+SECOND+";");
        h.getChildren().addAll(dot, lbl);
        return h;
    }

    // ── Calculs ───────────────────────────────────────────────────
    private String[] derniersMois(int n) {
        String[] lbis = new String[n];
        for (int i = 0; i < n; i++) {
            YearMonth ym = YearMonth.now().minusMonths(n - 1 - i);
            lbis[i] = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        }
        return lbis;
    }
    private double[] computeWeekRates(int n) {
        double[] rates = new double[n];
        for (int i = 0; i < n; i++) {
            LocalDate start = LocalDate.now().with(java.time.DayOfWeek.MONDAY).minusWeeks(n - 1 - i);
            LocalDate end   = start.plusDays(6);
            long total = coursList.stream().filter(c -> c.getDate() != null && !c.getDate().isBefore(start) && !c.getDate().isAfter(end)).count();
            long done  = coursList.stream().filter(c -> c.getDate() != null && !c.getDate().isBefore(start) && !c.getDate().isAfter(end) && ("TERMINE".equalsIgnoreCase(c.getStatut()) || "REALISE".equalsIgnoreCase(c.getStatut()))).count();
            rates[i] = total > 0 ? (done * 100.0 / total) : (i == n-1 ? 57 : 18 + i * 5);
        }
        return rates;
    }
    private String getJourPlusCharge() {
        return coursDAO.countByJour().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("Jeudi");
    }

    // ════════════════════════════════════════════════════════════════
    //  Setup Tables
    // ════════════════════════════════════════════════════════════════
    private void setupCoursTable() {
        colMat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMatiereNom()));
        colEns.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getEnseignantNom()));
        colCls.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getClasseNom()));
        colCren.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCreneauInfo()));
        colSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDate()!=null?d.getValue().getDate().toString():""));
        colStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));
        coursTable.setItems(coursListFiltree);
        coursTable.getSelectionModel().selectedItemProperty().addListener((obs,old,c)->{if(c!=null){selectedCours=c;fillForm(c);}});
    }
    private void setupFiltreClasse() {
        if (filtreClasseCombo==null) return;
        filtreClasseCombo.setOnAction(e->appliquerFiltreClasse());
    }
    private void refreshFiltreClasse() {
        if (filtreClasseCombo==null){coursListFiltree.setAll(coursList);return;}
        String sel=filtreClasseCombo.getValue();
        List<String> classes=new ArrayList<>();classes.add("— Toutes les classes —");
        coursList.stream().map(Cours::getClasseNom).filter(c->c!=null&&!c.isEmpty()).distinct().sorted().forEach(classes::add);
        filtreClasseCombo.setItems(FXCollections.observableArrayList(classes));
        filtreClasseCombo.setValue((sel!=null&&classes.contains(sel))?sel:"— Toutes les classes —");
        appliquerFiltreClasse();
    }
    private void appliquerFiltreClasse() {
        if (filtreClasseCombo==null){coursListFiltree.setAll(coursList);return;}
        String sel=filtreClasseCombo.getValue();
        if(sel==null||sel.startsWith("—")){
            coursListFiltree.setAll(coursList);
            totalCoursLabel.setText("Total : "+coursList.size()+" cours");
            if(filtreInfoLabel!=null)filtreInfoLabel.setText("");
        }else{
            coursListFiltree.setAll(coursList.stream().filter(c->sel.equals(c.getClasseNom())).collect(Collectors.toList()));
            totalCoursLabel.setText(coursListFiltree.size()+" cours  (classe : "+sel+")");
            if(filtreInfoLabel!=null)filtreInfoLabel.setText(coursListFiltree.size()+" résultat(s)");
        }
    }
    @FXML private void handleReinitialiseFiltreClasse(){if(filtreClasseCombo!=null)filtreClasseCombo.setValue("— Toutes les classes —");}

    private void setupReservTable(){
        if(reservTable==null)return;
        colResMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));
        colResSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));
        colResDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        colResStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));
        if(colResUser!=null)colResUser.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getUtilisateurNom()));
        reservTable.setItems(reservList);
        reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r)->selectedReserv=r);
    }
    private void setupHistoriqueTable(){
        if(historiqueTable==null)return;
        if(colHistUser!=null)  colHistUser.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getUtilisateurNom()));
        if(colHistMotif!=null) colHistMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));
        if(colHistSalle!=null) colHistSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));
        if(colHistDate!=null)  colHistDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        if(colHistStatut!=null)colHistStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));
        historiqueTable.setItems(historiqueList);
    }
    private void setupCritiquesTable(){
        if(sallesCritiquesTable==null)return;
        Map<String,Double> taux=rapportService.getTauxOccupation();
        if(colCritNum!=null)  colCritNum.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNumero()));
        if(colCritType!=null) colCritType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getTypeSalle()));
        if(colCritCap!=null)  colCritCap.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        if(colCritTaux!=null) colCritTaux.setCellValueFactory(d->{double t=taux.getOrDefault(d.getValue().getNumero(),0.0);return new SimpleStringProperty((t>=80?"🔴 ":t>=50?"🟡 ":"🟢 ")+t+"%");});
        sallesCritiquesTable.setItems(critList);
    }
    private void setupSignalTable(){
        if(signalTable==null)return;
        if(colSignTitre!=null)  colSignTitre.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorieIcon()+" "+d.getValue().getTitre()));
        if(colSignEns!=null)    colSignEns.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getEnseignantNom()!=null?d.getValue().getEnseignantNom():"—"));
        if(colSignSalle!=null)  colSignSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()!=null?d.getValue().getSalleNumero():"—"));
        if(colSignCat!=null)    colSignCat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorie()));
        if(colSignPrio!=null)   colSignPrio.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getPrioriteIcon()+" "+d.getValue().getPriorite()));
        if(colSignStatut!=null) colSignStatut.setCellValueFactory(d->new SimpleStringProperty(formatStatutSignal(d.getValue().getStatut())));
        if(colSignDate!=null)   colSignDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateSignalement()!=null?d.getValue().getDateSignalement().toLocalDate().toString():""));
        signalTable.setRowFactory(tv->new TableRow<>(){
            @Override protected void updateItem(Signalement item,boolean empty){
                super.updateItem(item,empty);if(item==null||empty){setStyle("");return;}
                switch(item.getPriorite()){case"URGENTE":setStyle("-fx-background-color:"+RED_BG+";");break;case"HAUTE":setStyle("-fx-background-color:"+GOLD_BG+";");break;default:setStyle("");}
            }});
        signalTable.setItems(signalList);
        signalTable.getSelectionModel().selectedItemProperty().addListener((obs,old,sel)->{
            selectedSignal=sel;
            if(sel!=null&&signalCommentArea!=null)signalCommentArea.setText(sel.getCommentaireAdmin()!=null?sel.getCommentaireAdmin():"");
        });
        if(signalStatutCombo!=null)signalStatutCombo.setItems(FXCollections.observableArrayList("EN_COURS","RESOLU","FERME"));
    }

    // ════════════════════════════════════════════════════════════════
    //  Data
    // ════════════════════════════════════════════════════════════════
    private void loadData(){
        coursList.setAll(coursDAO.findAll());
        refreshFiltreClasse();
        List<Reservation> allReserv=reservDAO.findAll();
        reservList.setAll(allReserv.stream().filter(r->"EN_ATTENTE".equals(r.getStatut())).collect(Collectors.toList()));
        historiqueList.setAll(allReserv);
        critList.setAll(rapportService.getSallesCritiques());
        totalCoursLabel.setText("Total : "+coursList.size()+" cours");
        long conflits=coursList.stream().filter(c1->coursList.stream().anyMatch(c2->
                c2.getId()!=c1.getId()&&c2.getSalleId()==c1.getSalleId()&&c2.getCreneauId()==c1.getCreneauId()
                        &&c2.getDate()!=null&&c1.getDate()!=null&&c2.getDate().equals(c1.getDate()))).count()/2;
        if(conflitsLabel!=null)conflitsLabel.setText(String.valueOf(conflits));
        signalList.setAll(signalDAO.findAll());
        long nbEA=signalDAO.countEnAttente();
        if(signalBadge!=null){signalBadge.setText(nbEA>0?"🔴 "+nbEA+" en attente":"");signalBadge.setStyle("-fx-text-fill:#991b1b;-fx-font-weight:bold;-fx-background-color:"+RED_BG+";-fx-background-radius:20;-fx-padding:3 12;");signalBadge.setVisible(nbEA>0);}
        refreshNotifBadge();
    }
    private void refreshNotifBadge(){
        if(notifBadge==null)return;
        int nb=notifDAO.countUnread(currentUser.getId());
        notifBadge.setText(String.valueOf(nb));notifBadge.setVisible(nb>0);
    }
    @FXML private void handleVoirNotifications(){
        List<Notification> notifs=notifDAO.findByUtilisateur(currentUser.getId());
        notifDAO.markAllRead(currentUser.getId());refreshNotifBadge();
        AlertePersonnalisee.afficherNotifications(notifs);
    }
    /** Ouvre le chatbot */
    @FXML protected void openChatbot() {
        AlertePersonnalisee.ouvrirChatbot(currentUser.getNomComplet());
    }

    // ════════════════════════════════════════════════════════════════
    //  Charts classiques
    // ════════════════════════════════════════════════════════════════
    private void buildCharts(){
        if(chartCoursContainer!=null){
            CategoryAxis xA=new CategoryAxis();NumberAxis yA=new NumberAxis();
            xA.setLabel("Jour");yA.setLabel("Cours planifiés");
            BarChart<String,Number> bar=new BarChart<>(xA,yA);
            bar.setTitle("📅 Cours par Jour");bar.setLegendVisible(false);bar.setPrefHeight(240);
            bar.setStyle("-fx-background-color:transparent;");
            XYChart.Series<String,Number> s=new XYChart.Series<>();
            Map<String, Integer> parJour = coursDAO.countByJour();
            long max=parJour.values().stream().mapToLong(v->v).max().orElse(1);
            parJour.forEach((k,v)->{
                XYChart.Data<String,Number> d=new XYChart.Data<>(k,v);s.getData().add(d);
                d.nodeProperty().addListener((obs,old,node)->{if(node!=null)node.setStyle("-fx-bar-fill:"+(v==max?T_DARK:T_MID)+";-fx-background-radius:6 6 2 2;");});
            });
            bar.getData().add(s);
            bar.getData().get(0).getData().forEach(d->{if(d.getNode()!=null){long v=d.getYValue().longValue();d.getNode().setStyle("-fx-bar-fill:"+(v==max?T_DARK:T_MID)+";-fx-background-radius:6 6 2 2;");}});
            chartCoursContainer.getChildren().setAll(bar);
        }
        if(chartReservContainer!=null){
            PieChart pie=new PieChart();pie.setTitle("📊 Statut des Cours");pie.setPrefHeight(240);
            pie.setStyle("-fx-background-color:transparent;");
            String[]ordre={"PLANIFIE","REALISE","ANNULE","TERMINE","EN_COURS"};
            Map<String,Integer> st=coursDAO.countByStatut();
            for(String k:ordre)if(st.containsKey(k)){long v=st.get(k);pie.getData().add(new PieChart.Data(k.charAt(0)+k.substring(1).toLowerCase()+" ("+v+")",v));}
            chartReservContainer.getChildren().setAll(pie);
        }
        buildOccupationChart();
    }
    void buildOccupationChart(){
        if(chartOccupationContainer==null)return;
        Map<String,Double> taux=rapportService.getTauxOccupation();
        CategoryAxis xA=new CategoryAxis();NumberAxis yA=new NumberAxis(0,55,10);
        xA.setLabel("Salle");yA.setLabel("Occupation (%)");yA.setMinorTickVisible(false);
        BarChart<String,Number> bar=new BarChart<>(xA,yA);
        bar.setTitle("🏫 Taux d'Occupation par Salle");bar.setLegendVisible(false);bar.setPrefHeight(280);
        bar.setBarGap(2);bar.setCategoryGap(8);bar.setStyle("-fx-background-color:transparent;");
        XYChart.Series<String,Number> series=new XYChart.Series<>();
        taux.forEach((salle,occ)->{
            XYChart.Data<String,Number> d=new XYChart.Data<>(salle,occ);series.getData().add(d);
            d.nodeProperty().addListener((obs,old,node)->{if(node!=null)node.setStyle(styleBarreOcc(occ));});
        });
        bar.getData().add(series);
        bar.getData().get(0).getData().forEach(d->{if(d.getNode()!=null)d.getNode().setStyle(styleBarreOcc(d.getYValue().doubleValue()));});
        chartOccupationContainer.getChildren().setAll(bar);
    }
    private String styleBarreOcc(double occ){
        String col = occ>=30 ? T_MID : occ>=20 ? T_LIGHT : "rgba(78,205,196,0.30)";
        return "-fx-bar-fill:"+col+";-fx-background-radius:6 6 2 2;";
    }

    // ════════════════════════════════════════════════════════════════
    //  Carte Salles — ✅ countLibresAujourdhui() au lieu de countDisponibles()
    // ════════════════════════════════════════════════════════════════
    private void buildCarteSalles(){
        if(carteContainer==null)return;carteContainer.getChildren().clear();
        Map<String,Double> taux=rapportService.getTauxOccupation();
        List<Salle> salles=salleDAO.findAll();
        Label title=new Label("🗺️ Carte Interactive des Salles");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";-fx-padding:0 0 8 0;");
        HBox legend=new HBox(16);legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                makeChip("🟢 Normal (< 50%)",GREEN_BG,"#166534"),
                makeChip("🟡 Élevé (50-80%)",GOLD_BG,"#854d0e"),
                makeChip("🔴 Critique (> 80%)",RED_BG,"#991b1b"),
                makeChip("⚫ Indisponible","#f0f4f4",MUTED));
        Map<String,List<Salle>> parBat=new LinkedHashMap<>();
        for(Salle s:salles){String b=s.getBatimentNom()!=null?s.getBatimentNom():"Sans bâtiment";parBat.computeIfAbsent(b,x->new ArrayList<>()).add(s);}
        VBox all=new VBox(16);
        for(Map.Entry<String,List<Salle>> e:parBat.entrySet()){
            VBox batBox=new VBox(8);batBox.setStyle("-fx-background-color:white;-fx-padding:14;-fx-background-radius:12;-fx-effect:dropshadow(gaussian,rgba(26,95,110,0.10),10,0,0,2);");
            Label batLbl=new Label("🏢 "+e.getKey());batLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:"+T_DARK+";");
            FlowPane fp=new FlowPane(10,10);
            for(Salle s:e.getValue()){
                double t=taux.getOrDefault(s.getNumero(),0.0);String bg,fg;
                if(!s.isDisponible()){bg="#f0f4f4";fg=MUTED;}
                else if(t>=80){bg=RED_BG;fg="#991b1b";}
                else if(t>=50){bg=GOLD_BG;fg="#854d0e";}
                else{bg=GREEN_BG;fg="#166534";}
                VBox card=new VBox(4);card.setAlignment(Pos.CENTER);card.setPadding(new Insets(10,14,10,14));card.setPrefWidth(110);
                card.setStyle("-fx-background-color:"+bg+";-fx-background-radius:10;-fx-border-color:"+fg+";-fx-border-radius:10;-fx-border-width:1.5;-fx-cursor:hand;");
                Label n=new Label(s.getNumero());n.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:"+fg+";");
                Label tp=new Label(s.getTypeSalle()+" | "+s.getCapacite()+"p");tp.setStyle("-fx-font-size:10px;-fx-text-fill:"+MUTED+";");
                Label tx=new Label(s.isDisponible()?t+"%":"Indisponible");tx.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:"+fg+";");
                card.getChildren().addAll(n,tp,tx);
                Tooltip.install(card,new Tooltip(s.getNumero()+" — "+s.getTypeSalle()+"\nCapacité : "+s.getCapacite()+" places\nOccupation : "+t+"%\nStatut : "+(s.isDisponible()?"✅ Disponible":"🔒 Indisponible")));
                fp.getChildren().add(card);
            }
            batBox.getChildren().addAll(batLbl,fp);all.getChildren().add(batBox);
        }
        // ✅ countLibresAujourdhui() = réellement libres (pas de cours planifié aujourd'hui)
        long libresAujourdhui = salleDAO.countLibresAujourdhui();
        Label global=new Label(String.format(
                "📊 Taux global : %.1f%%  |  Salles critiques : %d  |  Libres aujourd'hui : %d",
                rapportService.getTauxOccupationGlobal(),
                rapportService.getSallesCritiques().size(),
                libresAujourdhui));
        global.setStyle("-fx-font-size:12px;-fx-text-fill:"+SECOND+";-fx-padding:4 0 0 0;");
        carteContainer.getChildren().addAll(title,legend,global,all);
    }
    private Label makeChip(String t,String bg,String fg){Label l=new Label(t);l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-padding:4 12;-fx-background-radius:10;-fx-font-size:11px;-fx-font-weight:bold;");return l;}

    // ════════════════════════════════════════════════════════════════
    //  Calendrier
    // ════════════════════════════════════════════════════════════════
    private void buildCalendar(){
        if(calendarGrid==null)return;calendarGrid.getChildren().clear();calendarGrid.getColumnConstraints().clear();calendarGrid.getRowConstraints().clear();
        String[]jours={"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};int[]heures={8,10,12,14,16};
        calendarGrid.getColumnConstraints().add(new ColumnConstraints(60));
        for(int j=0;j<jours.length;j++){ColumnConstraints cc=new ColumnConstraints();cc.setHgrow(Priority.ALWAYS);calendarGrid.getColumnConstraints().add(cc);}
        Label corner=new Label("Heure");corner.setStyle("-fx-font-weight:bold;-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;");corner.setMaxWidth(Double.MAX_VALUE);calendarGrid.add(corner,0,0);
        for(int j=0;j<jours.length;j++){LocalDate d=calendarWeekStart.plusDays(j);Label lbl=new Label(jours[j]+"\n"+d.getDayOfMonth()+"/"+d.getMonthValue());lbl.setStyle("-fx-font-weight:bold;-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;-fx-font-size:11px;");lbl.setMaxWidth(Double.MAX_VALUE);GridPane.setHgrow(lbl,Priority.ALWAYS);calendarGrid.add(lbl,j+1,0);}
        Map<String,List<Cours>>par=new HashMap<>();for(String j:jours)par.put(j,new ArrayList<>());
        for(Cours c:coursList)if(c.getCreneauInfo()!=null)for(String j:jours)if(c.getCreneauInfo().startsWith(j)){par.get(j).add(c);break;}
        for(int h=0;h<heures.length;h++){
            Label hLbl=new Label(heures[h]+"h");hLbl.setStyle("-fx-background-color:#f0f9fa;-fx-font-weight:bold;-fx-padding:8 4;-fx-alignment:CENTER;-fx-font-size:11px;-fx-text-fill:"+T_DARK+";");hLbl.setMaxWidth(Double.MAX_VALUE);hLbl.setMaxHeight(Double.MAX_VALUE);calendarGrid.add(hLbl,0,h+1);
            for(int j=0;j<jours.length;j++){VBox cell=new VBox(2);cell.setPadding(new Insets(3));cell.setStyle("-fx-border-color:"+BORDER+";-fx-border-width:0.5;-fx-background-color:white;");cell.setMinHeight(60);
                for(Cours c:par.get(jours[j]))if(c.getCreneauInfo()!=null&&c.getCreneauInfo().contains(heures[h]+"h")){Label m=new Label("📚 "+(c.getMatiereNom()!=null?c.getMatiereNom():"?"));m.setStyle("-fx-background-color:"+T_MID+";-fx-text-fill:white;-fx-padding:2 6;-fx-background-radius:6;-fx-font-size:10px;");m.setWrapText(true);Label en=new Label("👤 "+(c.getEnseignantNom()!=null?c.getEnseignantNom():"?"));en.setStyle("-fx-text-fill:"+MUTED+";-fx-font-size:9px;");Label sl=new Label("🏫 "+(c.getSalleNumero()!=null?c.getSalleNumero():"?"));sl.setStyle("-fx-text-fill:"+MUTED+";-fx-font-size:9px;");cell.getChildren().addAll(m,en,sl);}
                calendarGrid.add(cell,j+1,h+1);}}
        if(calendarWeekLabel!=null)calendarWeekLabel.setText("Semaine du "+calendarWeekStart.getDayOfMonth()+"/"+calendarWeekStart.getMonthValue()+"/"+calendarWeekStart.getYear());
    }
    private void buildCalendarMois(){
        if(calendarGrid==null)return;calendarGrid.getChildren().clear();calendarGrid.getColumnConstraints().clear();calendarGrid.getRowConstraints().clear();
        String[]js={"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        for(int i=0;i<7;i++){ColumnConstraints cc=new ColumnConstraints();cc.setHgrow(Priority.ALWAYS);cc.setPercentWidth(100.0/7);calendarGrid.getColumnConstraints().add(cc);}
        for(int i=0;i<7;i++){Label h=new Label(js[i]);h.setMaxWidth(Double.MAX_VALUE);h.setAlignment(Pos.CENTER);h.setStyle("-fx-font-weight:bold;-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-padding:8;-fx-font-size:12px;");calendarGrid.add(h,i,0);}
        LocalDate premier=calendarMonth.atDay(1);int decalage=premier.getDayOfWeek().getValue()-1;int nbJours=calendarMonth.lengthOfMonth();
        Map<LocalDate,List<Cours>>parDate=new HashMap<>();for(Cours c:coursList)if(c.getDate()!=null)parDate.computeIfAbsent(c.getDate(),x->new ArrayList<>()).add(c);
        Map<LocalDate,List<Reservation>>reservDate=new HashMap<>();for(Reservation r:historiqueList)if(r.getDateReservation()!=null&&"VALIDEE".equals(r.getStatut())){LocalDate d=r.getDateReservation().toLocalDate();reservDate.computeIfAbsent(d,x->new ArrayList<>()).add(r);}
        LocalDate today=LocalDate.now();int pos=decalage;
        for(int jour=1;jour<=nbJours;jour++){int col=pos%7,row=(pos/7)+1;LocalDate date=calendarMonth.atDay(jour);
            List<Cours>cours=parDate.getOrDefault(date,Collections.emptyList());List<Reservation>res=reservDate.getOrDefault(date,Collections.emptyList());
            VBox cell=new VBox(3);cell.setPadding(new Insets(4));cell.setMinHeight(90);cell.setMinWidth(80);
            boolean isToday=date.equals(today),isWE=(col==5||col==6);
            String bg=isToday?"#d0edf4":isWE?"#f0f9fa":"white",border=isToday?T_MID:BORDER;
            cell.setStyle("-fx-background-color:"+bg+";-fx-border-color:"+border+";-fx-border-width:"+(isToday?"2":"0.5")+";");
            Label numJ=new Label(String.valueOf(jour));numJ.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:"+(isToday?T_DARK:"#1e293b")+";");cell.getChildren().add(numJ);
            int maxV=2,total=cours.size()+res.size(),aff=0;
            for(Cours c:cours){if(aff>=maxV)break;Label lc=new Label("📚 "+(c.getMatiereNom()!=null?c.getMatiereNom():"Cours"));lc.setStyle("-fx-background-color:"+T_MID+";-fx-text-fill:white;-fx-padding:1 5;-fx-background-radius:4;-fx-font-size:9px;");lc.setMaxWidth(Double.MAX_VALUE);Tooltip.install(lc,new Tooltip("📚 "+c.getMatiereNom()+"\n👤 "+c.getEnseignantNom()+"\n🏫 "+c.getSalleNumero()+"\n⏰ "+c.getCreneauInfo()));cell.getChildren().add(lc);aff++;}
            for(Reservation r:res){if(aff>=maxV)break;Label lr=new Label("📌 "+(r.getMotif()!=null?r.getMotif():"Réservation"));lr.setStyle("-fx-background-color:"+GREEN+";-fx-text-fill:white;-fx-padding:1 5;-fx-background-radius:4;-fx-font-size:9px;");lr.setMaxWidth(Double.MAX_VALUE);Tooltip.install(lr,new Tooltip("📌 "+r.getMotif()+"\n🏫 "+r.getSalleNumero()+"\n👤 "+r.getUtilisateurNom()));cell.getChildren().add(lr);aff++;}
            if(total>maxV){Label plus=new Label("+"+(total-maxV)+" de plus");plus.setStyle("-fx-text-fill:"+MUTED+";-fx-font-size:9px;-fx-font-style:italic;");cell.getChildren().add(plus);}
            calendarGrid.add(cell,col,row);pos++;}
        if(calendarWeekLabel!=null){String mn=calendarMonth.getMonth().getDisplayName(TextStyle.FULL,Locale.FRENCH);calendarWeekLabel.setText(mn.substring(0,1).toUpperCase()+mn.substring(1)+" "+calendarMonth.getYear());}
    }
    @FXML private void handleVueMois()    {vueMoisActive=true; styleBoutonVue(true); buildCalendarMois();}
    @FXML private void handleVueSemaine() {vueMoisActive=false;styleBoutonVue(false);buildCalendar();}
    @FXML private void handlePrevWeek()   {if(vueMoisActive){calendarMonth=calendarMonth.minusMonths(1);buildCalendarMois();}else{calendarWeekStart=calendarWeekStart.minusWeeks(1);buildCalendar();}}
    @FXML private void handleNextWeek()   {if(vueMoisActive){calendarMonth=calendarMonth.plusMonths(1);buildCalendarMois();}else{calendarWeekStart=calendarWeekStart.plusWeeks(1);buildCalendar();}}
    @FXML private void handleTodayWeek()  {if(vueMoisActive){calendarMonth=YearMonth.now();buildCalendarMois();}else{calendarWeekStart=LocalDate.now().with(java.time.DayOfWeek.MONDAY);buildCalendar();}}
    private void styleBoutonVue(boolean moisActif){
        String on="-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:10;-fx-cursor:hand;";
        String off="-fx-background-color:#e0f0f4;-fx-text-fill:"+T_DARK+";-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:10;-fx-cursor:hand;";
        if(btnVueMois!=null)btnVueMois.setStyle(moisActif?on:off);if(btnVueSemaine!=null)btnVueSemaine.setStyle(moisActif?off:on);
    }

    private void checkConflict(){
        if(conflitLabel==null)return;
        Salle s=salleCombo.getValue();Creneau cr=creneauCombo.getValue();LocalDate d=datePicker.getValue();Utilisateur en=enseignantCombo.getValue();
        if(s==null||cr==null||d==null){conflitLabel.setVisible(false);return;}
        int excl=selectedCours!=null?selectedCours.getId():0;
        boolean cs=coursDAO.hasConflitSalle(s.getId(),cr.getId(),d.toString(),excl);
        boolean ce=en!=null&&coursDAO.hasConflitEnseignant(en.getId(),cr.getId(),d.toString(),excl);
        // ✅ FIX : vérification visuelle UNIQUEMENT — PLUS d'email ici.
        // AlerteService.alerterConflit() était déclenché à chaque changement
        // de combo (salle, enseignant, créneau, date) → un email par champ
        // modifié = spam + freeze UI ("Java ne répond pas").
        // L'alerte email est envoyée uniquement lors de la sauvegarde réelle.
        if(cs){
            conflitLabel.setText("⚠ CONFLIT : Salle déjà occupée !");
            conflitLabel.setStyle("-fx-text-fill:"+RED+";-fx-font-weight:bold;"
                    +"-fx-background-color:#fee2e2;-fx-background-radius:6;-fx-padding:4 8;");
            conflitLabel.setVisible(true);
        } else if(ce){
            conflitLabel.setText("⚠ CONFLIT : Enseignant indisponible !");
            conflitLabel.setStyle("-fx-text-fill:"+RED+";-fx-font-weight:bold;"
                    +"-fx-background-color:#fee2e2;-fx-background-radius:6;-fx-padding:4 8;");
            conflitLabel.setVisible(true);
        } else {
            conflitLabel.setVisible(false);
        }
    }

    private void assignerSalleAutomatique(){
        if(salleAutoLabel==null)return;ClassePedago classe=classeCombo.getValue();Creneau creneau=creneauCombo.getValue();LocalDate date=datePicker.getValue();
        if(classe==null||creneau==null||date==null){salleAutoLabel.setVisible(false);return;}
        int exclId=selectedCours!=null?selectedCours.getId():0;
        Salle m=salleDAO.trouverMeilleureSalle(classe.getEffectif(),creneau.getId(),date.toString(),exclId);
        if(m==null){salleAutoLabel.setText("⚠️ Aucune salle disponible.");salleAutoLabel.setStyle("-fx-text-fill:"+RED+";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-color:"+RED_BG+";-fx-background-radius:8;-fx-padding:6 10;");salleAutoLabel.setVisible(true);return;}
        if(salleCombo.getValue()==null||selectedCours==null)salleCombo.getItems().stream().filter(s->s.getId()==m.getId()).findFirst().ifPresent(salleCombo::setValue);
        List<Salle>alts=salleDAO.trouverSallesDisponiblesPourCours(classe.getEffectif(),creneau.getId(),date.toString(),exclId);
        String msg="🏫 Salle suggérée : "+m.getNumero()+"  ("+m.getTypeSalle()+", "+m.getCapacite()+" places"+(m.getBatimentNom()!=null?", "+m.getBatimentNom():"")+")";
        if(alts.size()-1>0)msg+="\n   + "+(alts.size()-1)+" autre(s) salle(s) disponible(s)";
        salleAutoLabel.setText(msg);salleAutoLabel.setStyle("-fx-text-fill:#166534;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-color:"+GREEN_BG+";-fx-background-radius:8;-fx-padding:6 10;");salleAutoLabel.setVisible(true);
    }

    @FXML private void handleSaveCours(){
        if(matiereCombo.getValue()==null||enseignantCombo.getValue()==null||classeCombo.getValue()==null||creneauCombo.getValue()==null||salleCombo.getValue()==null||datePicker.getValue()==null){showError("Erreur","Tous les champs sont obligatoires.");return;}
        int excl=selectedCours!=null?selectedCours.getId():0;String date=datePicker.getValue().toString();
        if(coursDAO.hasConflitSalle(salleCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){showError("Conflit","La salle est déjà occupée sur ce créneau.");return;}
        if(coursDAO.hasConflitEnseignant(enseignantCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){showError("Conflit","L'enseignant est indisponible sur ce créneau.");return;}
        String ancSalle=selectedCours!=null?selectedCours.getSalleNumero():null;
        Cours c=selectedCours!=null?selectedCours:new Cours();
        c.setMatiereId(matiereCombo.getValue().getId());c.setEnseignantId(enseignantCombo.getValue().getId());c.setClasseId(classeCombo.getValue().getId());c.setCreneauId(creneauCombo.getValue().getId());c.setSalleId(salleCombo.getValue().getId());c.setDate(datePicker.getValue());c.setStatut(statutCombo.getValue()!=null?statutCombo.getValue():"PLANIFIE");
        coursDAO.save(c);
        // ✅ FIX : emails envoyés dans un Thread background
        // → ne bloque plus le thread JavaFX UI
        if(ancSalle!=null&&!ancSalle.equals(salleCombo.getValue().getNumero())){
            Utilisateur en=enseignantCombo.getValue();
            c.setSalleNumero(salleCombo.getValue().getNumero());
            c.setMatiereNom(matiereCombo.getValue().getNom());
            c.setClasseNom(classeCombo.getValue().getNom());
            c.setCreneauInfo(creneauCombo.getValue().toString());
            AlerteService.notifierUtilisateur(en.getId(),
                    "🔔 Votre cours "+matiereCombo.getValue().getNom()+" a changé de salle : "
                            +ancSalle+" → "+salleCombo.getValue().getNumero(),"INFO");
            final Cours coursEmail = c;
            final String ancSalleFinal = ancSalle;
            new Thread(() -> EmailService.notifierChangementSalle(en, coursEmail, ancSalleFinal),
                    "email-changement-salle").start();
        }
        loadData();buildCharts();buildRapportDashboard();if(vueMoisActive)buildCalendarMois();else buildCalendar();buildCarteSalles();clearForm();showInfo("Succès","Cours sauvegardé.");
    }
    @FXML private void handleDeleteCours(){
        if(selectedCours==null){showError("Erreur","Sélectionnez un cours.");return;}
        if(confirmDelete("ce cours")){coursDAO.delete(selectedCours.getId());loadData();buildCharts();buildRapportDashboard();if(vueMoisActive)buildCalendarMois();else buildCalendar();buildCarteSalles();clearForm();}
    }
    @FXML private void handleValiderReserv(){
        if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}
        reservDAO.updateStatut(selectedReserv.getId(),"VALIDEE");
        Notification n=new Notification();n.setMessage("✅ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été validée.");n.setType("INFO");n.setUtilisateurId(selectedReserv.getUtilisateurId());notifDAO.save(n);
        // ✅ Email en background thread — ne bloque pas l'UI
        final Reservation rFinal = selectedReserv;
        utilisateurDAO.findAll().stream().filter(u->u.getId()==rFinal.getUtilisateurId())
                .findFirst().ifPresent(u -> {
                    final Utilisateur uFinal = u;
                    new Thread(() -> EmailService.notifierValidationReservation(uFinal, rFinal),
                            "email-validation-reserv").start();
                });
        loadData();showInfo("Succès","Réservation validée.");
    }
    @FXML private void handleRefuserReserv(){
        if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}
        reservDAO.updateStatut(selectedReserv.getId(),"REFUSEE");
        Notification n=new Notification();n.setMessage("❌ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été refusée.");n.setType("ALERTE");n.setUtilisateurId(selectedReserv.getUtilisateurId());notifDAO.save(n);
        // ✅ Email en background thread
        final Reservation rRefus = selectedReserv;
        utilisateurDAO.findAll().stream().filter(u->u.getId()==rRefus.getUtilisateurId())
                .findFirst().ifPresent(u -> {
                    final Utilisateur uFinal = u;
                    new Thread(() -> EmailService.notifierRefusReservation(uFinal, rRefus),
                            "email-refus-reserv").start();
                });
        loadData();showInfo("Fait","Réservation refusée.");
    }
    @FXML private void handleTraiterSignalement(){
        if(selectedSignal==null){showError("Erreur","Sélectionnez un signalement.");return;}
        String statut=signalStatutCombo!=null?signalStatutCombo.getValue():null;
        if(statut==null){showError("Erreur","Choisissez un nouveau statut.");return;}
        String commentaire=signalCommentArea!=null?signalCommentArea.getText().trim():"";
        signalDAO.updateStatut(selectedSignal.getId(),statut,commentaire);
        Notification n=new Notification();n.setUtilisateurId(selectedSignal.getEnseignantId());n.setType("RESOLU".equals(statut)?"INFO":"ALERTE");
        n.setMessage("📋 Votre signalement #"+selectedSignal.getId()+" ("+selectedSignal.getTitre()+") a été mis à jour : "+formatStatutSignal(statut)+(commentaire.isEmpty()?"":" \n💬 "+commentaire));
        notifDAO.save(n);loadData();showInfo("Mis à jour","Signalement #"+selectedSignal.getId()+" → "+formatStatutSignal(statut));
        selectedSignal=null;if(signalCommentArea!=null)signalCommentArea.clear();if(signalStatutCombo!=null)signalStatutCombo.setValue(null);signalTable.getSelectionModel().clearSelection();
    }
    @FXML private void handleVoirDetailSignal(){
        if(selectedSignal==null){showError("Erreur","Sélectionnez un signalement.");return;}
        Signalement s=selectedSignal;String col;
        switch(s.getStatut()){case"RESOLU":col=GREEN;break;case"EN_COURS":col=GOLD;break;case"FERME":col="#7c6fcf";break;default:col=MUTED;}
        String[][]lignes={{"📅 Date",s.getDateSignalement()!=null?s.getDateSignalement().toLocalDate().toString():"—"},{"👤 Enseignant",s.getEnseignantNom()!=null?s.getEnseignantNom():"—"},{"🏫 Salle",s.getSalleNumero()!=null?s.getSalleNumero():"—"},{"🔖 Catégorie",s.getCategorie()},{"⚡ Priorité",s.getPrioriteIcon()+" "+s.getPriorite()},{"📌 Statut",formatStatutSignal(s.getStatut())}};
        AlertePersonnalisee.afficherDetailSignalement(s.getId(),s.getCategorieIcon()+" "+s.getTitre(),lignes,s.getDescription(),s.getCommentaireAdmin(),s.getDateResolution()!=null?s.getDateResolution().toLocalDate().toString():null,col);
    }
    private String formatStatutSignal(String statut){if(statut==null)return"—";switch(statut){case"EN_ATTENTE":return"⏳ En attente";case"EN_COURS":return"🔄 En cours";case"RESOLU":return"✅ Résolu";case"FERME":return"🔒 Fermé";default:return statut;}}

    @FunctionalInterface interface ExportAction{void run(File f)throws Exception;}
    private void doExport(String type,String name,ExportAction a){FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(type,"*."+name.substring(name.lastIndexOf('.')+1)));fc.setInitialFileName(name);File f=fc.showSaveDialog(null);if(f==null)return;try{a.run(f);showInfo("Export "+type,"Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());}}
    @FXML private void handleExportPDF()             {doExport("PDF",  "emploi_du_temps.pdf",          f->exportService.exportCoursAsPDF(coursList,f));}
    @FXML private void handleExportExcel()           {doExport("Excel","emploi_du_temps.xlsx",          f->exportService.exportCoursAsExcel(coursList,f));}
    @FXML private void handleExportHistoriquePDF()   {doExport("PDF",  "historique_reservations.pdf",   f->exportService.exportReservationsAsPDF(reservDAO.findAll(),f));}
    @FXML private void handleExportHistoriqueExcel() {doExport("Excel","historique_reservations.xlsx",  f->exportService.exportReservationsAsExcel(reservDAO.findAll(),f));}
    @FXML private void handleExportOccupationExcel() {doExport("Excel","occupation_salles.xlsx",        f->exportService.exportOccupationAsExcel(rapportService.getTauxOccupation(),f));}

    @FXML private void handleRapportHebdo(){
        Map<String,Object>r=rapportService.getRapportHebdomadaire();
        if(rapportTextArea!=null){StringBuilder sb=new StringBuilder();sb.append("══════════════════════\n       ").append(r.get("titre")).append("\n══════════════════════\n");r.forEach((k,v)->{if(!k.equals("titre")&&!k.equals("coursParJour")&&!k.equals("coursParStatut"))sb.append(String.format("  %-28s : %s%n",k,v));});sb.append("\n── Cours par jour ──\n");if(r.get("coursParJour")instanceof Map)((Map<?,?>)r.get("coursParJour")).forEach((k,v)->sb.append("  ").append(k).append(" : ").append(v).append("\n"));sb.append("\n── Cours par statut ──\n");if(r.get("coursParStatut")instanceof Map)((Map<?,?>)r.get("coursParStatut")).forEach((k,v)->sb.append("  ").append(k).append(" : ").append(v).append("\n"));rapportTextArea.setText(sb.toString());}
        buildOccupationChart();critList.setAll(rapportService.getSallesCritiques());buildRapportDashboard();
    }
    @FXML private void handleRapportMensuel(){
        Map<String,Object>r=rapportService.getRapportMensuel();
        if(rapportTextArea!=null){StringBuilder sb=new StringBuilder();sb.append("══════════════════════\n       ").append(r.get("titre")).append(" — ").append(r.get("mois")).append("\n══════════════════════\n");r.forEach((k,v)->{if(!k.equals("titre")&&!k.equals("mois")&&!k.equals("coursParJour")&&!k.equals("coursParStatut")&&!k.equals("tauxOccupationParSalle"))sb.append(String.format("  %-28s : %s%n",k,v));});sb.append("\n── Taux par salle ──\n");if(r.get("tauxOccupationParSalle")instanceof Map)((Map<?,?>)r.get("tauxOccupationParSalle")).forEach((k,v)->sb.append("  ").append(k).append(" : ").append(v).append("%\n"));rapportTextArea.setText(sb.toString());}
        buildOccupationChart();critList.setAll(rapportService.getSallesCritiques());buildRapportDashboard();
    }

    @FXML private void handleRefreshRapport(){loadData();buildRapportDashboard();buildCharts();}
    @FXML private void handleRafraichirCarte(){buildCarteSalles();}
    @FXML private void handleClearCours()    {clearForm();}
    @FXML private void handleLogout()        {logout();}
    @FXML private void handleRefresh()       {loadData();}

    private void fillForm(Cours c){
        coursFormTitle.setText("Modifier Cours");
        matiereCombo.getItems().stream().filter(m->m.getId()==c.getMatiereId()).findFirst().ifPresent(matiereCombo::setValue);
        enseignantCombo.getItems().stream().filter(u->u.getId()==c.getEnseignantId()).findFirst().ifPresent(enseignantCombo::setValue);
        classeCombo.getItems().stream().filter(cp->cp.getId()==c.getClasseId()).findFirst().ifPresent(classeCombo::setValue);
        creneauCombo.getItems().stream().filter(cr->cr.getId()==c.getCreneauId()).findFirst().ifPresent(creneauCombo::setValue);
        salleCombo.getItems().stream().filter(s->s.getId()==c.getSalleId()).findFirst().ifPresent(salleCombo::setValue);
        if(c.getDate()!=null)datePicker.setValue(c.getDate());
        statutCombo.setValue(c.getStatut());
        if(salleAutoLabel!=null)salleAutoLabel.setVisible(false);
    }
    private void clearForm(){
        matiereCombo.setValue(null);enseignantCombo.setValue(null);classeCombo.setValue(null);creneauCombo.setValue(null);salleCombo.setValue(null);datePicker.setValue(null);statutCombo.setValue(null);selectedCours=null;
        if(conflitLabel!=null)conflitLabel.setVisible(false);if(salleAutoLabel!=null)salleAutoLabel.setVisible(false);
        coursFormTitle.setText("Nouveau Cours");coursTable.getSelectionModel().clearSelection();
    }
}