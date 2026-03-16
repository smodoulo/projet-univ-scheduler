package com.univscheduler.controller;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Map;

public class AdminDashboardController extends BaseController {
    @FXML private Label welcomeLabel;
    @FXML private Label totalUsersLabel, totalSallesLabel, totalCoursLabel, totalEnsLabel;
    @FXML private Label sallesDispoLabel;

    @FXML private TableView<Utilisateur> userTable;
    @FXML private TableColumn<Utilisateur, Integer> colUserId;
    @FXML private TableColumn<Utilisateur, String> colUserNom, colUserPrenom, colUserEmail, colUserRole;
    @FXML private TextField nomField, prenomField, emailField, passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label userFormTitle;

    @FXML private TableView<Salle> salleTable;
    @FXML private TableColumn<Salle, Integer> colSalleId, colSalleCap;
    @FXML private TableColumn<Salle, String> colSalleNum, colSalleType, colSalleBat;
    @FXML private TableColumn<Salle, Boolean> colSalleDispo;
    @FXML private TextField salleNumField, sallecapField;
    @FXML private ComboBox<String> salleTypeCombo;
    @FXML private ComboBox<Batiment> salleBatCombo;
    @FXML private CheckBox salleDispoCheck;
    @FXML private Label salleFormTitle;

    @FXML private TableView<Batiment> batTable;
    @FXML private TableColumn<Batiment, Integer> colBatId, colBatEtages;
    @FXML private TableColumn<Batiment, String> colBatNom, colBatLoc;
    @FXML private TextField batNomField, batLocField, batEtagesField;
    @FXML private Label batFormTitle;

    @FXML private TableView<Equipement> equipTable;
    @FXML private TableColumn<Equipement, Integer> colEquipId;
    @FXML private TableColumn<Equipement, String> colEquipNom, colEquipType, colEquipEtat, colEquipSalle;
    @FXML private TextField equipNomField, equipDescField;
    @FXML private ComboBox<String> equipTypeCombo, equipEtatCombo;
    @FXML private ComboBox<Salle> equipSalleCombo;
    @FXML private CheckBox equipDispoCheck;
    @FXML private Label equipFormTitle;

    @FXML private VBox chartUsersContainer, chartSallesContainer, chartCoursContainer;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final CoursDAO coursDAO = new CoursDAO();
    private final BatimentDAO batimentDAO = new BatimentDAO();
    private final EquipementDAO equipementDAO = new EquipementDAO();

    private final ObservableList<Utilisateur> userList = FXCollections.observableArrayList();
    private final ObservableList<Salle> salleList = FXCollections.observableArrayList();
    private final ObservableList<Batiment> batList = FXCollections.observableArrayList();
    private final ObservableList<Equipement> equipList = FXCollections.observableArrayList();

    private Utilisateur selectedUser = null;
    private Salle selectedSalle = null;
    private Batiment selectedBat = null;
    private Equipement selectedEquip = null;

    @Override
    protected void onUserLoaded() {
        welcomeLabel.setText("Bonjour, " + currentUser.getNomComplet());
        setupUserTable(); setupSalleTable(); setupBatTable(); setupEquipTable();
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN","GESTIONNAIRE","ENSEIGNANT","ETUDIANT"));
        salleTypeCombo.setItems(FXCollections.observableArrayList("TD","TP","AMPHI"));
        equipTypeCombo.setItems(FXCollections.observableArrayList("PROJECTEUR","TABLEAU","CLIM","ORDINATEUR","AUTRE"));
        equipEtatCombo.setItems(FXCollections.observableArrayList("BON","MOYEN","MAUVAIS","EN_PANNE"));
        loadAll(); buildCharts();
    }

    private void setupUserTable() {
        colUserId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUserNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colUserPrenom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrenom()));
        colUserEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colUserRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        userTable.setItems(userList);
        userTable.getSelectionModel().selectedItemProperty().addListener((o,old,u) -> {
            if(u!=null){ selectedUser=u; fillUserForm(u); }
        });
    }

    private void setupSalleTable() {
        colSalleId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colSalleNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumero()));
        colSalleType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSalle()));
        colSalleCap.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCapacite()).asObject());
        colSalleBat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatimentNom()!=null?d.getValue().getBatimentNom():""));
        colSalleDispo.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isDisponible()));
        salleTable.setItems(salleList);
        salleTable.getSelectionModel().selectedItemProperty().addListener((o,old,s) -> {
            if(s!=null){ selectedSalle=s; fillSalleForm(s); }
        });
    }

    private void setupBatTable() {
        colBatId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colBatNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colBatLoc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocalisation()));
        colBatEtages.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNombreEtages()).asObject());
        batTable.setItems(batList);
        batTable.getSelectionModel().selectedItemProperty().addListener((o,old,b) -> {
            if(b!=null){ selectedBat=b; fillBatForm(b); }
        });
    }

    private void setupEquipTable() {
        colEquipId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colEquipNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colEquipType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeEquipement()));
        colEquipEtat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEtat()));
        colEquipSalle.setCellValueFactory(d -> {
            String sn = equipSalleCombo.getItems().stream()
                .filter(s -> s.getId()==d.getValue().getSalleId())
                .map(Salle::getNumero).findFirst().orElse(""+d.getValue().getSalleId());
            return new SimpleStringProperty(sn);
        });
        equipTable.setItems(equipList);
        equipTable.getSelectionModel().selectedItemProperty().addListener((o,old,e) -> {
            if(e!=null){ selectedEquip=e; fillEquipForm(e); }
        });
    }

    private void loadAll() {
        userList.setAll(utilisateurDAO.findAll());
        salleList.setAll(salleDAO.findAll());
        batList.setAll(batimentDAO.findAll());
        equipList.setAll(equipementDAO.findAll());
        salleBatCombo.setItems(FXCollections.observableArrayList(batimentDAO.findAll()));
        equipSalleCombo.setItems(FXCollections.observableArrayList(salleDAO.findAll()));
        totalUsersLabel.setText(String.valueOf(userList.size()));
        totalSallesLabel.setText(String.valueOf(salleList.size()));
        totalCoursLabel.setText(String.valueOf(coursDAO.count()));
        totalEnsLabel.setText(String.valueOf(utilisateurDAO.countByRole("ENSEIGNANT")));
        if (sallesDispoLabel != null) sallesDispoLabel.setText(String.valueOf(salleDAO.countDisponibles()));
    }

    private void buildCharts() {
        if (chartUsersContainer != null) {
            CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
            xA.setLabel("Rôle"); yA.setLabel("Nombre");
            BarChart<String,Number> bar = new BarChart<>(xA,yA);
            bar.setTitle("👥 Utilisateurs par Rôle"); bar.setLegendVisible(false); bar.setPrefHeight(220);
            XYChart.Series<String,Number> series = new XYChart.Series<>();
            utilisateurDAO.countByAllRoles().forEach((k,v) -> series.getData().add(new XYChart.Data<>(k,v)));
            bar.getData().add(series);
            chartUsersContainer.getChildren().setAll(bar);
        }
        if (chartSallesContainer != null) {
            PieChart pie = new PieChart(); pie.setTitle("🏫 Salles par Type"); pie.setPrefHeight(220);
            salleDAO.countByType().forEach((k,v) -> pie.getData().add(new PieChart.Data(k+" ("+v+")",v)));
            chartSallesContainer.getChildren().setAll(pie);
        }
        if (chartCoursContainer != null) {
            CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis();
            xA.setLabel("Jour"); yA.setLabel("Cours");
            BarChart<String,Number> bar = new BarChart<>(xA,yA);
            bar.setTitle("📅 Cours par Jour"); bar.setLegendVisible(false); bar.setPrefHeight(220);
            XYChart.Series<String,Number> series = new XYChart.Series<>();
            coursDAO.countByJour().forEach((k,v) -> series.getData().add(new XYChart.Data<>(k,v)));
            bar.getData().add(series);
            chartCoursContainer.getChildren().setAll(bar);
        }
    }

    // === User CRUD ===
    @FXML private void handleSaveUser() {
        String nom=nomField.getText().trim(), prenom=prenomField.getText().trim();
        String email=emailField.getText().trim(), password=passwordField.getText().trim();
        String role=roleCombo.getValue();
        if(nom.isEmpty()||prenom.isEmpty()||email.isEmpty()||password.isEmpty()||role==null){
            showError("Erreur","Tous les champs sont obligatoires."); return;
        }
        Utilisateur u;
        switch(role){
            case "ADMIN": u=new Administrateur(); break;
            case "GESTIONNAIRE": u=new Gestionnaire(); break;
            case "ENSEIGNANT": u=new Enseignant(); break;
            default: u=new Etudiant(); break;
        }
        if(selectedUser!=null) u.setId(selectedUser.getId());
        u.setNom(nom);u.setPrenom(prenom);u.setEmail(email);u.setMotDePasse(password);u.setRole(role);
        utilisateurDAO.save(u); loadAll(); buildCharts(); clearUserForm();
        showInfo("Succès","Utilisateur sauvegardé.");
    }
    @FXML private void handleDeleteUser() {
        if(selectedUser==null){ showError("Erreur","Sélectionnez un utilisateur."); return; }
        if(selectedUser.getId()==currentUser.getId()){ showError("Erreur","Impossible de se supprimer."); return; }
        if(confirmDelete(selectedUser.getNomComplet())){ utilisateurDAO.delete(selectedUser.getId()); loadAll(); buildCharts(); clearUserForm(); }
    }
    @FXML private void handleClearUser() { clearUserForm(); }
    private void fillUserForm(Utilisateur u) {
        nomField.setText(u.getNom()); prenomField.setText(u.getPrenom());
        emailField.setText(u.getEmail()); passwordField.setText(u.getMotDePasse());
        roleCombo.setValue(u.getRole()); userFormTitle.setText("Modifier Utilisateur");
    }
    private void clearUserForm() {
        nomField.clear(); prenomField.clear(); emailField.clear(); passwordField.clear();
        roleCombo.setValue(null); selectedUser=null; userFormTitle.setText("Nouvel Utilisateur");
        userTable.getSelectionModel().clearSelection();
    }

    // === Salle CRUD ===
    @FXML private void handleSaveSalle() {
        String num=salleNumField.getText().trim();
        if(num.isEmpty()||salleTypeCombo.getValue()==null||salleBatCombo.getValue()==null){
            showError("Erreur","Numéro, type et bâtiment obligatoires."); return;
        }
        int cap=30; try{ cap=Integer.parseInt(sallecapField.getText().trim()); }catch(Exception e){}
        Salle s=selectedSalle!=null?selectedSalle:new Salle();
        s.setNumero(num); s.setCapacite(cap); s.setTypeSalle(salleTypeCombo.getValue());
        s.setBatimentId(salleBatCombo.getValue().getId()); s.setDisponible(salleDispoCheck.isSelected());
        salleDAO.save(s); loadAll(); buildCharts(); clearSalleForm();
        showInfo("Succès","Salle sauvegardée.");
    }
    @FXML private void handleDeleteSalle() {
        if(selectedSalle==null){ showError("Erreur","Sélectionnez une salle."); return; }
        if(confirmDelete("la salle "+selectedSalle.getNumero())){ salleDAO.delete(selectedSalle.getId()); loadAll(); buildCharts(); clearSalleForm(); }
    }
    @FXML private void handleClearSalle() { clearSalleForm(); }
    private void fillSalleForm(Salle s) {
        salleNumField.setText(s.getNumero()); sallecapField.setText(String.valueOf(s.getCapacite()));
        salleTypeCombo.setValue(s.getTypeSalle()); salleDispoCheck.setSelected(s.isDisponible());
        salleBatCombo.getItems().stream().filter(b->b.getId()==s.getBatimentId()).findFirst().ifPresent(salleBatCombo::setValue);
        salleFormTitle.setText("Modifier Salle");
    }
    private void clearSalleForm() {
        salleNumField.clear(); sallecapField.clear(); salleTypeCombo.setValue(null);
        salleBatCombo.setValue(null); salleDispoCheck.setSelected(true);
        selectedSalle=null; salleFormTitle.setText("Nouvelle Salle");
        salleTable.getSelectionModel().clearSelection();
    }

    // === Bâtiment CRUD ===
    @FXML private void handleSaveBat() {
        String nom=batNomField.getText().trim(), loc=batLocField.getText().trim();
        if(nom.isEmpty()){ showError("Erreur","Nom obligatoire."); return; }
        int etages=1; try{ etages=Integer.parseInt(batEtagesField.getText().trim()); }catch(Exception e){}
        Batiment b=selectedBat!=null?selectedBat:new Batiment();
        b.setNom(nom); b.setLocalisation(loc); b.setNombreEtages(etages);
        batimentDAO.save(b); loadAll(); clearBatForm();
        showInfo("Succès","Bâtiment sauvegardé.");
    }
    @FXML private void handleDeleteBat() {
        if(selectedBat==null){ showError("Erreur","Sélectionnez un bâtiment."); return; }
        if(confirmDelete(selectedBat.getNom())){ batimentDAO.delete(selectedBat.getId()); loadAll(); clearBatForm(); }
    }
    @FXML private void handleClearBat() { clearBatForm(); }
    private void fillBatForm(Batiment b) {
        batNomField.setText(b.getNom()); batLocField.setText(b.getLocalisation());
        batEtagesField.setText(String.valueOf(b.getNombreEtages())); batFormTitle.setText("Modifier Bâtiment");
    }
    private void clearBatForm() {
        batNomField.clear(); batLocField.clear(); batEtagesField.clear();
        selectedBat=null; batFormTitle.setText("Nouveau Bâtiment");
        batTable.getSelectionModel().clearSelection();
    }

    // === Equipement CRUD ===
    @FXML private void handleSaveEquip() {
        String nom=equipNomField.getText().trim();
        if(nom.isEmpty()||equipTypeCombo.getValue()==null||equipSalleCombo.getValue()==null){
            showError("Erreur","Nom, type et salle obligatoires."); return;
        }
        Equipement e=selectedEquip!=null?selectedEquip:new Equipement();
        e.setNom(nom); e.setDescription(equipDescField.getText().trim());
        e.setTypeEquipement(equipTypeCombo.getValue());
        e.setEtat(equipEtatCombo.getValue()!=null?equipEtatCombo.getValue():"BON");
        e.setSalleId(equipSalleCombo.getValue().getId()); e.setDisponible(equipDispoCheck.isSelected());
        equipementDAO.save(e); loadAll(); clearEquipForm();
        showInfo("Succès","Équipement sauvegardé.");
    }
    @FXML private void handleDeleteEquip() {
        if(selectedEquip==null){ showError("Erreur","Sélectionnez un équipement."); return; }
        if(confirmDelete(selectedEquip.getNom())){ equipementDAO.delete(selectedEquip.getId()); loadAll(); clearEquipForm(); }
    }
    @FXML private void handleClearEquip() { clearEquipForm(); }
    private void fillEquipForm(Equipement e) {
        equipNomField.setText(e.getNom()); equipDescField.setText(e.getDescription()!=null?e.getDescription():"");
        equipTypeCombo.setValue(e.getTypeEquipement()); equipEtatCombo.setValue(e.getEtat());
        equipDispoCheck.setSelected(e.isDisponible());
        equipSalleCombo.getItems().stream().filter(s->s.getId()==e.getSalleId()).findFirst().ifPresent(equipSalleCombo::setValue);
        equipFormTitle.setText("Modifier Équipement");
    }
    private void clearEquipForm() {
        equipNomField.clear(); equipDescField.clear(); equipTypeCombo.setValue(null);
        equipEtatCombo.setValue(null); equipSalleCombo.setValue(null); equipDispoCheck.setSelected(true);
        selectedEquip=null; equipFormTitle.setText("Nouvel Équipement");
        equipTable.getSelectionModel().clearSelection();
    }

    @FXML private void handleRefresh() { loadAll(); buildCharts(); }
    @FXML private void handleLogout()  { logout(); }
}
