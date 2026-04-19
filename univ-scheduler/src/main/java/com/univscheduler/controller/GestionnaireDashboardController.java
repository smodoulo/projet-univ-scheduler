package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.model.Examen;
import com.univscheduler.model.DemandeDisponibilite;
import com.univscheduler.model.StatutDisponibilite;
import com.univscheduler.service.*;
import com.univscheduler.model.AlertePersonnalisee;
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

    // ── Labels KPI ───────────────────────────────────────────────
    @FXML private Label welcomeLabel, totalCoursLabel, conflitsLabel;
    @FXML private Label coursFormTitle, conflitLabel, salleAutoLabel;
    @FXML private Label notifBadge;

    // ── Timer unique (jamais dupliqué) ───────────────────────────
    private javafx.animation.Timeline refreshTimeline;

    // ── Filtre classe ─────────────────────────────────────────────
    @FXML private ComboBox<String> filtreClasseCombo;
    @FXML private Label            filtreInfoLabel;

    // ── Table cours ───────────────────────────────────────────────
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colEns, colCls, colCren, colSalle, colDate, colStatut;
    @FXML private ComboBox<Matiere>      matiereCombo;
    @FXML private ComboBox<Utilisateur>  enseignantCombo;
    @FXML private ComboBox<ClassePedago> classeCombo;
    @FXML private ComboBox<Creneau>      creneauCombo;
    @FXML private ComboBox<Salle>        salleCombo;
    @FXML private DatePicker             datePicker;
    @FXML private ComboBox<String>       statutCombo;

    // ── Réservations ──────────────────────────────────────────────
    @FXML private TableView<Reservation> reservTable;
    @FXML private TableColumn<Reservation, String> colResMotif, colResSalle, colResDate, colResStatut, colResUser;

    // ── Calendrier ────────────────────────────────────────────────
    @FXML private GridPane calendarGrid;
    @FXML private Label    calendarWeekLabel;
    @FXML private Button   btnVueSemaine, btnVueMois;

    // ── Rapport charts ────────────────────────────────────────────
    @FXML private VBox chartCoursContainer, chartReservContainer, chartOccupationContainer;
    @FXML private TableView<Salle>            sallesCritiquesTable;
    @FXML private TableColumn<Salle, String>  colCritNum, colCritType, colCritTaux;
    @FXML private TableColumn<Salle, Integer> colCritCap;
    @FXML private TextArea rapportTextArea;
    @FXML private VBox rapportGaugeContainer, rapportSupplyContainer,
            rapportTrendContainer, rapportHealthContainer;
    @FXML private VBox carteContainer;

    // ── Historique ────────────────────────────────────────────────
    @FXML private TableView<Reservation> historiqueTable;
    @FXML private TableColumn<Reservation, String> colHistUser, colHistMotif, colHistSalle, colHistDate, colHistStatut;

    // ── Signalements ──────────────────────────────────────────────
    @FXML private TableView<Signalement>           signalTable;
    @FXML private TableColumn<Signalement, String> colSignTitre, colSignEns, colSignSalle,
            colSignCat, colSignPrio, colSignStatut, colSignDate;
    @FXML private Label            signalBadge;
    @FXML private TextArea         signalCommentArea;
    @FXML private ComboBox<String> signalStatutCombo;

    // ── EXAMENS ───────────────────────────────────────────────────
    @FXML private TableView<Examen> examenTable;
    @FXML private TableColumn<Examen, String> colExType, colExEns, colExTitre,
            colExClasse, colExMatiere, colExDate, colExSalle, colExStatut;
    @FXML private ComboBox<String> examenFiltreCombo;
    @FXML private TextField        examenMotifField;
    @FXML private Label            examenBadge, examenInfoLabel, examenActionFeedback;

    // ── DISPONIBILITÉS (gestionnaire) ────────────────────────────
    @FXML private TableView<DemandeDisponibilite>           dispoGestTable;
    @FXML private TableColumn<DemandeDisponibilite, String> colGestDispoEns, colGestDispoMat,
            colGestDispoCls, colGestDispoCren, colGestDispoComm, colGestDispoStatut, colGestDispoDate;
    @FXML private ComboBox<String> dispoFiltreGestCombo;
    @FXML private Label            dispoBadgeGest, dispoInfoGestLabel, dispoActionFeedbackGest;

    // ── Palette ───────────────────────────────────────────────────
    private static final String T_DARK="#1a5f6e", T_MID="#2a9cb0", T_LIGHT="#4ecdc4";
    private static final String GREEN="#3ecf8e", GREEN_BG="#dcfce7";
    private static final String GOLD="#f0a500",  GOLD_BG="#fff8e6";
    private static final String RED="#e05c5c",   RED_BG="#fee2e2";
    private static final String MUTED="#9eb3bf", SECOND="#6b8394", BORDER="#d4ecf0";
    private static final String S1="#1a5f6e", S2="#4ecdc4", S3="#3ecf8e";

    // ── DAOs / Services ───────────────────────────────────────────
    private final CoursDAO           coursDAO       = new CoursDAO();
    private final MatiereDAO         matiereDAO     = new MatiereDAO();
    private final UtilisateurDAO     utilisateurDAO = new UtilisateurDAO();
    private final ClassePedagoDAO    classeDAO      = new ClassePedagoDAO();
    private final CreneauDAO         creneauDAO     = new CreneauDAO();
    private final SalleDAO           salleDAO       = new SalleDAO();
    private final ReservationDAO     reservDAO      = new ReservationDAO();
    private final NotificationDAO    notifDAO       = new NotificationDAO();
    private final SignalementDAO     signalDAO      = new SignalementDAO();
    private final RapportService     rapportService = new RapportService();
    private final ExportService      exportService  = new ExportService();
    private final ExamenService      examenService  = new ExamenService();
    private final DisponibiliteService dispoService = new DisponibiliteService();

    private final ObservableList<Cours>                coursList        = FXCollections.observableArrayList();
    private final ObservableList<Cours>                coursListFiltree = FXCollections.observableArrayList();
    private final ObservableList<Reservation>          reservList       = FXCollections.observableArrayList();
    private final ObservableList<Reservation>          historiqueList   = FXCollections.observableArrayList();
    private final ObservableList<Salle>                critList         = FXCollections.observableArrayList();
    private final ObservableList<Signalement>          signalList       = FXCollections.observableArrayList();
    private final ObservableList<DemandeDisponibilite> dispoGestList    = FXCollections.observableArrayList();

    private Cours       selectedCours  = null;
    private Reservation selectedReserv = null;
    private Signalement selectedSignal = null;

    private LocalDate calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    private YearMonth calendarMonth     = YearMonth.now();
    private boolean   vueMoisActive     = false;

    // ════════════════════════════════════════════════════════════════
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

        if (conflitLabel   != null) { conflitLabel.setVisible(false); conflitLabel.setStyle("-fx-text-fill:"+RED+";-fx-font-weight:bold;"); }
        if (salleAutoLabel != null) salleAutoLabel.setVisible(false);

        styleBoutonVue(false);
        loadData(); buildRapportDashboard(); buildCharts(); buildCalendar(); buildCarteSalles();

        // ✅ Timer unique — ne pas appeler buildRapportDashboard() dedans
        if (refreshTimeline == null) {
            refreshTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(60),
                            e -> buildCardGauge()));
            refreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            refreshTimeline.play();
        }

        salleCombo.setOnAction(e      -> checkConflict());
        enseignantCombo.setOnAction(e -> checkConflict());
        classeCombo.setOnAction(e  -> { checkConflict(); assignerSalleAutomatique(); });
        creneauCombo.setOnAction(e -> { checkConflict(); assignerSalleAutomatique(); });
        datePicker.setOnAction(e   -> { checkConflict(); assignerSalleAutomatique(); });

        initExamenTab();
        initDisponibilitesGestTab();
    }

    // ════════════════════════════════════════════════════════════════
    //  ONGLET DISPONIBILITÉS — GESTIONNAIRE
    // ════════════════════════════════════════════════════════════════

    private void initDisponibilitesGestTab() {
        if (dispoGestTable == null) return;

        if (colGestDispoEns    != null) colGestDispoEns.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getEnseignantNom())));
        if (colGestDispoMat    != null) colGestDispoMat.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getMatiereNom())));
        if (colGestDispoCls    != null) colGestDispoCls.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getClasseNom())));
        if (colGestDispoCren   != null) colGestDispoCren.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getCreneauInfo())));
        if (colGestDispoComm   != null) colGestDispoComm.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getCommentaire())));
        if (colGestDispoStatut != null) colGestDispoStatut.setCellValueFactory(d ->
                new SimpleStringProperty(formatStatutDispo(d.getValue().getStatut())));
        if (colGestDispoDate   != null) colGestDispoDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDateDemande() != null
                        ? d.getValue().getDateDemande().toLocalDate().toString() : "—"));

        dispoGestTable.setRowFactory(tv -> new TableRow<DemandeDisponibilite>() {
            @Override protected void updateItem(DemandeDisponibilite item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (nvl(item.getStatut())) {
                    case "ACCEPTE" -> setStyle("-fx-background-color:#dcfce7;");
                    case "REFUSE"  -> setStyle("-fx-background-color:#fee2e2;");
                    case "CONFLIT" -> setStyle("-fx-background-color:#fef9c3;");
                    default        -> setStyle("-fx-background-color:#eff6ff;");
                }
            }
        });
        dispoGestTable.setItems(dispoGestList);

        if (dispoFiltreGestCombo != null) {
            dispoFiltreGestCombo.setItems(FXCollections.observableArrayList(
                    "En attente", "Toutes", "Acceptées", "Refusées", "Conflits"));
            dispoFiltreGestCombo.setValue("En attente");
            dispoFiltreGestCombo.setOnAction(e -> handleFiltreDisponibilitesGest());
        }
        if (dispoActionFeedbackGest != null) dispoActionFeedbackGest.setVisible(false);

        handleRefreshDisponibilitesGest();
    }

    @FXML public void handleRefreshDisponibilitesGest() {
        if (dispoGestTable == null) return;
        long nb = dispoService.countEnAttente();
        if (dispoBadgeGest != null) {
            dispoBadgeGest.setText(nb > 0 ? "🔴 " + nb + " en attente" : "");
            dispoBadgeGest.setVisible(nb > 0);
        }
        handleFiltreDisponibilitesGest();
    }

    @FXML private void handleFiltreDisponibilitesGest() {
        List<DemandeDisponibilite> toutes = dispoService.getToutesLesDemandes();
        String filtre = dispoFiltreGestCombo != null ? dispoFiltreGestCombo.getValue() : "En attente";
        List<DemandeDisponibilite> filtrees = switch (filtre != null ? filtre : "") {
            case "En attente" -> toutes.stream().filter(d -> "EN_ATTENTE".equals(d.getStatut())).collect(Collectors.toList());
            case "Acceptées"  -> toutes.stream().filter(d -> "ACCEPTE".equals(d.getStatut())).collect(Collectors.toList());
            case "Refusées"   -> toutes.stream().filter(d -> "REFUSE".equals(d.getStatut())).collect(Collectors.toList());
            case "Conflits"   -> toutes.stream().filter(d -> "CONFLIT".equals(d.getStatut())).collect(Collectors.toList());
            default           -> toutes;
        };
        dispoGestList.setAll(filtrees);
        if (dispoInfoGestLabel != null)
            dispoInfoGestLabel.setText(filtrees.size() + " demande(s)");
    }

    @FXML private void handleAccepterDisponibilite() {
        DemandeDisponibilite sel = dispoGestTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Sélection", "Sélectionnez une demande."); return; }
        if (!"EN_ATTENTE".equals(sel.getStatut())) {
            showError("Impossible", "Cette demande a déjà été traitée."); return;
        }
        boolean ok = dispoService.accepter(sel.getId());
        if (ok) {
            showDispoGestFeedback("✅ Demande acceptée ! Le créneau du cours a été mis à jour.", "#16a34a");
        } else {
            showDispoGestFeedback("⚠️ Conflit détecté — statut CONFLIT. L'enseignant a été notifié.", "#d97706");
        }
        handleRefreshDisponibilitesGest();
        loadData();
    }

    @FXML private void handleRefuserDisponibilite() {
        DemandeDisponibilite sel = dispoGestTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Sélection", "Sélectionnez une demande."); return; }
        if (!"EN_ATTENTE".equals(sel.getStatut())) {
            showError("Impossible", "Cette demande a déjà été traitée."); return;
        }
        dispoService.refuser(sel.getId(), "Refusé par le gestionnaire.");
        showDispoGestFeedback("❌ Demande refusée. L'enseignant a été notifié.", "#ef4444");
        handleRefreshDisponibilitesGest();
    }

    private void showDispoGestFeedback(String msg, String color) {
        if (dispoActionFeedbackGest == null) return;
        dispoActionFeedbackGest.setText(msg);
        dispoActionFeedbackGest.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
        dispoActionFeedbackGest.setVisible(true);
        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4))
                .play();
    }

    private String formatStatutDispo(String statut) {
        if (statut == null) return "—";
        return switch (statut) {
            case "EN_ATTENTE" -> "⏳ En attente";
            case "ACCEPTE"    -> "✅ Acceptée";
            case "REFUSE"     -> "❌ Refusée";
            case "CONFLIT"    -> "⚠️ Conflit";
            default           -> statut;
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  ONGLET EXAMENS
    // ════════════════════════════════════════════════════════════════
    private void initExamenTab() {
        if (examenTable == null) return;
        if (colExType    != null) colExType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTypeIcon() + " " + d.getValue().getType()));
        if (colExEns     != null) colExEns.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEnseignantNom()));
        if (colExTitre   != null) colExTitre.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitre()));
        if (colExClasse  != null) colExClasse.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getClasseNom()));
        if (colExMatiere != null) colExMatiere.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getMatiereNom()));
        if (colExDate    != null) colExDate.setCellValueFactory(d ->
                new SimpleStringProperty(formatDateExam(d.getValue().getDateExamen())));
        if (colExSalle   != null) colExSalle.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getSalleNumero())));
        if (colExStatut  != null) colExStatut.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatutAffichage()));

        examenTable.setRowFactory(tv -> new TableRow<Examen>() {
            @Override protected void updateItem(Examen item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (item.getStatut()) {
                    case Examen.STATUT_VALIDE -> setStyle("-fx-background-color:#dcfce7;");
                    case Examen.STATUT_REFUSE -> setStyle("-fx-background-color:#fee2e2;");
                    default                   -> setStyle("-fx-background-color:#eff6ff;");
                }
            }
        });

        if (examenFiltreCombo != null) {
            examenFiltreCombo.setItems(FXCollections.observableArrayList("Toutes","En attente","Validées","Refusées"));
            examenFiltreCombo.setValue("En attente");
            examenFiltreCombo.setOnAction(e -> handleFiltreExamens());
        }
        if (examenActionFeedback != null) examenActionFeedback.setVisible(false);
        handleRefreshExamens();
    }

    @FXML public void handleRefreshExamens() {
        if (examenTable == null) return;
        long nb = examenService.countEnAttente();
        if (examenBadge != null) {
            examenBadge.setText(nb > 0 ? "🔴 " + nb + " en attente" : "");
            examenBadge.setVisible(nb > 0);
        }
        handleFiltreExamens();
    }

    @FXML private void handleFiltreExamens() {
        List<Examen> toutes = examenService.getTousLesExamens();
        String filtre = examenFiltreCombo != null ? examenFiltreCombo.getValue() : "Toutes";
        List<Examen> filtrees = switch (filtre != null ? filtre : "") {
            case "En attente" -> toutes.stream().filter(e -> Examen.STATUT_EN_ATTENTE.equals(e.getStatut())).collect(Collectors.toList());
            case "Validées"   -> toutes.stream().filter(e -> Examen.STATUT_VALIDE.equals(e.getStatut())).collect(Collectors.toList());
            case "Refusées"   -> toutes.stream().filter(e -> Examen.STATUT_REFUSE.equals(e.getStatut())).collect(Collectors.toList());
            default           -> toutes;
        };
        examenTable.setItems(FXCollections.observableArrayList(filtrees));
        if (examenInfoLabel != null) examenInfoLabel.setText(filtrees.size() + " demande(s)");
    }

    @FXML private void handleValiderExamen() {
        Examen sel = examenTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Sélection", "Sélectionnez une demande."); return; }
        if (!Examen.STATUT_EN_ATTENTE.equals(sel.getStatut())) {
            showError("Impossible", "Cette demande a déjà été traitée."); return;
        }
        String motif = examenMotifField != null ? examenMotifField.getText().trim() : "";
        boolean ok = examenService.valider(sel.getId(), motif);
        showExamenFeedback(ok ? "✅ Demande validée ! L'enseignant a été notifié."
                : "⚠️ Conflit salle détecté — refus automatique.", ok ? "#16a34a" : "#d97706");
        if (examenMotifField != null) examenMotifField.clear();
        handleRefreshExamens(); loadData();
    }

    @FXML private void handleRefuserExamen() {
        Examen sel = examenTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Sélection", "Sélectionnez une demande."); return; }
        if (!Examen.STATUT_EN_ATTENTE.equals(sel.getStatut())) {
            showError("Impossible", "Cette demande a déjà été traitée."); return;
        }
        String motif = examenMotifField != null ? examenMotifField.getText().trim() : "Refusé par le gestionnaire.";
        examenService.refuser(sel.getId(), motif);
        showExamenFeedback("❌ Demande refusée. L'enseignant a été notifié.", "#ef4444");
        if (examenMotifField != null) examenMotifField.clear();
        handleRefreshExamens();
    }

    private void showExamenFeedback(String msg, String color) {
        if (examenActionFeedback == null) return;
        examenActionFeedback.setText(msg);
        examenActionFeedback.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
        examenActionFeedback.setVisible(true);
        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4))
                .setOnFinished(e -> examenActionFeedback.setVisible(false));
    }

    private String formatDateExam(String d) {
        if (d == null || d.isEmpty()) return "—";
        try {
            String[] p = d.split("T"), ymd = p[0].split("-");
            return ymd[2] + "/" + ymd[1] + "/" + ymd[0]
                    + " à " + (p.length > 1 ? p[1].substring(0, 5) : "");
        } catch (Exception e) { return d; }
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
        rapportGaugeContainer.setSpacing(12); rapportGaugeContainer.setPadding(new Insets(20));
        rapportGaugeContainer.getChildren().add(cardHeader("Taux d'Occupation Moyen","Toutes salles confondues"));
        double taux   = rapportService.getTauxOccupationGlobal();
        long   totSal = salleDAO.findAll().size();
        final double CX=100, CY=100, R=68;
        javafx.scene.layout.Pane gaugePane = new javafx.scene.layout.Pane();
        gaugePane.setPrefSize(200,185); gaugePane.setMaxSize(200,185);
        Arc bgArc=new Arc(CX,CY,R,R,225,-270); bgArc.setType(ArcType.OPEN); bgArc.setFill(Color.TRANSPARENT);
        bgArc.setStroke(Color.web("#d8f0f4")); bgArc.setStrokeWidth(18); bgArc.setStrokeLineCap(StrokeLineCap.ROUND);
        Arc fillArc=new Arc(CX,CY,R,R,225,-(taux/100.0)*270.0); fillArc.setType(ArcType.OPEN); fillArc.setFill(Color.TRANSPARENT);
        fillArc.setStroke(Color.web(T_MID)); fillArc.setStrokeWidth(18); fillArc.setStrokeLineCap(StrokeLineCap.ROUND);
        Label valLbl=new Label(String.format("%.0f",taux)); valLbl.setStyle("-fx-font-size:38px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label pctLbl=new Label("%"); pctLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        HBox valRow=new HBox(2,valLbl,pctLbl); valRow.setAlignment(Pos.BOTTOM_CENTER);
        valRow.setLayoutX(CX-42); valRow.setLayoutY(CY-24);
        valRow.layoutBoundsProperty().addListener((o,old,nb)->{ valRow.setLayoutX(CX-nb.getWidth()/2.0); valRow.setLayoutY(CY-nb.getHeight()/2.0); });
        gaugePane.getChildren().addAll(bgArc,fillArc,valRow);
        HBox gaugeWrap=new HBox(gaugePane); gaugeWrap.setAlignment(Pos.CENTER);
        rapportGaugeContainer.getChildren().add(gaugeWrap);
        HBox pills=new HBox(10); pills.setAlignment(Pos.CENTER);
        pills.getChildren().addAll(pill("↓ Tendance",RED_BG,RED), pill("Objectif: 60%","#fff8e6","#f0a500"));
        rapportGaugeContainer.getChildren().add(pills);
        // ✅ countLibresAujourdhui() — dynamique par jour/heure
        long libres = salleDAO.countLibresAujourdhui();
        HBox mini=new HBox(20); mini.setAlignment(Pos.CENTER); mini.setPadding(new Insets(8,0,0,0));
        mini.getChildren().addAll(
                miniStat(String.valueOf(totSal),"Total salles",T_MID),
                miniStat(String.valueOf(critList.size()),"Critiques",RED),
                miniStat(String.valueOf(libres),"Libres auj.",GREEN));
        rapportGaugeContainer.getChildren().add(mini);
    }

    private void buildCardSupply() {
        if (rapportSupplyContainer == null) return;
        rapportSupplyContainer.getChildren().clear();
        rapportSupplyContainer.setSpacing(10); rapportSupplyContainer.setPadding(new Insets(20));
        HBox topRow=new HBox(); topRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox=new VBox(2);
        Label tit=new Label("Cours par Mois"); tit.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label sub=new Label("Planifiés · Réalisés · Annulés"); sub.setStyle("-fx-font-size:11px;-fx-text-fill:"+SECOND+";");
        titleBox.getChildren().addAll(tit,sub);
        Region esp=new Region(); HBox.setHgrow(esp,Priority.ALWAYS);
        Label totLbl=new Label(String.valueOf(coursList.size())); totLbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        topRow.getChildren().addAll(titleBox,esp,totLbl);
        rapportSupplyContainer.getChildren().add(topRow);
        String[] moisLbls=derniersMois(6);
        int[] p=new int[6],r=new int[6],a=new int[6];
        for (Cours c:coursList){ if(c.getDate()==null)continue;
            for(int i=0;i<6;i++){YearMonth ym=YearMonth.now().minusMonths(5-i);
                if(c.getDate().getYear()==ym.getYear()&&c.getDate().getMonthValue()==ym.getMonthValue()){
                    String s=c.getStatut()!=null?c.getStatut():"";
                    if("PLANIFIE".equalsIgnoreCase(s)||"EN_COURS".equalsIgnoreCase(s))p[i]++;
                    else if("TERMINE".equalsIgnoreCase(s)||"REALISE".equalsIgnoreCase(s))r[i]++;
                    else if("ANNULE".equalsIgnoreCase(s))a[i]++;
                    break;}}
        }
        int maxVal=1; for(int i=0;i<6;i++)maxVal=Math.max(maxVal,p[i]+r[i]+a[i]);
        HBox barsRow=new HBox(8); barsRow.setAlignment(Pos.BOTTOM_LEFT); barsRow.setPrefHeight(140); barsRow.setMaxHeight(140);
        for(int i=0;i<6;i++){VBox group=new VBox(3);group.setAlignment(Pos.BOTTOM_CENTER);HBox.setHgrow(group,Priority.ALWAYS);
            HBox bars=new HBox(2);bars.setAlignment(Pos.BOTTOM_CENTER);double scale=110.0/maxVal;
            bars.getChildren().addAll(barreSingle(p[i],scale,S1,9),barreSingle(r[i],scale,S2,9),barreSingle(a[i],scale,S3,9));
            Label ml=new Label(moisLbls[i]);ml.setStyle("-fx-font-size:9px;-fx-text-fill:"+MUTED+";-fx-font-weight:bold;");
            group.getChildren().addAll(bars,ml);barsRow.getChildren().add(group);}
        rapportSupplyContainer.getChildren().add(barsRow);
        HBox legendBar=new HBox(0);legendBar.setPrefHeight(8);
        Region lb1=new Region();HBox.setHgrow(lb1,Priority.ALWAYS);lb1.setStyle("-fx-background-color:"+S1+";-fx-background-radius:4 0 0 4;");
        Region lb2=new Region();HBox.setHgrow(lb2,Priority.ALWAYS);lb2.setStyle("-fx-background-color:"+S2+";");
        Region lb3=new Region();HBox.setHgrow(lb3,Priority.ALWAYS);lb3.setStyle("-fx-background-color:"+S3+";-fx-background-radius:0 4 4 0;");
        legendBar.getChildren().addAll(lb1,lb2,lb3);rapportSupplyContainer.getChildren().add(legendBar);
        HBox legend=new HBox(16);legend.setAlignment(Pos.CENTER_LEFT);legend.setPadding(new Insets(4,0,0,0));
        legend.getChildren().addAll(legendDot(S1,"Planifiés"),legendDot(S2,"Réalisés"),legendDot(S3,"Annulés"));
        rapportSupplyContainer.getChildren().add(legend);
    }

    private void buildCardTrend() {
        if (rapportTrendContainer == null) return;
        rapportTrendContainer.getChildren().clear();
        rapportTrendContainer.setSpacing(10); rapportTrendContainer.setPadding(new Insets(20));
        double[] rates=computeWeekRates(6);
        double prev=rates.length>=2?rates[rates.length-2]:38;
        double current=rates.length>=1?rates[rates.length-1]:82;
        boolean hausse=current>=prev;
        HBox header=new HBox(24);header.setAlignment(Pos.CENTER_LEFT);
        VBox prevBox=new VBox(2);Label pL=new Label("Précédent");pL.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:"+SECOND+";");
        Label pV=new Label(String.format("%.0f%%",prev));pV.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        prevBox.getChildren().addAll(pL,pV);
        VBox currBox=new VBox(2);Label cL=new Label("Actuel");cL.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:"+SECOND+";");
        Label cV=new Label(String.format("%.0f%%",current));cV.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        currBox.getChildren().addAll(cL,cV);
        Region espH=new Region();HBox.setHgrow(espH,Priority.ALWAYS);
        Region indic=new Region();indic.setPrefSize(4,44);indic.setStyle("-fx-background-color:"+(hausse?"#f0a500":RED)+";-fx-background-radius:4;");
        header.getChildren().addAll(prevBox,currBox,espH,indic);rapportTrendContainer.getChildren().add(header);
        double maxRate=1;for(double rate:rates)maxRate=Math.max(maxRate,rate);
        HBox barsRow=new HBox(6);barsRow.setAlignment(Pos.BOTTOM_LEFT);barsRow.setPrefHeight(110);
        String[] semLbls={"S-5","S-4","S-3","S-2","S-1","Cette sem."};int offset=semLbls.length-rates.length;
        for(int i=0;i<rates.length;i++){boolean isCur=(i==rates.length-1);double h=maxRate>0?(rates[i]/maxRate)*88:8;
            VBox col=new VBox(3);col.setAlignment(Pos.BOTTOM_CENTER);HBox.setHgrow(col,Priority.ALWAYS);
            Rectangle rect=new Rectangle();rect.setWidth(22);rect.setHeight(Math.max(8,h));rect.setArcWidth(6);rect.setArcHeight(6);
            rect.setFill(isCur?Color.web(T_MID):Color.web(T_MID,0.22));
            Label lbl=new Label(semLbls[offset+i]);lbl.setStyle("-fx-font-size:9px;-fx-text-fill:"+MUTED+";-fx-font-weight:bold;");
            col.getChildren().addAll(rect,lbl);barsRow.getChildren().add(col);}
        rapportTrendContainer.getChildren().add(barsRow);
        String salleCrit=critList.isEmpty()?"B03-TD":critList.get(0).getNumero();
        String jourMax=coursDAO.countByJour().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("Jeudi");
        Label aiMsg=new Label("La salle "+salleCrit+" affiche le taux le plus élevé. "+jourMax+" est le jour le plus chargé.");
        aiMsg.setWrapText(true);aiMsg.setStyle("-fx-font-size:11px;-fx-text-fill:"+SECOND+";-fx-padding:8 0 0 0;");
        rapportTrendContainer.getChildren().add(aiMsg);
    }

    private void buildCardHealth() {
        if (rapportHealthContainer == null) return;
        rapportHealthContainer.getChildren().clear();
        rapportHealthContainer.setSpacing(10); rapportHealthContainer.setPadding(new Insets(20));
        rapportHealthContainer.getChildren().add(cardHeader("Santé des Salles","Vue globale occupation"));
        Map<String,Double> taux=rapportService.getTauxOccupation();
        double global=rapportService.getTauxOccupationGlobal();
        int total=Math.max(1,taux.size());
        long surOcc=taux.values().stream().filter(t->t>=80).count();
        long sousOcc=taux.values().stream().filter(t->t<10).count();
        double pctSur=(surOcc*100.0)/total, pctSous=(sousOcc*100.0)/total;
        HBox metricsRow=new HBox(20);metricsRow.setAlignment(Pos.CENTER_LEFT);
        metricsRow.getChildren().addAll(
                healthMetric(String.format("%.0f%%",global),"Overall"),
                healthMetric(String.format("%.0f%%",pctSur),"Overcapacity"),
                healthMetric(String.format("%.0f%%",pctSous),"Undercapacity"));
        rapportHealthContainer.getChildren().add(metricsRow);
        HBox barsRow=new HBox(18);barsRow.setAlignment(Pos.BOTTOM_LEFT);barsRow.setPrefHeight(120);barsRow.setPadding(new Insets(8,0,0,0));
        barsRow.getChildren().addAll(barreV(global,T_MID,false),barreV(pctSur,T_DARK,true),barreVLight(pctSous,T_LIGHT));
        rapportHealthContainer.getChildren().add(barsRow);
        HBox legend=new HBox(14);legend.setAlignment(Pos.CENTER_LEFT);legend.setPadding(new Insets(6,0,0,0));
        legend.getChildren().addAll(legendDot(T_MID,"Taux global"),legendDot(T_DARK,"Suroccupées"),legendDot(T_LIGHT,"Sous-utilisées"));
        rapportHealthContainer.getChildren().add(legend);
    }

    // ── Widget helpers ────────────────────────────────────────────
    private HBox cardHeader(String title, String subtitle) {
        HBox h=new HBox();h.setAlignment(Pos.CENTER_LEFT);
        VBox tx=new VBox(2);
        Label t=new Label(title);t.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");
        Label s=new Label(subtitle);s.setStyle("-fx-font-size:11px;-fx-text-fill:"+SECOND+";");
        tx.getChildren().addAll(t,s);Region e=new Region();HBox.setHgrow(e,Priority.ALWAYS);
        h.getChildren().addAll(tx,e);return h;
    }
    private Label pill(String text,String bg,String fg){Label l=new Label(text);l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 12;-fx-background-radius:999;");return l;}
    private VBox miniStat(String value,String label,String color){VBox b=new VBox(2);b.setAlignment(Pos.CENTER);Label v=new Label(value);v.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:"+color+";");Label l=new Label(label);l.setStyle("-fx-font-size:10px;-fx-text-fill:"+MUTED+";");b.getChildren().addAll(v,l);return b;}
    private VBox barreSingle(int value,double scale,String color,int width){VBox b=new VBox();b.setAlignment(Pos.BOTTOM_CENTER);double h=Math.max(4,value*scale);Region r=new Region();r.setPrefSize(width,h);r.setMinHeight(h);r.setMaxHeight(h);r.setStyle("-fx-background-color:"+color+";-fx-background-radius:4 4 2 2;");b.getChildren().add(r);return b;}
    private VBox barreV(double pct,String color,boolean hachure){VBox col=new VBox(4);col.setAlignment(Pos.BOTTOM_CENTER);HBox.setHgrow(col,Priority.ALWAYS);double h=Math.max(12,(pct/100.0)*100);Region r=new Region();r.setPrefSize(54,h);r.setMinHeight(h);r.setStyle(hachure?"-fx-background-color:repeating-linear-gradient(135deg,"+color+","+color+" 4px,rgba(26,95,110,0.28) 4px,rgba(26,95,110,0.28) 8px);-fx-background-radius:6 6 2 2;":"-fx-background-color:"+color+";-fx-background-radius:6 6 2 2;");col.getChildren().add(r);return col;}
    private VBox barreVLight(double pct,String color){VBox col=new VBox(4);col.setAlignment(Pos.BOTTOM_CENTER);HBox.setHgrow(col,Priority.ALWAYS);double h=Math.max(12,(pct/100.0)*100);Region r=new Region();r.setPrefSize(54,h);r.setMinHeight(h);r.setStyle("-fx-background-color:"+color+";-fx-opacity:0.55;-fx-background-radius:6 6 2 2;");col.getChildren().add(r);return col;}
    private VBox healthMetric(String v,String l){VBox b=new VBox(2);Label lv=new Label(v);lv.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";");Label ll=new Label(l);ll.setStyle("-fx-font-size:10px;-fx-text-fill:"+MUTED+";");b.getChildren().addAll(lv,ll);return b;}
    private HBox legendDot(String color,String label){HBox h=new HBox(5);h.setAlignment(Pos.CENTER_LEFT);Region dot=new Region();dot.setPrefSize(8,8);dot.setMinSize(8,8);dot.setStyle("-fx-background-color:"+color+";-fx-background-radius:50;");Label lbl=new Label(label);lbl.setStyle("-fx-font-size:10px;-fx-text-fill:"+SECOND+";");h.getChildren().addAll(dot,lbl);return h;}

    // ── Calculs ──────────────────────────────────────────────────
    private String[] derniersMois(int n){String[] lb=new String[n];for(int i=0;i<n;i++){YearMonth ym=YearMonth.now().minusMonths(n-1-i);lb[i]=ym.getMonth().getDisplayName(TextStyle.SHORT,Locale.FRENCH);}return lb;}
    private double[] computeWeekRates(int n){double[] rates=new double[n];for(int i=0;i<n;i++){LocalDate start=LocalDate.now().with(java.time.DayOfWeek.MONDAY).minusWeeks(n-1-i);LocalDate end=start.plusDays(6);long total=coursList.stream().filter(c->c.getDate()!=null&&!c.getDate().isBefore(start)&&!c.getDate().isAfter(end)).count();long done=coursList.stream().filter(c->c.getDate()!=null&&!c.getDate().isBefore(start)&&!c.getDate().isAfter(end)&&("TERMINE".equalsIgnoreCase(c.getStatut())||"REALISE".equalsIgnoreCase(c.getStatut()))).count();rates[i]=total>0?(done*100.0/total):(i==n-1?57:18+i*5);}return rates;}

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
    private void setupFiltreClasse(){if(filtreClasseCombo==null)return;filtreClasseCombo.setOnAction(e->appliquerFiltreClasse());}
    private void refreshFiltreClasse(){if(filtreClasseCombo==null){coursListFiltree.setAll(coursList);return;}String sel=filtreClasseCombo.getValue();List<String>classes=new ArrayList<>();classes.add("— Toutes les classes —");coursList.stream().map(Cours::getClasseNom).filter(c->c!=null&&!c.isEmpty()).distinct().sorted().forEach(classes::add);filtreClasseCombo.setItems(FXCollections.observableArrayList(classes));filtreClasseCombo.setValue((sel!=null&&classes.contains(sel))?sel:"— Toutes les classes —");appliquerFiltreClasse();}
    private void appliquerFiltreClasse(){if(filtreClasseCombo==null){coursListFiltree.setAll(coursList);return;}String sel=filtreClasseCombo.getValue();if(sel==null||sel.startsWith("—")){coursListFiltree.setAll(coursList);totalCoursLabel.setText("Total : "+coursList.size()+" cours");if(filtreInfoLabel!=null)filtreInfoLabel.setText("");}else{coursListFiltree.setAll(coursList.stream().filter(c->sel.equals(c.getClasseNom())).collect(Collectors.toList()));totalCoursLabel.setText(coursListFiltree.size()+" cours (classe : "+sel+")");if(filtreInfoLabel!=null)filtreInfoLabel.setText(coursListFiltree.size()+" résultat(s)");}}
    @FXML private void handleReinitialiseFiltreClasse(){if(filtreClasseCombo!=null)filtreClasseCombo.setValue("— Toutes les classes —");}
    private void setupReservTable(){if(reservTable==null)return;colResMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));colResSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));colResDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));colResStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));if(colResUser!=null)colResUser.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getUtilisateurNom()));reservTable.setItems(reservList);reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r)->selectedReserv=r);}
    private void setupHistoriqueTable(){if(historiqueTable==null)return;if(colHistUser!=null)colHistUser.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getUtilisateurNom()));if(colHistMotif!=null)colHistMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));if(colHistSalle!=null)colHistSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));if(colHistDate!=null)colHistDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));if(colHistStatut!=null)colHistStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));historiqueTable.setItems(historiqueList);}
    private void setupCritiquesTable(){if(sallesCritiquesTable==null)return;Map<String,Double>taux=rapportService.getTauxOccupation();if(colCritNum!=null)colCritNum.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNumero()));if(colCritType!=null)colCritType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getTypeSalle()));if(colCritCap!=null)colCritCap.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());if(colCritTaux!=null)colCritTaux.setCellValueFactory(d->{double t=taux.getOrDefault(d.getValue().getNumero(),0.0);return new SimpleStringProperty((t>=80?"🔴 ":t>=50?"🟡 ":"🟢 ")+t+"%");});sallesCritiquesTable.setItems(critList);}
    private void setupSignalTable(){if(signalTable==null)return;if(colSignTitre!=null)colSignTitre.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorieIcon()+" "+d.getValue().getTitre()));if(colSignEns!=null)colSignEns.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getEnseignantNom()!=null?d.getValue().getEnseignantNom():"—"));if(colSignSalle!=null)colSignSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()!=null?d.getValue().getSalleNumero():"—"));if(colSignCat!=null)colSignCat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorie()));if(colSignPrio!=null)colSignPrio.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getPrioriteIcon()+" "+d.getValue().getPriorite()));if(colSignStatut!=null)colSignStatut.setCellValueFactory(d->new SimpleStringProperty(formatStatutSig(d.getValue().getStatut())));if(colSignDate!=null)colSignDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateSignalement()!=null?d.getValue().getDateSignalement().toLocalDate().toString():""));signalTable.setRowFactory(tv->new TableRow<>(){@Override protected void updateItem(Signalement item,boolean empty){super.updateItem(item,empty);if(item==null||empty){setStyle("");return;}switch(item.getPriorite()){case"URGENTE"->setStyle("-fx-background-color:"+RED_BG+";");case"HAUTE"->setStyle("-fx-background-color:"+"#fff8e6"+";");default->setStyle("");}}});signalTable.setItems(signalList);signalTable.getSelectionModel().selectedItemProperty().addListener((obs,old,sel)->{selectedSignal=sel;if(sel!=null&&signalCommentArea!=null)signalCommentArea.setText(sel.getCommentaireAdmin()!=null?sel.getCommentaireAdmin():"");});if(signalStatutCombo!=null)signalStatutCombo.setItems(FXCollections.observableArrayList("EN_COURS","RESOLU","FERME"));}

    // ════════════════════════════════════════════════════════════════
    //  Data
    // ════════════════════════════════════════════════════════════════
    private void loadData(){coursList.setAll(coursDAO.findAll());refreshFiltreClasse();List<Reservation>allReserv=reservDAO.findAll();reservList.setAll(allReserv.stream().filter(r->"EN_ATTENTE".equals(r.getStatut())).collect(Collectors.toList()));historiqueList.setAll(allReserv);critList.setAll(rapportService.getSallesCritiques());totalCoursLabel.setText("Total : "+coursList.size()+" cours");long conflits=coursList.stream().filter(c1->coursList.stream().anyMatch(c2->c2.getId()!=c1.getId()&&c2.getSalleId()==c1.getSalleId()&&c2.getCreneauId()==c1.getCreneauId()&&c2.getDate()!=null&&c1.getDate()!=null&&c2.getDate().equals(c1.getDate()))).count()/2;if(conflitsLabel!=null)conflitsLabel.setText(String.valueOf(conflits));signalList.setAll(signalDAO.findAll());long nbEA=signalDAO.countEnAttente();if(signalBadge!=null){signalBadge.setText(nbEA>0?"🔴 "+nbEA+" en attente":"");signalBadge.setVisible(nbEA>0);}refreshNotifBadge();}
    private void refreshNotifBadge(){if(notifBadge==null)return;int nb=notifDAO.countUnread(currentUser.getId());notifBadge.setText(String.valueOf(nb));notifBadge.setVisible(nb>0);}
    @FXML private void handleVoirNotifications(){List<Notification>notifs=notifDAO.findByUtilisateur(currentUser.getId());notifDAO.markAllRead(currentUser.getId());refreshNotifBadge();AlertePersonnalisee.afficherNotifications(notifs);}

    // ════════════════════════════════════════════════════════════════
    //  Calendrier (compact)
    // ════════════════════════════════════════════════════════════════
    private void buildCalendar(){if(calendarGrid==null)return;calendarGrid.getChildren().clear();calendarGrid.getColumnConstraints().clear();calendarGrid.getRowConstraints().clear();String[]jours={"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};int[]heures={8,10,12,14,16};calendarGrid.getColumnConstraints().add(new ColumnConstraints(60));for(int j=0;j<jours.length;j++){ColumnConstraints cc=new ColumnConstraints();cc.setHgrow(Priority.ALWAYS);calendarGrid.getColumnConstraints().add(cc);}Label corner=new Label("Heure");corner.setStyle("-fx-font-weight:bold;-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;");corner.setMaxWidth(Double.MAX_VALUE);calendarGrid.add(corner,0,0);for(int j=0;j<jours.length;j++){LocalDate d=calendarWeekStart.plusDays(j);Label lbl=new Label(jours[j]+"\n"+d.getDayOfMonth()+"/"+d.getMonthValue());lbl.setStyle("-fx-font-weight:bold;-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;-fx-font-size:11px;");lbl.setMaxWidth(Double.MAX_VALUE);GridPane.setHgrow(lbl,Priority.ALWAYS);calendarGrid.add(lbl,j+1,0);}Map<String,List<Cours>>par=new HashMap<>();for(String j:jours)par.put(j,new ArrayList<>());for(Cours c:coursList)if(c.getCreneauInfo()!=null)for(String j:jours)if(c.getCreneauInfo().startsWith(j)){par.get(j).add(c);break;}for(int h=0;h<heures.length;h++){Label hLbl=new Label(heures[h]+"h");hLbl.setStyle("-fx-background-color:#f0f9fa;-fx-font-weight:bold;-fx-padding:8 4;-fx-alignment:CENTER;-fx-font-size:11px;-fx-text-fill:"+T_DARK+";");hLbl.setMaxWidth(Double.MAX_VALUE);hLbl.setMaxHeight(Double.MAX_VALUE);calendarGrid.add(hLbl,0,h+1);for(int j=0;j<jours.length;j++){VBox cell=new VBox(2);cell.setPadding(new Insets(3));cell.setStyle("-fx-border-color:"+BORDER+";-fx-border-width:0.5;-fx-background-color:white;");cell.setMinHeight(60);for(Cours c:par.get(jours[j]))if(c.getCreneauInfo()!=null&&c.getCreneauInfo().contains(heures[h]+"h")){Label m=new Label("📚 "+(c.getMatiereNom()!=null?c.getMatiereNom():"?"));m.setStyle("-fx-background-color:"+T_MID+";-fx-text-fill:white;-fx-padding:2 6;-fx-background-radius:6;-fx-font-size:10px;");cell.getChildren().add(m);}calendarGrid.add(cell,j+1,h+1);}}if(calendarWeekLabel!=null)calendarWeekLabel.setText("Semaine du "+calendarWeekStart.getDayOfMonth()+"/"+calendarWeekStart.getMonthValue()+"/"+calendarWeekStart.getYear());}
    @FXML private void handleVueMois()    {vueMoisActive=true; styleBoutonVue(true);}
    @FXML private void handleVueSemaine() {vueMoisActive=false;styleBoutonVue(false);buildCalendar();}
    @FXML private void handlePrevWeek()   {if(!vueMoisActive){calendarWeekStart=calendarWeekStart.minusWeeks(1);buildCalendar();}}
    @FXML private void handleNextWeek()   {if(!vueMoisActive){calendarWeekStart=calendarWeekStart.plusWeeks(1);buildCalendar();}}
    @FXML private void handleTodayWeek()  {calendarWeekStart=LocalDate.now().with(java.time.DayOfWeek.MONDAY);buildCalendar();}
    private void styleBoutonVue(boolean moisActif){String on="-fx-background-color:"+T_DARK+";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:10;-fx-cursor:hand;";String off="-fx-background-color:#e0f0f4;-fx-text-fill:"+T_DARK+";-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:10;-fx-cursor:hand;";if(btnVueMois!=null)btnVueMois.setStyle(moisActif?on:off);if(btnVueSemaine!=null)btnVueSemaine.setStyle(moisActif?off:on);}

    // ════════════════════════════════════════════════════════════════
    //  Carte salles
    // ════════════════════════════════════════════════════════════════
    private void buildCarteSalles(){if(carteContainer==null)return;carteContainer.getChildren().clear();Map<String,Double>taux=rapportService.getTauxOccupation();List<Salle>salles=salleDAO.findAll();Label title=new Label("🗺️ Carte Interactive des Salles");title.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:"+T_DARK+";-fx-padding:0 0 8 0;");Map<String,List<Salle>>parBat=new LinkedHashMap<>();for(Salle s:salles){String b=s.getBatimentNom()!=null?s.getBatimentNom():"Sans bâtiment";parBat.computeIfAbsent(b,x->new ArrayList<>()).add(s);}VBox all=new VBox(16);for(Map.Entry<String,List<Salle>>e:parBat.entrySet()){VBox batBox=new VBox(8);batBox.setStyle("-fx-background-color:white;-fx-padding:14;-fx-background-radius:12;");Label batLbl=new Label("🏢 "+e.getKey());batLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:"+T_DARK+";");FlowPane fp=new FlowPane(10,10);for(Salle s:e.getValue()){double t=taux.getOrDefault(s.getNumero(),0.0);String bg,fg;if(!s.isDisponible()){bg="#f0f4f4";fg=MUTED;}else if(t>=80){bg=RED_BG;fg="#991b1b";}else if(t>=50){bg="#fff8e6";fg="#854d0e";}else{bg=GREEN_BG;fg="#166534";}VBox card=new VBox(4);card.setAlignment(Pos.CENTER);card.setPadding(new Insets(10,14,10,14));card.setPrefWidth(110);card.setStyle("-fx-background-color:"+bg+";-fx-background-radius:10;-fx-border-color:"+fg+";-fx-border-radius:10;-fx-border-width:1.5;");Label n=new Label(s.getNumero());n.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:"+fg+";");Label tx=new Label(s.isDisponible()?t+"%":"Indisponible");tx.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:"+fg+";");card.getChildren().addAll(n,tx);fp.getChildren().add(card);}batBox.getChildren().addAll(batLbl,fp);all.getChildren().add(batBox);}long libres=salleDAO.countLibresAujourdhui();Label global=new Label(String.format("📊 Taux global : %.1f%%  |  Libres aujourd'hui : %d",rapportService.getTauxOccupationGlobal(),libres));global.setStyle("-fx-font-size:12px;-fx-text-fill:"+SECOND+";-fx-padding:4 0 0 0;");carteContainer.getChildren().addAll(title,global,all);}

    private void checkConflict(){if(conflitLabel==null)return;Salle s=salleCombo.getValue();Creneau cr=creneauCombo.getValue();LocalDate d=datePicker.getValue();Utilisateur en=enseignantCombo.getValue();if(s==null||cr==null||d==null){conflitLabel.setVisible(false);return;}int excl=selectedCours!=null?selectedCours.getId():0;boolean cs=coursDAO.hasConflitSalle(s.getId(),cr.getId(),d.toString(),excl);boolean ce=en!=null&&coursDAO.hasConflitEnseignant(en.getId(),cr.getId(),d.toString(),excl);if(cs){conflitLabel.setText("⚠ CONFLIT : Salle déjà occupée !");conflitLabel.setVisible(true);}else if(ce){conflitLabel.setText("⚠ CONFLIT : Enseignant indisponible !");conflitLabel.setVisible(true);}else{conflitLabel.setVisible(false);}}
    private void assignerSalleAutomatique(){if(salleAutoLabel==null)return;ClassePedago classe=classeCombo.getValue();Creneau creneau=creneauCombo.getValue();LocalDate date=datePicker.getValue();if(classe==null||creneau==null||date==null){salleAutoLabel.setVisible(false);return;}int exclId=selectedCours!=null?selectedCours.getId():0;Salle m=salleDAO.trouverMeilleureSalle(classe.getEffectif(),creneau.getId(),date.toString(),exclId);if(m==null){salleAutoLabel.setText("⚠️ Aucune salle disponible.");salleAutoLabel.setVisible(true);return;}if(salleCombo.getValue()==null||selectedCours==null)salleCombo.getItems().stream().filter(s->s.getId()==m.getId()).findFirst().ifPresent(salleCombo::setValue);salleAutoLabel.setText("🏫 Salle suggérée : "+m.getNumero());salleAutoLabel.setVisible(true);}

    @FXML private void handleSaveCours(){if(matiereCombo.getValue()==null||enseignantCombo.getValue()==null||classeCombo.getValue()==null||creneauCombo.getValue()==null||salleCombo.getValue()==null||datePicker.getValue()==null){showError("Erreur","Tous les champs sont obligatoires.");return;}int excl=selectedCours!=null?selectedCours.getId():0;String date=datePicker.getValue().toString();if(coursDAO.hasConflitSalle(salleCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){showError("Conflit","La salle est déjà occupée.");return;}if(coursDAO.hasConflitEnseignant(enseignantCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){showError("Conflit","L'enseignant est indisponible.");return;}Cours c=selectedCours!=null?selectedCours:new Cours();c.setMatiereId(matiereCombo.getValue().getId());c.setEnseignantId(enseignantCombo.getValue().getId());c.setClasseId(classeCombo.getValue().getId());c.setCreneauId(creneauCombo.getValue().getId());c.setSalleId(salleCombo.getValue().getId());c.setDate(datePicker.getValue());c.setStatut(statutCombo.getValue()!=null?statutCombo.getValue():"PLANIFIE");coursDAO.save(c);loadData();buildCharts();buildRapportDashboard();buildCalendar();buildCarteSalles();clearForm();showInfo("Succès","Cours sauvegardé.");}
    @FXML private void handleDeleteCours(){if(selectedCours==null){showError("Erreur","Sélectionnez un cours.");return;}if(confirmDelete("ce cours")){coursDAO.delete(selectedCours.getId());loadData();buildCharts();buildRapportDashboard();buildCalendar();buildCarteSalles();clearForm();}}
    @FXML private void handleValiderReserv(){if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}reservDAO.updateStatut(selectedReserv.getId(),"VALIDEE");Notification n=new Notification();n.setMessage("✅ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été validée.");n.setType("INFO");n.setUtilisateurId(selectedReserv.getUtilisateurId());notifDAO.save(n);loadData();showInfo("Succès","Réservation validée.");}
    @FXML private void handleRefuserReserv(){if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}reservDAO.updateStatut(selectedReserv.getId(),"REFUSEE");Notification n=new Notification();n.setMessage("❌ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été refusée.");n.setType("ALERTE");n.setUtilisateurId(selectedReserv.getUtilisateurId());notifDAO.save(n);loadData();showInfo("Fait","Réservation refusée.");}
    @FXML private void handleTraiterSignalement(){if(selectedSignal==null){showError("Erreur","Sélectionnez un signalement.");return;}String statut=signalStatutCombo!=null?signalStatutCombo.getValue():null;if(statut==null){showError("Erreur","Choisissez un nouveau statut.");return;}String commentaire=signalCommentArea!=null?signalCommentArea.getText().trim():"";signalDAO.updateStatut(selectedSignal.getId(),statut,commentaire);Notification n=new Notification();n.setUtilisateurId(selectedSignal.getEnseignantId());n.setType("RESOLU".equals(statut)?"INFO":"ALERTE");n.setMessage("📋 Signalement #"+selectedSignal.getId()+" mis à jour : "+formatStatutSig(statut));notifDAO.save(n);loadData();showInfo("Mis à jour","Signalement traité.");selectedSignal=null;if(signalCommentArea!=null)signalCommentArea.clear();if(signalStatutCombo!=null)signalStatutCombo.setValue(null);signalTable.getSelectionModel().clearSelection();}
    @FXML private void handleVoirDetailSignal(){if(selectedSignal==null){showError("Erreur","Sélectionnez un signalement.");return;}Signalement s=selectedSignal;String col;switch(s.getStatut()){case"RESOLU"->col=GREEN;case"EN_COURS"->col="#f0a500";default->col=MUTED;}String[][]lignes={{"📅 Date",s.getDateSignalement()!=null?s.getDateSignalement().toLocalDate().toString():"—"},{"👤 Enseignant",s.getEnseignantNom()!=null?s.getEnseignantNom():"—"},{"🏫 Salle",s.getSalleNumero()!=null?s.getSalleNumero():"—"},{"🔖 Catégorie",s.getCategorie()},{"⚡ Priorité",s.getPrioriteIcon()+" "+s.getPriorite()},{"📌 Statut",formatStatutSig(s.getStatut())}};AlertePersonnalisee.afficherDetailSignalement(s.getId(),s.getCategorieIcon()+" "+s.getTitre(),lignes,s.getDescription(),s.getCommentaireAdmin(),s.getDateResolution()!=null?s.getDateResolution().toLocalDate().toString():null,col);}
    private String formatStatutSig(String st){if(st==null)return"—";return switch(st){case"EN_ATTENTE"->"⏳ En attente";case"EN_COURS"->"🔄 En cours";case"RESOLU"->"✅ Résolu";case"FERME"->"🔒 Fermé";default->st;};}

    private void buildCharts(){if(chartCoursContainer!=null){CategoryAxis xA=new CategoryAxis();NumberAxis yA=new NumberAxis();xA.setLabel("Jour");yA.setLabel("Cours");BarChart<String,Number>bar=new BarChart<>(xA,yA);bar.setTitle("📅 Cours par Jour");bar.setLegendVisible(false);bar.setPrefHeight(240);bar.setStyle("-fx-background-color:transparent;");XYChart.Series<String,Number>s=new XYChart.Series<>();coursDAO.countByJour().forEach((k,v)->{XYChart.Data<String,Number>d=new XYChart.Data<>(k,v);s.getData().add(d);d.nodeProperty().addListener((obs,old,node)->{if(node!=null)node.setStyle("-fx-bar-fill:"+T_MID+";-fx-background-radius:6 6 2 2;");});});bar.getData().add(s);chartCoursContainer.getChildren().setAll(bar);}if(chartReservContainer!=null){PieChart pie=new PieChart();pie.setTitle("📊 Statut des Cours");pie.setPrefHeight(240);pie.setStyle("-fx-background-color:transparent;");String[]ordre={"PLANIFIE","REALISE","ANNULE","TERMINE","EN_COURS"};Map<String,Integer>st=coursDAO.countByStatut();for(String k:ordre)if(st.containsKey(k)){long v=st.get(k);pie.getData().add(new PieChart.Data(k.charAt(0)+k.substring(1).toLowerCase()+" ("+v+")",v));}chartReservContainer.getChildren().setAll(pie);}if(chartOccupationContainer!=null){Map<String,Double>taux=rapportService.getTauxOccupation();CategoryAxis xA=new CategoryAxis();NumberAxis yA=new NumberAxis(0,55,10);BarChart<String,Number>bar=new BarChart<>(xA,yA);bar.setTitle("🏫 Occupation par Salle");bar.setLegendVisible(false);bar.setPrefHeight(280);bar.setStyle("-fx-background-color:transparent;");XYChart.Series<String,Number>series=new XYChart.Series<>();taux.forEach((salle,occ)->{XYChart.Data<String,Number>d=new XYChart.Data<>(salle,occ);series.getData().add(d);d.nodeProperty().addListener((obs,old,node)->{if(node!=null)node.setStyle("-fx-bar-fill:"+T_MID+";-fx-background-radius:6 6 2 2;");});});bar.getData().add(series);chartOccupationContainer.getChildren().setAll(bar);}}

    @FunctionalInterface interface ExportAction{void run(File f)throws Exception;}
    private void doExport(String type,String name,ExportAction a){FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(type,"*."+name.substring(name.lastIndexOf('.')+1)));fc.setInitialFileName(name);File f=fc.showSaveDialog(null);if(f==null)return;try{a.run(f);showInfo("Export "+type,"Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());}}
    @FXML private void handleExportPDF()             {doExport("PDF","emploi_du_temps.pdf",f->exportService.exportCoursAsPDF(coursList,f));}
    @FXML private void handleExportExcel()           {doExport("Excel","emploi_du_temps.xlsx",f->exportService.exportCoursAsExcel(coursList,f));}
    @FXML private void handleExportHistoriquePDF()   {doExport("PDF","historique.pdf",f->exportService.exportReservationsAsPDF(reservDAO.findAll(),f));}
    @FXML private void handleExportHistoriqueExcel() {doExport("Excel","historique.xlsx",f->exportService.exportReservationsAsExcel(reservDAO.findAll(),f));}
    @FXML private void handleExportOccupationExcel() {doExport("Excel","occupation.xlsx",f->exportService.exportOccupationAsExcel(rapportService.getTauxOccupation(),f));}
    @FXML private void handleRapportHebdo(){Map<String,Object>r=rapportService.getRapportHebdomadaire();if(rapportTextArea!=null){StringBuilder sb=new StringBuilder();r.forEach((k,v)->sb.append(String.format("  %-24s : %s%n",k,v)));rapportTextArea.setText(sb.toString());}buildRapportDashboard();}
    @FXML private void handleRapportMensuel(){Map<String,Object>r=rapportService.getRapportMensuel();if(rapportTextArea!=null){StringBuilder sb=new StringBuilder();r.forEach((k,v)->sb.append(String.format("  %-24s : %s%n",k,v)));rapportTextArea.setText(sb.toString());}buildRapportDashboard();}
    @FXML private void handleRefreshRapport(){loadData();buildRapportDashboard();buildCharts();}
    @FXML private void handleRafraichirCarte(){buildCarteSalles();}
    @FXML private void handleClearCours()    {clearForm();}
    @FXML private void handleLogout()        {logout();}
    @FXML private void handleRefresh()       {loadData();}

    private void fillForm(Cours c){coursFormTitle.setText("Modifier Cours");matiereCombo.getItems().stream().filter(m->m.getId()==c.getMatiereId()).findFirst().ifPresent(matiereCombo::setValue);enseignantCombo.getItems().stream().filter(u->u.getId()==c.getEnseignantId()).findFirst().ifPresent(enseignantCombo::setValue);classeCombo.getItems().stream().filter(cp->cp.getId()==c.getClasseId()).findFirst().ifPresent(classeCombo::setValue);creneauCombo.getItems().stream().filter(cr->cr.getId()==c.getCreneauId()).findFirst().ifPresent(creneauCombo::setValue);salleCombo.getItems().stream().filter(s->s.getId()==c.getSalleId()).findFirst().ifPresent(salleCombo::setValue);if(c.getDate()!=null)datePicker.setValue(c.getDate());statutCombo.setValue(c.getStatut());if(salleAutoLabel!=null)salleAutoLabel.setVisible(false);}
    private void clearForm(){matiereCombo.setValue(null);enseignantCombo.setValue(null);classeCombo.setValue(null);creneauCombo.setValue(null);salleCombo.setValue(null);datePicker.setValue(null);statutCombo.setValue(null);selectedCours=null;if(conflitLabel!=null)conflitLabel.setVisible(false);if(salleAutoLabel!=null)salleAutoLabel.setVisible(false);coursFormTitle.setText("Nouveau Cours");coursTable.getSelectionModel().clearSelection();}
    private static String nvl(String s){return(s!=null&&!s.isBlank())?s:"—";}
}