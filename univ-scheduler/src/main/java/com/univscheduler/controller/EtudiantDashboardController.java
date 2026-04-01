package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.service.ExportService;
import javafx.beans.property.*;
import com.univscheduler.service.EmailService;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class EtudiantDashboardController extends BaseController {

    @FXML private Label welcomeLabel, totalCoursLabel;
    @FXML private Label statCoursLabel, statMatiereLabel, statHeuresLabel, statNiveauLabel;

    // ─── EDT ───
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colEns, colCren, colSalle, colDate;
    @FXML private TableColumn<Cours, String> colHeureDebut, colHeureFin; // ✅ AJOUT
    @FXML private ComboBox<ClassePedago> classeCombo;
    @FXML private VBox chartEtuContainer;

    // ─── CALENDRIER ───
    @FXML private GridPane calendarGrid;
    @FXML private Label    calendarWeekLabel;

    // ─── RECHERCHE ───
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

    // ─── PROFIL ───
    @FXML private Label profilNom, profilEmail, profilNiveau, profilINE, profilClasse;

    // ─── NOTIFICATIONS ───
    @FXML private TableView<Notification>           notifTable;
    @FXML private TableColumn<Notification, String> colNotifMsg, colNotifType, colNotifDate;
    @FXML private Label                             notifBadge;

    // ─── DAOs ───
    private final CoursDAO        coursDAO  = new CoursDAO();
    private final ClassePedagoDAO classeDAO = new ClassePedagoDAO();
    private final SalleDAO        salleDAO  = new SalleDAO();
    private final NotificationDAO notifDAO  = new NotificationDAO();
    private final ExportService   exportSvc = new ExportService();

    private final ObservableList<Cours>        coursList = FXCollections.observableArrayList();
    private final ObservableList<Salle>        salleList = FXCollections.observableArrayList();
    private final ObservableList<Notification> notifList = FXCollections.observableArrayList();

    private LocalDate calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

    private static final List<String> HEURES = List.of(
            "08:00","09:00","10:00","11:00","12:00","13:00",
            "14:00","15:00","16:00","17:00","18:00");

    // ═══════════════════════════ INIT ════════════════════════════
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupEdtTable();
        setupSalleTable();
        setupNotifTable();
        setupSearchControls();
        List<ClassePedago> classes = classeDAO.findAll();
        classeCombo.setItems(FXCollections.observableArrayList(classes));
        if (!classes.isEmpty()) { classeCombo.setValue(classes.get(0)); loadCours(); }
        classeCombo.setOnAction(e -> { loadCours(); buildChart(); buildCalendar(); });
        loadNotifications();
        buildProfil();
        buildStats();
        handleRechercherSalles();
    }

    // ═══════════════════════════ EDT ═════════════════════════════
    private void setupEdtTable() {
        colMat.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colEns.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getEnseignantNom()));
        // ✅ MODIFIÉ : colCren affiche seulement le jour
        colCren.setCellValueFactory(d -> new SimpleStringProperty(extraireJour(d.getValue().getCreneauInfo())));
        colSalle.setCellValueFactory(d-> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        // ✅ AJOUT : heure début et heure fin
        if (colHeureDebut != null)
            colHeureDebut.setCellValueFactory(d ->
                    new SimpleStringProperty(extraireHeureDebut(d.getValue().getCreneauInfo())));
        if (colHeureFin != null)
            colHeureFin.setCellValueFactory(d ->
                    new SimpleStringProperty(extraireHeureFin(d.getValue().getCreneauInfo())));
        coursTable.setItems(coursList);
    }

    // ── Helpers extraction créneau ────────────────────────────────
    /** "SAMEDI 10h (2h)" → "SAMEDI" */
    private String extraireJour(String info) {
        if (info == null) return "—";
        String[] parts = info.split(" ");
        return parts.length > 0 ? parts[0] : info;
    }

    /** "SAMEDI 10h" → "10:00" */
    private String extraireHeureDebut(String info) {
        if (info == null) return "—";
        try {
            for (String p : info.split(" "))
                if (p.endsWith("h") && !p.startsWith("("))
                    return String.format("%02d:00", Integer.parseInt(p.replace("h","")));
        } catch (Exception ignored) {}
        return "—";
    }

    /** "SAMEDI 10h (2h)" → "12:00" | "SAMEDI 10h" → "12:00" (2h par défaut) */
    private String extraireHeureFin(String info) {
        if (info == null) return "—";
        try {
            int debut = -1, duree = 2;
            for (String p : info.split(" ")) {
                if (p.endsWith("h") && !p.startsWith("(") && debut == -1)
                    debut = Integer.parseInt(p.replace("h",""));
                if (p.startsWith("(") && p.endsWith("h)"))
                    duree = Integer.parseInt(p.replace("(","").replace("h)",""));
            }
            if (debut >= 0) return String.format("%02d:00", debut + duree);
        } catch (Exception ignored) {}
        return "—";
    }

    private void loadCours() {
        ClassePedago c = classeCombo.getValue();
        if (c == null) return;
        coursList.setAll(coursDAO.findByClasse(c.getId()));
        if (totalCoursLabel != null)
            totalCoursLabel.setText("Cours de " + c.getNom() + " : " + coursList.size());
        buildChart(); buildCalendar(); buildStats();
    }

    private void buildChart() {
        if (chartEtuContainer == null) return;
        Map<String,Integer> parMat = new LinkedHashMap<>();
        for (Cours c : coursList)
            parMat.merge(c.getMatiereNom() != null ? c.getMatiereNom() : "?", 1, Integer::sum);
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
        xA.setLabel("Matière"); yA.setLabel("Séances");
        BarChart<String,Number> chart = new BarChart<>(xA, yA);
        chart.setTitle("📚 Cours par Matière"); chart.setLegendVisible(false); chart.setPrefHeight(220);
        XYChart.Series<String,Number> s = new XYChart.Series<>();
        parMat.forEach((k,v) -> s.getData().add(new XYChart.Data<>(k, v)));
        chart.getData().add(s);
        chartEtuContainer.getChildren().setAll(chart);
    }

    // ═══════════════════════════ CALENDRIER ══════════════════════
    private void buildCalendar() {
        if (calendarGrid == null) return;
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        String[] jours = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};
        int[]    heures = {8, 10, 12, 14, 16};

        calendarGrid.getColumnConstraints().add(new ColumnConstraints(55));
        for (int j = 0; j < jours.length; j++) {
            ColumnConstraints cc = new ColumnConstraints(); cc.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(cc);
        }

        Label corner = new Label("Heure");
        corner.setStyle("-fx-font-weight:bold;-fx-background-color:#0f172a;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;");
        corner.setMaxWidth(Double.MAX_VALUE); calendarGrid.add(corner, 0, 0);

        for (int j = 0; j < jours.length; j++) {
            LocalDate d = calendarWeekStart.plusDays(j);
            Label lbl = new Label(jours[j] + "\n" + d.getDayOfMonth() + "/" + d.getMonthValue());
            lbl.setStyle("-fx-font-weight:bold;-fx-background-color:#0f172a;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;-fx-font-size:11px;");
            lbl.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(lbl, Priority.ALWAYS);
            calendarGrid.add(lbl, j + 1, 0);
        }

        Map<String,List<Cours>> par = new HashMap<>();
        for (String j : jours) par.put(j, new ArrayList<>());
        for (Cours c : coursList)
            if (c.getCreneauInfo() != null)
                for (String j : jours)
                    if (c.getCreneauInfo().toUpperCase().startsWith(j.toUpperCase())) { par.get(j).add(c); break; }

        for (int h = 0; h < heures.length; h++) {
            Label hLbl = new Label(heures[h] + "h");
            hLbl.setStyle("-fx-background-color:#f1f5f9;-fx-font-weight:bold;-fx-padding:8 4;-fx-alignment:CENTER;-fx-font-size:11px;");
            hLbl.setMaxWidth(Double.MAX_VALUE); hLbl.setMaxHeight(Double.MAX_VALUE);
            calendarGrid.add(hLbl, 0, h + 1);
            for (int j = 0; j < jours.length; j++) {
                VBox cell = new VBox(2); cell.setPadding(new Insets(3));
                cell.setStyle("-fx-border-color:#e2e8f0;-fx-border-width:0.5;-fx-background-color:white;");
                cell.setMinHeight(60);
                for (Cours c : par.get(jours[j])) {
                    if (c.getCreneauInfo() != null && c.getCreneauInfo().contains(heures[h] + "h")) {
                        String hD = extraireHeureDebut(c.getCreneauInfo());
                        String hF = extraireHeureFin(c.getCreneauInfo());
                        Label m = new Label("📚 " + (c.getMatiereNom() != null ? c.getMatiereNom() : "?"));
                        m.setStyle("-fx-background-color:#0ea5e9;-fx-text-fill:white;-fx-padding:2 6;-fx-background-radius:4;-fx-font-size:10px;");
                        m.setWrapText(true);
                        Label horaire = new Label("⏰ " + hD + " → " + hF); // ✅ heure début → fin
                        horaire.setStyle("-fx-text-fill:#0ea5e9;-fx-font-size:9px;-fx-font-weight:bold;");
                        Label e = new Label("👤 " + (c.getEnseignantNom() != null ? c.getEnseignantNom() : "?"));
                        e.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");
                        Label s = new Label("🏫 " + (c.getSalleNumero() != null ? c.getSalleNumero() : "?"));
                        s.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");
                        cell.getChildren().addAll(m, horaire, e, s);
                    }
                }
                calendarGrid.add(cell, j + 1, h + 1);
            }
        }
        if (calendarWeekLabel != null)
            calendarWeekLabel.setText("Semaine du " + calendarWeekStart.getDayOfMonth()
                    + "/" + calendarWeekStart.getMonthValue() + "/" + calendarWeekStart.getYear());
    }

    @FXML private void handlePrevWeek()  { calendarWeekStart = calendarWeekStart.minusWeeks(1); buildCalendar(); }
    @FXML private void handleNextWeek()  { calendarWeekStart = calendarWeekStart.plusWeeks(1);  buildCalendar(); }
    @FXML private void handleTodayWeek() { calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY); buildCalendar(); }

    // ═══════════════════════════ STATS ═══════════════════════════
    private void buildStats() {
        if (statCoursLabel   != null) statCoursLabel.setText(String.valueOf(coursList.size()));
        long nbMat = coursList.stream().map(Cours::getMatiereNom).filter(Objects::nonNull).distinct().count();
        if (statMatiereLabel != null) statMatiereLabel.setText(String.valueOf(nbMat));
        if (statHeuresLabel  != null) statHeuresLabel.setText(coursList.size() * 2 + "h");
        if (statNiveauLabel  != null) {
            ClassePedago cls = classeCombo.getValue();
            statNiveauLabel.setText(cls != null && cls.getNiveau() != null ? cls.getNiveau() : "—");
        }
    }

    // ═══════════════════════════ PROFIL ══════════════════════════
    private void buildProfil() {
        if (profilNom   != null) profilNom.setText(currentUser.getNomComplet());
        if (profilEmail != null) profilEmail.setText(currentUser.getEmail());
        if (currentUser instanceof Etudiant) {
            Etudiant etu = (Etudiant) currentUser;
            if (profilINE    != null) profilINE.setText(etu.getINE() != null && !etu.getINE().isEmpty() ? etu.getINE() : "—");
            if (profilNiveau != null) profilNiveau.setText(etu.getNiveau() != null ? etu.getNiveau() : "—");
        } else {
            if (profilINE    != null) profilINE.setText("—");
            if (profilNiveau != null) profilNiveau.setText("—");
        }
        ClassePedago cls = classeCombo.getValue();
        if (profilClasse != null) profilClasse.setText(cls != null ? cls.getNom() : "—");
    }

    // ═══════════════════════════ NOTIFICATIONS ═══════════════════
    private void setupNotifTable() {
        if (notifTable == null) return;
        if (colNotifMsg  != null) colNotifMsg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        if (colNotifType != null) colNotifType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        if (colNotifDate != null) colNotifDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateEnvoi() != null ? d.getValue().getDateEnvoi().toLocalDate().toString() : ""));
        notifTable.setItems(notifList);
    }

    private void loadNotifications() {
        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
        int unread = notifDAO.countUnread(currentUser.getId());
        if (notifBadge != null) {
            notifBadge.setText(unread > 0 ? "🔔 " + unread + " non lue(s)" : "🔔 Aucune nouvelle");
            notifBadge.setStyle(unread > 0 ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;" : "-fx-text-fill:#64748b;");
        }
    }

    @FXML private void handleMarkAllRead() { notifDAO.markAllRead(currentUser.getId()); loadNotifications(); }

    // ═══════════════════════════ RECHERCHE ════════════════════════
    private void setupSearchControls() {
        typeFilter.setItems(FXCollections.observableArrayList("TOUS","TD","TP","AMPHI"));
        typeFilter.setValue("TOUS");
        capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1));
        heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES)); heureDebutCombo.setValue("08:00");
        heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));   heureFinCombo.setValue("10:00");
        if (dateHeureBox != null) dateHeureBox.setDisable(true);
        radioMaintenantBtn.setSelected(true);
        radioMaintenantBtn.setOnAction(e -> { if (dateHeureBox != null) dateHeureBox.setDisable(true); });
        radioDateHeureBtn.setOnAction(e -> {
            if (dateHeureBox != null) dateHeureBox.setDisable(false);
            if (datePicker.getValue() == null) datePicker.setValue(LocalDate.now());
        });
    }

    private void setupSalleTable() {
        colSalleNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colSalleType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSalle()));
        colSalleCap.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        colSalleBat.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBatimentNom() != null ? d.getValue().getBatimentNom() : ""));
        colSalleEquip.setCellValueFactory(d -> {
            List<String> eq = salleDAO.getEquipementsDisponibles(d.getValue().getId());
            return new SimpleStringProperty(eq.isEmpty() ? "—" : String.join(", ", eq));
        });
        colSalleStatut.setCellValueFactory(d -> new SimpleStringProperty("🟢 Libre"));
        salleLibreTable.setItems(salleList);
    }

    @FXML private void handleRechercherSalles() {
        int cap = capaciteSpinner.getValue();
        String type = typeFilter.getValue();
        if ("TOUS".equals(type)) type = null;
        List<String> equips = new ArrayList<>();
        if (checkProjecteur.isSelected()) equips.add("PROJECTEUR");
        if (checkTableau.isSelected())    equips.add("TABLEAU");
        if (checkClim.isSelected())       equips.add("CLIM");
        if (checkOrdinateur.isSelected()) equips.add("ORDINATEUR");
        LocalDate date = null; Integer hDebut = null, hFin = null; String label = "";
        if (radioMaintenantBtn.isSelected()) {
            date = LocalDate.now(); hDebut = LocalTime.now().getHour(); hFin = hDebut + 1;
            label = "Maintenant (" + String.format("%02d:00", hDebut) + ")";
        } else if (radioDateHeureBtn.isSelected()) {
            date = datePicker.getValue();
            if (heureDebutCombo.getValue() != null) hDebut = Integer.parseInt(heureDebutCombo.getValue().split(":")[0]);
            if (heureFinCombo.getValue()   != null) hFin   = Integer.parseInt(heureFinCombo.getValue().split(":")[0]);
            if (date != null && hDebut != null) label = date + " " + heureDebutCombo.getValue() + "→" + heureFinCombo.getValue();
        }
        List<Salle> result = salleDAO.findDisponiblesAvancee(cap, type, date, hDebut, hFin, equips);
        salleList.setAll(result);
        String eq = equips.isEmpty() ? "" : " +équip: " + String.join("+", equips);
        if (result.isEmpty()) {
            resultLabel.setText("❌ Aucune salle libre" + eq + (label.isEmpty() ? "" : " @ " + label));
            resultLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;-fx-font-size:12px;");
        } else {
            resultLabel.setText("✅ " + result.size() + " salle(s) disponible(s)" + eq + (label.isEmpty() ? "" : " @ " + label));
            resultLabel.setStyle("-fx-text-fill:#16a34a;-fx-font-weight:bold;-fx-font-size:12px;");
        }
    }

    // ═══════════════════════════ EXPORTS ══════════════════════════
    @FXML private void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'emploi du temps en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));
        fc.setInitialFileName("edt_classe.pdf");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try { exportSvc.exportCoursAsPDF(coursList, f); showInfo("Export PDF", "Exporté : " + f.getName()); }
        catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    @FXML private void handleLogout() { logout(); }
}