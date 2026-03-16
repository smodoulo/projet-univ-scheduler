package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
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

public class EnseignantDashboardController extends BaseController {

    // ─── EDT ───
    @FXML private Label welcomeLabel, totalCoursLabel, totalReservsLabel;
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colCls, colCren, colSalle, colDate, colStatut;
    @FXML private VBox chartEnsContainer;

    // ─── RECHERCHE RAPIDE ───
    @FXML private TableView<Salle> salleLibreTable;
    @FXML private TableColumn<Salle, String>  colSalleNum, colSalleType, colSalleBat, colSalleEquip;
    @FXML private TableColumn<Salle, Integer> colSalleCap;
    @FXML private DatePicker       datePicker;
    @FXML private ComboBox<String> heureDebutCombo, heureFinCombo, typeFilter;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private CheckBox         checkProjecteur, checkTableau, checkClim;
    @FXML private Label            resultLabel;

    // ─── RÉSERVATION ───
    @FXML private TableView<Reservation> reservTable;
    @FXML private TableColumn<Reservation, String> colResMotif, colResSalle, colResDate, colResStatut;
    @FXML private ComboBox<Salle>  salleReservCombo;
    @FXML private DatePicker       dateReservPicker;
    @FXML private ComboBox<String> heureReservCombo;
    @FXML private TextField        motifField;
    @FXML private Label            conflitReservLabel;

    // ─── NOTIFICATIONS ───
    @FXML private TextArea problemeArea;
    @FXML private TableView<Notification> notifTable;
    @FXML private TableColumn<Notification, String> colNotifMsg, colNotifType, colNotifDate;
    @FXML private Label notifBadge;

    private final CoursDAO       coursDAO   = new CoursDAO();
    private final ReservationDAO reservDAO  = new ReservationDAO();
    private final SalleDAO       salleDAO   = new SalleDAO();
    private final NotificationDAO notifDAO  = new NotificationDAO();
    private final ExportService  exportSvc  = new ExportService();

    private final ObservableList<Cours>       coursList  = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservList = FXCollections.observableArrayList();
    private final ObservableList<Notification> notifList = FXCollections.observableArrayList();
    private final ObservableList<Salle>       salleList  = FXCollections.observableArrayList();
    private Reservation selectedReserv = null;

    private static final List<String> HEURES = List.of(
        "08:00","09:00","10:00","11:00","12:00","13:00",
        "14:00","15:00","16:00","17:00","18:00");

    // ═══════════════════════════ INIT ════════════════════════════
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupCoursTable();
        setupReservTable();
        setupSalleTable();
        setupNotifTable();
        setupSearchControls();
        loadData();
        buildChart();
        handleRechercherSalles(); // pre-load available rooms
    }

    private void setupSearchControls() {
        if (typeFilter != null) {
            typeFilter.setItems(FXCollections.observableArrayList("TOUS","TD","TP","AMPHI"));
            typeFilter.setValue("TOUS");
        }
        if (capaciteSpinner != null)
            capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,500,1));

        if (heureDebutCombo != null) { heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES)); heureDebutCombo.setValue("08:00"); }
        if (heureFinCombo   != null) { heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));   heureFinCombo.setValue("10:00");   }
        if (heureReservCombo!= null) { heureReservCombo.setItems(FXCollections.observableArrayList(HEURES)); heureReservCombo.setValue("08:00"); }

        // Pre-set date to today
        if (datePicker != null && datePicker.getValue() == null)
            datePicker.setValue(LocalDate.now());
        if (dateReservPicker != null && dateReservPicker.getValue() == null)
            dateReservPicker.setValue(LocalDate.now());

        // Live conflict check
        if (salleReservCombo  != null) salleReservCombo.setOnAction(e -> checkReservConflict());
        if (dateReservPicker  != null) dateReservPicker.setOnAction(e -> checkReservConflict());
        if (heureReservCombo  != null) heureReservCombo.setOnAction(e -> checkReservConflict());

        salleReservCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
    }

    // ═══════════════════════════ EDT ═════════════════════════════
    private void setupCoursTable() {
        colMat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colCls.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClasseNom()));
        colCren.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreneauInfo()));
        colSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        coursTable.setItems(coursList);
    }

    private void loadData() {
        coursList.setAll(coursDAO.findByEnseignant(currentUser.getId()));
        reservList.setAll(reservDAO.findByUtilisateur(currentUser.getId()));
        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
        if (totalCoursLabel  != null) totalCoursLabel.setText("Mes cours : " + coursList.size());
        if (totalReservsLabel!= null) totalReservsLabel.setText("Mes réservations : " + reservList.size());
        int unread = notifDAO.countUnread(currentUser.getId());
        if (notifBadge != null) {
            notifBadge.setText(unread > 0 ? "🔔 "+unread+" non lue(s)" : "🔔 Aucune nouvelle");
            notifBadge.setStyle(unread > 0
                ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                : "-fx-text-fill:#64748b;");
        }
    }

    private void buildChart() {
        if (chartEnsContainer == null) return;
        Map<String,Integer> statuts = coursDAO.countByStatut();
        PieChart pie = new PieChart(); pie.setTitle("📊 Mes cours par statut"); pie.setPrefHeight(220);
        statuts.forEach((k,v) -> pie.getData().add(new PieChart.Data(k+" ("+v+")",v)));
        chartEnsContainer.getChildren().setAll(pie);
    }

    // ═══════════════════════════ RECHERCHE ════════════════════════
    private void setupSalleTable() {
        if (salleLibreTable == null) return;
        if (colSalleNum  != null) colSalleNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        if (colSalleType != null) colSalleType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSalle()));
        if (colSalleCap  != null) colSalleCap.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        if (colSalleBat  != null) colSalleBat.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getBatimentNom() != null ? d.getValue().getBatimentNom() : ""));
        if (colSalleEquip != null) colSalleEquip.setCellValueFactory(d -> {
            List<String> eq = salleDAO.getEquipementsDisponibles(d.getValue().getId());
            return new SimpleStringProperty(eq.isEmpty() ? "—" : String.join(", ", eq));
        });
        salleLibreTable.setItems(salleList);

        // Click → pre-fill reservation form
        salleLibreTable.getSelectionModel().selectedItemProperty().addListener((o,old,s) -> {
            if (s != null) { salleReservCombo.setValue(s); checkReservConflict(); }
        });
    }

    @FXML
    private void handleRechercherSalles() {
        if (salleLibreTable == null) return;

        int cap  = capaciteSpinner != null ? capaciteSpinner.getValue() : 1;
        String t = typeFilter != null ? typeFilter.getValue() : null;
        String type = (t == null || "TOUS".equals(t)) ? null : t;

        List<String> equips = new ArrayList<>();
        if (checkProjecteur != null && checkProjecteur.isSelected()) equips.add("PROJECTEUR");
        if (checkTableau    != null && checkTableau.isSelected())    equips.add("TABLEAU");
        if (checkClim       != null && checkClim.isSelected())       equips.add("CLIM");

        LocalDate date   = datePicker != null ? datePicker.getValue() : LocalDate.now();
        Integer   hDebut = null, hFin = null;
        if (heureDebutCombo != null && heureDebutCombo.getValue() != null)
            hDebut = Integer.parseInt(heureDebutCombo.getValue().split(":")[0]);
        if (heureFinCombo != null && heureFinCombo.getValue() != null)
            hFin = Integer.parseInt(heureFinCombo.getValue().split(":")[0]);

        // If no date given, use current time
        if (date == null) { date = LocalDate.now(); hDebut = LocalTime.now().getHour(); hFin = hDebut + 1; }

        List<Salle> result = salleDAO.findDisponiblesAvancee(cap, type, date, hDebut, hFin, equips);
        salleList.setAll(result);

        if (resultLabel != null) {
            if (result.isEmpty()) {
                resultLabel.setText("❌ Aucune salle disponible pour ces critères.");
                resultLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");
            } else {
                resultLabel.setText("✅ " + result.size() + " salle(s) disponible(s)");
                resultLabel.setStyle("-fx-text-fill:#16a34a;-fx-font-weight:bold;");
            }
        }
        // Update reservation combo
        salleReservCombo.setItems(FXCollections.observableArrayList(result.isEmpty() ? salleDAO.findAll() : result));
    }

    // ═══════════════════════════ RÉSERVATION ══════════════════════
    private void setupReservTable() {
        colResMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));
        colResSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colResDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDateReservation() != null
                ? d.getValue().getDateReservation().toLocalDate().toString() : ""));
        colResStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        reservTable.setItems(reservList);
        reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r) -> selectedReserv = r);
    }

    private void checkReservConflict() {
        if (conflitReservLabel == null) return;
        Salle s    = salleReservCombo != null ? salleReservCombo.getValue() : null;
        LocalDate d = dateReservPicker != null ? dateReservPicker.getValue() : null;
        String hStr = heureReservCombo != null ? heureReservCombo.getValue() : null;
        if (s == null || d == null || hStr == null) { conflitReservLabel.setVisible(false); return; }

        int h = Integer.parseInt(hStr.split(":")[0]);
        boolean libre = salleDAO.isSalleLibre(s.getId(), d, h, h + 2);
        conflitReservLabel.setText(libre
            ? "✅ Salle disponible sur ce créneau"
            : "⚠️ Salle déjà occupée sur ce créneau !");
        conflitReservLabel.setStyle(libre
            ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;"
            : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        conflitReservLabel.setVisible(true);
    }

    @FXML
    private void handleSaveReservation() {
        Salle s   = salleReservCombo != null ? salleReservCombo.getValue() : null;
        String motif = motifField != null ? motifField.getText().trim() : "";
        LocalDate d   = dateReservPicker != null ? dateReservPicker.getValue() : null;
        String hStr   = heureReservCombo != null && heureReservCombo.getValue() != null
                        ? heureReservCombo.getValue() : "08:00";

        if (s == null || motif.isEmpty() || d == null) {
            showError("Erreur", "Veuillez remplir tous les champs."); return;
        }
        int h = Integer.parseInt(hStr.split(":")[0]);

        // Hard conflict check
        if (!salleDAO.isSalleLibre(s.getId(), d, h, h + 2)) {
            showError("Conflit de réservation",
                "La salle " + s.getNumero() + " est déjà occupée\n"
                + "le " + d + " de " + hStr + " à " + String.format("%02d:00", h+2) + ".\n\n"
                + "Veuillez choisir un autre créneau ou une autre salle.");
            return;
        }

        Reservation r = new Reservation();
        r.setSalleId(s.getId());
        r.setMotif(motif);
        r.setDateReservation(d.atTime(h, 0));
        r.setStatut("EN_ATTENTE");
        r.setUtilisateurId(currentUser.getId());
        reservDAO.save(r);

        if (motifField != null) motifField.clear();
        if (salleReservCombo  != null) salleReservCombo.setValue(null);
        if (conflitReservLabel!= null) conflitReservLabel.setVisible(false);
        loadData();
        showInfo("Réservation enregistrée",
            "Votre demande pour la salle " + s.getNumero()
            + " le " + d + " à " + hStr + " est en attente de validation.");
    }

    @FXML
    private void handleDeleteReservation() {
        if (selectedReserv == null) { showError("Erreur", "Sélectionnez une réservation."); return; }
        if ("VALIDEE".equals(selectedReserv.getStatut())) {
            showError("Action impossible", "Impossible d'annuler une réservation validée.\nContactez le gestionnaire."); return;
        }
        if (confirmDelete("cette réservation")) { reservDAO.delete(selectedReserv.getId()); loadData(); }
    }

    // ═══════════════════════════ NOTIFICATIONS ════════════════════
    private void setupNotifTable() {
        if (notifTable == null) return;
        if (colNotifMsg  != null) colNotifMsg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        if (colNotifType != null) colNotifType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        if (colNotifDate != null) colNotifDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDateEnvoi() != null ? d.getValue().getDateEnvoi().toLocalDate().toString() : ""));
        notifTable.setItems(notifList);
    }

    @FXML
    private void handleSendProbleme() {
        String txt = problemeArea != null ? problemeArea.getText().trim() : "";
        if (txt.isEmpty()) { showError("Erreur", "Décrivez le problème."); return; }
        Notification n = new Notification();
        n.setMessage("[PROBLÈME] " + currentUser.getNomComplet() + " : " + txt);
        n.setType("PROBLEME"); n.setUtilisateurId(1);
        notifDAO.save(n);
        if (problemeArea != null) problemeArea.clear();
        showInfo("Envoyé", "Signalement transmis à l'administration.");
    }

    @FXML private void handleMarkAllRead() { notifDAO.markAllRead(currentUser.getId()); loadData(); }

    // ═══════════════════════════ EXPORTS ══════════════════════════
    @FXML
    private void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter mon emploi du temps en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));
        fc.setInitialFileName("mon_edt.pdf");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try { exportSvc.exportCoursAsPDF(coursList, f); showInfo("Export PDF", "Exporté : " + f.getName()); }
        catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel","*.xlsx"));
        fc.setInitialFileName("mon_edt.xlsx");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try { exportSvc.exportCoursAsExcel(coursList, f); showInfo("Export Excel", "Exporté : " + f.getName()); }
        catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    @FXML private void handleLogout() { logout(); }
}
