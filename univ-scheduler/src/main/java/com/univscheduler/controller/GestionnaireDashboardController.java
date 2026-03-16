package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.service.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class GestionnaireDashboardController extends BaseController {

    @FXML private Label welcomeLabel, totalCoursLabel, conflitsLabel;
    @FXML private Label coursFormTitle, conflitLabel;

    // Cours table
    @FXML private TableView<Cours> coursTable;
    @FXML private TableColumn<Cours, String> colMat, colEns, colCls, colCren, colSalle, colDate, colStatut;
    @FXML private ComboBox<Matiere> matiereCombo;
    @FXML private ComboBox<Utilisateur> enseignantCombo;
    @FXML private ComboBox<ClassePedago> classeCombo;
    @FXML private ComboBox<Creneau> creneauCombo;
    @FXML private ComboBox<Salle> salleCombo;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> statutCombo;

    // Reservations
    @FXML private TableView<Reservation> reservTable;
    @FXML private TableColumn<Reservation, String> colResMotif, colResSalle, colResDate, colResStatut, colResUser;

    // Calendar
    @FXML private GridPane calendarGrid;
    @FXML private Label calendarWeekLabel;

    // Charts / rapports
    @FXML private VBox chartCoursContainer, chartReservContainer;
    @FXML private VBox chartOccupationContainer;
    @FXML private TableView<Salle> sallesCritiquesTable;
    @FXML private TableColumn<Salle, String> colCritNum, colCritType, colCritTaux;
    @FXML private TableColumn<Salle, Integer> colCritCap;
    @FXML private TextArea rapportTextArea;

    // Carte interactive
    @FXML private VBox carteContainer;

    // Historique
    @FXML private TableView<Reservation> historiqueTable;
    @FXML private TableColumn<Reservation, String> colHistUser, colHistMotif, colHistSalle, colHistDate, colHistStatut;

    private final CoursDAO coursDAO = new CoursDAO();
    private final MatiereDAO matiereDAO = new MatiereDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final ClassePedagoDAO classeDAO = new ClassePedagoDAO();
    private final CreneauDAO creneauDAO = new CreneauDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final ReservationDAO reservDAO = new ReservationDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private final RapportService rapportService = new RapportService();
    private final ExportService exportService = new ExportService();

    private final ObservableList<Cours> coursList = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservList = FXCollections.observableArrayList();
    private final ObservableList<Reservation> historiqueList = FXCollections.observableArrayList();
    private final ObservableList<Salle> critList = FXCollections.observableArrayList();

    private Cours selectedCours = null;
    private Reservation selectedReserv = null;
    private LocalDate calendarWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupCoursTable();
        setupReservTable();
        setupHistoriqueTable();
        setupCritiquesTable();

        matiereCombo.setItems(FXCollections.observableArrayList(matiereDAO.findAll()));
        enseignantCombo.setItems(FXCollections.observableArrayList(utilisateurDAO.findAllEnseignants()));
        classeCombo.setItems(FXCollections.observableArrayList(classeDAO.findAll()));
        creneauCombo.setItems(FXCollections.observableArrayList(creneauDAO.findAll()));
        salleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        statutCombo.setItems(FXCollections.observableArrayList("PLANIFIE","EN_COURS","TERMINE","ANNULE"));

        if (conflitLabel != null) {
            conflitLabel.setVisible(false);
            conflitLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");
        }

        loadData();
        buildCharts();
        buildCalendar();
        buildCarteSalles();

        salleCombo.setOnAction(e -> checkConflict());
        creneauCombo.setOnAction(e -> checkConflict());
        datePicker.setOnAction(e -> checkConflict());
        enseignantCombo.setOnAction(e -> checkConflict());
    }

    // ========================= TABLE SETUP =========================

    private void setupCoursTable() {
        colMat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMatiereNom()));
        colEns.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEnseignantNom()));
        colCls.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClasseNom()));
        colCren.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreneauInfo()));
        colSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()!=null?d.getValue().getDate().toString():""));
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        coursTable.setItems(coursList);
        coursTable.getSelectionModel().selectedItemProperty().addListener((obs,old,c) -> {
            if(c!=null){ selectedCours=c; fillForm(c); }
        });
    }

    private void setupReservTable() {
        if (reservTable == null) return;
        colResMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));
        colResSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        colResDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        colResStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        if (colResUser != null)
            colResUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUtilisateurNom()));
        reservTable.setItems(reservList);
        reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r) -> selectedReserv=r);
    }

    private void setupHistoriqueTable() {
        if (historiqueTable == null) return;
        if (colHistUser != null)  colHistUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUtilisateurNom()));
        if (colHistMotif != null) colHistMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));
        if (colHistSalle != null) colHistSalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSalleNumero()));
        if (colHistDate != null)  colHistDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        if (colHistStatut != null) colHistStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut()));
        historiqueTable.setItems(historiqueList);
    }

    private void setupCritiquesTable() {
        if (sallesCritiquesTable == null) return;
        Map<String, Double> taux = rapportService.getTauxOccupation();
        if (colCritNum  != null) colCritNum.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().getNumero()));
        if (colCritType != null) colCritType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSalle()));
        if (colCritCap  != null) colCritCap.setCellValueFactory(d  -> new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        if (colCritTaux != null) colCritTaux.setCellValueFactory(d -> {
            double t = taux.getOrDefault(d.getValue().getNumero(), 0.0);
            String niveau = t >= 80 ? "🔴 " : t >= 50 ? "🟡 " : "🟢 ";
            return new SimpleStringProperty(niveau + t + "%");
        });
        sallesCritiquesTable.setItems(critList);
    }

    // ========================= DATA =========================

    private void loadData() {
        coursList.setAll(coursDAO.findAll());
        List<Reservation> allReserv = reservDAO.findAll();
        reservList.setAll(allReserv.stream()
            .filter(r -> "EN_ATTENTE".equals(r.getStatut())).collect(java.util.stream.Collectors.toList()));
        historiqueList.setAll(allReserv);
        critList.setAll(rapportService.getSallesCritiques());
        totalCoursLabel.setText("Total : " + coursList.size() + " cours");
        long conflits = coursList.stream().filter(c -> c.getStatut()!=null && c.getStatut().equals("CONFLIT")).count();
        if (conflitsLabel != null) conflitsLabel.setText(String.valueOf(conflits));
    }

    // ========================= CHARTS =========================

    private void buildCharts() {
        if (chartCoursContainer != null) {
            CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
            xA.setLabel("Jour"); yA.setLabel("Cours planifiés");
            BarChart<String,Number> bar = new BarChart<>(xA,yA);
            bar.setTitle("📅 Cours par Jour"); bar.setLegendVisible(false); bar.setPrefHeight(240);
            XYChart.Series<String,Number> series = new XYChart.Series<>();
            coursDAO.countByJour().forEach((k,v) -> series.getData().add(new XYChart.Data<>(k,v)));
            bar.getData().add(series);
            chartCoursContainer.getChildren().setAll(bar);
        }
        if (chartReservContainer != null) {
            PieChart pie = new PieChart(); pie.setTitle("📊 Statut des Cours"); pie.setPrefHeight(240);
            coursDAO.countByStatut().forEach((k,v) -> pie.getData().add(new PieChart.Data(k+" ("+v+")",v)));
            chartReservContainer.getChildren().setAll(pie);
        }
        buildOccupationChart();
    }

    private void buildOccupationChart() {
        if (chartOccupationContainer == null) return;
        Map<String,Double> taux = rapportService.getTauxOccupation();
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
        yA.setUpperBound(100); yA.setLowerBound(0); yA.setTickUnit(10);
        xA.setLabel("Salle"); yA.setLabel("Occupation (%)");
        BarChart<String,Number> bar = new BarChart<>(xA,yA);
        bar.setTitle("🏫 Taux d'Occupation par Salle"); bar.setLegendVisible(false); bar.setPrefHeight(280);
        XYChart.Series<String,Number> series = new XYChart.Series<>();
        taux.forEach((k,v) -> series.getData().add(new XYChart.Data<>(k,v)));
        bar.getData().add(series);
        chartOccupationContainer.getChildren().setAll(bar);
    }

    // ========================= CARTE INTERACTIVE =========================

    private void buildCarteSalles() {
        if (carteContainer == null) return;
        carteContainer.getChildren().clear();
        Map<String, Double> taux = rapportService.getTauxOccupation();
        List<Salle> salles = salleDAO.findAll();

        // Title
        Label title = new Label("🗺️ Carte Interactive des Salles");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1e293b;-fx-padding:0 0 8 0;");

        // Legend
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
            makeChip("🟢 Normal (< 50%)", "#dcfce7", "#166534"),
            makeChip("🟡 Élevé (50–80%)", "#fef9c3", "#854d0e"),
            makeChip("🔴 Critique (> 80%)", "#fee2e2", "#991b1b"),
            makeChip("⚫ Indisponible", "#f1f5f9", "#475569")
        );

        // Buildings grouped view
        Map<String, List<Salle>> parBatiment = new LinkedHashMap<>();
        for (Salle s : salles) {
            String b = s.getBatimentNom() != null ? s.getBatimentNom() : "Sans bâtiment";
            parBatiment.computeIfAbsent(b, x -> new ArrayList<>()).add(s);
        }

        VBox allBuildings = new VBox(16);
        for (Map.Entry<String, List<Salle>> entry : parBatiment.entrySet()) {
            VBox batBox = new VBox(8);
            batBox.setStyle("-fx-background-color:white;-fx-padding:14;-fx-background-radius:10;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");

            Label batLabel = new Label("🏢 " + entry.getKey());
            batLabel.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1e293b;");

            FlowPane sallesPane = new FlowPane(10, 10);
            for (Salle s : entry.getValue()) {
                double t = taux.getOrDefault(s.getNumero(), 0.0);
                String bg, fg;
                if (!s.isDisponible()) { bg="#f1f5f9"; fg="#475569"; }
                else if (t >= 80)      { bg="#fee2e2"; fg="#991b1b"; }
                else if (t >= 50)      { bg="#fef9c3"; fg="#854d0e"; }
                else                   { bg="#dcfce7"; fg="#166534"; }

                VBox card = new VBox(4);
                card.setAlignment(Pos.CENTER);
                card.setPadding(new Insets(10, 14, 10, 14));
                card.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:8;"
                    + "-fx-border-color:" + fg + ";-fx-border-radius:8;-fx-border-width:1.5;"
                    + "-fx-cursor:hand;");
                card.setPrefWidth(110);

                Label numLbl = new Label(s.getNumero());
                numLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + fg + ";");

                Label typeLbl = new Label(s.getTypeSalle() + " | " + s.getCapacite() + "p");
                typeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#64748b;");

                Label tauxLbl = new Label(s.isDisponible() ? t + "%" : "Indisponible");
                tauxLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + fg + ";");

                card.getChildren().addAll(numLbl, typeLbl, tauxLbl);

                // Tooltip with details
                Tooltip tip = new Tooltip(
                    s.getNumero() + " — " + s.getTypeSalle() + "\n"
                    + "Capacité : " + s.getCapacite() + " places\n"
                    + "Bâtiment : " + (s.getBatimentNom()!=null?s.getBatimentNom():"N/A") + "\n"
                    + "Occupation : " + t + "%\n"
                    + "Statut : " + (s.isDisponible() ? "✅ Disponible" : "🔒 Indisponible")
                );
                Tooltip.install(card, tip);
                sallesPane.getChildren().add(card);
            }
            batBox.getChildren().addAll(batLabel, sallesPane);
            allBuildings.getChildren().add(batBox);
        }

        // Global stat summary
        Label globalTaux = new Label(String.format(
            "📊 Taux d'occupation global : %.1f%%  |  Salles critiques : %d  |  Salles disponibles : %d",
            rapportService.getTauxOccupationGlobal(),
            rapportService.getSallesCritiques().size(),
            salleDAO.countDisponibles()
        ));
        globalTaux.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;-fx-padding:4 0 0 0;");

        carteContainer.getChildren().addAll(title, legend, globalTaux, allBuildings);
    }

    private Label makeChip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg
            + ";-fx-padding:3 10;-fx-background-radius:8;-fx-font-size:11px;-fx-font-weight:bold;");
        return l;
    }

    // ========================= CALENDAR =========================

    private void buildCalendar() {
        if (calendarGrid == null) return;
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        String[] jours = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};
        int[] heures = {8,10,12,14,16};

        ColumnConstraints ccTime = new ColumnConstraints(60);
        calendarGrid.getColumnConstraints().add(ccTime);
        for (int j = 0; j < jours.length; j++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(cc);
        }

        Label corner = new Label("Heure");
        corner.setStyle("-fx-font-weight:bold;-fx-background-color:#1e293b;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;");
        corner.setMaxWidth(Double.MAX_VALUE);
        calendarGrid.add(corner, 0, 0);

        for (int j = 0; j < jours.length; j++) {
            LocalDate d = calendarWeekStart.plusDays(j);
            Label lbl = new Label(jours[j] + "\n" + d.getDayOfMonth()+"/"+d.getMonthValue());
            lbl.setStyle("-fx-font-weight:bold;-fx-background-color:#1e293b;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;-fx-font-size:11px;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(lbl, Priority.ALWAYS);
            calendarGrid.add(lbl, j+1, 0);
        }

        Map<String,List<Cours>> par = new HashMap<>();
        for (String j : jours) par.put(j, new ArrayList<>());
        for (Cours c : coursList) {
            if (c.getCreneauInfo()!=null)
                for (String j : jours) if (c.getCreneauInfo().startsWith(j)) { par.get(j).add(c); break; }
        }

        for (int h = 0; h < heures.length; h++) {
            Label hLbl = new Label(heures[h]+"h");
            hLbl.setStyle("-fx-background-color:#f1f5f9;-fx-font-weight:bold;-fx-padding:8 4;-fx-alignment:CENTER;-fx-font-size:11px;");
            hLbl.setMaxWidth(Double.MAX_VALUE); hLbl.setMaxHeight(Double.MAX_VALUE);
            calendarGrid.add(hLbl, 0, h+1);

            for (int j = 0; j < jours.length; j++) {
                VBox cell = new VBox(2);
                cell.setPadding(new Insets(3));
                cell.setStyle("-fx-border-color:#e2e8f0;-fx-border-width:0.5;-fx-background-color:white;");
                cell.setMinHeight(60);
                for (Cours c : par.get(jours[j])) {
                    if (c.getCreneauInfo()!=null && c.getCreneauInfo().contains(heures[h]+"h")) {
                        Label m = new Label("📚 "+(c.getMatiereNom()!=null?c.getMatiereNom():"?"));
                        m.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-padding:2 6;-fx-background-radius:4;-fx-font-size:10px;");
                        m.setWrapText(true);
                        Label e = new Label("👤 "+(c.getEnseignantNom()!=null?c.getEnseignantNom():"?"));
                        e.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");
                        Label s = new Label("🏫 "+(c.getSalleNumero()!=null?c.getSalleNumero():"?"));
                        s.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");
                        cell.getChildren().addAll(m,e,s);
                    }
                }
                calendarGrid.add(cell, j+1, h+1);
            }
        }
        if (calendarWeekLabel!=null)
            calendarWeekLabel.setText("Semaine du " + calendarWeekStart.getDayOfMonth()+"/"+calendarWeekStart.getMonthValue()+"/"+calendarWeekStart.getYear());
    }

    @FXML private void handlePrevWeek()  { calendarWeekStart=calendarWeekStart.minusWeeks(1); buildCalendar(); }
    @FXML private void handleNextWeek()  { calendarWeekStart=calendarWeekStart.plusWeeks(1);  buildCalendar(); }
    @FXML private void handleTodayWeek() { calendarWeekStart=LocalDate.now().with(java.time.DayOfWeek.MONDAY); buildCalendar(); }

    // ========================= CONFLIT CHECK =========================

    private void checkConflict() {
        if (conflitLabel == null) return;
        Salle salle=salleCombo.getValue(); Creneau cren=creneauCombo.getValue();
        LocalDate date=datePicker.getValue(); Utilisateur ens=enseignantCombo.getValue();
        if(salle==null||cren==null||date==null){ conflitLabel.setVisible(false); return; }
        int excl=selectedCours!=null?selectedCours.getId():0;
        boolean cs=coursDAO.hasConflitSalle(salle.getId(),cren.getId(),date.toString(),excl);
        boolean ce=ens!=null&&coursDAO.hasConflitEnseignant(ens.getId(),cren.getId(),date.toString(),excl);
        if(cs) {
            conflitLabel.setText("⚠ CONFLIT : Salle déjà occupée !");
            conflitLabel.setVisible(true);
            AlerteService.alerterConflit("Salle " + salle.getNumero() + " occupée le " + date + " créneau " + cren);
        } else if(ce) {
            conflitLabel.setText("⚠ CONFLIT : Enseignant indisponible !");
            conflitLabel.setVisible(true);
            AlerteService.alerterConflit("Enseignant " + ens.getNomComplet() + " indisponible le " + date + " créneau " + cren);
        } else {
            conflitLabel.setVisible(false);
        }
    }

    // ========================= COURS CRUD =========================

    @FXML private void handleSaveCours() {
        if(matiereCombo.getValue()==null||enseignantCombo.getValue()==null||classeCombo.getValue()==null
                ||creneauCombo.getValue()==null||salleCombo.getValue()==null||datePicker.getValue()==null){
            showError("Erreur","Tous les champs sont obligatoires."); return;
        }
        int excl=selectedCours!=null?selectedCours.getId():0;
        String date=datePicker.getValue().toString();
        if(coursDAO.hasConflitSalle(salleCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){
            showError("Conflit","La salle est déjà occupée sur ce créneau."); return;
        }
        if(coursDAO.hasConflitEnseignant(enseignantCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){
            showError("Conflit","L'enseignant est indisponible sur ce créneau."); return;
        }

        // Detect room change to notify teacher
        String ancienneSalle = selectedCours != null ? selectedCours.getSalleNumero() : null;

        Cours c=selectedCours!=null?selectedCours:new Cours();
        c.setMatiereId(matiereCombo.getValue().getId());
        c.setEnseignantId(enseignantCombo.getValue().getId());
        c.setClasseId(classeCombo.getValue().getId());
        c.setCreneauId(creneauCombo.getValue().getId());
        c.setSalleId(salleCombo.getValue().getId());
        c.setDate(datePicker.getValue());
        c.setStatut(statutCombo.getValue()!=null?statutCombo.getValue():"PLANIFIE");
        coursDAO.save(c);

        // Email notification if room changed
        if (ancienneSalle != null && !ancienneSalle.equals(salleCombo.getValue().getNumero())) {
            Utilisateur ens = enseignantCombo.getValue();
            c.setSalleNumero(salleCombo.getValue().getNumero());
            c.setMatiereNom(matiereCombo.getValue().getNom());
            c.setClasseNom(classeCombo.getValue().getNom());
            c.setCreneauInfo(creneauCombo.getValue().toString());
            EmailService.notifierChangementSalle(ens, c, ancienneSalle);
            AlerteService.notifierUtilisateur(ens.getId(),
                "🔔 Votre cours " + matiereCombo.getValue().getNom() + " a changé de salle : " + ancienneSalle + " → " + salleCombo.getValue().getNumero(),
                "INFO");
        }

        loadData(); buildCharts(); buildCalendar(); buildCarteSalles(); clearForm();
        showInfo("Succès","Cours sauvegardé.");
    }

    @FXML private void handleDeleteCours() {
        if(selectedCours==null){ showError("Erreur","Sélectionnez un cours."); return; }
        if(confirmDelete("ce cours")){ coursDAO.delete(selectedCours.getId()); loadData(); buildCharts(); buildCalendar(); buildCarteSalles(); clearForm(); }
    }

    // ========================= RESERVATIONS =========================

    @FXML private void handleValiderReserv() {
        if(selectedReserv==null){ showError("Erreur","Sélectionnez une réservation."); return; }
        reservDAO.updateStatut(selectedReserv.getId(),"VALIDEE");
        Notification n=new Notification();
        n.setMessage("✅ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été validée.");
        n.setType("INFO"); n.setUtilisateurId(selectedReserv.getUtilisateurId()); notifDAO.save(n);
        // Email
        utilisateurDAO.findAll().stream()
            .filter(u -> u.getId()==selectedReserv.getUtilisateurId()).findFirst()
            .ifPresent(u -> EmailService.notifierValidationReservation(u, selectedReserv));
        loadData(); showInfo("Succès","Réservation validée.");
    }

    @FXML private void handleRefuserReserv() {
        if(selectedReserv==null){ showError("Erreur","Sélectionnez une réservation."); return; }
        reservDAO.updateStatut(selectedReserv.getId(),"REFUSEE");
        Notification n=new Notification();
        n.setMessage("❌ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été refusée.");
        n.setType("ALERTE"); n.setUtilisateurId(selectedReserv.getUtilisateurId()); notifDAO.save(n);
        // Email
        utilisateurDAO.findAll().stream()
            .filter(u -> u.getId()==selectedReserv.getUtilisateurId()).findFirst()
            .ifPresent(u -> EmailService.notifierRefusReservation(u, selectedReserv));
        loadData(); showInfo("Fait","Réservation refusée.");
    }

    // ========================= EXPORTS =========================

    @FXML private void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'emploi du temps en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("emploi_du_temps.pdf");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            exportService.exportCoursAsPDF(coursList, f);
            showInfo("Export PDF", "Emploi du temps exporté : " + f.getName());
        } catch (Exception e) { showError("Erreur", "Export PDF échoué : " + e.getMessage()); }
    }

    @FXML private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("emploi_du_temps.xlsx");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            exportService.exportCoursAsExcel(coursList, f);
            showInfo("Export Excel", "Exporté : " + f.getName());
        } catch (Exception e) { showError("Erreur", "Export Excel échoué : " + e.getMessage()); }
    }

    @FXML private void handleExportHistoriquePDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'historique en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("historique_reservations.pdf");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            exportService.exportReservationsAsPDF(reservDAO.findAll(), f);
            showInfo("Export PDF", "Historique exporté : " + f.getName());
        } catch (Exception e) { showError("Erreur", "Export échoué : " + e.getMessage()); }
    }

    @FXML private void handleExportHistoriqueExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'historique en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("historique_reservations.xlsx");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            exportService.exportReservationsAsExcel(reservDAO.findAll(), f);
            showInfo("Export Excel", "Exporté : " + f.getName());
        } catch (Exception e) { showError("Erreur", "Export échoué : " + e.getMessage()); }
    }

    @FXML private void handleExportOccupationExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter le rapport d'occupation en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("occupation_salles.xlsx");
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            exportService.exportOccupationAsExcel(rapportService.getTauxOccupation(), f);
            showInfo("Export Excel", "Exporté : " + f.getName());
        } catch (Exception e) { showError("Erreur", "Export échoué : " + e.getMessage()); }
    }

    // ========================= RAPPORTS =========================

    @FXML private void handleRapportHebdo() {
        Map<String,Object> r = rapportService.getRapportHebdomadaire();
        if (rapportTextArea != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("══════════════════════════════════════\n");
            sb.append("       ").append(r.get("titre")).append("\n");
            sb.append("══════════════════════════════════════\n");
            r.forEach((k,v) -> {
                if (!k.equals("titre") && !k.equals("coursParJour") && !k.equals("coursParStatut"))
                    sb.append(String.format("  %-30s : %s%n", k, v));
            });
            sb.append("\n── Cours par jour ──\n");
            if (r.get("coursParJour") instanceof Map) {
                ((Map<?,?>)r.get("coursParJour")).forEach((k,v) -> sb.append("  ").append(k).append(" : ").append(v).append("\n"));
            }
            sb.append("\n── Cours par statut ──\n");
            if (r.get("coursParStatut") instanceof Map) {
                ((Map<?,?>)r.get("coursParStatut")).forEach((k,v) -> sb.append("  ").append(k).append(" : ").append(v).append("\n"));
            }
            rapportTextArea.setText(sb.toString());
        }
        buildOccupationChart();
        critList.setAll(rapportService.getSallesCritiques());
    }

    @FXML private void handleRapportMensuel() {
        Map<String,Object> r = rapportService.getRapportMensuel();
        if (rapportTextArea != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("══════════════════════════════════════\n");
            sb.append("       ").append(r.get("titre")).append(" — ").append(r.get("mois")).append("\n");
            sb.append("══════════════════════════════════════\n");
            r.forEach((k,v) -> {
                if (!k.equals("titre") && !k.equals("mois") && !k.equals("coursParJour")
                        && !k.equals("coursParStatut") && !k.equals("tauxOccupationParSalle"))
                    sb.append(String.format("  %-30s : %s%n", k, v));
            });
            sb.append("\n── Taux par salle ──\n");
            if (r.get("tauxOccupationParSalle") instanceof Map) {
                ((Map<?,?>)r.get("tauxOccupationParSalle")).forEach((k,v) ->
                    sb.append("  ").append(k).append(" : ").append(v).append("%\n"));
            }
            rapportTextArea.setText(sb.toString());
        }
        buildOccupationChart();
        critList.setAll(rapportService.getSallesCritiques());
    }

    @FXML private void handleRafraichirCarte() { buildCarteSalles(); }

    @FXML private void handleClearCours() { clearForm(); }
    @FXML private void handleLogout() { logout(); }

    private void fillForm(Cours c) {
        coursFormTitle.setText("Modifier Cours");
        matiereCombo.getItems().stream().filter(m->m.getId()==c.getMatiereId()).findFirst().ifPresent(matiereCombo::setValue);
        enseignantCombo.getItems().stream().filter(u->u.getId()==c.getEnseignantId()).findFirst().ifPresent(enseignantCombo::setValue);
        classeCombo.getItems().stream().filter(cp->cp.getId()==c.getClasseId()).findFirst().ifPresent(classeCombo::setValue);
        creneauCombo.getItems().stream().filter(cr->cr.getId()==c.getCreneauId()).findFirst().ifPresent(creneauCombo::setValue);
        salleCombo.getItems().stream().filter(s->s.getId()==c.getSalleId()).findFirst().ifPresent(salleCombo::setValue);
        if(c.getDate()!=null) datePicker.setValue(c.getDate());
        statutCombo.setValue(c.getStatut());
    }

    private void clearForm() {
        matiereCombo.setValue(null); enseignantCombo.setValue(null); classeCombo.setValue(null);
        creneauCombo.setValue(null); salleCombo.setValue(null); datePicker.setValue(null);
        statutCombo.setValue(null); selectedCours=null;
        if(conflitLabel!=null) conflitLabel.setVisible(false);
        coursFormTitle.setText("Nouveau Cours");
        coursTable.getSelectionModel().clearSelection();
    }
}
