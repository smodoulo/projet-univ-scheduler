package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.model.Servicerappel;
import com.univscheduler.model.AlertePersonnalisee;
import com.univscheduler.model.Examen;
import com.univscheduler.model.DemandeDisponibilite;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import com.univscheduler.service.EmailService;
import com.univscheduler.service.ExamenService;
import com.univscheduler.service.DisponibiliteService;
import com.univscheduler.dao.EtudiantDAO;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import com.univscheduler.service.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EnseignantDashboardController extends BaseController {

    // ─── EDT ───────────────────────────────────────────────────────
    @FXML private Label  welcomeLabel, totalCoursLabel, totalReservsLabel;
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colCls, colCren, colSalle, colDate, colStatut;
    @FXML private TableColumn<Cours, String> colHeureDebut, colHeureFin;
    @FXML private VBox chartEnsContainer;

    // ─── CHIPS FILTRE ──────────────────────────────────────────────
    @FXML private FlowPane chipsContainer;
    @FXML private Label    filtreResultLabel;
    private String         classeSelectionnee = "TOUTES";
    private final ObservableList<Cours> coursListFiltered = FXCollections.observableArrayList();

    // ─── RECHERCHE SALLE ───────────────────────────────────────────
    @FXML private TableView<Salle> salleLibreTable;
    @FXML private TableColumn<Salle, String>  colSalleNum, colSalleType, colSalleBat, colSalleEquip;
    @FXML private TableColumn<Salle, Integer> colSalleCap;
    @FXML private DatePicker       datePicker;
    @FXML private ComboBox<String> heureDebutCombo, heureFinCombo, typeFilter;
    @FXML private Spinner<Integer> capaciteSpinner;
    @FXML private CheckBox         checkProjecteur, checkTableau, checkClim;
    @FXML private Label            resultLabel;

    // ─── RÉSERVATION ───────────────────────────────────────────────
    @FXML private TableView<Reservation> reservTable;
    @FXML private TableColumn<Reservation, String> colResMotif, colResSalle, colResDate, colResStatut;
    @FXML private ComboBox<Salle>  salleReservCombo;
    @FXML private DatePicker       dateReservPicker;
    @FXML private ComboBox<String> heureReservCombo, heureFinReservCombo;
    @FXML private TextField        motifField;
    @FXML private Label            conflitReservLabel;

    // ─── NOTIFICATIONS ─────────────────────────────────────────────
    @FXML private TableView<Notification> notifTable;
    @FXML private TableColumn<Notification, String> colNotifMsg, colNotifType, colNotifDate;
    @FXML private Button notifBadgeBtn;

    // ─── SIGNALEMENT ───────────────────────────────────────────────
    @FXML private TextField        sigTitreField;
    @FXML private TextArea         sigDescArea;
    @FXML private ComboBox<String> sigCategorieCombo, sigPrioriteCombo;
    @FXML private ComboBox<Salle>  sigSalleCombo;
    @FXML private Label            sigFeedbackLabel;
    @FXML private TableView<Signalement>           mesSignalementsTable;
    @FXML private TableColumn<Signalement, String> colSigTitre, colSigCat,
            colSigPrio, colSigStatut, colSigDate, colSigSalle;
    @FXML private Label sigBadge;

    // ─── EXAMENS & DEVOIRS ─────────────────────────────────────────
    @FXML private ComboBox<String>       examenTypeCombo;
    @FXML private TextField              examenTitreField;
    @FXML private ComboBox<ClassePedago> examenClasseCombo;
    @FXML private ComboBox<Matiere>      examenMatiereCombo;
    @FXML private DatePicker             examenDatePicker;
    @FXML private ComboBox<String>       examenHeureCombo;
    @FXML private Spinner<Integer>       examenDureeSpinner;
    @FXML private ComboBox<Salle>        examenSalleCombo;
    @FXML private TextArea               examenDescArea;
    @FXML private Label                  examenFeedbackLabel, examenConflitLabel;
    @FXML private TableView<Examen>      examenTable;
    @FXML private TableColumn<Examen, String> colExType, colExTitre, colExClasse,
            colExMatiere, colExDate, colExSalle, colExStatut, colExComm;

    // ─── DISPONIBILITÉS ────────────────────────────────────────────
    @FXML private ComboBox<Cours>   dispoCoursCombo;
    @FXML private ComboBox<Creneau> dispoCreneauCombo;
    @FXML private TextArea          dispoCommentaireArea;
    @FXML private Label             dispoFeedbackLabel, dispoBadgeLabel;
    @FXML private TableView<DemandeDisponibilite>           dispoTable;
    @FXML private TableColumn<DemandeDisponibilite, String> colDispoMat, colDispoClasse,
            colDispoCren, colDispoComm, colDispoStatut, colDispoDate;

    // ─── DAOs / Services ───────────────────────────────────────────
    private final CoursDAO            coursDAO      = new CoursDAO();
    private final ReservationDAO      reservDAO     = new ReservationDAO();
    private final SalleDAO            salleDAO      = new SalleDAO();
    private final NotificationDAO     notifDAO      = new NotificationDAO();
    private final SignalementDAO      sigDAO        = new SignalementDAO();
    private final CreneauDAO          creneauDAO    = new CreneauDAO();
    private final ExportService       exportSvc     = new ExportService();
    private final ClassePedagoDAO     classeDAO2    = new ClassePedagoDAO();
    private final MatiereDAO          matiereDAO2   = new MatiereDAO();
    private final ExamenService       examenService = new ExamenService();
    private final DisponibiliteService dispoService  = new DisponibiliteService();

    private final ObservableList<Cours>                   coursList  = FXCollections.observableArrayList();
    private final ObservableList<Reservation>             reservList = FXCollections.observableArrayList();
    private final ObservableList<Notification>            notifList  = FXCollections.observableArrayList();
    private final ObservableList<Salle>                   salleList  = FXCollections.observableArrayList();
    private final ObservableList<Signalement>             sigList    = FXCollections.observableArrayList();
    private final ObservableList<DemandeDisponibilite>    dispoList  = FXCollections.observableArrayList();

    private Reservation selectedReserv = null;
    private ScheduledExecutorService rappelStatutScheduler;
    private final Set<Integer> coursDejaNotifies = new HashSet<>();

    private static final List<String> HEURES = List.of(
            "08:00","09:00","10:00","11:00","12:00","13:00",
            "14:00","15:00","16:00","17:00","18:00","19:00","20:00");
    private static final List<String> HEURES_EXAM = List.of(
            "07:00","08:00","09:00","10:00","11:00","12:00",
            "13:00","14:00","15:00","16:00","17:00","18:00");

    private static final Map<String, String> COULEURS_STATUT = Map.of(
            "PLANIFIE","#2a9cb0","REALISE","#1a5f6e","ANNULE","#e05c5c",
            "EN_COURS","#4ecdc4","TERMINE","#9eb3bf");
    private static final Map<String, String> LABELS_STATUT = Map.of(
            "PLANIFIE","Planifié","REALISE","Réalisé","ANNULE","Annulé",
            "EN_COURS","En cours","TERMINE","Terminé");

    private static boolean isGmail(Utilisateur u) {
        return u != null && u.getEmail() != null
                && u.getEmail().toLowerCase().endsWith("@gmail.com");
    }

    // ════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        classeSelectionnee = "TOUTES";
        setupCoursTable(); setupReservTable(); setupSalleTable();
        setupNotifTable(); setupSearchControls();
        setupSignalementForm(); setupMesSignalementsTable();
        loadData(); buildChart(); handleRechercherSalles();
        Servicerappel.getInstance().demarrer();
        demarrerRappelStatutCours();
        initExamenTab();
        initDisponibilitesTab();
    }

    // ════════════════════════════════════════════════════════════════
    //  EXAMENS & DEVOIRS
    // ════════════════════════════════════════════════════════════════
    private void initExamenTab() {
        if (examenTable == null) return;
        if (colExType    != null) colExType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTypeIcon() + " " + d.getValue().getType()));
        if (colExTitre   != null) colExTitre.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitre()));
        if (colExClasse  != null) colExClasse.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getClasseNom()));
        if (colExMatiere != null) colExMatiere.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getMatiereNom()));
        if (colExDate    != null) colExDate.setCellValueFactory(d ->
                new SimpleStringProperty(formatDateExam(d.getValue().getDateExamen())));
        if (colExSalle   != null) colExSalle.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getSalleNumero(), "Maison")));
        if (colExStatut  != null) colExStatut.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatutAffichage()));
        if (colExComm    != null) colExComm.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getCommentaireGestionnaire(), "")));

        examenTable.setRowFactory(tv -> new TableRow<Examen>() {
            @Override protected void updateItem(Examen item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (item.getStatut()) {
                    case Examen.STATUT_VALIDE: setStyle("-fx-background-color:#dcfce7;"); break;
                    case Examen.STATUT_REFUSE: setStyle("-fx-background-color:#fee2e2;"); break;
                    case Examen.STATUT_ANNULE: setStyle("-fx-background-color:#f1f5f9;"); break;
                    default:                   setStyle("-fx-background-color:#eff6ff;"); break;
                }
            }
        });

        if (examenTypeCombo    != null) examenTypeCombo.setItems(FXCollections.observableArrayList("EXAMEN","DEVOIR","CONTROLE"));
        if (examenClasseCombo  != null) examenClasseCombo.setItems(FXCollections.observableArrayList(classeDAO2.findAll()));
        if (examenMatiereCombo != null) examenMatiereCombo.setItems(FXCollections.observableArrayList(matiereDAO2.findAll()));
        if (examenHeureCombo   != null) { examenHeureCombo.setItems(FXCollections.observableArrayList(HEURES_EXAM)); examenHeureCombo.setValue("09:00"); }
        if (examenDureeSpinner != null) examenDureeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15,360,120,15));
        if (examenSalleCombo   != null) examenSalleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        if (examenFeedbackLabel != null) examenFeedbackLabel.setVisible(false);
        if (examenConflitLabel  != null) examenConflitLabel.setVisible(false);
        if (examenDatePicker    != null) examenDatePicker.setValue(LocalDate.now().plusDays(7));
        handleRefreshExamens();
    }

    @FXML public void handleRefreshExamens() {
        if (currentUser == null || examenTable == null) return;
        examenTable.setItems(FXCollections.observableArrayList(
                examenService.getMesExamens(currentUser.getId())));
    }

    @FXML private void handleSoumettreExamen() {
        String       type   = examenTypeCombo    != null ? examenTypeCombo.getValue()       : null;
        String       titre  = examenTitreField   != null ? examenTitreField.getText().trim() : "";
        ClassePedago classe = examenClasseCombo  != null ? examenClasseCombo.getValue()      : null;
        Matiere      mat    = examenMatiereCombo != null ? examenMatiereCombo.getValue()      : null;
        LocalDate    date   = examenDatePicker   != null ? examenDatePicker.getValue()       : null;
        String       heure  = examenHeureCombo   != null ? examenHeureCombo.getValue()       : null;

        List<String> manquants = new ArrayList<>();
        if (type  == null || type.isBlank())   manquants.add("📋  Type");
        if (titre.isEmpty())                   manquants.add("📌  Titre");
        if (classe == null)                    manquants.add("🎓  Classe");
        if (date   == null)                    manquants.add("📅  Date");
        if (heure  == null || heure.isBlank()) manquants.add("⏰  Heure");
        if (!manquants.isEmpty()) { AlertePersonnalisee.examenChampsManquants(manquants); return; }
        if (date.isBefore(LocalDate.now())) {
            AlertePersonnalisee.examenDatePassee(
                    date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear()); return;
        }

        Salle  salle = examenSalleCombo   != null ? examenSalleCombo.getValue()   : null;
        String desc  = examenDescArea     != null ? examenDescArea.getText().trim(): "";
        int    duree = examenDureeSpinner != null ? examenDureeSpinner.getValue()  : 120;

        Examen ex = new Examen();
        ex.setType(type); ex.setTitre(titre);
        ex.setEnseignantId(currentUser.getId());
        ex.setEnseignantNom(currentUser.getPrenom() + " " + currentUser.getNom());
        ex.setClasseId(classe.getId()); ex.setClasseNom(classe.getNom());
        if (mat   != null) { ex.setMatiereId(mat.getId()); ex.setMatiereNom(mat.getNom()); }
        if (salle != null) { ex.setSalleId(salle.getId()); ex.setSalleNumero(salle.getNumero()); }
        ex.setDateExamen(date + "T" + heure + ":00");
        ex.setDureeMinutes(duree); ex.setDescription(desc);
        examenService.soumettre(ex);

        final ClassePedago classeFinal  = classe;
        final String       titreFinal   = titre;
        final String       heureFinal   = heure;
        final Salle        salleFinal   = salle;
        final int          dureeFinal   = duree;
        final String       matNomFinal  = mat != null ? mat.getNom() : "Non précisée";
        final String       dateFmt = String.format("%02d/%02d/%04d",
                date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        final String typeIcon = switch (type) {
            case "EXAMEN" -> "📋"; case "DEVOIR" -> "📝"; default -> "✍️";
        };

        new UtilisateurDAO().findAll().stream()
                .filter(u -> "GESTIONNAIRE".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                .forEach(u -> {
                    Notification n = new Notification();
                    n.setUtilisateurId(u.getId()); n.setType("INFO");
                    n.setMessage(typeIcon + " Nouvelle demande : " + titreFinal
                            + " (" + classeFinal.getNom() + ")"
                            + " par " + currentUser.getNomComplet()
                            + " | " + dateFmt + " à " + heureFinal
                            + (salleFinal != null ? " | Salle : " + salleFinal.getNumero() : ""));
                    n.setDateEnvoi(LocalDateTime.now());
                    notifDAO.save(n);
                });

        new Thread(() -> {
            for (Utilisateur etu : new EtudiantDAO().findByClasseId(classeFinal.getId())) {
                Notification nEtu = new Notification();
                nEtu.setUtilisateurId(etu.getId()); nEtu.setType("INFO");
                nEtu.setMessage(typeIcon + " " + titreFinal + " le " + dateFmt + " à " + heureFinal
                        + " | " + matNomFinal + " | " + dureeFinal + " min"
                        + (salleFinal != null ? " | Salle " + salleFinal.getNumero() : " | À la maison")
                        + " | " + currentUser.getNomComplet());
                nEtu.setDateEnvoi(LocalDateTime.now()); notifDAO.save(nEtu);
                if (isGmail(etu))
                    EmailService.sendNotification(etu,
                            typeIcon + " " + titreFinal + " — " + classeFinal.getNom(),
                            "Bonjour " + etu.getNomComplet() + ",\n\n"
                                    + "Un " + type.toLowerCase() + " a été programmé :\n"
                                    + "  Titre : " + titreFinal + "\n"
                                    + "  Date  : " + dateFmt + " à " + heureFinal + "\n"
                                    + "  Durée : " + dureeFinal + " min\n"
                                    + (salleFinal != null ? "  Salle : " + salleFinal.getNumero() + "\n" : "  Lieu : Maison\n")
                                    + "\nCordialement,\nUNIV-SCHEDULER");
            }
        }, "email-examen").start();

        AlertePersonnalisee.examenSoumisAvecSucces(type, titre, classe.getNom(),
                matNomFinal, dateFmt, heure, salle != null ? salle.getNumero() : null, duree);
        handleClearExamen(); handleRefreshExamens();
    }

    @FXML private void handleClearExamen() {
        if (examenTypeCombo    != null) examenTypeCombo.setValue(null);
        if (examenTitreField   != null) examenTitreField.clear();
        if (examenClasseCombo  != null) examenClasseCombo.setValue(null);
        if (examenMatiereCombo != null) examenMatiereCombo.setValue(null);
        if (examenDatePicker   != null) examenDatePicker.setValue(LocalDate.now().plusDays(7));
        if (examenHeureCombo   != null) examenHeureCombo.setValue("09:00");
        if (examenDureeSpinner != null) examenDureeSpinner.getValueFactory().setValue(120);
        if (examenSalleCombo   != null) examenSalleCombo.setValue(null);
        if (examenDescArea     != null) examenDescArea.clear();
        if (examenFeedbackLabel != null) examenFeedbackLabel.setVisible(false);
        if (examenConflitLabel  != null) examenConflitLabel.setVisible(false);
    }

    private String formatDateExam(String d) {
        if (d == null || d.isEmpty()) return "—";
        try {
            String[] p = d.split("T"), ymd = p[0].split("-");
            return ymd[2] + "/" + ymd[1] + "/" + ymd[0]
                    + " à " + (p.length > 1 ? p[1].substring(0, 5) : "—");
        } catch (Exception e) { return d; }
    }

    // ════════════════════════════════════════════════════════════════
    //  DISPONIBILITÉS
    // ════════════════════════════════════════════════════════════════
    private void initDisponibilitesTab() {
        if (dispoTable == null) return;

        if (colDispoMat    != null) colDispoMat.setCellValueFactory(d ->
                new SimpleStringProperty(nvlStr(d.getValue().getMatiereNom())));
        if (colDispoClasse != null) colDispoClasse.setCellValueFactory(d ->
                new SimpleStringProperty(nvlStr(d.getValue().getClasseNom())));
        if (colDispoCren   != null) colDispoCren.setCellValueFactory(d ->
                new SimpleStringProperty(nvlStr(d.getValue().getCreneauInfo())));
        if (colDispoComm   != null) colDispoComm.setCellValueFactory(d ->
                new SimpleStringProperty(nvlStr(d.getValue().getCommentaire())));
        if (colDispoStatut != null) colDispoStatut.setCellValueFactory(d ->
                new SimpleStringProperty(formatStatutDispo(d.getValue().getStatut())));
        if (colDispoDate   != null) colDispoDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDateDemande() != null
                        ? d.getValue().getDateDemande().toLocalDate().toString() : ""));

        dispoTable.setRowFactory(tv -> new TableRow<DemandeDisponibilite>() {
            @Override protected void updateItem(DemandeDisponibilite item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (item.getStatut()) {
                    case "ACCEPTE":    setStyle("-fx-background-color:#dcfce7;"); break;
                    case "REFUSE":     setStyle("-fx-background-color:#fee2e2;"); break;
                    case "CONFLIT":    setStyle("-fx-background-color:#fef9c3;"); break;
                    case "EN_ATTENTE": setStyle("-fx-background-color:#eff6ff;"); break;
                    default:           setStyle("");
                }
            }
        });

        // Charger les cours planifiés de l'enseignant
        if (dispoCoursCombo != null) {
            List<Cours> mesCours = coursDAO.findByEnseignant(currentUser.getId());
            dispoCoursCombo.setItems(FXCollections.observableArrayList(mesCours));
            dispoCoursCombo.setOnAction(e -> chargerCreneauxDispo());
        }

        if (dispoFeedbackLabel != null) dispoFeedbackLabel.setVisible(false);
        handleRefreshDisponibilites();
    }

    private void chargerCreneauxDispo() {
        if (dispoCreneauCombo == null) return;
        dispoCreneauCombo.setItems(FXCollections.observableArrayList(creneauDAO.findAll()));
    }

    @FXML public void handleRefreshDisponibilites() {
        if (currentUser == null || dispoTable == null) return;
        List<DemandeDisponibilite> demandes =
                dispoService.getMesDemandesEnseignant(currentUser.getId());
        dispoList.setAll(demandes);
        dispoTable.setItems(dispoList);

        long enAttente = demandes.stream()
                .filter(d -> "EN_ATTENTE".equals(d.getStatut())).count();
        if (dispoBadgeLabel != null) {
            dispoBadgeLabel.setText(enAttente > 0 ? "⏳ " + enAttente + " en attente" : "");
            dispoBadgeLabel.setVisible(enAttente > 0);
        }
    }

    @FXML private void handleSoumettreDisponibilite() {
        Cours   cours   = dispoCoursCombo    != null ? dispoCoursCombo.getValue()    : null;
        Creneau creneau = dispoCreneauCombo  != null ? dispoCreneauCombo.getValue()  : null;
        String  comment = dispoCommentaireArea != null ? dispoCommentaireArea.getText().trim() : "";

        if (cours == null || creneau == null) {
            showDispoFeedback("⚠️ Veuillez sélectionner un cours et un créneau.", false);
            return;
        }

        DemandeDisponibilite demande = new DemandeDisponibilite();
        demande.setEnseignantId(currentUser.getId());
        demande.setCoursId(cours.getId());
        demande.setMatiereNom(cours.getMatiereNom());
        demande.setClasseNom(cours.getClasseNom());
        demande.setCreneauPropose(creneau.getId());
        demande.setCreneauInfo(creneau.toString());
        demande.setCommentaire(comment);
        demande.setStatut(StatutDisponibilite.EN_ATTENTE);
        demande.setDateDemande(LocalDateTime.now());

        // Le service gère la persistance + notifications gestionnaires
        dispoService.soumettre(demande);

        showDispoFeedback("✅ Demande soumise ! Le gestionnaire a été notifié.", true);
        handleClearDisponibilite();
        handleRefreshDisponibilites();
    }

    @FXML private void handleClearDisponibilite() {
        if (dispoCoursCombo      != null) dispoCoursCombo.setValue(null);
        if (dispoCreneauCombo    != null) dispoCreneauCombo.setValue(null);
        if (dispoCommentaireArea != null) dispoCommentaireArea.clear();
        if (dispoFeedbackLabel   != null) dispoFeedbackLabel.setVisible(false);
    }

    private void showDispoFeedback(String msg, boolean ok) {
        if (dispoFeedbackLabel == null) { showInfo("Disponibilité", msg); return; }
        dispoFeedbackLabel.setText(msg);
        dispoFeedbackLabel.setStyle(ok
                ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;"
                : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        dispoFeedbackLabel.setVisible(true);
    }

    private String formatStatutDispo(String statut) {
        if (statut == null) return "—";
        switch (statut) {
            case "EN_ATTENTE": return "⏳ En attente";
            case "ACCEPTE":    return "✅ Accepté";
            case "REFUSE":     return "❌ Refusé";
            case "CONFLIT":    return "⚠️ Conflit";
            default:           return statut;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  RAPPEL STATUT COURS
    // ════════════════════════════════════════════════════════════════
    private void demarrerRappelStatutCours() {
        if (rappelStatutScheduler != null && !rappelStatutScheduler.isShutdown()) return;
        rappelStatutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RappelStatutCours"); t.setDaemon(true); return t;
        });
        coursDejaNotifies.clear();
        rappelStatutScheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDate today = LocalDate.now();
                for (Cours c : coursDAO.findByEnseignant(currentUser.getId())) {
                    if (!"PLANIFIE".equals(c.getStatut()) || c.getDate() == null) continue;
                    if (c.getDate().isBefore(today) && !coursDejaNotifies.contains(c.getId())) {
                        coursDejaNotifies.add(c.getId()); envoyerRappelStatut(c);
                    }
                }
                Platform.runLater(this::rafraichirBadgeNotif);
            } catch (Exception e) { System.err.println("[RappelStatut] " + e.getMessage()); }
        }, 0, 1, TimeUnit.HOURS);
    }

    private void envoyerRappelStatut(Cours c) {
        Notification n = new Notification();
        n.setUtilisateurId(currentUser.getId()); n.setType("ALERTE");
        n.setMessage("⏰ Cours « " + c.getMatiereNom() + " » (" + c.getClasseNom()
                + ") du " + c.getDate() + " est toujours PLANIFIÉ. Merci de mettre à jour.");
        n.setDateEnvoi(LocalDateTime.now()); notifDAO.save(n);
        if (isGmail(currentUser))
            EmailService.sendNotification(currentUser,
                    "⏰ Statut à mettre à jour — " + c.getMatiereNom(),
                    "Bonjour " + currentUser.getNomComplet() + ",\n\n"
                            + "Le cours « " + c.getMatiereNom() + " » du " + c.getDate()
                            + " est toujours PLANIFIÉ.\nMerci de le marquer RÉALISÉ ou ANNULÉ.\n\nUNIV-SCHEDULER");
    }

    public void arreterRappelStatutCours() {
        if (rappelStatutScheduler != null && !rappelStatutScheduler.isShutdown())
            rappelStatutScheduler.shutdown();
    }

    // ════════════════════════════════════════════════════════════════
    //  NOTIFICATIONS
    // ════════════════════════════════════════════════════════════════
    @FXML private void handleOpenNotifications() {
        AlertePersonnalisee.afficherNotifications(notifDAO.findByUtilisateur(currentUser.getId()));
        notifDAO.markAllRead(currentUser.getId());
        loadData();
    }

    private void rafraichirBadgeNotif() {
        int unread = notifDAO.countUnread(currentUser.getId());
        if (notifBadgeBtn == null) return;
        if (unread > 0) {
            notifBadgeBtn.setText("🔔 " + unread + " non lue(s)");
            notifBadgeBtn.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                    + "-fx-cursor:hand;-fx-padding:4 12;-fx-background-radius:20;"
                    + "-fx-border-color:#fca5a5;-fx-border-width:1.5;-fx-border-radius:20;-fx-font-size:12px;");
        } else {
            notifBadgeBtn.setText("🔔 Aucune nouvelle");
            notifBadgeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-weight:bold;"
                    + "-fx-cursor:hand;-fx-padding:4 12;-fx-background-radius:20;"
                    + "-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:20;-fx-font-size:12px;");
        }
    }

    // ── Helpers créneau ─────────────────────────────────────────────
    private String extraireJour(String info) {
        if (info == null) return "—"; return info.split(" ")[0];
    }
    private String extraireHeureDebut(String info) {
        if (info == null) return "—";
        try { for (String p : info.split(" "))
            if (p.endsWith("h") && !p.startsWith("("))
                return String.format("%02d:00", Integer.parseInt(p.replace("h",""))); }
        catch (Exception ignored) {} return "—";
    }
    private String extraireHeureFin(String info) {
        if (info == null) return "—";
        try { int d=-1,dur=2; for (String p : info.split(" ")) {
            if (p.endsWith("h")&&!p.startsWith("(")&&d==-1) d=Integer.parseInt(p.replace("h",""));
            if (p.startsWith("(")&&p.endsWith("h)")) dur=Integer.parseInt(p.replace("(","").replace("h)",""));
        } if (d>=0) return String.format("%02d:00",d+dur); }
        catch (Exception ignored) {} return "—";
    }

    // ════════════════════════════════════════════════════════════════
    //  TABLE EDT + CHANGEMENT STATUT
    // ════════════════════════════════════════════════════════════════
    private void setupCoursTable() {
        colMat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colCls.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClasseNom()));
        colCren.setCellValueFactory(d -> new SimpleStringProperty(extraireJour(d.getValue().getCreneauInfo())));
        if (colHeureDebut != null) colHeureDebut.setCellValueFactory(d ->
                new SimpleStringProperty(extraireHeureDebut(d.getValue().getCreneauInfo())));
        if (colHeureFin   != null) colHeureFin.setCellValueFactory(d ->
                new SimpleStringProperty(extraireHeureFin(d.getValue().getCreneauInfo())));
        colSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        colStatut.setCellFactory(col -> new TableCell<Cours, String>() {
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setText(null); setStyle(""); setGraphic(null); return; }
                String af, fond, texte;
                switch (statut) {
                    case "PLANIFIE": af="📅 Planifié"; fond="#dbeafe"; texte="#1e40af"; break;
                    case "REALISE":  af="✅ Réalisé";  fond="#dcfce7"; texte="#166534"; break;
                    case "ANNULE":   af="❌ Annulé";   fond="#fee2e2"; texte="#991b1b"; break;
                    case "EN_COURS": af="🔄 En cours"; fond="#fef9c3"; texte="#854d0e"; break;
                    case "TERMINE":  af="🏁 Terminé";  fond="#f3f4f6"; texte="#374151"; break;
                    default:         af=statut;         fond="#f1f5f9"; texte="#475569";
                }
                boolean final_ = "REALISE".equals(statut) || "ANNULE".equals(statut);
                Label badge = new Label(af + (final_ ? "" : "  ▾"));
                badge.setStyle("-fx-background-color:"+fond+";-fx-text-fill:"+texte+";"
                        +"-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8;"
                        +"-fx-background-radius:12;"+(final_?"":"-fx-cursor:hand;"));
                if (!final_) {
                    ContextMenu menu = new ContextMenu();
                    CustomMenuItem hdr = new CustomMenuItem(new Label("  Changer le statut :"), false);
                    hdr.setHideOnClick(false);
                    MenuItem mR = new MenuItem("✅  Marquer comme Réalisé");
                    MenuItem mA = new MenuItem("❌  Marquer comme Annulé");
                    MenuItem mP = new MenuItem("📅  Remettre en Planifié");
                    mR.setOnAction(e -> changerStatutCours(getTableView().getItems().get(getIndex()), "REALISE"));
                    mA.setOnAction(e -> changerStatutCours(getTableView().getItems().get(getIndex()), "ANNULE"));
                    mP.setOnAction(e -> changerStatutCours(getTableView().getItems().get(getIndex()), "PLANIFIE"));
                    if ("PLANIFIE".equals(statut)) mP.setDisable(true);
                    menu.getItems().addAll(hdr, new SeparatorMenuItem(), mR, mA, new SeparatorMenuItem(), mP);
                    badge.setOnMouseClicked(e -> menu.show(badge, e.getScreenX(), e.getScreenY()));
                }
                setGraphic(badge); setText(null);
            }
        });
        coursTable.setRowFactory(tv -> new TableRow<Cours>() {
            @Override protected void updateItem(Cours c, boolean empty) {
                super.updateItem(c, empty);
                if (c == null || empty) { setStyle(""); return; }
                switch (c.getStatut() != null ? c.getStatut() : "") {
                    case "REALISE":  setStyle("-fx-background-color:#f0fdf4;"); break;
                    case "ANNULE":   setStyle("-fx-background-color:#fff1f2;"); break;
                    case "EN_COURS": setStyle("-fx-background-color:#fefce8;"); break;
                    default:         setStyle("");
                }
            }
        });
        coursListFiltered.setAll(coursList);
        coursTable.setItems(coursListFiltered);
    }

    private void changerStatutCours(Cours cours, String nouveauStatut) {
        if (cours == null) return;
        String ancien = cours.getStatut() != null ? cours.getStatut() : "PLANIFIE";
        if ("REALISE".equals(ancien) || "ANNULE".equals(ancien)) {
            showError("Action impossible", "Ce cours est déjà « " + ancien + " »."); return;
        }
        if (nouveauStatut.equals(ancien)) return;

        String motif = null;
        if ("ANNULE".equals(nouveauStatut)) {
            motif = AlertePersonnalisee.demanderMotifAnnulation(
                    cours.getMatiereNom() + " — " + cours.getClasseNom(),
                    extraireJour(cours.getCreneauInfo()),
                    extraireHeureDebut(cours.getCreneauInfo()),
                    extraireHeureFin(cours.getCreneauInfo()));
            if (motif == null) return;
        }
        cours.setStatut(nouveauStatut);
        coursDAO.updateStatut(cours.getId(), nouveauStatut);
        if ("REALISE".equals(nouveauStatut) || "ANNULE".equals(nouveauStatut))
            coursDejaNotifies.add(cours.getId());

        final String icone = "REALISE".equals(nouveauStatut) ? "✅" : "❌";
        final String motifFinal = motif;
        final String classeNom = cours.getClasseNom();
        final String matiereNom = cours.getMatiereNom();
        final String salleNum = cours.getSalleNumero();
        final int classeId = cours.getClasseId();

        String msg = icone + " " + matiereNom + " (" + classeNom + ")"
                + " | " + ancien + " → " + nouveauStatut
                + (motifFinal != null ? " | Motif : " + motifFinal : "");

        new UtilisateurDAO().findAll().stream()
                .filter(u -> "GESTIONNAIRE".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                .forEach(u -> {
                    Notification n = new Notification();
                    n.setUtilisateurId(u.getId());
                    n.setType("ANNULE".equals(nouveauStatut) ? "ALERTE" : "INFO");
                    n.setMessage(msg); n.setDateEnvoi(LocalDateTime.now());
                    notifDAO.save(n);
                });

        if ("ANNULE".equals(nouveauStatut)) {
            new Thread(() -> {
                for (Utilisateur etu : new EtudiantDAO().findByClasseId(classeId)) {
                    Notification nEtu = new Notification();
                    nEtu.setUtilisateurId(etu.getId()); nEtu.setType("ALERTE");
                    nEtu.setMessage("❌ Cours annulé : " + matiereNom
                            + (motifFinal != null ? " | Motif : " + motifFinal : ""));
                    nEtu.setDateEnvoi(LocalDateTime.now()); notifDAO.save(nEtu);
                    if (isGmail(etu))
                        EmailService.sendNotification(etu,
                                "❌ Cours annulé — " + matiereNom,
                                "Bonjour " + etu.getNomComplet() + ",\nLe cours "
                                        + matiereNom + " (" + classeNom + ") a été annulé."
                                        + (motifFinal != null ? "\nMotif : " + motifFinal : "")
                                        + "\n\nCordialement,\nUNIV-SCHEDULER");
                }
            }, "email-annul").start();
        }

        loadData(); buildChart();
        showInfo("Statut mis à jour", icone + "  « " + matiereNom + " » → " + nouveauStatut);
    }

    // ════════════════════════════════════════════════════════════════
    //  DONNÉES
    // ════════════════════════════════════════════════════════════════
    private void loadData() {
        coursList.setAll(coursDAO.findByEnseignant(currentUser.getId()));
        reservList.setAll(reservDAO.findByUtilisateur(currentUser.getId()));
        notifList.setAll(notifDAO.findByUtilisateur(currentUser.getId()));
        sigList.setAll(sigDAO.findByEnseignant(currentUser.getId()));
        if (totalCoursLabel   != null) totalCoursLabel.setText("Mes cours : " + coursList.size());
        if (totalReservsLabel != null) totalReservsLabel.setText("Mes réservations : " + reservList.size());
        rafraichirBadgeNotif();
        long resolus = sigList.stream().filter(s -> "RESOLU".equals(s.getStatut())).count();
        if (sigBadge != null) { sigBadge.setText(resolus>0?"✅ "+resolus+" résolu(s)":""); sigBadge.setVisible(resolus>0); }
        buildChipsClasse();
    }

    private void buildChipsClasse() {
        if (chipsContainer == null) return;
        chipsContainer.getChildren().clear();
        List<String> classes = new ArrayList<>(); classes.add("TOUTES");
        coursList.stream().map(Cours::getClasseNom).filter(Objects::nonNull)
                .distinct().sorted().forEach(classes::add);
        if (!classes.contains(classeSelectionnee)) classeSelectionnee = "TOUTES";
        for (String classe : classes) {
            Button chip = new Button("TOUTES".equals(classe) ? "📚 Toutes" : classe);
            chip.setUserData(classe); chip.setStyle(chipStyle(classe.equals(classeSelectionnee)));
            chip.setOnAction(e -> {
                classeSelectionnee = classe;
                chipsContainer.getChildren().forEach(node -> {
                    if (node instanceof Button) {
                        Button b = (Button) node;
                        b.setStyle(chipStyle(b.getUserData().equals(classeSelectionnee)));
                    }
                });
                appliquerFiltreClasse();
            });
            chipsContainer.getChildren().add(chip);
        }
        appliquerFiltreClasse();
    }

    private String chipStyle(boolean actif) {
        return actif
                ? "-fx-background-color:#1d4ed8;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:5 16;-fx-background-radius:20;-fx-cursor:hand;"
                : "-fx-background-color:white;-fx-text-fill:#334155;-fx-font-size:12px;-fx-padding:5 16;-fx-background-radius:20;-fx-cursor:hand;-fx-border-color:#cbd5e1;-fx-border-width:1;-fx-border-radius:20;";
    }

    private void appliquerFiltreClasse() {
        List<Cours> filtered = "TOUTES".equals(classeSelectionnee)
                ? new ArrayList<>(coursList)
                : coursList.stream().filter(c -> classeSelectionnee.equals(c.getClasseNom()))
                .collect(Collectors.toList());
        coursListFiltered.setAll(filtered);
        if (filtreResultLabel != null)
            filtreResultLabel.setText("TOUTES".equals(classeSelectionnee)
                    ? "📋 " + filtered.size() + " cours au total"
                    : "🎓 " + classeSelectionnee + " — " + filtered.size() + " cours sur " + coursList.size());
    }

    // ════════════════════════════════════════════════════════════════
    //  GRAPHIQUE DONUT
    // ════════════════════════════════════════════════════════════════
    private void buildChart() {
        if (chartEnsContainer == null) return;
        chartEnsContainer.getChildren().clear();
        Map<String, Integer> statuts = new LinkedHashMap<>();
        for (Cours c : coursList) statuts.merge(c.getStatut()!=null?c.getStatut():"INCONNU",1,Integer::sum);
        if (statuts.isEmpty()) {
            Label vide = new Label("Aucune donnée");
            vide.setStyle("-fx-text-fill:#9eb3bf;-fx-font-style:italic;");
            chartEnsContainer.getChildren().add(vide); return;
        }
        final int total = statuts.values().stream().mapToInt(Integer::intValue).sum();
        final String dominant = statuts.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("");
        final int cD = statuts.getOrDefault(dominant,0);
        final int pD = total>0?Math.round(cD*100f/total):0;
        final String nD = LABELS_STATUT.getOrDefault(dominant,"—");

        PieChart pie = new PieChart();
        pie.setLabelsVisible(false); pie.setLegendVisible(false); pie.setStartAngle(90);
        pie.setPrefSize(180,180); pie.setMinSize(180,180); pie.setMaxSize(180,180);
        pie.setStyle("-fx-background-color:transparent;");
        List<String> ordre = new ArrayList<>();
        statuts.forEach((s,c) -> { pie.getData().add(new PieChart.Data("",c)); ordre.add(s); });

        Label cPct=new Label(pD+"%"); cPct.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1a5f6e;");
        Label cNom=new Label(nD);     cNom.setStyle("-fx-font-size:10px;-fx-text-fill:#6b8394;");
        Label cTot=new Label(total+" cours"); cTot.setStyle("-fx-font-size:10px;-fx-text-fill:#9eb3bf;");
        VBox center = new VBox(1,cPct,cNom,cTot); center.setAlignment(Pos.CENTER);
        Circle hole = new Circle(60, Color.WHITE);
        StackPane donut = new StackPane(pie,hole,center); donut.setAlignment(Pos.CENTER);
        donut.setPrefSize(186,186); donut.setMaxSize(186,186);

        GridPane legend = new GridPane(); legend.setHgap(10); legend.setVgap(8);
        legend.setPadding(new Insets(10,4,4,4));
        legend.setStyle("-fx-border-color:#e8f4f4;-fx-border-width:1 0 0 0;");
        List<Map.Entry<String,Integer>> entries = new ArrayList<>(statuts.entrySet());
        for (int i=0;i<entries.size();i++) {
            final String s=entries.get(i).getKey(); final int cnt=entries.get(i).getValue();
            final int pct=total>0?Math.round(cnt*100f/total):0;
            final String coul=COULEURS_STATUT.getOrDefault(s,"#94a3b8");
            final String nom=LABELS_STATUT.getOrDefault(s,s);
            Circle dot=new Circle(5,Color.web(coul));
            Label lNom=new Label(nom); lNom.setStyle("-fx-font-size:10px;-fx-text-fill:#6b8394;");
            HBox dn=new HBox(5,dot,lNom); dn.setAlignment(Pos.CENTER_LEFT);
            Label lN=new Label(String.valueOf(cnt)); lN.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a5f6e;");
            Label lP=new Label(pct+"%"); lP.setStyle("-fx-font-size:10px;-fx-text-fill:#9eb3bf;");
            VBox item=new VBox(2,dn,lN,lP); item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-cursor:hand;-fx-padding:4 6;-fx-background-radius:6;");
            item.setOnMouseEntered(e->{item.setStyle("-fx-cursor:hand;-fx-padding:4 6;-fx-background-radius:6;-fx-background-color:#f0f9fa;");cPct.setText(pct+"%");cNom.setText(nom);cTot.setText(cnt+" cours");});
            item.setOnMouseExited(e->{item.setStyle("-fx-cursor:hand;-fx-padding:4 6;-fx-background-radius:6;");cPct.setText(pD+"%");cNom.setText(nD);cTot.setText(total+" cours");});
            legend.add(item,i%2,i/2);
        }
        Platform.runLater(()->{
            List<PieChart.Data> dl=new ArrayList<>(pie.getData());
            for (int i=0;i<dl.size()&&i<ordre.size();i++){
                PieChart.Data data=dl.get(i); if(data.getNode()==null)continue;
                final String s=ordre.get(i); final String c=COULEURS_STATUT.getOrDefault(s,"#94a3b8");
                final String n=LABELS_STATUT.getOrDefault(s,s); final int cnt=statuts.getOrDefault(s,0);
                final int pct=total>0?Math.round(cnt*100f/total):0;
                data.getNode().setStyle("-fx-pie-color:"+c+";");
                data.getNode().setOnMouseEntered(e->{data.getNode().setStyle("-fx-pie-color:"+c+";-fx-scale-x:1.04;-fx-scale-y:1.04;");cPct.setText(pct+"%");cNom.setText(n);cTot.setText(cnt+" cours");});
                data.getNode().setOnMouseExited(e->{data.getNode().setStyle("-fx-pie-color:"+c+";");cPct.setText(pD+"%");cNom.setText(nD);cTot.setText(total+" cours");});
            }
        });
        Label titre=new Label("Statut de mes cours"); titre.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a5f6e;");
        VBox card=new VBox(8,titre,donut,legend); card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(14,12,12,12));
        card.setStyle("-fx-background-color:white;-fx-border-color:#c0dde4;-fx-border-width:1;-fx-background-radius:12;-fx-border-radius:12;");
        card.setPrefWidth(220); card.setMaxWidth(220);
        chartEnsContainer.getChildren().add(card);
    }

    // ════════════════════════════════════════════════════════════════
    //  RECHERCHE SALLE
    // ════════════════════════════════════════════════════════════════
    private void setupSearchControls() {
        if (typeFilter!=null){typeFilter.setItems(FXCollections.observableArrayList("TOUS","TD","TP","AMPHI"));typeFilter.setValue("TOUS");}
        if (capaciteSpinner!=null) capaciteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,500,1));
        if (heureDebutCombo!=null){heureDebutCombo.setItems(FXCollections.observableArrayList(HEURES));heureDebutCombo.setValue("08:00");}
        if (heureFinCombo!=null){heureFinCombo.setItems(FXCollections.observableArrayList(HEURES));heureFinCombo.setValue("10:00");}
        if (heureReservCombo!=null){heureReservCombo.setItems(FXCollections.observableArrayList(HEURES));heureReservCombo.setValue("08:00");}
        if (heureFinReservCombo!=null){heureFinReservCombo.setItems(FXCollections.observableArrayList(HEURES));heureFinReservCombo.setValue("10:00");}
        if (datePicker!=null&&datePicker.getValue()==null) datePicker.setValue(LocalDate.now());
        if (dateReservPicker!=null&&dateReservPicker.getValue()==null) dateReservPicker.setValue(LocalDate.now());
        if (salleReservCombo!=null){salleReservCombo.setOnAction(e->checkReservConflict());salleReservCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));}
        if (dateReservPicker!=null) dateReservPicker.setOnAction(e->checkReservConflict());
        if (heureReservCombo!=null) heureReservCombo.setOnAction(e->checkReservConflict());
        if (heureFinReservCombo!=null) heureFinReservCombo.setOnAction(e->checkReservConflict());
    }

    private void setupSalleTable() {
        if (salleLibreTable==null) return;
        if (colSalleNum!=null)  colSalleNum.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNumero()));
        if (colSalleType!=null) colSalleType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getTypeSalle()));
        if (colSalleCap!=null)  colSalleCap.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        if (colSalleBat!=null)  colSalleBat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getBatimentNom()!=null?d.getValue().getBatimentNom():""));
        if (colSalleEquip!=null)colSalleEquip.setCellValueFactory(d->{List<String>eq=salleDAO.getEquipementsDisponibles(d.getValue().getId());return new SimpleStringProperty(eq.isEmpty()?"—":String.join(", ",eq));});
        salleLibreTable.setItems(salleList);
        salleLibreTable.getSelectionModel().selectedItemProperty().addListener((o,old,s)->{if(s!=null&&salleReservCombo!=null){salleReservCombo.setValue(s);checkReservConflict();}});
    }

    @FXML private void handleRechercherSalles() {
        if (salleLibreTable==null) return;
        int cap=capaciteSpinner!=null?capaciteSpinner.getValue():1;
        String t=typeFilter!=null?typeFilter.getValue():null;
        String type=(t==null||"TOUS".equals(t))?null:t;
        List<String> equips=new ArrayList<>();
        if(checkProjecteur!=null&&checkProjecteur.isSelected())equips.add("PROJECTEUR");
        if(checkTableau!=null&&checkTableau.isSelected())equips.add("TABLEAU");
        if(checkClim!=null&&checkClim.isSelected())equips.add("CLIM");
        LocalDate date=datePicker!=null?datePicker.getValue():LocalDate.now();
        Integer hDebut=null,hFin=null;
        if(heureDebutCombo!=null&&heureDebutCombo.getValue()!=null)hDebut=Integer.parseInt(heureDebutCombo.getValue().split(":")[0]);
        if(heureFinCombo!=null&&heureFinCombo.getValue()!=null)hFin=Integer.parseInt(heureFinCombo.getValue().split(":")[0]);
        if(date==null){date=LocalDate.now();hDebut=LocalTime.now().getHour();hFin=hDebut+1;}
        List<Salle> result=salleDAO.findDisponiblesAvancee(cap,type,date,hDebut,hFin,equips);
        salleList.setAll(result);
        if(resultLabel!=null){resultLabel.setText(result.isEmpty()?"❌ Aucune salle disponible.":"✅ "+result.size()+" salle(s)");resultLabel.setStyle(result.isEmpty()?"-fx-text-fill:#dc2626;-fx-font-weight:bold;":"-fx-text-fill:#16a34a;-fx-font-weight:bold;");}
        if(salleReservCombo!=null)salleReservCombo.setItems(FXCollections.observableArrayList(result.isEmpty()?salleDAO.findAll():result));
    }

    // ════════════════════════════════════════════════════════════════
    //  RÉSERVATION
    // ════════════════════════════════════════════════════════════════
    private void setupReservTable() {
        colResMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));
        colResSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));
        colResDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        colResStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));
        reservTable.setItems(reservList);
        reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r)->selectedReserv=r);
    }

    private void checkReservConflict() {
        if(conflitReservLabel==null)return;
        Salle s=salleReservCombo!=null?salleReservCombo.getValue():null;
        LocalDate d=dateReservPicker!=null?dateReservPicker.getValue():null;
        String hD=heureReservCombo!=null?heureReservCombo.getValue():null;
        String hF=heureFinReservCombo!=null?heureFinReservCombo.getValue():null;
        if(s==null||d==null||hD==null){conflitReservLabel.setVisible(false);return;}
        int debut=Integer.parseInt(hD.split(":")[0]);
        int fin=hF!=null?Integer.parseInt(hF.split(":")[0]):debut+2;
        if(fin<=debut){conflitReservLabel.setText("⚠️ Heure fin doit être après début !");conflitReservLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");conflitReservLabel.setVisible(true);return;}
        boolean libre=salleDAO.isSalleLibre(s.getId(),d,debut,fin);
        conflitReservLabel.setText(libre?"✅ Salle disponible":"⚠️ Salle déjà occupée !");
        conflitReservLabel.setStyle(libre?"-fx-text-fill:#16a34a;-fx-font-weight:bold;":"-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        conflitReservLabel.setVisible(true);
    }

    @FXML private void handleSaveReservation() {
        Salle s=salleReservCombo!=null?salleReservCombo.getValue():null;
        String motif=motifField!=null?motifField.getText().trim():"";
        LocalDate d=dateReservPicker!=null?dateReservPicker.getValue():null;
        String hD=(heureReservCombo!=null&&heureReservCombo.getValue()!=null)?heureReservCombo.getValue():"08:00";
        String hF=(heureFinReservCombo!=null&&heureFinReservCombo.getValue()!=null)?heureFinReservCombo.getValue():null;
        if(s==null||motif.isEmpty()||d==null){showError("Erreur","Veuillez remplir tous les champs.");return;}
        int debut=Integer.parseInt(hD.split(":")[0]);
        int fin=hF!=null?Integer.parseInt(hF.split(":")[0]):debut+2;
        if(fin<=debut){showError("Erreur","L'heure de fin doit être après l'heure de début.");return;}
        if(!salleDAO.isSalleLibre(s.getId(),d,debut,fin)){showError("Conflit","La salle "+s.getNumero()+" est déjà occupée.");return;}
        Reservation r=new Reservation();
        r.setSalleId(s.getId());r.setMotif(motif);r.setDateReservation(d.atTime(debut,0));r.setDateFin(d.atTime(fin,0));
        r.setStatut("EN_ATTENTE");r.setUtilisateurId(currentUser.getId());
        reservDAO.save(r);r.setSalleNumero(s.getNumero());
        final String hDF=hD;final int fF=fin;
        new Thread(()->new UtilisateurDAO().findAll().stream()
                .filter(u->("GESTIONNAIRE".equals(u.getRole())||"ADMIN".equals(u.getRole()))&&isGmail(u))
                .forEach(u->EmailService.sendNotification(u,"📋 Réservation — "+s.getNumero(),
                        "Bonjour "+u.getNomComplet()+",\n\n"+currentUser.getNomComplet()+" a demandé la salle "
                                +s.getNumero()+" le "+d+" de "+hDF+" à "+fF+":00.\nMotif : "+motif+"\n\nCordialement,\nUNIV-SCHEDULER")),
                "email-reserv").start();
        if(motifField!=null)motifField.clear();
        if(salleReservCombo!=null)salleReservCombo.setValue(null);
        if(heureFinReservCombo!=null)heureFinReservCombo.setValue("10:00");
        if(conflitReservLabel!=null)conflitReservLabel.setVisible(false);
        loadData();
        showInfo("Réservation enregistrée","Demande pour "+s.getNumero()+" le "+d+" → en attente de validation.");
    }

    @FXML private void handleDeleteReservation() {
        if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}
        if("VALIDEE".equals(selectedReserv.getStatut())){showError("Impossible","Impossible d'annuler une réservation validée.");return;}
        if(confirmDelete("cette réservation")){reservDAO.delete(selectedReserv.getId());loadData();}
    }

    // ════════════════════════════════════════════════════════════════
    //  NOTIFICATIONS TABLE
    // ════════════════════════════════════════════════════════════════
    private void setupNotifTable() {
        if(notifTable==null)return;
        if(colNotifMsg!=null)  colNotifMsg.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMessage()));
        if(colNotifType!=null) colNotifType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getType()));
        if(colNotifDate!=null) colNotifDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateEnvoi()!=null?d.getValue().getDateEnvoi().toLocalDate().toString():""));
        notifTable.setItems(notifList);
    }

    @FXML private void handleMarkAllRead() { notifDAO.markAllRead(currentUser.getId()); loadData(); }

    // ════════════════════════════════════════════════════════════════
    //  SIGNALEMENT
    // ════════════════════════════════════════════════════════════════
    private void setupSignalementForm() {
        if(sigCategorieCombo!=null){sigCategorieCombo.setItems(FXCollections.observableArrayList("EQUIPEMENT","SALLE","AUTRE"));sigCategorieCombo.setValue("EQUIPEMENT");}
        if(sigPrioriteCombo!=null){sigPrioriteCombo.setItems(FXCollections.observableArrayList("BASSE","NORMALE","HAUTE","URGENTE"));sigPrioriteCombo.setValue("NORMALE");}
        if(sigSalleCombo!=null) sigSalleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        if(sigFeedbackLabel!=null) sigFeedbackLabel.setVisible(false);
    }

    private void setupMesSignalementsTable() {
        if(mesSignalementsTable==null)return;
        if(colSigTitre!=null)  colSigTitre.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorieIcon()+" "+d.getValue().getTitre()));
        if(colSigCat!=null)    colSigCat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorie()));
        if(colSigPrio!=null)   colSigPrio.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getPrioriteIcon()+" "+d.getValue().getPriorite()));
        if(colSigStatut!=null) colSigStatut.setCellValueFactory(d->new SimpleStringProperty(formatStatut(d.getValue().getStatut())));
        if(colSigDate!=null)   colSigDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateSignalement()!=null?d.getValue().getDateSignalement().toLocalDate().toString():""));
        if(colSigSalle!=null)  colSigSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()!=null?d.getValue().getSalleNumero():"—"));
        mesSignalementsTable.setRowFactory(tv->new TableRow<>(){
            @Override protected void updateItem(Signalement item,boolean empty){super.updateItem(item,empty);if(item==null||empty){setStyle("");return;}
                switch(item.getStatut()){case"RESOLU":setStyle("-fx-background-color:#f0fdf4;");break;case"EN_COURS":setStyle("-fx-background-color:#fffbeb;");break;default:setStyle("");}}
        });
        mesSignalementsTable.setItems(sigList);
    }

    @FXML private void handleSendProbleme() {
        String titre=sigTitreField!=null?sigTitreField.getText().trim():"";
        if(titre.isEmpty()){showFeedback("⚠ Veuillez saisir un titre.",false);return;}
        Signalement sig=new Signalement();
        sig.setTitre(titre); sig.setDescription(sigDescArea!=null?sigDescArea.getText().trim():"");
        sig.setCategorie(sigCategorieCombo!=null&&sigCategorieCombo.getValue()!=null?sigCategorieCombo.getValue():"AUTRE");
        sig.setPriorite(sigPrioriteCombo!=null&&sigPrioriteCombo.getValue()!=null?sigPrioriteCombo.getValue():"NORMALE");
        sig.setStatut("EN_ATTENTE"); sig.setEnseignantId(currentUser.getId());
        sig.setDateSignalement(LocalDateTime.now());
        Salle salle=sigSalleCombo!=null?sigSalleCombo.getValue():null;
        if(salle!=null){sig.setSalleId(salle.getId());sig.setSalleNumero(salle.getNumero());}
        sigDAO.save(sig);
        new UtilisateurDAO().findAll().stream()
                .filter(u->"GESTIONNAIRE".equals(u.getRole())||"ADMIN".equals(u.getRole()))
                .forEach(u->{Notification n=new Notification();n.setUtilisateurId(u.getId());
                    n.setType("URGENTE".equals(sig.getPriorite())?"ALERTE":"INFO");
                    n.setMessage(sig.getPrioriteIcon()+" Signalement — "+sig.getTitre()+" | Par : "+currentUser.getNomComplet());
                    notifDAO.save(n);});
        if(isGmail(currentUser)) EmailService.sendNotification(currentUser,"✅ Signalement enregistré",
                "Bonjour "+currentUser.getNomComplet()+",\n\nVotre signalement a été transmis.\n\nCordialement,\nUNIV-SCHEDULER");
        showFeedback("✅ Signalement #"+sig.getId()+" transmis à l'administration.",true);
        clearSignalementForm(); loadData();
    }

    @FXML private void handleVoirMonSignalement() {
        if(mesSignalementsTable==null)return;
        Signalement sel=mesSignalementsTable.getSelectionModel().getSelectedItem();
        if(sel==null){showError("Erreur","Sélectionnez un signalement.");return;}
        String c;switch(sel.getStatut()){case"RESOLU":c="#10b981";break;case"EN_COURS":c="#f59e0b";break;case"FERME":c="#6366f1";break;default:c="#94a3b8";}
        String[][]l={{"📅 Date",sel.getDateSignalement()!=null?sel.getDateSignalement().toLocalDate().toString():"—"},{"🏫 Salle",sel.getSalleNumero()!=null?sel.getSalleNumero():"—"},{"🔖 Catégorie",sel.getCategorie()},{"⚡ Priorité",sel.getPrioriteIcon()+" "+sel.getPriorite()},{"📌 Statut",formatStatut(sel.getStatut())}};
        AlertePersonnalisee.afficherDetailSignalement(sel.getId(),sel.getCategorieIcon()+" "+sel.getTitre(),
                l,sel.getDescription(),sel.getCommentaireAdmin(),
                sel.getDateResolution()!=null?sel.getDateResolution().toLocalDate().toString():null,c);
    }

    private void showFeedback(String msg,boolean ok){
        if(sigFeedbackLabel==null){showInfo("Signalement",msg);return;}
        sigFeedbackLabel.setText(msg);
        sigFeedbackLabel.setStyle(ok?"-fx-text-fill:#16a34a;-fx-font-weight:bold;":"-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        sigFeedbackLabel.setVisible(true);
    }

    private void clearSignalementForm(){
        if(sigTitreField!=null)sigTitreField.clear();if(sigDescArea!=null)sigDescArea.clear();
        if(sigCategorieCombo!=null)sigCategorieCombo.setValue("EQUIPEMENT");
        if(sigPrioriteCombo!=null)sigPrioriteCombo.setValue("NORMALE");
        if(sigSalleCombo!=null)sigSalleCombo.setValue(null);
    }

    private String formatStatut(String s){
        if(s==null)return"—";
        switch(s){case"EN_ATTENTE":return"⏳ En attente";case"EN_COURS":return"🔄 En cours";case"RESOLU":return"✅ Résolu";case"FERME":return"🔒 Fermé";default:return s;}
    }

    // ════════════════════════════════════════════════════════════════
    //  EXPORTS
    // ════════════════════════════════════════════════════════════════
    @FXML private void handleExportPDF() {
        FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));fc.setInitialFileName("mon_edt.pdf");
        File f=fc.showSaveDialog(null);if(f==null)return;
        try{exportSvc.exportCoursAsPDF(coursList,f);showInfo("Export PDF","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());}
    }

    @FXML private void handleExportExcel() {
        FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel","*.xlsx"));fc.setInitialFileName("mon_edt.xlsx");
        File f=fc.showSaveDialog(null);if(f==null)return;
        try{exportSvc.exportCoursAsExcel(coursList,f);showInfo("Export Excel","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());}
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private static String nvl(String s, String def) { return (s!=null&&!s.isBlank())?s:def; }
    private static String nvlStr(String s) { return (s!=null&&!s.isBlank())?s:"—"; }

    @FXML protected void openChatbot() { AlertePersonnalisee.ouvrirChatbot(currentUser.getNomComplet()); }
    @FXML private void handleLogout() { arreterRappelStatutCours(); Servicerappel.getInstance().arreter(); logout(); }
    @FXML private void handleRefresh() { loadData(); buildChart(); }
}