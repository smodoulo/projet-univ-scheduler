package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import com.univscheduler.model.AlertePersonnalisee;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.*;

public class AdminDashboardController extends BaseController {

    // ── Palette UNIV-SCHEDULER ────────────────────────────────────
    private static final String T_DARK   = "#1a5f6e";
    private static final String T_MID    = "#2a9cb0";
    private static final String T_LIGHT  = "#4ecdc4";
    private static final String GREEN    = "#3ecf8e";
    private static final String GOLD     = "#f0a500";
    private static final String RED      = "#e05c5c";
    private static final String PURPLE   = "#7c6fcf";
    private static final String MUTED    = "#9eb3bf";
    private static final String SECOND   = "#6b8394";

    // ── Couleurs donut Salles par Type ────────────────────────────
    private static final Map<String, String> COULEURS_TYPE = Map.of(
            "TD",    "#2a9cb0",   // teal mid
            "TP",    "#4ecdc4",   // teal light
            "AMPHI", "#1a5f6e"    // teal dark
    );

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private HBox  statCardsContainer;

    @FXML private TableView<Utilisateur>           userTable;
    @FXML private TableColumn<Utilisateur,Integer> colUserId;
    @FXML private TableColumn<Utilisateur,String>  colUserNom, colUserPrenom, colUserEmail, colUserRole;
    @FXML private TextField nomField, prenomField, emailField, passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label userFormTitle;

    @FXML private TableView<Salle>           salleTable;
    @FXML private TableColumn<Salle,Integer> colSalleId, colSalleCap;
    @FXML private TableColumn<Salle,String>  colSalleNum, colSalleType, colSalleBat;
    @FXML private TableColumn<Salle,Boolean> colSalleDispo;
    @FXML private TextField salleNumField, salleCapField;
    @FXML private ComboBox<String>   salleTypeCombo;
    @FXML private ComboBox<Batiment> salleBatCombo;
    @FXML private CheckBox salleDispoCheck;
    @FXML private Label salleFormTitle;

    @FXML private TableView<Batiment>           batTable;
    @FXML private TableColumn<Batiment,Integer> colBatId, colBatEtages;
    @FXML private TableColumn<Batiment,String>  colBatNom, colBatLoc;
    @FXML private TextField batNomField, batLocField, batEtagesField;
    @FXML private Label batFormTitle;

    @FXML private TableView<Equipement>           equipTable;
    @FXML private TableColumn<Equipement,Integer> colEquipId;
    @FXML private TableColumn<Equipement,String>  colEquipNom, colEquipType, colEquipEtat, colEquipSalle;
    @FXML private TextField equipNomField, equipDescField;
    @FXML private ComboBox<String> equipTypeCombo, equipEtatCombo;
    @FXML private ComboBox<Salle>  equipSalleCombo;
    @FXML private CheckBox equipDispoCheck;
    @FXML private Label equipFormTitle;

    @FXML private VBox chartUsersContainer, chartSallesContainer, chartCoursContainer;

    // ── DAO ───────────────────────────────────────────────────────
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final SalleDAO       salleDAO       = new SalleDAO();
    private final CoursDAO       coursDAO       = new CoursDAO();
    private final BatimentDAO    batimentDAO    = new BatimentDAO();
    private final EquipementDAO  equipementDAO  = new EquipementDAO();

    private final ObservableList<Utilisateur> userList  = FXCollections.observableArrayList();
    private final ObservableList<Salle>       salleList = FXCollections.observableArrayList();
    private final ObservableList<Batiment>    batList   = FXCollections.observableArrayList();
    private final ObservableList<Equipement>  equipList = FXCollections.observableArrayList();

    private Utilisateur selectedUser  = null;
    private Salle       selectedSalle = null;
    private Batiment    selectedBat   = null;
    private Equipement  selectedEquip = null;

    private long kpiUsers, kpiSalles, kpiCours;

    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupUserTable(); setupSalleTable(); setupBatTable(); setupEquipTable();
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN","GESTIONNAIRE","ENSEIGNANT","ETUDIANT"));
        salleTypeCombo.setItems(FXCollections.observableArrayList("TD","TP","AMPHI"));
        equipTypeCombo.setItems(FXCollections.observableArrayList("PROJECTEUR","TABLEAU","CLIM","ORDINATEUR","AUTRE"));
        equipEtatCombo.setItems(FXCollections.observableArrayList("BON","MOYEN","MAUVAIS","EN_PANNE"));
        loadAll();
        buildCharts();
    }

    // ════════════════════════════════════════════════════════════════
    //  5 CARTES KPI CLIQUABLES
    // ════════════════════════════════════════════════════════════════

    private void buildStatCards() {
        if (statCardsContainer == null) return;
        statCardsContainer.getChildren().clear();

        long nbAdmin = utilisateurDAO.countByRole("ADMIN");
        long nbGest  = utilisateurDAO.countByRole("GESTIONNAIRE");
        long nbEns   = utilisateurDAO.countByRole("ENSEIGNANT");
        long nbEtu   = utilisateurDAO.countByRole("ETUDIANT");

        List<Salle> salles = salleDAO.findAll();
        long nbTD    = salles.stream().filter(s -> "TD".equalsIgnoreCase(s.getTypeSalle())).count();
        long nbTP    = salles.stream().filter(s -> "TP".equalsIgnoreCase(s.getTypeSalle())).count();
        long nbAmphi = salles.stream().filter(s -> "AMPHI".equalsIgnoreCase(s.getTypeSalle())).count();

        Map<String, Long> dispoDetail = salleDAO.getDisponibiliteDetail();
        long nbLibres      = dispoDetail.getOrDefault("libres",      0L);
        long nbOccupees    = dispoDetail.getOrDefault("occupees",    0L);
        long nbMaintenance = dispoDetail.getOrDefault("maintenance", 0L);

        List<Cours> cours = coursDAO.findAll();
        long nbPlanif  = cours.stream().filter(c -> "PLANIFIE".equalsIgnoreCase(c.getStatut())).count();
        long nbRealise = cours.stream().filter(c -> "TERMINE".equalsIgnoreCase(c.getStatut())
                || "REALISE".equalsIgnoreCase(c.getStatut())).count();
        long nbAnnule  = cours.stream().filter(c -> "ANNULE".equalsIgnoreCase(c.getStatut())).count();
        String tauxAnnulation = kpiCours > 0
                ? String.format("%.0f%%", nbAnnule * 100.0 / kpiCours) : "—";

        List<Batiment> bats = batimentDAO.findAll();
        long kpiBats       = bats.size();
        long nbEtagesTotal = bats.stream().mapToLong(Batiment::getNombreEtages).sum();
        String moyParBat   = kpiBats > 0
                ? String.format("%.1f", (double) kpiSalles / kpiBats) : "—";

        List<Equipement> equips = equipementDAO.findAll();
        long kpiEquips    = equips.size();
        long nbEquipDispo = equips.stream().filter(Equipement::isDisponible).count();
        long nbEnPanne    = equips.stream().filter(e -> "EN_PANNE".equalsIgnoreCase(e.getEtat())).count();
        long nbProjecteur = equips.stream().filter(e -> "PROJECTEUR".equalsIgnoreCase(e.getTypeEquipement())).count();
        long nbTableau    = equips.stream().filter(e -> "TABLEAU".equalsIgnoreCase(e.getTypeEquipement())).count();
        long nbOrdinateur = equips.stream().filter(e -> "ORDINATEUR".equalsIgnoreCase(e.getTypeEquipement())).count();
        long nbAutre      = Math.max(0, kpiEquips - nbProjecteur - nbTableau - nbOrdinateur);

        statCardsContainer.getChildren().add(buildCard(
                "UTILISATEURS", String.valueOf(kpiUsers),
                "Admin · Gest. · Ens. · Etud.", "👥", T_MID,
                () -> showDetailPopup("👥 Utilisateurs — Détail par rôle", new String[][]{
                        {"👑 Administrateurs", String.valueOf(nbAdmin)},
                        {"📋 Gestionnaires",   String.valueOf(nbGest)},
                        {"👨 Enseignants",     String.valueOf(nbEns)},
                        {"🎓 Etudiants",       String.valueOf(nbEtu)},
                        {"📊 Total",           String.valueOf(kpiUsers)},
                })
        ));

        statCardsContainer.getChildren().add(buildCard(
                "SALLES", String.valueOf(kpiSalles),
                "Libres auj. : " + nbLibres + " / " + kpiSalles, "🏫", GREEN,
                () -> showDetailPopup("🏫 Salles — Disponibilité " + java.time.LocalDate.now(), new String[][]{
                        {"📊 Total salles",           String.valueOf(kpiSalles)},
                        {"✅ Libres aujourd'hui",     String.valueOf(nbLibres)},
                        {"📚 Occupées (cours actif)", String.valueOf(nbOccupees)},
                        {"🔧 En maintenance",         String.valueOf(nbMaintenance)},
                        {"📐 TD",                     String.valueOf(nbTD)},
                        {"🔬 TP",                     String.valueOf(nbTP)},
                        {"🎤 Amphithéatres",          String.valueOf(nbAmphi)},
                })
        ));

        statCardsContainer.getChildren().add(buildCard(
                "COURS PLANIFIES", String.valueOf(nbPlanif),
                "Sur " + kpiCours + " cours total", "📅", GOLD,
                () -> showDetailPopup("📅 Cours — Répartition par statut", new String[][]{
                        {"📅 Planifiés",       String.valueOf(nbPlanif)},
                        {"✅ Réalisés",        String.valueOf(nbRealise)},
                        {"❌ Annulés",         String.valueOf(nbAnnule)},
                        {"📊 Total",           String.valueOf(kpiCours)},
                        {"📉 Taux annulation", tauxAnnulation},
                })
        ));

        statCardsContainer.getChildren().add(buildCard(
                "BATIMENTS", String.valueOf(kpiBats),
                "Bâtiments du campus", "🏢", PURPLE,
                () -> showDetailPopup("🏢 Bâtiments", new String[][]{
                        {"📊 Total bâtiments",      String.valueOf(kpiBats)},
                        {"🏫 Total salles",         String.valueOf(kpiSalles)},
                        {"📐 Etages total",         String.valueOf(nbEtagesTotal)},
                        {"📍 Salles/bâtiment moy.", moyParBat},
                })
        ));

        statCardsContainer.getChildren().add(buildCard(
                "EQUIPEMENTS", String.valueOf(kpiEquips),
                "Matériel des salles", "🔧", RED,
                () -> showDetailPopup("🔧 Equipements", new String[][]{
                        {"📊 Total équipements", String.valueOf(kpiEquips)},
                        {"✅ Disponibles",       String.valueOf(nbEquipDispo)},
                        {"🔴 En panne",          String.valueOf(nbEnPanne)},
                        {"📽 Projecteurs",       String.valueOf(nbProjecteur)},
                        {"📋 Tableaux",          String.valueOf(nbTableau)},
                        {"💻 Ordinateurs",       String.valueOf(nbOrdinateur)},
                        {"📦 Autres",            String.valueOf(nbAutre)},
                })
        ));
    }

    private VBox buildCard(String label, String value, String subtitle,
                           String icon, String color, Runnable onClick) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 22, 20, 22));
        HBox.setHgrow(card, Priority.ALWAYS);
        String styleBase =
                "-fx-background-color:white;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + color + " transparent transparent transparent;" +
                        "-fx-border-width:4 0 0 0;" +
                        "-fx-border-radius:16;" +
                        "-fx-cursor:hand;";
        card.setStyle(styleBase);
        DropShadow shadow = new DropShadow(12, Color.color(0, 0, 0, 0.07));
        shadow.setOffsetY(3);
        card.setEffect(shadow);
        HBox topRow = new HBox(); topRow.setAlignment(Pos.CENTER_LEFT);
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + SECOND + ";");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:28px;-fx-opacity:0.18;");
        topRow.getChildren().addAll(labelLbl, spacer, iconLbl);
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size:42px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + MUTED + ";");
        card.getChildren().addAll(topRow, valueLbl, subtitleLbl);
        DropShadow hoverShadow = new DropShadow(20, Color.web(color, 0.18));
        hoverShadow.setOffsetY(5);
        card.setOnMouseEntered(e -> { card.setStyle(styleBase.replace("white","#f7fcfd")); card.setEffect(hoverShadow); });
        card.setOnMouseExited(e  -> { card.setStyle(styleBase); card.setEffect(shadow); });
        card.setOnMouseClicked(e -> { if (onClick != null) onClick.run(); });
        return card;
    }

    private void showDetailPopup(String titre, String[][] lignes) {
        AlertePersonnalisee.afficherDetailSignalement(0, titre, lignes, null, null, null, T_MID);
    }

    // ════════════════════════════════════════════════════════════════
    //  Setup Tables
    // ════════════════════════════════════════════════════════════════

    private void setupUserTable() {
        colUserId.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUserNom.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNom()));
        colUserPrenom.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getPrenom()));
        colUserEmail.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getEmail()));
        colUserRole.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getRole()));
        userTable.setItems(userList);
        userTable.getSelectionModel().selectedItemProperty().addListener((o,old,u)->{if(u!=null){selectedUser=u;fillUserForm(u);}});
    }
    private void setupSalleTable() {
        colSalleId.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colSalleNum.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNumero()));
        colSalleType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getTypeSalle()));
        colSalleCap.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        colSalleBat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getBatimentNom()!=null?d.getValue().getBatimentNom():""));
        colSalleDispo.setCellValueFactory(d->new SimpleBooleanProperty(d.getValue().isDisponible()));
        salleTable.setItems(salleList);
        salleTable.getSelectionModel().selectedItemProperty().addListener((o,old,s)->{if(s!=null){selectedSalle=s;fillSalleForm(s);}});
    }
    private void setupBatTable() {
        colBatId.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colBatNom.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNom()));
        colBatLoc.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getLocalisation()));
        colBatEtages.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getNombreEtages()).asObject());
        batTable.setItems(batList);
        batTable.getSelectionModel().selectedItemProperty().addListener((o,old,b)->{if(b!=null){selectedBat=b;fillBatForm(b);}});
    }
    private void setupEquipTable() {
        colEquipId.setCellValueFactory(d->new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colEquipNom.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNom()));
        colEquipType.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getTypeEquipement()));
        colEquipEtat.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getEtat()));
        colEquipSalle.setCellValueFactory(d->{
            String sn=equipSalleCombo.getItems().stream()
                    .filter(s->s.getId()==d.getValue().getSalleId())
                    .map(Salle::getNumero).findFirst()
                    .orElse(String.valueOf(d.getValue().getSalleId()));
            return new SimpleStringProperty(sn);
        });
        equipTable.setItems(equipList);
        equipTable.getSelectionModel().selectedItemProperty().addListener((o,old,e)->{if(e!=null){selectedEquip=e;fillEquipForm(e);}});
    }

    // ════════════════════════════════════════════════════════════════
    //  Data
    // ════════════════════════════════════════════════════════════════

    private void loadAll() {
        userList.setAll(utilisateurDAO.findAll());
        salleList.setAll(salleDAO.findAll());
        batList.setAll(batimentDAO.findAll());
        equipList.setAll(equipementDAO.findAll());
        salleBatCombo.setItems(FXCollections.observableArrayList(batimentDAO.findAll()));
        equipSalleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        kpiUsers  = userList.size();
        kpiSalles = salleList.size();
        kpiCours  = coursDAO.count();
        buildStatCards();
    }

    // ════════════════════════════════════════════════════════════════
    //  Charts — dont le donut Salles par Type style HR dashboard
    // ════════════════════════════════════════════════════════════════

    private void buildCharts() {
        buildChartUsers();
        buildChartSallesDonut();   // ✅ NOUVEAU donut
        buildChartCours();
    }

    // ── Graphe 1 : Utilisateurs par Rôle (BarChart) ───────────────
    private void buildChartUsers() {
        if (chartUsersContainer == null) return;
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
        xA.setLabel("Rôle"); yA.setLabel("Nombre");
        BarChart<String, Number> bar = new BarChart<>(xA, yA);
        bar.setTitle("👥 Utilisateurs par Rôle");
        bar.setLegendVisible(false);
        bar.setPrefHeight(220);
        bar.setStyle("-fx-background-color:transparent;");
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        utilisateurDAO.countByAllRoles().forEach((k, v) ->
                s.getData().add(new XYChart.Data<>(k, v)));
        bar.getData().add(s);
        bar.getData().get(0).getData().forEach(d -> {
            if (d.getNode() != null)
                d.getNode().setStyle("-fx-bar-fill:" + T_MID + ";-fx-background-radius:6 6 2 2;");
        });
        chartUsersContainer.getChildren().setAll(bar);
    }

    // ── Graphe 2 : Salles par Type — DONUT card style HR dashboard ─
    /**
     * ✅ Même technique que le donut enseignant :
     *   - PieChart plein + Circle blanc par-dessus = anneau
     *   - Centre interactif : % type dominant · nom · total
     *   - Légende GridPane 2 colonnes : dot · nom · nb · %
     *   - Hover tranche ET hover légende synchronisés
     *   - Palette teal UNIV-SCHEDULER
     */
    private void buildChartSallesDonut() {
        if (chartSallesContainer == null) return;
        chartSallesContainer.getChildren().clear();

        // ── Données ───────────────────────────────────────────────
        Map<String, Integer> typesRaw = salleDAO.countByType();
        // Convertir en LinkedHashMap<String,Integer> trié TD → TP → AMPHI
        Map<String, Integer> types = new LinkedHashMap<>();
        for (String key : new String[]{"TD", "TP", "AMPHI"}) {
            if (typesRaw.containsKey(key)) types.put(key, typesRaw.get(key).intValue());
        }
        // Ajouter les types inconnus éventuels
        typesRaw.forEach((k, v) -> { if (!types.containsKey(k)) types.put(k, v); });

        if (types.isEmpty()) {
            Label vide = new Label("Aucune donnée disponible");
            vide.setStyle("-fx-text-fill:#9eb3bf;-fx-font-style:italic;-fx-font-size:12px;");
            chartSallesContainer.getChildren().add(vide);
            return;
        }

        final int total = types.values().stream().mapToInt(Integer::intValue).sum();

        // ── Type dominant pour le centre par défaut ───────────────
        final String typeDominant = types.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        final int countDominant = types.getOrDefault(typeDominant, 0);
        final int pctDominant   = total > 0 ? Math.round(countDominant * 100f / total) : 0;
        final String nomDominant = typeDominant;

        // ── PieChart ──────────────────────────────────────────────
        PieChart pie = new PieChart();
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setStartAngle(90);
        pie.setPrefSize(180, 180);
        pie.setMinSize(180, 180);
        pie.setMaxSize(180, 180);
        pie.setStyle("-fx-background-color:transparent;");

        List<String> ordreTypes = new ArrayList<>();
        types.forEach((type, count) -> {
            pie.getData().add(new PieChart.Data("", count));
            ordreTypes.add(type);
        });

        // ── Labels centre ─────────────────────────────────────────
        Label centerPct = new Label(pctDominant + "%");
        centerPct.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1a5f6e;");
        Label centerNom = new Label(nomDominant);
        centerNom.setStyle("-fx-font-size:10px;-fx-text-fill:#6b8394;");
        Label centerTotal = new Label(total + " salles");
        centerTotal.setStyle("-fx-font-size:10px;-fx-text-fill:#9eb3bf;");

        VBox centerBox = new VBox(1, centerPct, centerNom, centerTotal);
        centerBox.setAlignment(Pos.CENTER);

        // ── Trou du donut ─────────────────────────────────────────
        Circle hole = new Circle(60, Color.WHITE);

        StackPane donutStack = new StackPane(pie, hole, centerBox);
        donutStack.setAlignment(Pos.CENTER);
        donutStack.setPrefSize(186, 186);
        donutStack.setMaxSize(186, 186);

        // ── Légende GridPane 2 colonnes ───────────────────────────
        GridPane legendGrid = new GridPane();
        legendGrid.setHgap(10);
        legendGrid.setVgap(8);
        legendGrid.setPadding(new Insets(10, 4, 4, 4));
        legendGrid.setStyle("-fx-border-color:#e8f4f4;-fx-border-width:1 0 0 0;");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(types.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            final String type    = entries.get(i).getKey();
            final int    count   = entries.get(i).getValue();
            final int    pct     = total > 0 ? Math.round(count * 100f / total) : 0;
            final String couleur = COULEURS_TYPE.getOrDefault(type, "#9eb3bf");

            Circle dot = new Circle(5, Color.web(couleur));
            Label lblNom = new Label(type);
            lblNom.setStyle("-fx-font-size:10px;-fx-text-fill:#6b8394;");
            HBox dotNom = new HBox(5, dot, lblNom);
            dotNom.setAlignment(Pos.CENTER_LEFT);
            Label lblNb  = new Label(String.valueOf(count));
            lblNb.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a5f6e;");
            Label lblPct = new Label(pct + "%");
            lblPct.setStyle("-fx-font-size:10px;-fx-text-fill:#9eb3bf;");

            VBox item = new VBox(2, dotNom, lblNb, lblPct);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-cursor:hand;-fx-padding:4 6;-fx-background-radius:6;");

            item.setOnMouseEntered(e -> {
                item.setStyle("-fx-cursor:hand;-fx-padding:4 6;-fx-background-radius:6;" +
                        "-fx-background-color:#f0f9fa;");
                centerPct.setText(pct + "%");
                centerNom.setText(type);
                centerTotal.setText(count + " salles");
            });
            item.setOnMouseExited(e -> {
                item.setStyle("-fx-cursor:hand;-fx-padding:4 6;-fx-background-radius:6;");
                centerPct.setText(pctDominant + "%");
                centerNom.setText(nomDominant);
                centerTotal.setText(total + " salles");
            });

            legendGrid.add(item, i % 2, i / 2);
        }

        // ── Couleurs + hover tranches ─────────────────────────────
        Platform.runLater(() -> {
            List<PieChart.Data> dataList = new ArrayList<>(pie.getData());
            for (int i = 0; i < dataList.size() && i < ordreTypes.size(); i++) {
                PieChart.Data data = dataList.get(i);
                if (data.getNode() == null) continue;
                final String type    = ordreTypes.get(i);
                final int    count   = types.getOrDefault(type, 0);
                final int    pct     = total > 0 ? Math.round(count * 100f / total) : 0;
                final String couleur = COULEURS_TYPE.getOrDefault(type, "#9eb3bf");

                data.getNode().setStyle("-fx-pie-color:" + couleur + ";");

                Tooltip tip = new Tooltip(type + "\n" + count + " salles · " + pct + "%");
                tip.setStyle(
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                                "-fx-background-color:" + couleur + ";-fx-text-fill:white;" +
                                "-fx-background-radius:6;-fx-padding:6 10;");
                Tooltip.install(data.getNode(), tip);

                data.getNode().setOnMouseEntered(e -> {
                    data.getNode().setStyle(
                            "-fx-pie-color:" + couleur +
                                    ";-fx-scale-x:1.04;-fx-scale-y:1.04;");
                    centerPct.setText(pct + "%");
                    centerNom.setText(type);
                    centerTotal.setText(count + " salles");
                });
                data.getNode().setOnMouseExited(e -> {
                    data.getNode().setStyle("-fx-pie-color:" + couleur + ";");
                    centerPct.setText(pctDominant + "%");
                    centerNom.setText(nomDominant);
                    centerTotal.setText(total + " salles");
                });
            }
        });

        // ── Titre + assemblage carte ──────────────────────────────
        Label titre = new Label("Types de Salles");
        titre.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a5f6e;");

        VBox card = new VBox(8, titre, donutStack, legendGrid);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(14, 12, 12, 12));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:#c0dde4;-fx-border-width:1;" +
                        "-fx-background-radius:12;-fx-border-radius:12;");
        card.setPrefWidth(220);
        card.setMaxWidth(220);

        chartSallesContainer.getChildren().add(card);
    }

    // ── Graphe 3 : Cours par Jour (BarChart) ─────────────────────
    private void buildChartCours() {
        if (chartCoursContainer == null) return;
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
        xA.setLabel("Jour"); yA.setLabel("Cours");
        BarChart<String, Number> bar = new BarChart<>(xA, yA);
        bar.setTitle("📅 Cours par Jour");
        bar.setLegendVisible(false);
        bar.setPrefHeight(260);
        bar.setStyle("-fx-background-color:transparent;");
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        coursDAO.countByJour().forEach((k, v) ->
                s.getData().add(new XYChart.Data<>(k, v)));
        bar.getData().add(s);
        bar.getData().get(0).getData().forEach(d -> {
            if (d.getNode() != null)
                d.getNode().setStyle("-fx-bar-fill:" + T_LIGHT + ";-fx-background-radius:6 6 2 2;");
        });
        chartCoursContainer.getChildren().setAll(bar);
    }

    // ════════════════════════════════════════════════════════════════
    //  CRUD Utilisateurs
    // ════════════════════════════════════════════════════════════════
    @FXML private void handleSaveUser() {
        String nom=nomField.getText().trim(),prenom=prenomField.getText().trim();
        String email=emailField.getText().trim(),password=passwordField.getText().trim();
        String role=roleCombo.getValue();
        if(nom.isEmpty()||prenom.isEmpty()||email.isEmpty()||password.isEmpty()||role==null){showError("Erreur","Tous les champs sont obligatoires.");return;}
        Utilisateur u = switch(role){
            case"ADMIN"->new Administrateur();case"GESTIONNAIRE"->new Gestionnaire();
            case"ENSEIGNANT"->new Enseignant();default->new Etudiant();};
        if(selectedUser!=null)u.setId(selectedUser.getId());
        u.setNom(nom);u.setPrenom(prenom);u.setEmail(email);u.setMotDePasse(password);u.setRole(role);
        utilisateurDAO.save(u);loadAll();buildCharts();clearUserForm();showInfo("Succès","Utilisateur sauvegardé.");
    }
    @FXML private void handleDeleteUser(){
        if(selectedUser==null){showError("Erreur","Sélectionnez un utilisateur.");return;}
        if(selectedUser.getId()==currentUser.getId()){showError("Erreur","Impossible de supprimer votre propre compte.");return;}
        if(confirmDelete(selectedUser.getNomComplet())){utilisateurDAO.delete(selectedUser.getId());loadAll();buildCharts();clearUserForm();}
    }
    @FXML private void handleClearUser(){clearUserForm();}
    private void fillUserForm(Utilisateur u){nomField.setText(u.getNom());prenomField.setText(u.getPrenom());emailField.setText(u.getEmail());passwordField.setText(u.getMotDePasse());roleCombo.setValue(u.getRole());userFormTitle.setText("Modifier Utilisateur");}
    private void clearUserForm(){nomField.clear();prenomField.clear();emailField.clear();passwordField.clear();roleCombo.setValue(null);selectedUser=null;userFormTitle.setText("Nouvel Utilisateur");userTable.getSelectionModel().clearSelection();}

    // ════════════════════════════════════════════════════════════════
    //  CRUD Salles
    // ════════════════════════════════════════════════════════════════
    @FXML private void handleSaveSalle(){
        String num=salleNumField.getText().trim();
        if(num.isEmpty()||salleTypeCombo.getValue()==null||salleBatCombo.getValue()==null){showError("Erreur","Numéro, type et bâtiment obligatoires.");return;}
        int cap=30;try{cap=Integer.parseInt(salleCapField.getText().trim());}catch(NumberFormatException ex){System.err.println("Capacité invalide");}
        Salle s=selectedSalle!=null?selectedSalle:new Salle();
        s.setNumero(num);s.setCapacite(cap);s.setTypeSalle(salleTypeCombo.getValue());s.setBatimentId(salleBatCombo.getValue().getId());s.setDisponible(salleDispoCheck.isSelected());
        salleDAO.save(s);loadAll();buildCharts();clearSalleForm();showInfo("Succès","Salle sauvegardée.");
    }
    @FXML private void handleDeleteSalle(){
        if(selectedSalle==null){showError("Erreur","Sélectionnez une salle.");return;}
        if(confirmDelete("la salle "+selectedSalle.getNumero())){salleDAO.delete(selectedSalle.getId());loadAll();buildCharts();clearSalleForm();}
    }
    @FXML private void handleClearSalle(){clearSalleForm();}
    private void fillSalleForm(Salle s){salleNumField.setText(s.getNumero());salleCapField.setText(String.valueOf(s.getCapacite()));salleTypeCombo.setValue(s.getTypeSalle());salleDispoCheck.setSelected(s.isDisponible());salleBatCombo.getItems().stream().filter(b->b.getId()==s.getBatimentId()).findFirst().ifPresent(salleBatCombo::setValue);salleFormTitle.setText("Modifier Salle");}
    private void clearSalleForm(){salleNumField.clear();salleCapField.clear();salleTypeCombo.setValue(null);salleBatCombo.setValue(null);salleDispoCheck.setSelected(true);selectedSalle=null;salleFormTitle.setText("Nouvelle Salle");salleTable.getSelectionModel().clearSelection();}

    // ════════════════════════════════════════════════════════════════
    //  CRUD Bâtiments
    // ════════════════════════════════════════════════════════════════
    @FXML private void handleSaveBat(){
        String nom=batNomField.getText().trim(),loc=batLocField.getText().trim();
        if(nom.isEmpty()){showError("Erreur","Nom obligatoire.");return;}
        int et=1;try{et=Integer.parseInt(batEtagesField.getText().trim());}catch(NumberFormatException ex){System.err.println("Etages invalide");}
        Batiment b=selectedBat!=null?selectedBat:new Batiment();b.setNom(nom);b.setLocalisation(loc);b.setNombreEtages(et);
        batimentDAO.save(b);loadAll();clearBatForm();showInfo("Succès","Bâtiment sauvegardé.");
    }
    @FXML private void handleDeleteBat(){if(selectedBat==null){showError("Erreur","Sélectionnez un bâtiment.");return;}if(confirmDelete(selectedBat.getNom())){batimentDAO.delete(selectedBat.getId());loadAll();clearBatForm();}}
    @FXML private void handleClearBat(){clearBatForm();}
    private void fillBatForm(Batiment b){batNomField.setText(b.getNom());batLocField.setText(b.getLocalisation());batEtagesField.setText(String.valueOf(b.getNombreEtages()));batFormTitle.setText("Modifier Bâtiment");}
    private void clearBatForm(){batNomField.clear();batLocField.clear();batEtagesField.clear();selectedBat=null;batFormTitle.setText("Nouveau Bâtiment");batTable.getSelectionModel().clearSelection();}

    // ════════════════════════════════════════════════════════════════
    //  CRUD Equipements
    // ════════════════════════════════════════════════════════════════
    @FXML private void handleSaveEquip(){
        String nom=equipNomField.getText().trim();
        if(nom.isEmpty()||equipTypeCombo.getValue()==null||equipSalleCombo.getValue()==null){showError("Erreur","Nom, type et salle obligatoires.");return;}
        Equipement e=selectedEquip!=null?selectedEquip:new Equipement();
        e.setNom(nom);e.setDescription(equipDescField.getText().trim());e.setTypeEquipement(equipTypeCombo.getValue());e.setEtat(equipEtatCombo.getValue()!=null?equipEtatCombo.getValue():"BON");e.setSalleId(equipSalleCombo.getValue().getId());e.setDisponible(equipDispoCheck.isSelected());
        equipementDAO.save(e);loadAll();clearEquipForm();showInfo("Succès","Equipement sauvegardé.");
    }
    @FXML private void handleDeleteEquip(){if(selectedEquip==null){showError("Erreur","Sélectionnez un équipement.");return;}if(confirmDelete(selectedEquip.getNom())){equipementDAO.delete(selectedEquip.getId());loadAll();clearEquipForm();}}
    @FXML private void handleClearEquip(){clearEquipForm();}
    private void fillEquipForm(Equipement e){equipNomField.setText(e.getNom());equipDescField.setText(e.getDescription()!=null?e.getDescription():"");equipTypeCombo.setValue(e.getTypeEquipement());equipEtatCombo.setValue(e.getEtat());equipDispoCheck.setSelected(e.isDisponible());equipSalleCombo.getItems().stream().filter(s->s.getId()==e.getSalleId()).findFirst().ifPresent(equipSalleCombo::setValue);equipFormTitle.setText("Modifier Equipement");}
    private void clearEquipForm(){equipNomField.clear();equipDescField.clear();equipTypeCombo.setValue(null);equipEtatCombo.setValue(null);equipSalleCombo.setValue(null);equipDispoCheck.setSelected(true);selectedEquip=null;equipFormTitle.setText("Nouvel Equipement");equipTable.getSelectionModel().clearSelection();}

    @FXML private void handleRefresh() { loadAll(); buildCharts(); }
    @FXML private void handleLogout()  { logout(); }
    @FXML protected void openChatbot() { AlertePersonnalisee.ouvrirChatbot(currentUser.getNomComplet()); }
}