package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
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
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

public class GestionnaireDashboardController extends BaseController {

    @FXML private Label welcomeLabel, totalCoursLabel, conflitsLabel;
    @FXML private Label coursFormTitle, conflitLabel;
    @FXML private Label salleAutoLabel;

    // ── badge notifications navbar ────────────────────────────────
    @FXML private Label notifBadge;

    // ── Filtre classe tableau cours ───────────────────────────────
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

    @FXML private VBox carteContainer;

    @FXML private TableView<Reservation> historiqueTable;
    @FXML private TableColumn<Reservation, String> colHistUser, colHistMotif, colHistSalle, colHistDate, colHistStatut;

    @FXML private TableView<Signalement>           signalTable;
    @FXML private TableColumn<Signalement, String> colSignTitre, colSignEns, colSignSalle,
            colSignCat, colSignPrio, colSignStatut, colSignDate;
    @FXML private Label            signalBadge;
    @FXML private TextArea         signalCommentArea;
    @FXML private ComboBox<String> signalStatutCombo;

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

    private final ObservableList<Cours>       coursList      = FXCollections.observableArrayList();
    private final ObservableList<Cours>       coursListFiltree = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservList     = FXCollections.observableArrayList();
    private final ObservableList<Reservation> historiqueList = FXCollections.observableArrayList();
    private final ObservableList<Salle>       critList       = FXCollections.observableArrayList();
    private final ObservableList<Signalement> signalList     = FXCollections.observableArrayList();

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

        if (conflitLabel  != null) { conflitLabel.setVisible(false); conflitLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;"); }
        if (salleAutoLabel != null) salleAutoLabel.setVisible(false);

        styleBoutonVue(false);
        loadData(); buildCharts(); buildCalendar(); buildCarteSalles();

        salleCombo.setOnAction(e      -> checkConflict());
        enseignantCombo.setOnAction(e -> checkConflict());
        classeCombo.setOnAction(e  -> { checkConflict(); assignerSalleAutomatique(); });
        creneauCombo.setOnAction(e -> { checkConflict(); assignerSalleAutomatique(); });
        datePicker.setOnAction(e   -> { checkConflict(); assignerSalleAutomatique(); });
    }

    // ── Assignation automatique ───────────────────────────────────
    private void assignerSalleAutomatique() {
        if (salleAutoLabel == null) return;
        ClassePedago classe  = classeCombo.getValue();
        Creneau      creneau = creneauCombo.getValue();
        LocalDate    date    = datePicker.getValue();
        if (classe == null || creneau == null || date == null) { salleAutoLabel.setVisible(false); return; }
        int effectif = classe.getEffectif(); int creneauId = creneau.getId();
        String dateStr = date.toString(); int exclId = selectedCours != null ? selectedCours.getId() : 0;
        Salle meilleure = salleDAO.trouverMeilleureSalle(effectif, creneauId, dateStr, exclId);
        if (meilleure == null) {
            salleAutoLabel.setText("⚠️ Aucune salle disponible pour " + effectif + " étudiants sur ce créneau.");
            salleAutoLabel.setStyle("-fx-text-fill:#dc2626;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:2 0;");
            salleAutoLabel.setVisible(true); return;
        }
        if (salleCombo.getValue() == null || selectedCours == null)
            salleCombo.getItems().stream().filter(s->s.getId()==meilleure.getId()).findFirst().ifPresent(salleCombo::setValue);
        List<Salle> alts = salleDAO.trouverSallesDisponiblesPourCours(effectif, creneauId, dateStr, exclId);
        int nbAlts = alts.size() - 1;
        String msg = "🏫 Salle suggérée : " + meilleure.getNumero() + "  (" + meilleure.getTypeSalle()
                + ", " + meilleure.getCapacite() + " places"
                + (meilleure.getBatimentNom() != null ? ", " + meilleure.getBatimentNom() : "") + ")";
        if (nbAlts > 0) msg += "\n   + " + nbAlts + " autre(s) salle(s) disponible(s)";
        salleAutoLabel.setText(msg);
        salleAutoLabel.setStyle("-fx-text-fill:#166534;-fx-font-size:11px;-fx-font-weight:bold;"
                + "-fx-background-color:#dcfce7;-fx-background-radius:6;-fx-padding:6 10;");
        salleAutoLabel.setVisible(true);
    }

    // ── Tables ────────────────────────────────────────────────────
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
    // ── Filtre par classe ─────────────────────────────────────────
    private void setupFiltreClasse() {
        if (filtreClasseCombo == null) return;
        filtreClasseCombo.setOnAction(e -> appliquerFiltreClasse());
    }

    private void refreshFiltreClasse() {
        if (filtreClasseCombo == null) { coursListFiltree.setAll(coursList); return; }
        String sel = filtreClasseCombo.getValue();
        List<String> classes = new ArrayList<>();
        classes.add("— Toutes les classes —");
        coursList.stream().map(Cours::getClasseNom)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted().forEach(classes::add);
        filtreClasseCombo.setItems(FXCollections.observableArrayList(classes));
        String restore = (sel != null && classes.contains(sel)) ? sel : "— Toutes les classes —";
        filtreClasseCombo.setValue(restore);
        appliquerFiltreClasse();
    }

    private void appliquerFiltreClasse() {
        if (filtreClasseCombo == null) { coursListFiltree.setAll(coursList); return; }
        String sel = filtreClasseCombo.getValue();
        if (sel == null || sel.startsWith("—")) {
            coursListFiltree.setAll(coursList);
            totalCoursLabel.setText("Total : " + coursList.size() + " cours");
            if (filtreInfoLabel != null) filtreInfoLabel.setText("");
        } else {
            coursListFiltree.setAll(
                    coursList.stream().filter(c -> sel.equals(c.getClasseNom()))
                            .collect(java.util.stream.Collectors.toList())
            );
            totalCoursLabel.setText(coursListFiltree.size() + " cours  (classe : " + sel + ")");
            if (filtreInfoLabel != null) filtreInfoLabel.setText(coursListFiltree.size() + " résultat(s)");
        }
    }

    @FXML private void handleReinitialiseFiltreClasse() {
        if (filtreClasseCombo != null) filtreClasseCombo.setValue("— Toutes les classes —");
    }

    private void setupReservTable() {
        if(reservTable==null)return;
        colResMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));
        colResSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));
        colResDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        colResStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));
        if(colResUser!=null) colResUser.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getUtilisateurNom()));
        reservTable.setItems(reservList);
        reservTable.getSelectionModel().selectedItemProperty().addListener((o,old,r)->selectedReserv=r);
    }
    private void setupHistoriqueTable() {
        if(historiqueTable==null)return;
        if(colHistUser  !=null) colHistUser.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getUtilisateurNom()));
        if(colHistMotif !=null) colHistMotif.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getMotif()));
        if(colHistSalle !=null) colHistSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()));
        if(colHistDate  !=null) colHistDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateReservation()!=null?d.getValue().getDateReservation().toLocalDate().toString():""));
        if(colHistStatut!=null) colHistStatut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getStatut()));
        historiqueTable.setItems(historiqueList);
    }
    private void setupCritiquesTable() {
        if(sallesCritiquesTable==null)return;
        Map<String,Double> taux=rapportService.getTauxOccupation();
        if(colCritNum !=null) colCritNum.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNumero()));
        if(colCritType!=null) colCritType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getTypeSalle()));
        if(colCritCap !=null) colCritCap.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        if(colCritTaux!=null) colCritTaux.setCellValueFactory(d->{double t=taux.getOrDefault(d.getValue().getNumero(),0.0);return new SimpleStringProperty((t>=80?"🔴 ":t>=50?"🟡 ":"🟢 ")+t+"%");});
        sallesCritiquesTable.setItems(critList);
    }
    private void setupSignalTable() {
        if(signalTable==null)return;
        if(colSignTitre !=null) colSignTitre.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorieIcon()+" "+d.getValue().getTitre()));
        if(colSignEns   !=null) colSignEns.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getEnseignantNom()!=null?d.getValue().getEnseignantNom():"—"));
        if(colSignSalle !=null) colSignSalle.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getSalleNumero()!=null?d.getValue().getSalleNumero():"—"));
        if(colSignCat   !=null) colSignCat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategorie()));
        if(colSignPrio  !=null) colSignPrio.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getPrioriteIcon()+" "+d.getValue().getPriorite()));
        if(colSignStatut!=null) colSignStatut.setCellValueFactory(d->new SimpleStringProperty(formatStatutSignal(d.getValue().getStatut())));
        if(colSignDate  !=null) colSignDate.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDateSignalement()!=null?d.getValue().getDateSignalement().toLocalDate().toString():""));
        signalTable.setRowFactory(tv->new TableRow<>(){
            @Override protected void updateItem(Signalement item,boolean empty){
                super.updateItem(item,empty);if(item==null||empty){setStyle("");return;}
                switch(item.getPriorite()){case "URGENTE":setStyle("-fx-background-color:#fee2e2;");break;case "HAUTE":setStyle("-fx-background-color:#ffedd5;");break;default:setStyle("");}
            }
        });
        signalTable.setItems(signalList);
        signalTable.getSelectionModel().selectedItemProperty().addListener((obs,old,sel)->{
            selectedSignal=sel;
            if(sel!=null&&signalCommentArea!=null) signalCommentArea.setText(sel.getCommentaireAdmin()!=null?sel.getCommentaireAdmin():"");
        });
        if(signalStatutCombo!=null) signalStatutCombo.setItems(FXCollections.observableArrayList("EN_COURS","RESOLU","FERME"));
    }

    // ── Data ──────────────────────────────────────────────────────
    private void loadData() {
        coursList.setAll(coursDAO.findAll());
        refreshFiltreClasse();
        List<Reservation> allReserv=reservDAO.findAll();
        reservList.setAll(allReserv.stream().filter(r->"EN_ATTENTE".equals(r.getStatut())).collect(java.util.stream.Collectors.toList()));
        historiqueList.setAll(allReserv); critList.setAll(rapportService.getSallesCritiques());
        totalCoursLabel.setText("Total : "+coursList.size()+" cours");
        long conflits=coursList.stream().filter(c1->coursList.stream().anyMatch(c2->c2.getId()!=c1.getId()&&c2.getSalleId()==c1.getSalleId()&&c2.getCreneauId()==c1.getCreneauId()&&c2.getDate()!=null&&c1.getDate()!=null&&c2.getDate().equals(c1.getDate()))).count()/2;
        if(conflitsLabel!=null) conflitsLabel.setText(String.valueOf(conflits));
        signalList.setAll(signalDAO.findAll());
        long nbEnAttente=signalDAO.countEnAttente();
        if(signalBadge!=null){signalBadge.setText(nbEnAttente>0?"🔴 "+nbEnAttente+" en attente":"");signalBadge.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");signalBadge.setVisible(nbEnAttente>0);}
        refreshNotifBadge();
    }

    // ── Badge notifications ───────────────────────────────────────
    private void refreshNotifBadge() {
        if (notifBadge == null) return;
        int nb = notifDAO.countUnread(currentUser.getId());
        if (nb > 0) {
            notifBadge.setText(String.valueOf(nb));
            notifBadge.setVisible(true);
        } else {
            notifBadge.setVisible(false);
        }
    }

    // ── ✅ Notifications : délégation à AlertePersonnalisee ───────
    @FXML
    private void handleVoirNotifications() {
        List<Notification> notifs = notifDAO.findByUtilisateur(currentUser.getId());
        notifDAO.markAllRead(currentUser.getId());
        refreshNotifBadge();
        AlertePersonnalisee.afficherNotifications(notifs);
    }

    // ── Charts ────────────────────────────────────────────────────
    private void buildCharts() {
        if(chartCoursContainer!=null){CategoryAxis xA=new CategoryAxis();NumberAxis yA=new NumberAxis();xA.setLabel("Jour");yA.setLabel("Cours planifiés");BarChart<String,Number> bar=new BarChart<>(xA,yA);bar.setTitle("📅 Cours par Jour");bar.setLegendVisible(false);bar.setPrefHeight(240);XYChart.Series<String,Number> series=new XYChart.Series<>();coursDAO.countByJour().forEach((k,v)->series.getData().add(new XYChart.Data<>(k,v)));bar.getData().add(series);chartCoursContainer.getChildren().setAll(bar);}
        if(chartReservContainer!=null){PieChart pie=new PieChart();pie.setTitle("📊 Statut des Cours");pie.setPrefHeight(240);coursDAO.countByStatut().forEach((k,v)->pie.getData().add(new PieChart.Data(k+" ("+v+")",v)));chartReservContainer.getChildren().setAll(pie);}
        buildOccupationChart();
    }
    private void buildOccupationChart() {
        if(chartOccupationContainer==null)return;
        Map<String,Double> taux=rapportService.getTauxOccupation();
        CategoryAxis xA=new CategoryAxis();NumberAxis yA=new NumberAxis();yA.setUpperBound(100);yA.setLowerBound(0);yA.setTickUnit(10);xA.setLabel("Salle");yA.setLabel("Occupation (%)");
        BarChart<String,Number> bar=new BarChart<>(xA,yA);bar.setTitle("🏫 Taux d'Occupation par Salle");bar.setLegendVisible(false);bar.setPrefHeight(280);
        XYChart.Series<String,Number> series=new XYChart.Series<>();taux.forEach((k,v)->series.getData().add(new XYChart.Data<>(k,v)));bar.getData().add(series);chartOccupationContainer.getChildren().setAll(bar);
    }

    // ── Carte ─────────────────────────────────────────────────────
    private void buildCarteSalles() {
        if(carteContainer==null)return;carteContainer.getChildren().clear();
        Map<String,Double> taux=rapportService.getTauxOccupation();List<Salle> salles=salleDAO.findAll();
        Label title=new Label("🗺️ Carte Interactive des Salles");title.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1e293b;-fx-padding:0 0 8 0;");
        HBox legend=new HBox(16);legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(makeChip("🟢 Normal (< 50%)","#dcfce7","#166534"),makeChip("🟡 Élevé (50–80%)","#fef9c3","#854d0e"),makeChip("🔴 Critique (> 80%)","#fee2e2","#991b1b"),makeChip("⚫ Indisponible","#f1f5f9","#475569"));
        Map<String,List<Salle>> parBatiment=new LinkedHashMap<>();
        for(Salle s:salles){String b=s.getBatimentNom()!=null?s.getBatimentNom():"Sans bâtiment";parBatiment.computeIfAbsent(b,x->new ArrayList<>()).add(s);}
        VBox allBuildings=new VBox(16);
        for(Map.Entry<String,List<Salle>> entry:parBatiment.entrySet()){
            VBox batBox=new VBox(8);batBox.setStyle("-fx-background-color:white;-fx-padding:14;-fx-background-radius:10;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");
            Label batLabel=new Label("🏢 "+entry.getKey());batLabel.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1e293b;");
            FlowPane sallesPane=new FlowPane(10,10);
            for(Salle s:entry.getValue()){
                double t=taux.getOrDefault(s.getNumero(),0.0);String bg,fg;
                if(!s.isDisponible()){bg="#f1f5f9";fg="#475569";}else if(t>=80){bg="#fee2e2";fg="#991b1b";}else if(t>=50){bg="#fef9c3";fg="#854d0e";}else{bg="#dcfce7";fg="#166634";}
                VBox card=new VBox(4);card.setAlignment(Pos.CENTER);card.setPadding(new Insets(10,14,10,14));card.setPrefWidth(110);
                card.setStyle("-fx-background-color:"+bg+";-fx-background-radius:8;-fx-border-color:"+fg+";-fx-border-radius:8;-fx-border-width:1.5;-fx-cursor:hand;");
                Label numLbl=new Label(s.getNumero());numLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:"+fg+";");
                Label typeLbl=new Label(s.getTypeSalle()+" | "+s.getCapacite()+"p");typeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#64748b;");
                Label tauxLbl=new Label(s.isDisponible()?t+"%":"Indisponible");tauxLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:"+fg+";");
                card.getChildren().addAll(numLbl,typeLbl,tauxLbl);
                Tooltip.install(card,new Tooltip(s.getNumero()+" — "+s.getTypeSalle()+"\nCapacité : "+s.getCapacite()+" places\nOccupation : "+t+"%\nStatut : "+(s.isDisponible()?"✅ Disponible":"🔒 Indisponible")));
                sallesPane.getChildren().add(card);
            }
            batBox.getChildren().addAll(batLabel,sallesPane);allBuildings.getChildren().add(batBox);
        }
        Label globalTaux=new Label(String.format("📊 Taux global : %.1f%%  |  Salles critiques : %d  |  Disponibles : %d",rapportService.getTauxOccupationGlobal(),rapportService.getSallesCritiques().size(),salleDAO.countDisponibles()));
        globalTaux.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;-fx-padding:4 0 0 0;");
        carteContainer.getChildren().addAll(title,legend,globalTaux,allBuildings);
    }
    private Label makeChip(String text,String bg,String fg){Label l=new Label(text);l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-padding:3 10;-fx-background-radius:8;-fx-font-size:11px;-fx-font-weight:bold;");return l;}

    // ── Calendrier semaine ────────────────────────────────────────
    private void buildCalendar() {
        if(calendarGrid==null)return;calendarGrid.getChildren().clear();calendarGrid.getColumnConstraints().clear();calendarGrid.getRowConstraints().clear();
        String[] jours={"Lundi","Mardi","Mercredi","Jeudi","Vendredi"};int[] heures={8,10,12,14,16};
        calendarGrid.getColumnConstraints().add(new ColumnConstraints(60));
        for(int j=0;j<jours.length;j++){ColumnConstraints cc=new ColumnConstraints();cc.setHgrow(Priority.ALWAYS);calendarGrid.getColumnConstraints().add(cc);}
        Label corner=new Label("Heure");corner.setStyle("-fx-font-weight:bold;-fx-background-color:#1e293b;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;");corner.setMaxWidth(Double.MAX_VALUE);calendarGrid.add(corner,0,0);
        for(int j=0;j<jours.length;j++){LocalDate d=calendarWeekStart.plusDays(j);Label lbl=new Label(jours[j]+"\n"+d.getDayOfMonth()+"/"+d.getMonthValue());lbl.setStyle("-fx-font-weight:bold;-fx-background-color:#1e293b;-fx-text-fill:white;-fx-padding:8;-fx-alignment:CENTER;-fx-font-size:11px;");lbl.setMaxWidth(Double.MAX_VALUE);GridPane.setHgrow(lbl,Priority.ALWAYS);calendarGrid.add(lbl,j+1,0);}
        Map<String,List<Cours>> par=new HashMap<>();for(String j:jours)par.put(j,new ArrayList<>());
        for(Cours c:coursList)if(c.getCreneauInfo()!=null)for(String j:jours)if(c.getCreneauInfo().startsWith(j)){par.get(j).add(c);break;}
        for(int h=0;h<heures.length;h++){
            Label hLbl=new Label(heures[h]+"h");hLbl.setStyle("-fx-background-color:#f1f5f9;-fx-font-weight:bold;-fx-padding:8 4;-fx-alignment:CENTER;-fx-font-size:11px;");hLbl.setMaxWidth(Double.MAX_VALUE);hLbl.setMaxHeight(Double.MAX_VALUE);calendarGrid.add(hLbl,0,h+1);
            for(int j=0;j<jours.length;j++){
                VBox cell=new VBox(2);cell.setPadding(new Insets(3));cell.setStyle("-fx-border-color:#e2e8f0;-fx-border-width:0.5;-fx-background-color:white;");cell.setMinHeight(60);
                for(Cours c:par.get(jours[j]))if(c.getCreneauInfo()!=null&&c.getCreneauInfo().contains(heures[h]+"h")){Label m=new Label("📚 "+(c.getMatiereNom()!=null?c.getMatiereNom():"?"));m.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-padding:2 6;-fx-background-radius:4;-fx-font-size:10px;");m.setWrapText(true);Label e=new Label("👤 "+(c.getEnseignantNom()!=null?c.getEnseignantNom():"?"));e.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");Label s=new Label("🏫 "+(c.getSalleNumero()!=null?c.getSalleNumero():"?"));s.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;");cell.getChildren().addAll(m,e,s);}
                calendarGrid.add(cell,j+1,h+1);
            }
        }
        if(calendarWeekLabel!=null)calendarWeekLabel.setText("Semaine du "+calendarWeekStart.getDayOfMonth()+"/"+calendarWeekStart.getMonthValue()+"/"+calendarWeekStart.getYear());
    }

    // ── Calendrier mois ───────────────────────────────────────────
    private void buildCalendarMois() {
        if(calendarGrid==null)return;calendarGrid.getChildren().clear();calendarGrid.getColumnConstraints().clear();calendarGrid.getRowConstraints().clear();
        String[] joursSemaine={"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        for(int i=0;i<7;i++){ColumnConstraints cc=new ColumnConstraints();cc.setHgrow(Priority.ALWAYS);cc.setPercentWidth(100.0/7);calendarGrid.getColumnConstraints().add(cc);}
        for(int i=0;i<7;i++){Label h=new Label(joursSemaine[i]);h.setMaxWidth(Double.MAX_VALUE);h.setAlignment(Pos.CENTER);h.setStyle("-fx-font-weight:bold;-fx-background-color:#1e293b;-fx-text-fill:white;-fx-padding:8;-fx-font-size:12px;");calendarGrid.add(h,i,0);}
        LocalDate premierJour=calendarMonth.atDay(1);int debutDecalage=premierJour.getDayOfWeek().getValue()-1;int nbJours=calendarMonth.lengthOfMonth();
        Map<LocalDate,List<Cours>> parDate=new HashMap<>();for(Cours c:coursList){if(c.getDate()!=null)parDate.computeIfAbsent(c.getDate(),x->new ArrayList<>()).add(c);}
        Map<LocalDate,List<Reservation>> reservParDate=new HashMap<>();for(Reservation r:historiqueList){if(r.getDateReservation()!=null&&"VALIDEE".equals(r.getStatut())){LocalDate d=r.getDateReservation().toLocalDate();reservParDate.computeIfAbsent(d,x->new ArrayList<>()).add(r);}}
        LocalDate today=LocalDate.now();int position=debutDecalage;
        for(int jour=1;jour<=nbJours;jour++){
            int col=position%7;int row=(position/7)+1;LocalDate date=calendarMonth.atDay(jour);
            List<Cours> cours=parDate.getOrDefault(date,Collections.emptyList());List<Reservation> reservs=reservParDate.getOrDefault(date,Collections.emptyList());
            VBox cellule=new VBox(3);cellule.setPadding(new Insets(4));cellule.setMinHeight(90);cellule.setMinWidth(80);
            boolean estAujourdhui=date.equals(today);boolean estWeekend=(col==5||col==6);
            String bgColor=estAujourdhui?"#dbeafe":estWeekend?"#f8fafc":"white";String borderColor=estAujourdhui?"#3b82f6":"#e2e8f0";
            cellule.setStyle("-fx-background-color:"+bgColor+";-fx-border-color:"+borderColor+";-fx-border-width:"+(estAujourdhui?"2":"0.5")+";");
            Label numJour=new Label(String.valueOf(jour));numJour.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:"+(estAujourdhui?"#1d4ed8":"#1e293b")+";");cellule.getChildren().add(numJour);
            int maxVisible=2;int total=cours.size()+reservs.size();int affiche=0;
            for(Cours c:cours){if(affiche>=maxVisible)break;Label lc=new Label("📚 "+(c.getMatiereNom()!=null?c.getMatiereNom():"Cours"));lc.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-padding:1 5;-fx-background-radius:3;-fx-font-size:9px;");lc.setMaxWidth(Double.MAX_VALUE);Tooltip.install(lc,new Tooltip("📚 "+c.getMatiereNom()+"\n👤 "+c.getEnseignantNom()+"\n🏫 "+c.getSalleNumero()+"\n⏰ "+c.getCreneauInfo()));cellule.getChildren().add(lc);affiche++;}
            for(Reservation r:reservs){if(affiche>=maxVisible)break;Label lr=new Label("📌 "+(r.getMotif()!=null?r.getMotif():"Réservation"));lr.setStyle("-fx-background-color:#10b981;-fx-text-fill:white;-fx-padding:1 5;-fx-background-radius:3;-fx-font-size:9px;");lr.setMaxWidth(Double.MAX_VALUE);Tooltip.install(lr,new Tooltip("📌 "+r.getMotif()+"\n🏫 "+r.getSalleNumero()+"\n👤 "+r.getUtilisateurNom()));cellule.getChildren().add(lr);affiche++;}
            if(total>maxVisible){Label plus=new Label("+"+(total-maxVisible)+" de plus");plus.setStyle("-fx-text-fill:#64748b;-fx-font-size:9px;-fx-font-style:italic;");cellule.getChildren().add(plus);}
            calendarGrid.add(cellule,col,row);position++;
        }
        if(calendarWeekLabel!=null){String moisNom=calendarMonth.getMonth().getDisplayName(TextStyle.FULL,Locale.FRENCH);calendarWeekLabel.setText(moisNom.substring(0,1).toUpperCase()+moisNom.substring(1)+" "+calendarMonth.getYear());}
    }

    @FXML private void handleVueMois()    { vueMoisActive=true;  styleBoutonVue(true);  buildCalendarMois(); }
    @FXML private void handleVueSemaine() { vueMoisActive=false; styleBoutonVue(false); buildCalendar(); }
    @FXML private void handlePrevWeek()   { if(vueMoisActive){calendarMonth=calendarMonth.minusMonths(1);buildCalendarMois();}else{calendarWeekStart=calendarWeekStart.minusWeeks(1);buildCalendar();} }
    @FXML private void handleNextWeek()   { if(vueMoisActive){calendarMonth=calendarMonth.plusMonths(1);buildCalendarMois();}else{calendarWeekStart=calendarWeekStart.plusWeeks(1);buildCalendar();} }
    @FXML private void handleTodayWeek()  { if(vueMoisActive){calendarMonth=YearMonth.now();buildCalendarMois();}else{calendarWeekStart=LocalDate.now().with(java.time.DayOfWeek.MONDAY);buildCalendar();} }
    private void styleBoutonVue(boolean moisActif) {
        String actif="-fx-background-color:#1e293b;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;";
        String inactif="-fx-background-color:#e2e8f0;-fx-text-fill:#1e293b;-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;";
        if(btnVueMois!=null)btnVueMois.setStyle(moisActif?actif:inactif);
        if(btnVueSemaine!=null)btnVueSemaine.setStyle(moisActif?inactif:actif);
    }

    // ── Conflit ───────────────────────────────────────────────────
    private void checkConflict() {
        if(conflitLabel==null)return;
        Salle salle=salleCombo.getValue();Creneau cren=creneauCombo.getValue();LocalDate date=datePicker.getValue();Utilisateur ens=enseignantCombo.getValue();
        if(salle==null||cren==null||date==null){conflitLabel.setVisible(false);return;}
        int excl=selectedCours!=null?selectedCours.getId():0;
        boolean cs=coursDAO.hasConflitSalle(salle.getId(),cren.getId(),date.toString(),excl);
        boolean ce=ens!=null&&coursDAO.hasConflitEnseignant(ens.getId(),cren.getId(),date.toString(),excl);
        if(cs){conflitLabel.setText("⚠ CONFLIT : Salle déjà occupée !");conflitLabel.setVisible(true);AlerteService.alerterConflit("Salle "+salle.getNumero()+" occupée le "+date+" créneau "+cren);}
        else if(ce){conflitLabel.setText("⚠ CONFLIT : Enseignant indisponible !");conflitLabel.setVisible(true);AlerteService.alerterConflit("Enseignant "+ens.getNomComplet()+" indisponible le "+date+" créneau "+cren);}
        else{conflitLabel.setVisible(false);}
    }

    // ── CRUD Cours ────────────────────────────────────────────────
    @FXML private void handleSaveCours() {
        if(matiereCombo.getValue()==null||enseignantCombo.getValue()==null||classeCombo.getValue()==null||creneauCombo.getValue()==null||salleCombo.getValue()==null||datePicker.getValue()==null){showError("Erreur","Tous les champs sont obligatoires.");return;}
        int excl=selectedCours!=null?selectedCours.getId():0;String date=datePicker.getValue().toString();
        if(coursDAO.hasConflitSalle(salleCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){showError("Conflit","La salle est déjà occupée sur ce créneau.");return;}
        if(coursDAO.hasConflitEnseignant(enseignantCombo.getValue().getId(),creneauCombo.getValue().getId(),date,excl)){showError("Conflit","L'enseignant est indisponible sur ce créneau.");return;}
        String ancienneSalle=selectedCours!=null?selectedCours.getSalleNumero():null;
        Cours c=selectedCours!=null?selectedCours:new Cours();
        c.setMatiereId(matiereCombo.getValue().getId());c.setEnseignantId(enseignantCombo.getValue().getId());c.setClasseId(classeCombo.getValue().getId());c.setCreneauId(creneauCombo.getValue().getId());c.setSalleId(salleCombo.getValue().getId());c.setDate(datePicker.getValue());c.setStatut(statutCombo.getValue()!=null?statutCombo.getValue():"PLANIFIE");
        coursDAO.save(c);
        if(ancienneSalle!=null&&!ancienneSalle.equals(salleCombo.getValue().getNumero())){Utilisateur ens=enseignantCombo.getValue();c.setSalleNumero(salleCombo.getValue().getNumero());c.setMatiereNom(matiereCombo.getValue().getNom());c.setClasseNom(classeCombo.getValue().getNom());c.setCreneauInfo(creneauCombo.getValue().toString());EmailService.notifierChangementSalle(ens,c,ancienneSalle);AlerteService.notifierUtilisateur(ens.getId(),"🔔 Votre cours "+matiereCombo.getValue().getNom()+" a changé de salle : "+ancienneSalle+" → "+salleCombo.getValue().getNumero(),"INFO");}
        loadData();buildCharts();if(vueMoisActive)buildCalendarMois();else buildCalendar();buildCarteSalles();clearForm();showInfo("Succès","Cours sauvegardé.");
    }
    @FXML private void handleDeleteCours() {
        if(selectedCours==null){showError("Erreur","Sélectionnez un cours.");return;}
        if(confirmDelete("ce cours")){coursDAO.delete(selectedCours.getId());loadData();buildCharts();if(vueMoisActive)buildCalendarMois();else buildCalendar();buildCarteSalles();clearForm();}
    }

    // ── Réservations ──────────────────────────────────────────────
    @FXML private void handleValiderReserv() {
        if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}
        reservDAO.updateStatut(selectedReserv.getId(),"VALIDEE");
        Notification n=new Notification();n.setMessage("✅ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été validée.");n.setType("INFO");n.setUtilisateurId(selectedReserv.getUtilisateurId());notifDAO.save(n);
        utilisateurDAO.findAll().stream().filter(u->u.getId()==selectedReserv.getUtilisateurId()).findFirst().ifPresent(u->EmailService.notifierValidationReservation(u,selectedReserv));
        loadData();showInfo("Succès","Réservation validée.");
    }
    @FXML private void handleRefuserReserv() {
        if(selectedReserv==null){showError("Erreur","Sélectionnez une réservation.");return;}
        reservDAO.updateStatut(selectedReserv.getId(),"REFUSEE");
        Notification n=new Notification();n.setMessage("❌ Votre réservation salle "+selectedReserv.getSalleNumero()+" a été refusée.");n.setType("ALERTE");n.setUtilisateurId(selectedReserv.getUtilisateurId());notifDAO.save(n);
        utilisateurDAO.findAll().stream().filter(u->u.getId()==selectedReserv.getUtilisateurId()).findFirst().ifPresent(u->EmailService.notifierRefusReservation(u,selectedReserv));
        loadData();showInfo("Fait","Réservation refusée.");
    }

    // ── Signalements ──────────────────────────────────────────────
    @FXML private void handleTraiterSignalement() {
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

    @FXML private void handleVoirDetailSignal() {
        if (selectedSignal == null) { showError("Erreur","Sélectionnez un signalement."); return; }
        Signalement s = selectedSignal;
        String couleurStatut;
        switch (s.getStatut()) {
            case "RESOLU":   couleurStatut = "#10b981"; break;
            case "EN_COURS": couleurStatut = "#f59e0b"; break;
            case "FERME":    couleurStatut = "#6366f1"; break;
            default:         couleurStatut = "#94a3b8"; break;
        }
        String[][] lignes = {
                {"📅 Date signalé",  s.getDateSignalement() != null ? s.getDateSignalement().toLocalDate().toString() : "—"},
                {"👤 Enseignant",    s.getEnseignantNom()   != null ? s.getEnseignantNom()   : "—"},
                {"🏫 Salle",         s.getSalleNumero()     != null ? s.getSalleNumero()     : "—"},
                {"🔖 Catégorie",     s.getCategorie()},
                {"⚡ Priorité",      s.getPrioriteIcon() + " " + s.getPriorite()},
                {"📌 Statut",        formatStatutSignal(s.getStatut())},
        };
        AlertePersonnalisee.afficherDetailSignalement(
                s.getId(), s.getCategorieIcon()+" "+s.getTitre(), lignes,
                s.getDescription(), s.getCommentaireAdmin(),
                s.getDateResolution()!=null?s.getDateResolution().toLocalDate().toString():null,
                couleurStatut);
    }

    private String formatStatutSignal(String statut) {
        if(statut==null)return "—";
        switch(statut){case "EN_ATTENTE":return "⏳ En attente";case "EN_COURS":return "🔄 En cours";case "RESOLU":return "✅ Résolu";case "FERME":return "🔒 Fermé";default:return statut;}
    }

    // ── Exports ───────────────────────────────────────────────────
    @FXML private void handleExportPDF()            { FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));fc.setInitialFileName("emploi_du_temps.pdf");File f=fc.showSaveDialog(null);if(f==null)return;try{exportService.exportCoursAsPDF(coursList,f);showInfo("Export PDF","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());} }
    @FXML private void handleExportExcel()          { FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel","*.xlsx"));fc.setInitialFileName("emploi_du_temps.xlsx");File f=fc.showSaveDialog(null);if(f==null)return;try{exportService.exportCoursAsExcel(coursList,f);showInfo("Export Excel","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());} }
    @FXML private void handleExportHistoriquePDF()  { FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));fc.setInitialFileName("historique_reservations.pdf");File f=fc.showSaveDialog(null);if(f==null)return;try{exportService.exportReservationsAsPDF(reservDAO.findAll(),f);showInfo("Export PDF","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());} }
    @FXML private void handleExportHistoriqueExcel(){ FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel","*.xlsx"));fc.setInitialFileName("historique_reservations.xlsx");File f=fc.showSaveDialog(null);if(f==null)return;try{exportService.exportReservationsAsExcel(reservDAO.findAll(),f);showInfo("Export Excel","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());} }
    @FXML private void handleExportOccupationExcel(){ FileChooser fc=new FileChooser();fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel","*.xlsx"));fc.setInitialFileName("occupation_salles.xlsx");File f=fc.showSaveDialog(null);if(f==null)return;try{exportService.exportOccupationAsExcel(rapportService.getTauxOccupation(),f);showInfo("Export Excel","Exporté : "+f.getName());}catch(Exception e){showError("Erreur",e.getMessage());} }

    // ── Rapports ──────────────────────────────────────────────────
    @FXML private void handleRapportHebdo() {
        Map<String,Object> r=rapportService.getRapportHebdomadaire();
        if(rapportTextArea!=null){StringBuilder sb=new StringBuilder();sb.append("══════════════════════════════════════\n       ").append(r.get("titre")).append("\n══════════════════════════════════════\n");r.forEach((k,v)->{if(!k.equals("titre")&&!k.equals("coursParJour")&&!k.equals("coursParStatut"))sb.append(String.format("  %-30s : %s%n",k,v));});sb.append("\n── Cours par jour ──\n");if(r.get("coursParJour")instanceof Map)((Map<?,?>)r.get("coursParJour")).forEach((k,v)->sb.append("  ").append(k).append(" : ").append(v).append("\n"));sb.append("\n── Cours par statut ──\n");if(r.get("coursParStatut")instanceof Map)((Map<?,?>)r.get("coursParStatut")).forEach((k,v)->sb.append("  ").append(k).append(" : ").append(v).append("\n"));rapportTextArea.setText(sb.toString());}
        buildOccupationChart();critList.setAll(rapportService.getSallesCritiques());
    }
    @FXML private void handleRapportMensuel() {
        Map<String,Object> r=rapportService.getRapportMensuel();
        if(rapportTextArea!=null){StringBuilder sb=new StringBuilder();sb.append("══════════════════════════════════════\n       ").append(r.get("titre")).append(" — ").append(r.get("mois")).append("\n══════════════════════════════════════\n");r.forEach((k,v)->{if(!k.equals("titre")&&!k.equals("mois")&&!k.equals("coursParJour")&&!k.equals("coursParStatut")&&!k.equals("tauxOccupationParSalle"))sb.append(String.format("  %-30s : %s%n",k,v));});sb.append("\n── Taux par salle ──\n");if(r.get("tauxOccupationParSalle")instanceof Map)((Map<?,?>)r.get("tauxOccupationParSalle")).forEach((k,v)->sb.append("  ").append(k).append(" : ").append(v).append("%\n"));rapportTextArea.setText(sb.toString());}
        buildOccupationChart();critList.setAll(rapportService.getSallesCritiques());
    }

    @FXML private void handleRafraichirCarte() { buildCarteSalles(); }
    @FXML private void handleClearCours()      { clearForm(); }
    @FXML private void handleLogout()          { logout(); }
    @FXML private void handleRefresh()         { loadData(); }

    private void fillForm(Cours c) {
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
    private void clearForm() {
        matiereCombo.setValue(null);enseignantCombo.setValue(null);classeCombo.setValue(null);
        creneauCombo.setValue(null);salleCombo.setValue(null);datePicker.setValue(null);
        statutCombo.setValue(null);selectedCours=null;
        if(conflitLabel !=null)conflitLabel.setVisible(false);
        if(salleAutoLabel!=null)salleAutoLabel.setVisible(false);
        coursFormTitle.setText("Nouveau Cours");
        coursTable.getSelectionModel().clearSelection();
    }
}