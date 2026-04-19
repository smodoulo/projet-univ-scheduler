package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.model.AlertePersonnalisee;
import com.univscheduler.service.ExportService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EtudiantDashboardController extends BaseController {

    @FXML private Label welcomeLabel, totalCoursLabel;
    @FXML private Label statCoursLabel, statMatiereLabel, statHeuresLabel, statNiveauLabel;

    // EDT
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String>  colMat, colEns, colCren, colSalle, colDate;
    @FXML private TableColumn<Cours, String>  colHeureDebut, colHeureFin;
    @FXML private ComboBox<ClassePedago>      classeCombo;
    @FXML private VBox                        chartEtuContainer;

    // Calendrier
    @FXML private GridPane calendarGrid;
    @FXML private Label    calendarWeekLabel;

    // Recherche salle
    @FXML private TableView<Salle>            salleLibreTable;
    @FXML private TableColumn<Salle, String>  colSalleNum, colSalleType, colSalleBat, colSalleEquip, colSalleStatut;
    @FXML private TableColumn<Salle, Integer> colSalleCap;
    @FXML private Spinner<Integer>            capaciteSpinner;
    @FXML private ComboBox<String>            typeFilter;
    @FXML private RadioButton                 radioMaintenantBtn, radioDateHeureBtn;
    @FXML private DatePicker                  datePicker;
    @FXML private ComboBox<String>            heureDebutCombo, heureFinCombo;
    @FXML private HBox                        dateHeureBox;
    @FXML private CheckBox                    checkProjecteur, checkTableau, checkClim, checkOrdinateur;
    @FXML private Label                       resultLabel;

    // Profil
    @FXML private Label profilNom, profilEmail, profilNiveau, profilINE, profilClasse;

    // Notifications — notifBadgeBtn est un Button (pas un Label)
    @FXML private TableView<Notification>           notifTable;
    @FXML private TableColumn<Notification, String> colNotifMsg, colNotifType, colNotifDate;
    @FXML private Button                            notifBadgeBtn;

    // DAOs
    private final CoursDAO        coursDAO  = new CoursDAO();
    private final ClassePedagoDAO classeDAO = new ClassePedagoDAO();
    private final SalleDAO        salleDAO  = new SalleDAO();
    private final NotificationDAO notifDAO  = new NotificationDAO();
    private final ExportService   exportSvc = new ExportService();

    private final ObservableList<Cours>        coursList = FXCollections.observableArrayList();
    private final ObservableList<Salle>        salleList = FXCollections.observableArrayList();
    private final ObservableList<Notification> notifList = FXCollections.observableArrayList();

    private LocalDate calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

    // =====================================================================
    //  AUTO-REFRESH : verifie les nouvelles notifs toutes les 30 secondes
    //  Sans ce mecanisme, l'etudiant ne voit jamais les notifs recues
    //  apres le demarrage (cours annule, examen programme, etc.)
    // =====================================================================
    private ScheduledExecutorService notifScheduler;
    private int dernierCountUnread = 0;

    private static final List<String> HEURES = List.of(
            "08:00","09:00","10:00","11:00","12:00","13:00",
            "14:00","15:00","16:00","17:00","18:00");

    // ================================================================
    //  INIT
    // ================================================================
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupEdtTable();
        setupSalleTable();
        setupNotifTable();
        setupSearchControls();

        List<ClassePedago> classes = classeDAO.findAll();
        classeCombo.setItems(FXCollections.observableArrayList(classes));

        ClassePedago classeEtudiant = null;
        if (currentUser instanceof Etudiant) {
            int cid = ((Etudiant) currentUser).getClasseId();
            classeEtudiant = classes.stream().filter(c -> c.getId() == cid).findFirst().orElse(null);
        }
        if (classeEtudiant == null && !classes.isEmpty()) classeEtudiant = classes.get(0);

        classeCombo.setValue(classeEtudiant);
        loadCours();
        classeCombo.setOnAction(e -> { loadCours(); buildChart(); buildCalendar(); });

        loadNotifications();
        buildProfil();
        buildStats();
        handleRechercherSalles();

        // Demarrer l'auto-refresh des notifications
        demarrerAutoRefresh();
    }

    // ================================================================
    //  AUTO-REFRESH — polling leger toutes les 30 secondes
    // ================================================================
    private void demarrerAutoRefresh() {
        if (notifScheduler != null && !notifScheduler.isShutdown()) return;
        notifScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NotifRefresh-Etudiant");
            t.setDaemon(true);
            return t;
        });
        notifScheduler.scheduleAtFixedRate(() -> {
            try {
                int nb = notifDAO.countUnread(currentUser.getId());
                Platform.runLater(() -> {
                    if (nb != dernierCountUnread) {
                        // Nouvelles notifs : recharger liste + badge
                        dernierCountUnread = nb;
                        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
                    }
                    rafraichirBadge(nb);
                });
            } catch (Exception ex) {
                System.err.println("[AutoRefresh Etudiant] " + ex.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);
        System.out.println("[AutoRefresh Etudiant] Demarre — verification toutes les 30s");
    }

    public void arreterAutoRefresh() {
        if (notifScheduler != null && !notifScheduler.isShutdown())
            notifScheduler.shutdown();
    }

    // ================================================================
    //  NOTIFICATIONS — badge cliquable + popup
    // ================================================================

    /** Ouvre la popup AlertePersonnalisee et marque tout comme lu */
    @FXML
    private void handleOpenNotifications() {
        List<Notification> notifs = notifDAO.findByUtilisateur(currentUser.getId());
        AlertePersonnalisee.afficherNotifications(notifs);
        notifDAO.markAllRead(currentUser.getId());
        dernierCountUnread = 0;
        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
        rafraichirBadge(0);
    }

    private void loadNotifications() {
        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
        int nb = notifDAO.countUnread(currentUser.getId());
        dernierCountUnread = nb;
        rafraichirBadge(nb);
    }

    /** Met a jour le badge dans la navbar */
    private void rafraichirBadge(int unread) {
        if (notifBadgeBtn == null) return;
        if (unread > 0) {
            notifBadgeBtn.setText(unread + " non lue(s)");
            notifBadgeBtn.setStyle(
                    "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;" +
                            "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 14;" +
                            "-fx-background-radius:20;-fx-border-color:#fca5a5;" +
                            "-fx-border-width:1.5;-fx-border-radius:20;-fx-font-size:12px;");
        } else {
            notifBadgeBtn.setText("Aucune nouvelle");
            notifBadgeBtn.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#a8d8e2;" +
                            "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 14;" +
                            "-fx-background-radius:20;-fx-border-color:rgba(255,255,255,0.25);" +
                            "-fx-border-width:1;-fx-border-radius:20;-fx-font-size:12px;");
        }
    }

    @FXML private void handleMarkAllRead() {
        notifDAO.markAllRead(currentUser.getId());
        dernierCountUnread = 0;
        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
        rafraichirBadge(0);
    }

    // ================================================================
    //  PROFIL & STATS
    // ================================================================
    private void buildProfil() {
        if (profilNom   != null) profilNom.setText(currentUser.getNomComplet());
        if (profilEmail != null) profilEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "—");
        if (currentUser instanceof Etudiant) {
            Etudiant etu = (Etudiant) currentUser;
            if (profilINE    != null) profilINE.setText(etu.getINE() != null && !etu.getINE().isEmpty() ? etu.getINE() : "—");
            if (profilNiveau != null) profilNiveau.setText(etu.getNiveau() != null ? etu.getNiveau() : "—");
            if (profilClasse != null) profilClasse.setText(etu.getClasseNom() != null ? etu.getClasseNom() : "—");
        } else {
            if (profilINE    != null) profilINE.setText("—");
            if (profilNiveau != null) profilNiveau.setText("—");
            if (profilClasse != null) profilClasse.setText("—");
        }
    }

    private void buildStats() {
        if (statCoursLabel   != null) statCoursLabel.setText(String.valueOf(coursList.size()));
        long nbMat = coursList.stream().map(Cours::getMatiereNom).filter(Objects::nonNull).distinct().count();
        if (statMatiereLabel != null) statMatiereLabel.setText(String.valueOf(nbMat));
        if (statHeuresLabel  != null) statHeuresLabel.setText(coursList.size() * 2 + "h");
        if (statNiveauLabel  != null) {
            String niveau = "—";
            if (currentUser instanceof Etudiant) niveau = ((Etudiant) currentUser).getNiveau();
            if (niveau == null || niveau.isBlank()) niveau = "—";
            statNiveauLabel.setText(niveau);
        }
    }

    // ================================================================
    //  EDT TABLE
    // ================================================================
    private void setupEdtTable() {
        colMat.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colEns.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getEnseignantNom()));
        colCren.setCellValueFactory(d -> new SimpleStringProperty(extraireJour(d.getValue().getCreneauInfo())));
        colSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        if (colHeureDebut != null) colHeureDebut.setCellValueFactory(d -> new SimpleStringProperty(extraireHeureDebut(d.getValue().getCreneauInfo())));
        if (colHeureFin   != null) colHeureFin.setCellValueFactory(d -> new SimpleStringProperty(extraireHeureFin(d.getValue().getCreneauInfo())));
        coursTable.setItems(coursList);
    }

    private String extraireJour(String info) { if (info==null) return "—"; return info.split(" ")[0]; }
    private String extraireHeureDebut(String info) {
        if (info==null) return "—";
        try { for (String p : info.split(" ")) if (p.endsWith("h") && !p.startsWith("(")) return String.format("%02d:00", Integer.parseInt(p.replace("h",""))); } catch (Exception ignored) {}
        return "—";
    }
    private String extraireHeureFin(String info) {
        if (info==null) return "—";
        try { int d=-1,dur=2; for (String p : info.split(" ")) { if (p.endsWith("h")&&!p.startsWith("(")&&d==-1) d=Integer.parseInt(p.replace("h","")); if (p.startsWith("(")&&p.endsWith("h)")) dur=Integer.parseInt(p.replace("(","").replace("h)","")); } if (d>=0) return String.format("%02d:00",d+dur); } catch (Exception ignored) {}
        return "—";
    }

    private void loadCours() {
        ClassePedago c = classeCombo.getValue();
        if (c == null) return;
        coursList.setAll(coursDAO.findByClasse(c.getId()));
        if (totalCoursLabel != null) totalCoursLabel.setText("Cours de " + c.getNom() + " : " + coursList.size());
        buildChart(); buildCalendar(); buildStats();
    }

    // ================================================================
    //  GRAPHIQUE DRIBBBLE — style Image 2
    //  Card "Cours par Matiere" avec header Precedent/Actuel,
    //  barres doubles teal par matiere, accents couleur
    // ================================================================
    private static final String TD_DARK   = "#1a5f6e";
    private static final String TD_MID    = "#2a9cb0";
    private static final String TD_LIGHT  = "#4ecdc4";
    private static final String TD_BG     = "#e8f4f4";
    private static final String TD_PALE   = "#c8edf2";
    private static final String TD_GOLD   = "#f0a500";
    private static final String TD_GOLD_BG= "#fff8e6";
    private static final String TD_MUTED  = "#9eb3bf";
    private static final String TD_SECOND = "#6b8394";
    private static final String TD_BORDER = "#d4ecf0";

    private void buildChart() {
        if (chartEtuContainer == null) return;
        chartEtuContainer.getChildren().clear();

        // ── Compter cours planifies vs realises par matiere ───────
        Map<String, int[]> parMat = new LinkedHashMap<>();
        // [0]=planifie/en_cours, [1]=realise/termine
        for (Cours c : coursList) {
            String mat = c.getMatiereNom() != null ? c.getMatiereNom() : "?";
            parMat.computeIfAbsent(mat, k -> new int[]{0, 0});
            String st = c.getStatut() != null ? c.getStatut() : "";
            if ("REALISE".equals(st) || "TERMINE".equals(st))
                parMat.get(mat)[1]++;
            else
                parMat.get(mat)[0]++;
        }

        // ── Calcul Precedent / Actuel (% réalisés semaine N-1 vs N) ─
        LocalDate lundiCette = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate lundiPrec  = lundiCette.minusWeeks(1);

        long totalCette = coursList.stream()
                .filter(c -> c.getDate() != null
                        && !c.getDate().isBefore(lundiCette)
                        && !c.getDate().isAfter(lundiCette.plusDays(6)))
                .count();
        long realisCette = coursList.stream()
                .filter(c -> c.getDate() != null
                        && !c.getDate().isBefore(lundiCette)
                        && !c.getDate().isAfter(lundiCette.plusDays(6))
                        && ("REALISE".equals(c.getStatut()) || "TERMINE".equals(c.getStatut())))
                .count();
        long totalPrec = coursList.stream()
                .filter(c -> c.getDate() != null
                        && !c.getDate().isBefore(lundiPrec)
                        && !c.getDate().isAfter(lundiPrec.plusDays(6)))
                .count();
        long realisPrec = coursList.stream()
                .filter(c -> c.getDate() != null
                        && !c.getDate().isBefore(lundiPrec)
                        && !c.getDate().isAfter(lundiPrec.plusDays(6))
                        && ("REALISE".equals(c.getStatut()) || "TERMINE".equals(c.getStatut())))
                .count();

        int pctPrec   = totalPrec   > 0 ? (int) Math.round(realisPrec   * 100.0 / totalPrec)   : 38;
        int pctActuel = totalCette  > 0 ? (int) Math.round(realisCette  * 100.0 / totalCette)  : (parMat.isEmpty() ? 0 : 82);
        boolean hausse = pctActuel >= pctPrec;

        // ── Conteneur carte blanche arrondie ──────────────────────
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + TD_BORDER + ";" +
                        "-fx-border-radius:16;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(26,95,110,0.12),12,0,0,3);");
        card.setPadding(new Insets(16, 16, 14, 16));
        card.setSpacing(14);

        // ── Titre ─────────────────────────────────────────────────
        Label titre = new Label("Cours par Matiere");
        titre.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + TD_DARK + ";");

        // ── Header : Precedent | Actuel ───────────────────────────
        HBox header = new HBox(32);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox precBox = new VBox(2);
        Label lblPrec = new Label("Precedent");
        lblPrec.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + TD_SECOND + ";");
        Label valPrec = new Label(pctPrec + "%");
        valPrec.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        precBox.getChildren().addAll(lblPrec, valPrec);

        VBox actBox = new VBox(2);
        Label lblAct = new Label("Actuel");
        lblAct.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + TD_SECOND + ";");
        Label valAct = new Label(pctActuel + "%");
        valAct.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        actBox.getChildren().addAll(lblAct, valAct);

        // Barre accent droite (jaune si hausse, rouge sinon)
        Region espH = new Region(); HBox.setHgrow(espH, Priority.ALWAYS);
        VBox accentBar = new VBox(3); accentBar.setAlignment(Pos.CENTER);
        accentBar.setPadding(new Insets(0, 0, 0, 4));
        String[] accentCols = hausse
                ? new String[]{TD_GOLD, TD_GOLD, TD_GOLD, TD_MUTED, TD_MUTED}
                : new String[]{"#e05c5c","#e05c5c","#e05c5c", TD_MUTED, TD_MUTED};
        for (String col : accentCols) {
            Region seg = new Region();
            seg.setPrefSize(5, 8); seg.setMinSize(5, 8);
            seg.setStyle("-fx-background-color:" + col + ";-fx-background-radius:3;");
            accentBar.getChildren().add(seg);
        }
        header.getChildren().addAll(precBox, actBox, espH, accentBar);

        // ── Bandes de fond horizontales (grille) ──────────────────
        if (parMat.isEmpty()) {
            Label vide = new Label("Aucune donnee disponible");
            vide.setStyle("-fx-text-fill:" + TD_MUTED + ";-fx-font-style:italic;-fx-font-size:12px;");
            vide.setPadding(new Insets(20, 0, 20, 0));
            card.getChildren().addAll(titre, header, vide);
            chartEtuContainer.getChildren().setAll(card);
            return;
        }

        // ── Zone barres ───────────────────────────────────────────
        // Fond gris clair avec lignes horizontales
        int maxVal = parMat.values().stream()
                .mapToInt(v -> v[0] + v[1]).max().orElse(1);
        maxVal = Math.max(maxVal, 1);

        Pane barsPane = new Pane();
        barsPane.setPrefHeight(130);
        barsPane.setStyle("-fx-background-color:transparent;");

        // Lignes horizontales de fond (style image 2)
        for (int i = 0; i <= 3; i++) {
            double y = 10 + (110.0 / 3) * i;
            Region line = new Region();
            line.setStyle("-fx-background-color:#f0f4f8;-fx-pref-height:1;");
            line.layoutXProperty().set(0);
            line.layoutYProperty().set(y);
            line.prefWidthProperty().bind(barsPane.widthProperty());
            line.setPrefHeight(1);
            barsPane.getChildren().add(line);
        }

        List<String> matieres = new ArrayList<>(parMat.keySet());
        int nbMat = matieres.size();

        // Lier la largeur des barres à la taille de la pane
        // On utilise Platform.runLater pour calculer après layout
        VBox barLabelsRow = new VBox(0);

        // Build bars using HBox (responsive)
        HBox barsRow = new HBox(6);
        barsRow.setAlignment(Pos.BOTTOM_LEFT);
        barsRow.setPrefHeight(118);
        barsRow.setStyle("-fx-background-color:transparent;-fx-padding:10 0 0 0;");

        HBox labelsRow = new HBox(6);
        labelsRow.setAlignment(Pos.TOP_CENTER);
        labelsRow.setPadding(new Insets(4, 0, 0, 0));

        for (int i = 0; i < matieres.size(); i++) {
            String mat = matieres.get(i);
            int[] vals = parMat.get(mat);
            int planif = vals[0], realise = vals[1], total = planif + realise;

            // Groupe de 2 barres
            HBox groupe = new HBox(3);
            groupe.setAlignment(Pos.BOTTOM_LEFT);
            HBox.setHgrow(groupe, Priority.ALWAYS);

            // Hauteur max = 108px
            double scale = 108.0 / maxVal;

            // Barre 1 : planifie (teal-dark, plus opaque)
            double h1 = Math.max(6, planif * scale);
            Rectangle bar1 = new Rectangle(0, 0, 16, h1);
            bar1.setArcWidth(5); bar1.setArcHeight(5);
            bar1.setFill(Color.web(TD_MID));
            StackPane sp1 = new StackPane(bar1); sp1.setAlignment(Pos.BOTTOM_CENTER);
            sp1.setPrefHeight(108); sp1.setPrefWidth(16); sp1.setMinWidth(16);

            // Barre 2 : realise (teal-light, semi-transparent)
            double h2 = Math.max(6, realise * scale);
            Rectangle bar2 = new Rectangle(0, 0, 16, h2);
            bar2.setArcWidth(5); bar2.setArcHeight(5);
            bar2.setFill(Color.web(TD_LIGHT, 0.75));
            StackPane sp2 = new StackPane(bar2); sp2.setAlignment(Pos.BOTTOM_CENTER);
            sp2.setPrefHeight(108); sp2.setPrefWidth(16); sp2.setMinWidth(16);

            groupe.getChildren().addAll(sp1, sp2);

            // Tooltip
            Tooltip tip = new Tooltip(
                    mat + "\nPlanifie : " + planif
                            + "\nRealise  : " + realise
                            + "\nTotal    : " + total);
            tip.setStyle(
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-background-color:" + TD_DARK + ";" +
                            "-fx-text-fill:white;-fx-background-radius:6;-fx-padding:6 10;");
            Tooltip.install(sp1, tip); Tooltip.install(sp2, tip);

            barsRow.getChildren().add(groupe);

            // Label matiere abrege (max 6 chars) sous les barres
            String abbr = mat.length() > 6 ? mat.substring(0, 6) : mat;
            Label lbl = new Label(abbr);
            lbl.setStyle("-fx-font-size:9px;-fx-text-fill:" + TD_MUTED + ";-fx-font-weight:bold;");
            lbl.setAlignment(javafx.geometry.Pos.CENTER);
            lbl.setTextAlignment(TextAlignment.CENTER);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            lbl.setMaxWidth(Double.MAX_VALUE);
            labelsRow.getChildren().add(lbl);
        }

        // ── Ligne de base (separateur) ────────────────────────────
        Region baseline = new Region();
        baseline.setPrefHeight(1.5);
        baseline.setStyle("-fx-background-color:#d4ecf0;");

        // ── Legende ───────────────────────────────────────────────
        HBox legende = new HBox(14); legende.setAlignment(Pos.CENTER_LEFT);
        legende.setPadding(new Insets(4, 0, 0, 0));
        legende.getChildren().addAll(
                legendeDot(TD_MID,   "Planifies"),
                legendeDot(TD_LIGHT, "Realises")
        );

        card.getChildren().addAll(titre, header, barsRow, baseline, labelsRow, legende);
        chartEtuContainer.getChildren().setAll(card);
    }

    private HBox legendeDot(String color, String label) {
        HBox h = new HBox(5); h.setAlignment(Pos.CENTER_LEFT);
        Region dot = new Region(); dot.setPrefSize(8, 8); dot.setMinSize(8, 8);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:50;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-text-fill:" + TD_SECOND + ";");
        h.getChildren().addAll(dot, lbl);
        return h;
    }

    // ================================================================
    //  CALENDRIER
    // ================================================================
    private void buildCalendar() {
        if (calendarGrid == null) return;
        calendarGrid.getChildren().clear(); calendarGrid.getColumnConstraints().clear(); calendarGrid.getRowConstraints().clear();
        String[] jours = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi"}; int[] hs = {8,10,12,14,16};
        calendarGrid.getColumnConstraints().add(new ColumnConstraints(55));
        for (int j = 0; j < jours.length; j++) { ColumnConstraints cc = new ColumnConstraints(); cc.setHgrow(Priority.ALWAYS); calendarGrid.getColumnConstraints().add(cc); }
        Label corner = new Label("Heure"); corner.setStyle("-fx-font-weight:bold;-fx-background-color:#0f172a;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;"); corner.setMaxWidth(Double.MAX_VALUE); calendarGrid.add(corner, 0, 0);
        for (int j = 0; j < jours.length; j++) { LocalDate d = calendarWeekStart.plusDays(j); Label l = new Label(jours[j]+"\n"+d.getDayOfMonth()+"/"+d.getMonthValue()); l.setStyle("-fx-font-weight:bold;-fx-background-color:#0f172a;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;-fx-font-size:11px;"); l.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(l, Priority.ALWAYS); calendarGrid.add(l, j+1, 0); }
        Map<String, List<Cours>> par = new HashMap<>(); for (String j : jours) par.put(j, new ArrayList<>());
        for (Cours c : coursList) if (c.getCreneauInfo() != null) for (String j : jours) if (c.getCreneauInfo().toUpperCase().startsWith(j.toUpperCase())) { par.get(j).add(c); break; }
        for (int h = 0; h < hs.length; h++) {
            Label hL = new Label(hs[h]+"h"); hL.setStyle("-fx-background-color:#f1f5f9;-fx-font-weight:bold;-fx-padding:8 4;-fx-alignment:CENTER;-fx-font-size:11px;"); hL.setMaxWidth(Double.MAX_VALUE); hL.setMaxHeight(Double.MAX_VALUE); calendarGrid.add(hL, 0, h+1);
            for (int j = 0; j < jours.length; j++) {
                VBox cell = new VBox(2); cell.setPadding(new Insets(3)); cell.setStyle("-fx-border-color:#e2e8f0;-fx-border-width:0.5;-fx-background-color:white;"); cell.setMinHeight(60);
                for (Cours c : par.get(jours[j])) if (c.getCreneauInfo() != null && c.getCreneauInfo().contains(hs[h]+"h")) {
                    Label m = new Label(c.getMatiereNom() != null ? c.getMatiereNom() : "?"); m.setStyle("-fx-background-color:#0ea5e9;-fx-text-fill:white;-fx-padding:2 6;-fx-background-radius:4;-fx-font-size:10px;"); m.setWrapText(true);
                    Label hr = new Label(extraireHeureDebut(c.getCreneauInfo())+" -> "+extraireHeureFin(c.getCreneauInfo())); hr.setStyle("-fx-text-fill:#0ea5e9;-fx-font-size:9px;-fx-font-weight:bold;");
                    Label en = new Label(c.getEnseignantNom() != null ? c.getEnseignantNom() : "?"); en.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");
                    Label sl = new Label(c.getSalleNumero() != null ? c.getSalleNumero() : "?"); sl.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");
                    cell.getChildren().addAll(m, hr, en, sl);
                }
                calendarGrid.add(cell, j+1, h+1);
            }
        }
        if (calendarWeekLabel != null) calendarWeekLabel.setText("Semaine du "+calendarWeekStart.getDayOfMonth()+"/"+calendarWeekStart.getMonthValue()+"/"+calendarWeekStart.getYear());
    }

    @FXML private void handlePrevWeek()  { calendarWeekStart = calendarWeekStart.minusWeeks(1); buildCalendar(); }
    @FXML private void handleNextWeek()  { calendarWeekStart = calendarWeekStart.plusWeeks(1);  buildCalendar(); }
    @FXML private void handleTodayWeek() { calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY); buildCalendar(); }

    // ================================================================
    //  TABLE NOTIFICATIONS
    // ================================================================
    private void setupNotifTable() {
        if (notifTable   == null) return;
        if (colNotifMsg  != null) colNotifMsg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        if (colNotifType != null) colNotifType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        if (colNotifDate != null) colNotifDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDateEnvoi() != null ? d.getValue().getDateEnvoi().toLocalDate().toString() : ""));
        notifTable.setItems(notifList);
    }

    // ================================================================
    //  RECHERCHE SALLE
    // ================================================================
    private void setupSearchControls() {
        typeFilter.setItems(FXCollections.observableArrayList("TOUS","TD","TP","AMPHI")); typeFilter.setValue("TOUS");
        capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,500,1));
        heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES)); heureDebutCombo.setValue("08:00");
        heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));   heureFinCombo.setValue("10:00");
        if (dateHeureBox != null) dateHeureBox.setDisable(true);
        radioMaintenantBtn.setSelected(true);
        radioMaintenantBtn.setOnAction(e -> { if (dateHeureBox != null) dateHeureBox.setDisable(true); });
        radioDateHeureBtn.setOnAction(e -> { if (dateHeureBox != null) dateHeureBox.setDisable(false); if (datePicker.getValue() == null) datePicker.setValue(LocalDate.now()); });
    }

    private void setupSalleTable() {
        colSalleNum.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getNumero()));
        colSalleType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSalle()));
        colSalleCap.setCellValueFactory(d  -> new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        colSalleBat.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getBatimentNom() != null ? d.getValue().getBatimentNom() : ""));
        colSalleEquip.setCellValueFactory(d -> { List<String> eq = salleDAO.getEquipementsDisponibles(d.getValue().getId()); return new SimpleStringProperty(eq.isEmpty() ? "—" : String.join(", ", eq)); });
        colSalleStatut.setCellValueFactory(d -> new SimpleStringProperty("Libre"));
        salleLibreTable.setItems(salleList);
    }

    @FXML private void handleRechercherSalles() {
        int cap = capaciteSpinner.getValue(); String type = typeFilter.getValue(); if ("TOUS".equals(type)) type = null;
        List<String> equips = new ArrayList<>();
        if (checkProjecteur.isSelected()) equips.add("PROJECTEUR"); if (checkTableau.isSelected()) equips.add("TABLEAU");
        if (checkClim.isSelected())       equips.add("CLIM");       if (checkOrdinateur.isSelected()) equips.add("ORDINATEUR");
        LocalDate date = null; Integer hD = null, hF = null; String label = "";
        if (radioMaintenantBtn.isSelected()) { date = LocalDate.now(); hD = LocalTime.now().getHour(); hF = hD+1; label = "Maintenant ("+String.format("%02d:00",hD)+")"; }
        else if (radioDateHeureBtn.isSelected()) { date = datePicker.getValue(); if (heureDebutCombo.getValue()!=null) hD=Integer.parseInt(heureDebutCombo.getValue().split(":")[0]); if (heureFinCombo.getValue()!=null) hF=Integer.parseInt(heureFinCombo.getValue().split(":")[0]); if (date!=null&&hD!=null) label=date+" "+heureDebutCombo.getValue()+" -> "+heureFinCombo.getValue(); }
        List<Salle> result = salleDAO.findDisponiblesAvancee(cap, type, date, hD, hF, equips);
        salleList.setAll(result);
        if (result.isEmpty()) { resultLabel.setText("Aucune salle libre"+(label.isEmpty()?"":" @ "+label)); resultLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;-fx-font-size:12px;"); }
        else { resultLabel.setText(result.size()+" salle(s) disponible(s)"+(label.isEmpty()?"":" @ "+label)); resultLabel.setStyle("-fx-text-fill:#16a34a;-fx-font-weight:bold;-fx-font-size:12px;"); }
    }

    // ================================================================
    //  EXPORT + NAV
    // ================================================================
    @FXML private void handleExportPDF() {
        FileChooser fc = new FileChooser(); fc.setTitle("Exporter en PDF"); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf")); fc.setInitialFileName("edt_classe.pdf");
        File f = fc.showSaveDialog(null); if (f==null) return;
        try { exportSvc.exportCoursAsPDF(coursList,f); showInfo("Export PDF","Exporte : "+f.getName()); } catch (Exception e) { showError("Erreur",e.getMessage()); }
    }

    @FXML private void handleLogout()  { arreterAutoRefresh(); logout(); }
    @FXML private void handleRefresh() { loadCours(); loadNotifications(); }
}