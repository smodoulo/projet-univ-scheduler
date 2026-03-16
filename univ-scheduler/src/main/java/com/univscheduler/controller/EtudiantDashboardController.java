package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.service.ExportService;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class EtudiantDashboardController extends BaseController {

    // ─── EDT ───
    @FXML private Label welcomeLabel, totalCoursLabel;
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colEns, colCren, colSalle, colDate;
    @FXML private ComboBox<ClassePedago> classeCombo;
    @FXML private VBox chartEtuContainer;

    // ─── RECHERCHE AVANCÉE ───
    @FXML private TableView<Salle> salleLibreTable;
    @FXML private TableColumn<Salle, String>  colSalleNum, colSalleType, colSalleBat, colSalleEquip, colSalleStatut;
    @FXML private TableColumn<Salle, Integer> colSalleCap;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private ComboBox<String> typeFilter;
    @FXML private RadioButton radioMaintenantBtn, radioDateHeureBtn;
    @FXML private DatePicker   datePicker;
    @FXML private ComboBox<String> heureDebutCombo, heureFinCombo;
    @FXML private javafx.scene.layout.HBox dateHeureBox;
    @FXML private CheckBox checkProjecteur, checkTableau, checkClim, checkOrdinateur;
    @FXML private Label resultLabel;

    private final CoursDAO        coursDAO  = new CoursDAO();
    private final ClassePedagoDAO classeDAO = new ClassePedagoDAO();
    private final SalleDAO        salleDAO  = new SalleDAO();
    private final ExportService   exportSvc = new ExportService();

    private final ObservableList<Cours> coursList = FXCollections.observableArrayList();
    private final ObservableList<Salle> salleList = FXCollections.observableArrayList();

    private static final List<String> HEURES = List.of(
            "08:00","09:00","10:00","11:00","12:00","13:00",
            "14:00","15:00","16:00","17:00","18:00");

    // ═══════════════════════════ INIT ════════════════════════════
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupEdtTable();
        setupSalleTable();
        setupSearchControls();

        List<ClassePedago> classes = classeDAO.findAll();
        classeCombo.setItems(FXCollections.observableArrayList(classes));
        if (!classes.isEmpty()) { classeCombo.setValue(classes.get(0)); loadCours(); }
        classeCombo.setOnAction(e -> { loadCours(); buildChart(); });

        handleRechercherSalles();
    }

    private void setupSearchControls() {
        typeFilter.setItems(FXCollections.observableArrayList("TOUS","TD","TP","AMPHI"));
        typeFilter.setValue("TOUS");
        capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,500,1));
        heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES)); heureDebutCombo.setValue("08:00");
        heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));   heureFinCombo.setValue("10:00");

        dateHeureBox.setDisable(true);
        radioMaintenantBtn.setSelected(true);
        radioMaintenantBtn.setOnAction(e -> dateHeureBox.setDisable(true));
        radioDateHeureBtn.setOnAction(e -> {
            dateHeureBox.setDisable(false);
            if (datePicker.getValue() == null) datePicker.setValue(LocalDate.now());
        });
    }

    // ═══════════════════════════ EDT ═════════════════════════════
    private void setupEdtTable() {
        colMat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colEns.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEnseignantNom()));
        colCren.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreneauInfo()));
        colSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        coursTable.setItems(coursList);
    }

    private void loadCours() {
        ClassePedago c = classeCombo.getValue();
        if (c == null) return;
        coursList.setAll(coursDAO.findByClasse(c.getId()));
        if (totalCoursLabel != null)
            totalCoursLabel.setText("Cours de " + c.getNom() + " : " + coursList.size());
        buildChart();
    }

    private void buildChart() {
        if (chartEtuContainer == null) return;
        Map<String,Integer> parMat = new LinkedHashMap<>();
        for (Cours c : coursList)
            parMat.merge(c.getMatiereNom() != null ? c.getMatiereNom() : "?", 1, Integer::sum);
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
        xA.setLabel("Matière"); yA.setLabel("Séances");
        BarChart<String,Number> chart = new BarChart<>(xA,yA);
        chart.setTitle("📚 Cours par Matière"); chart.setLegendVisible(false); chart.setPrefHeight(220);
        XYChart.Series<String,Number> s = new XYChart.Series<>();
        parMat.forEach((k,v) -> s.getData().add(new XYChart.Data<>(k,v)));
        chart.getData().add(s);
        chartEtuContainer.getChildren().setAll(chart);
    }

    // ═══════════════════════════ RECHERCHE ════════════════════════
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

    @FXML
    private void handleRechercherSalles() {
        int cap     = capaciteSpinner.getValue();
        String type = typeFilter.getValue();
        if ("TOUS".equals(type)) type = null;

        List<String> equips = new ArrayList<>();
        if (checkProjecteur.isSelected()) equips.add("PROJECTEUR");
        if (checkTableau.isSelected())    equips.add("TABLEAU");
        if (checkClim.isSelected())       equips.add("CLIM");
        if (checkOrdinateur.isSelected()) equips.add("ORDINATEUR");

        LocalDate date   = null;
        Integer   hDebut = null;
        Integer   hFin   = null;
        String    label  = "";

        if (radioMaintenantBtn.isSelected()) {
            date   = LocalDate.now();
            hDebut = LocalTime.now().getHour();
            hFin   = hDebut + 1;
            label  = "Maintenant (" + String.format("%02d:00", hDebut) + ")";
        } else if (radioDateHeureBtn.isSelected()) {
            date   = datePicker.getValue();
            if (heureDebutCombo.getValue() != null)
                hDebut = Integer.parseInt(heureDebutCombo.getValue().split(":")[0]);
            if (heureFinCombo.getValue() != null)
                hFin   = Integer.parseInt(heureFinCombo.getValue().split(":")[0]);
            if (date != null && hDebut != null)
                label = date + "  " + heureDebutCombo.getValue() + "→" + heureFinCombo.getValue();
        }

        List<Salle> result = salleDAO.findDisponiblesAvancee(cap, type, date, hDebut, hFin, equips);
        salleList.setAll(result);

        String equipStr = equips.isEmpty() ? "" : "  +équip: " + String.join("+", equips);
        if (result.isEmpty()) {
            resultLabel.setText("❌ Aucune salle libre" + equipStr + (label.isEmpty() ? "" : " @ " + label));
            resultLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;-fx-font-size:12px;");
        } else {
            resultLabel.setText("✅ " + result.size() + " salle(s) disponible(s)" + equipStr + (label.isEmpty() ? "" : " @ " + label));
            resultLabel.setStyle("-fx-text-fill:#16a34a;-fx-font-weight:bold;-fx-font-size:12px;");
        }
    }

    @FXML
    private void handleExportPDF() {
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