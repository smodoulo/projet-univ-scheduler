package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.model.Servicerappel;
import com.univscheduler.model.AlertePersonnalisee;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.univscheduler.service.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class EnseignantDashboardController extends BaseController {

    // ─── EDT ───
    @FXML private Label welcomeLabel, totalCoursLabel, totalReservsLabel;
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colCls, colCren, colSalle, colDate, colStatut;
    // ✅ AJOUT : colonnes heure début et heure fin
    @FXML private TableColumn<Cours, String> colHeureDebut, colHeureFin;
    @FXML private VBox chartEnsContainer;

    // ─── RECHERCHE ───
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
    @FXML private ComboBox<String> heureFinReservCombo;
    @FXML private TextField        motifField;
    @FXML private Label            conflitReservLabel;

    // ─── NOTIFICATIONS ───
    @FXML private TableView<Notification> notifTable;
    @FXML private TableColumn<Notification, String> colNotifMsg, colNotifType, colNotifDate;
    @FXML private Label notifBadge;

    // ─── SIGNALEMENT ───
    @FXML private TextField        sigTitreField;
    @FXML private TextArea         sigDescArea;
    @FXML private ComboBox<String> sigCategorieCombo;
    @FXML private ComboBox<String> sigPrioriteCombo;
    @FXML private ComboBox<Salle>  sigSalleCombo;
    @FXML private Label            sigFeedbackLabel;
    @FXML private TableView<Signalement>           mesSignalementsTable;
    @FXML private TableColumn<Signalement, String> colSigTitre, colSigCat,
            colSigPrio, colSigStatut, colSigDate, colSigSalle;
    @FXML private Label sigBadge;

    // ─── DAOs ───
    private final CoursDAO        coursDAO  = new CoursDAO();
    private final ReservationDAO  reservDAO = new ReservationDAO();
    private final SalleDAO        salleDAO  = new SalleDAO();
    private final NotificationDAO notifDAO  = new NotificationDAO();
    private final SignalementDAO  sigDAO    = new SignalementDAO();
    private final ExportService   exportSvc = new ExportService();

    private final ObservableList<Cours>        coursList  = FXCollections.observableArrayList();
    private final ObservableList<Reservation>  reservList = FXCollections.observableArrayList();
    private final ObservableList<Notification> notifList  = FXCollections.observableArrayList();
    private final ObservableList<Salle>        salleList  = FXCollections.observableArrayList();
    private final ObservableList<Signalement>  sigList    = FXCollections.observableArrayList();

    private Reservation selectedReserv = null;

    private static final List<String> HEURES = List.of(
            "08:00","09:00","10:00","11:00","12:00","13:00",
            "14:00","15:00","16:00","17:00","18:00","19:00","20:00");

    // ═══════════════════════════ INIT ════════════════════════════
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupCoursTable();
        setupReservTable();
        setupSalleTable();
        setupNotifTable();
        setupSearchControls();
        setupSignalementForm();
        setupMesSignalementsTable();
        loadData();
        buildChart();
        handleRechercherSalles();
        Servicerappel.getInstance().demarrer();
    }

    // ── Helpers extraction créneau ────────────────────────────────
    /** "LUNDI 12h" → "LUNDI" */
    private String extraireJour(String info) {
        if (info == null) return "—";
        String[] p = info.split(" ");
        return p.length > 0 ? p[0] : info;
    }

    /** "LUNDI 12h" → "12:00" */
    private String extraireHeureDebut(String info) {
        if (info == null) return "—";
        try {
            for (String p : info.split(" "))
                if (p.endsWith("h") && !p.startsWith("("))
                    return String.format("%02d:00", Integer.parseInt(p.replace("h", "")));
        } catch (Exception ignored) {}
        return "—";
    }

    /** "LUNDI 12h (2h)" → "14:00" | "LUNDI 12h" → "14:00" (2h par défaut) */
    private String extraireHeureFin(String info) {
        if (info == null) return "—";
        try {
            int debut = -1, duree = 2;
            for (String p : info.split(" ")) {
                if (p.endsWith("h") && !p.startsWith("(") && debut == -1)
                    debut = Integer.parseInt(p.replace("h", ""));
                if (p.startsWith("(") && p.endsWith("h)"))
                    duree = Integer.parseInt(p.replace("(", "").replace("h)", ""));
            }
            if (debut >= 0) return String.format("%02d:00", debut + duree);
        } catch (Exception ignored) {}
        return "—";
    }

    // ═══════════════════════════ EDT ═════════════════════════════
    private void setupCoursTable() {
        colMat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colCls.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClasseNom()));
        // ✅ MODIFIÉ : Jour seulement dans colCren
        colCren.setCellValueFactory(d -> new SimpleStringProperty(extraireJour(d.getValue().getCreneauInfo())));
        // ✅ AJOUT : colonnes Début et Fin
        if (colHeureDebut != null)
            colHeureDebut.setCellValueFactory(d ->
                    new SimpleStringProperty(extraireHeureDebut(d.getValue().getCreneauInfo())));
        if (colHeureFin != null)
            colHeureFin.setCellValueFactory(d ->
                    new SimpleStringProperty(extraireHeureFin(d.getValue().getCreneauInfo())));
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
        sigList.setAll(sigDAO.findByEnseignant(currentUser.getId()));
        if (totalCoursLabel   != null) totalCoursLabel.setText("Mes cours : " + coursList.size());
        if (totalReservsLabel != null) totalReservsLabel.setText("Mes réservations : " + reservList.size());
        int unread = notifDAO.countUnread(currentUser.getId());
        if (notifBadge != null) {
            notifBadge.setText(unread > 0 ? "🔔 " + unread + " non lue(s)" : "🔔 Aucune nouvelle");
            notifBadge.setStyle(unread > 0
                    ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                    : "-fx-text-fill:#64748b;");
        }
        long resolus = sigList.stream().filter(s -> "RESOLU".equals(s.getStatut())).count();
        if (sigBadge != null) { sigBadge.setText(resolus > 0 ? "✅ " + resolus + " résolu(s)" : ""); sigBadge.setVisible(resolus > 0); }
    }

    // ✅ MODIFIÉ : labels du camembert en français avec icônes
    private void buildChart() {
        if (chartEnsContainer == null) return;
        Map<String,Integer> statuts = coursDAO.countByStatut();
        PieChart pie = new PieChart();
        pie.setTitle("Répartition de mes cours");
        pie.setPrefHeight(220);
        pie.setLegendVisible(true);
        statuts.forEach((k, v) -> {
            String label;
            switch (k) {
                case "PLANIFIE":  label = "📅 Planifié";  break;
                case "REALISE":   label = "✅ Réalisé";   break;
                case "ANNULE":    label = "❌ Annulé";    break;
                case "EN_COURS":  label = "🔄 En cours";  break;
                case "TERMINE":   label = "🏁 Terminé";   break;
                default:          label = k;
            }
            pie.getData().add(new PieChart.Data(label + " (" + v + ")", v));
        });
        chartEnsContainer.getChildren().setAll(pie);
    }

    // ═══════════════════════════ RECHERCHE ════════════════════════
    private void setupSearchControls() {
        if (typeFilter != null) { typeFilter.setItems(FXCollections.observableArrayList("TOUS","TD","TP","AMPHI")); typeFilter.setValue("TOUS"); }
        if (capaciteSpinner != null) capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,500,1));
        if (heureDebutCombo    != null) { heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES));    heureDebutCombo.setValue("08:00"); }
        if (heureFinCombo      != null) { heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));      heureFinCombo.setValue("10:00"); }
        if (heureReservCombo   != null) { heureReservCombo.setItems(FXCollections.observableArrayList(HEURES));   heureReservCombo.setValue("08:00"); }
        if (heureFinReservCombo != null) { heureFinReservCombo.setItems(FXCollections.observableArrayList(HEURES)); heureFinReservCombo.setValue("10:00"); }
        if (datePicker       != null && datePicker.getValue()       == null) datePicker.setValue(LocalDate.now());
        if (dateReservPicker != null && dateReservPicker.getValue() == null) dateReservPicker.setValue(LocalDate.now());
        if (salleReservCombo    != null) salleReservCombo.setOnAction(e -> checkReservConflict());
        if (dateReservPicker    != null) dateReservPicker.setOnAction(e -> checkReservConflict());
        if (heureReservCombo    != null) heureReservCombo.setOnAction(e -> checkReservConflict());
        if (heureFinReservCombo != null) heureFinReservCombo.setOnAction(e -> checkReservConflict());
        salleReservCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
    }

    private void setupSalleTable() {
        if (salleLibreTable == null) return;
        if (colSalleNum  != null) colSalleNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        if (colSalleType != null) colSalleType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSalle()));
        if (colSalleCap  != null) colSalleCap.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        if (colSalleBat  != null) colSalleBat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatimentNom() != null ? d.getValue().getBatimentNom() : ""));
        if (colSalleEquip != null) colSalleEquip.setCellValueFactory(d -> {
            List<String> eq = salleDAO.getEquipementsDisponibles(d.getValue().getId());
            return new SimpleStringProperty(eq.isEmpty() ? "—" : String.join(", ", eq));
        });
        salleLibreTable.setItems(salleList);
        salleLibreTable.getSelectionModel().selectedItemProperty().addListener((o,old,s) -> {
            if (s != null) { salleReservCombo.setValue(s); checkReservConflict(); }
        });
    }

    @FXML private void handleRechercherSalles() {
        if (salleLibreTable == null) return;
        int cap = capaciteSpinner != null ? capaciteSpinner.getValue() : 1;
        String t = typeFilter != null ? typeFilter.getValue() : null;
        String type = (t == null || "TOUS".equals(t)) ? null : t;
        List<String> equips = new ArrayList<>();
        if (checkProjecteur != null && checkProjecteur.isSelected()) equips.add("PROJECTEUR");
        if (checkTableau    != null && checkTableau.isSelected())    equips.add("TABLEAU");
        if (checkClim       != null && checkClim.isSelected())       equips.add("CLIM");
        LocalDate date = datePicker != null ? datePicker.getValue() : LocalDate.now();
        Integer hDebut = null, hFin = null;
        if (heureDebutCombo != null && heureDebutCombo.getValue() != null)
            hDebut = Integer.parseInt(heureDebutCombo.getValue().split(":")[0]);
        if (heureFinCombo != null && heureFinCombo.getValue() != null)
            hFin = Integer.parseInt(heureFinCombo.getValue().split(":")[0]);
        if (date == null) { date = LocalDate.now(); hDebut = LocalTime.now().getHour(); hFin = hDebut + 1; }
        List<Salle> result = salleDAO.findDisponiblesAvancee(cap, type, date, hDebut, hFin, equips);
        salleList.setAll(result);
        if (resultLabel != null) {
            resultLabel.setText(result.isEmpty()
                    ? "❌ Aucune salle disponible pour ces critères."
                    : "✅ " + result.size() + " salle(s) disponible(s)");
            resultLabel.setStyle(result.isEmpty()
                    ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                    : "-fx-text-fill:#16a34a;-fx-font-weight:bold;");
        }
        salleReservCombo.setItems(FXCollections.observableArrayList(result.isEmpty() ? salleDAO.findAll() : result));
    }

    // ═══════════════════════════ RÉSERVATION ══════════════════════
    private void setupReservTable() {
        colResMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));
        colResSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colResDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateReservation() != null ? d.getValue().getDateReservation().toLocalDate().toString() : ""));
        colResStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        reservTable.setItems(reservList);
        reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r) -> selectedReserv = r);
    }

    private void checkReservConflict() {
        if (conflitReservLabel == null) return;
        Salle s     = salleReservCombo    != null ? salleReservCombo.getValue()    : null;
        LocalDate d = dateReservPicker    != null ? dateReservPicker.getValue()    : null;
        String hDeb = heureReservCombo    != null ? heureReservCombo.getValue()    : null;
        String hFin = heureFinReservCombo != null ? heureFinReservCombo.getValue() : null;
        if (s == null || d == null || hDeb == null) { conflitReservLabel.setVisible(false); return; }
        int debut = Integer.parseInt(hDeb.split(":")[0]);
        int fin   = hFin != null ? Integer.parseInt(hFin.split(":")[0]) : debut + 2;
        if (fin <= debut) {
            conflitReservLabel.setText("⚠️ L'heure de fin doit être après l'heure de début !");
            conflitReservLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");
            conflitReservLabel.setVisible(true); return;
        }
        boolean libre = salleDAO.isSalleLibre(s.getId(), d, debut, fin);
        conflitReservLabel.setText(libre ? "✅ Salle disponible" : "⚠️ Salle déjà occupée !");
        conflitReservLabel.setStyle(libre
                ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;"
                : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        conflitReservLabel.setVisible(true);
    }

    @FXML private void handleSaveReservation() {
        Salle s      = salleReservCombo  != null ? salleReservCombo.getValue()  : null;
        String motif = motifField        != null ? motifField.getText().trim()   : "";
        LocalDate d  = dateReservPicker  != null ? dateReservPicker.getValue()  : null;
        String hDeb  = heureReservCombo  != null && heureReservCombo.getValue() != null ? heureReservCombo.getValue() : "08:00";
        String hFin  = heureFinReservCombo != null && heureFinReservCombo.getValue() != null ? heureFinReservCombo.getValue() : null;
        if (s == null || motif.isEmpty() || d == null) { showError("Erreur","Veuillez remplir tous les champs."); return; }
        int debut = Integer.parseInt(hDeb.split(":")[0]);
        int fin   = hFin != null ? Integer.parseInt(hFin.split(":")[0]) : debut + 2;
        if (fin <= debut) { showError("Erreur","L'heure de fin doit être après l'heure de début."); return; }
        if (!salleDAO.isSalleLibre(s.getId(), d, debut, fin)) {
            showError("Conflit","La salle " + s.getNumero() + " est déjà occupée le " + d + " de " + hDeb + " à " + fin + ":00."); return;
        }
        Reservation r = new Reservation();
        r.setSalleId(s.getId()); r.setMotif(motif);
        r.setDateReservation(d.atTime(debut, 0));
        r.setDateFin(d.atTime(fin, 0));
        r.setStatut("EN_ATTENTE"); r.setUtilisateurId(currentUser.getId());
        reservDAO.save(r);
        if (motifField          != null) motifField.clear();
        if (salleReservCombo    != null) salleReservCombo.setValue(null);
        if (heureFinReservCombo != null) heureFinReservCombo.setValue("10:00");
        if (conflitReservLabel  != null) conflitReservLabel.setVisible(false);
        loadData();
        showInfo("Réservation enregistrée",
                "Demande pour " + s.getNumero() + " le " + d + " de " + hDeb + " à " + fin + ":00 → en attente de validation.");
    }

    @FXML private void handleDeleteReservation() {
        if (selectedReserv == null) { showError("Erreur","Sélectionnez une réservation."); return; }
        if ("VALIDEE".equals(selectedReserv.getStatut())) {
            showError("Action impossible","Impossible d'annuler une réservation validée.\nContactez le gestionnaire."); return;
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

    @FXML private void handleMarkAllRead() { notifDAO.markAllRead(currentUser.getId()); loadData(); }

    // ═══════════════════════════ SIGNALEMENT ══════════════════════
    private void setupSignalementForm() {
        if (sigCategorieCombo != null) sigCategorieCombo.setItems(FXCollections.observableArrayList("EQUIPEMENT","SALLE","AUTRE"));
        if (sigPrioriteCombo  != null) sigPrioriteCombo.setItems(FXCollections.observableArrayList("BASSE","NORMALE","HAUTE","URGENTE"));
        if (sigSalleCombo     != null) sigSalleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        if (sigCategorieCombo != null) sigCategorieCombo.setValue("EQUIPEMENT");
        if (sigPrioriteCombo  != null) sigPrioriteCombo.setValue("NORMALE");
        if (sigFeedbackLabel  != null) sigFeedbackLabel.setVisible(false);
    }

    private void setupMesSignalementsTable() {
        if (mesSignalementsTable == null) return;
        if (colSigTitre  != null) colSigTitre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategorieIcon() + " " + d.getValue().getTitre()));
        if (colSigCat    != null) colSigCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategorie()));
        if (colSigPrio   != null) colSigPrio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrioriteIcon() + " " + d.getValue().getPriorite()));
        if (colSigStatut != null) colSigStatut.setCellValueFactory(d -> new SimpleStringProperty(formatStatut(d.getValue().getStatut())));
        if (colSigDate   != null) colSigDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDateSignalement() != null ? d.getValue().getDateSignalement().toLocalDate().toString() : ""));
        if (colSigSalle  != null) colSigSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero() != null ? d.getValue().getSalleNumero() : "—"));
        mesSignalementsTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Signalement item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (item.getStatut()) {
                    case "RESOLU":   setStyle("-fx-background-color:#f0fdf4;"); break;
                    case "EN_COURS": setStyle("-fx-background-color:#fffbeb;"); break;
                    default:         setStyle("");
                }
            }
        });
        mesSignalementsTable.setItems(sigList);
    }

    @FXML private void handleSendProbleme() {
        String titre = sigTitreField != null ? sigTitreField.getText().trim() : "";
        if (titre.isEmpty()) { showFeedback("⚠ Veuillez saisir un titre pour le signalement.", false); return; }
        String    desc      = sigDescArea       != null ? sigDescArea.getText().trim()  : "";
        String    categorie = sigCategorieCombo != null ? sigCategorieCombo.getValue()  : "AUTRE";
        String    priorite  = sigPrioriteCombo  != null ? sigPrioriteCombo.getValue()   : "NORMALE";
        Salle     salle     = sigSalleCombo     != null ? sigSalleCombo.getValue()      : null;
        Signalement sig = new Signalement();
        sig.setTitre(titre); sig.setDescription(desc);
        sig.setCategorie(categorie != null ? categorie : "AUTRE");
        sig.setPriorite(priorite  != null ? priorite  : "NORMALE");
        sig.setStatut("EN_ATTENTE"); sig.setEnseignantId(currentUser.getId());
        sig.setDateSignalement(LocalDateTime.now());
        if (salle != null) { sig.setSalleId(salle.getId()); sig.setSalleNumero(salle.getNumero()); }
        sigDAO.save(sig);
        new UtilisateurDAO().findAll().stream()
                .filter(u -> "GESTIONNAIRE".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                .forEach(u -> {
                    Notification n = new Notification();
                    n.setUtilisateurId(u.getId()); n.setType("URGENTE".equals(sig.getPriorite()) ? "ALERTE" : "INFO");
                    n.setMessage(sig.getPrioriteIcon() + " Signalement #" + sig.getId() + " — " + sig.getTitre()
                            + " | Priorité : " + sig.getPriorite()
                            + (sig.getSalleNumero() != null ? " | Salle : " + sig.getSalleNumero() : "")
                            + " | Par : " + currentUser.getNomComplet());
                    notifDAO.save(n);
                });
        showFeedback("✅ Signalement #" + sig.getId() + " transmis à l'administration.", true);
        clearSignalementForm(); loadData();
    }

    @FXML private void handleVoirMonSignalement() {
        if (mesSignalementsTable == null) return;
        Signalement sel = mesSignalementsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Erreur","Sélectionnez un signalement."); return; }
        String couleurStatut;
        switch (sel.getStatut()) {
            case "RESOLU":   couleurStatut = "#10b981"; break;
            case "EN_COURS": couleurStatut = "#f59e0b"; break;
            case "FERME":    couleurStatut = "#6366f1"; break;
            default:         couleurStatut = "#94a3b8"; break;
        }
        String[][] lignes = {
                {"📅 Date",      sel.getDateSignalement() != null ? sel.getDateSignalement().toLocalDate().toString() : "—"},
                {"🏫 Salle",     sel.getSalleNumero()    != null ? sel.getSalleNumero()    : "—"},
                {"🔖 Catégorie", sel.getCategorie()},
                {"⚡ Priorité",  sel.getPrioriteIcon()   + " " + sel.getPriorite()},
                {"📌 Statut",    formatStatut(sel.getStatut())},
        };
        AlertePersonnalisee.afficherDetailSignalement(sel.getId(),
                sel.getCategorieIcon() + " " + sel.getTitre(),
                lignes, sel.getDescription(), sel.getCommentaireAdmin(),
                sel.getDateResolution() != null ? sel.getDateResolution().toLocalDate().toString() : null,
                couleurStatut);
    }

    private void showFeedback(String msg, boolean ok) {
        if (sigFeedbackLabel == null) { showInfo("Signalement", msg); return; }
        sigFeedbackLabel.setText(msg);
        sigFeedbackLabel.setStyle(ok ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;" : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        sigFeedbackLabel.setVisible(true);
    }

    private void clearSignalementForm() {
        if (sigTitreField     != null) sigTitreField.clear();
        if (sigDescArea       != null) sigDescArea.clear();
        if (sigCategorieCombo != null) sigCategorieCombo.setValue("EQUIPEMENT");
        if (sigPrioriteCombo  != null) sigPrioriteCombo.setValue("NORMALE");
        if (sigSalleCombo     != null) sigSalleCombo.setValue(null);
    }

    private String formatStatut(String statut) {
        if (statut == null) return "—";
        switch (statut) {
            case "EN_ATTENTE": return "⏳ En attente";
            case "EN_COURS":   return "🔄 En cours";
            case "RESOLU":     return "✅ Résolu";
            case "FERME":      return "🔒 Fermé";
            default:           return statut;
        }
    }

    // ═══════════════════════════ EXPORTS ══════════════════════════
    @FXML private void handleExportPDF() {
        FileChooser fc = new FileChooser(); fc.setTitle("Exporter mon emploi du temps en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf")); fc.setInitialFileName("mon_edt.pdf");
        File f = fc.showSaveDialog(null); if (f == null) return;
        try { exportSvc.exportCoursAsPDF(coursList, f); showInfo("Export PDF","Exporté : " + f.getName()); }
        catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    @FXML private void handleExportExcel() {
        FileChooser fc = new FileChooser(); fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel","*.xlsx")); fc.setInitialFileName("mon_edt.xlsx");
        File f = fc.showSaveDialog(null); if (f == null) return;
        try { exportSvc.exportCoursAsExcel(coursList, f); showInfo("Export Excel","Exporté : " + f.getName()); }
        catch (Exception e) { showError("Erreur", e.getMessage()); }
    }

    @FXML private void handleLogout()  { logout(); }
    @FXML private void handleRefresh() { loadData(); }
}