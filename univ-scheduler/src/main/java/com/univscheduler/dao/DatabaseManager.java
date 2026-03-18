package com.univscheduler.dao;
import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/univ_scheduler"
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "modoulo328";

    public DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            stmt.execute("CREATE TABLE IF NOT EXISTS utilisateurs ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "nom TEXT NOT NULL,"
                    + "prenom TEXT NOT NULL,"
                    + "email VARCHAR(255) UNIQUE NOT NULL,"
                    + "mot_de_passe TEXT NOT NULL,"
                    + "role TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS etudiants ("
                    + "id INT PRIMARY KEY,"
                    + "INE TEXT,"
                    + "niveau TEXT,"
                    + "FOREIGN KEY(id) REFERENCES utilisateurs(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS batiments ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "nom TEXT NOT NULL,"
                    + "localisation TEXT,"
                    + "nombre_etages INT DEFAULT 1)");

            stmt.execute("CREATE TABLE IF NOT EXISTS salles ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "numero VARCHAR(50) NOT NULL,"
                    + "capacite INT DEFAULT 30,"
                    + "type_salle VARCHAR(20) DEFAULT 'TD',"
                    + "disponible TINYINT(1) DEFAULT 1,"
                    + "batiment_id INT,"
                    + "FOREIGN KEY(batiment_id) REFERENCES batiments(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS equipements ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "nom VARCHAR(100) NOT NULL,"
                    + "description TEXT,"
                    + "etat VARCHAR(20) DEFAULT 'BON',"
                    + "type_equipement VARCHAR(50),"
                    + "disponible TINYINT(1) DEFAULT 1,"
                    + "salle_id INT,"
                    + "FOREIGN KEY(salle_id) REFERENCES salles(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS matieres ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "nom TEXT NOT NULL,"
                    + "description TEXT,"
                    + "volume_cm INT DEFAULT 0,"
                    + "volume_td INT DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS classes_pedago ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "nom VARCHAR(100) NOT NULL,"
                    + "niveau TEXT,"
                    + "effectif INT DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS creneaux ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "jour TEXT NOT NULL,"
                    + "heure_debut INT NOT NULL,"
                    + "duree INT DEFAULT 2)");

            stmt.execute("CREATE TABLE IF NOT EXISTS cours ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "statut VARCHAR(20) DEFAULT 'PLANIFIE',"
                    + "date TEXT,"
                    + "matiere_id INT,"
                    + "enseignant_id INT,"
                    + "classe_id INT,"
                    + "creneau_id INT,"
                    + "salle_id INT,"
                    + "FOREIGN KEY(matiere_id) REFERENCES matieres(id),"
                    + "FOREIGN KEY(enseignant_id) REFERENCES utilisateurs(id),"
                    + "FOREIGN KEY(classe_id) REFERENCES classes_pedago(id),"
                    + "FOREIGN KEY(creneau_id) REFERENCES creneaux(id),"
                    + "FOREIGN KEY(salle_id) REFERENCES salles(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS reservations ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "motif TEXT,"
                    + "statut VARCHAR(20) DEFAULT 'EN_ATTENTE',"
                    + "date_reservation TEXT,"
                    + "salle_id INT,"
                    + "utilisateur_id INT,"
                    + "FOREIGN KEY(salle_id) REFERENCES salles(id),"
                    + "FOREIGN KEY(utilisateur_id) REFERENCES utilisateurs(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS notifications ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "message TEXT NOT NULL,"
                    + "date_envoi TEXT,"
                    + "lu TINYINT(1) DEFAULT 0,"
                    + "type VARCHAR(20) DEFAULT 'INFO',"
                    + "utilisateur_id INT,"
                    + "FOREIGN KEY(utilisateur_id) REFERENCES utilisateurs(id))");

            insertDemoData(conn);
            System.out.println("DB initialisee avec succes.");
        } catch (SQLException e) {
            System.err.println("Erreur DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertDemoData(Connection conn) throws SQLException {
        String[][] users = {
                {"Diallo",  "Moussa",       "moussa.diallo@univ-thies.sn",        "admin123", "ADMIN"},
                {"Ndiaye",  "Fatou",        "fatou.ndiaye@univ-thies.sn",         "gest123",  "GESTIONNAIRE"},
                {"Sarr",    "Aminata",      "aminata.sarr@univ-thies.sn",         "gest456",  "GESTIONNAIRE"},
                {"Fall",    "Ibrahima",     "ibrahima.fall@univ-thies.sn",        "ens123",   "ENSEIGNANT"},
                {"Sow",     "Mariama",      "mariama.sow@univ-thies.sn",          "ens456",   "ENSEIGNANT"},
                {"Diop",    "Cheikh",       "cheikh.diop@univ-thies.sn",          "ens789",   "ENSEIGNANT"},
                {"Mbaye",   "Rokhaya",      "rokhaya.mbaye@univ-thies.sn",        "ens321",   "ENSEIGNANT"},
                {"Gueye",   "Pape Demba",   "papedemba.gueye@etu.univ-thies.sn",  "etu001",   "ETUDIANT"},
                {"Faye",    "Ndeye Coumba", "ndeye.faye@etu.univ-thies.sn",       "etu002",   "ETUDIANT"},
                {"Badji",   "Ousmane",      "ousmane.badji@etu.univ-thies.sn",    "etu003",   "ETUDIANT"},
                {"Cisse",   "Aissatou",     "aissatou.cisse@etu.univ-thies.sn",   "etu004",   "ETUDIANT"},
                {"Niang",   "Babacar",      "babacar.niang@etu.univ-thies.sn",    "etu005",   "ETUDIANT"},
                {"Toure",   "Mareme",       "mareme.toure@etu.univ-thies.sn",     "etu006",   "ETUDIANT"},
                {"Diouf",   "Serigne",      "serigne.diouf@etu.univ-thies.sn",    "etu007",   "ETUDIANT"},
                {"Thiaw",   "Khadija",      "khadija.thiaw@etu.univ-thies.sn",    "etu008",   "ETUDIANT"},
                {"Wade",    "Abdoulaye",    "abdoulaye.wade@etu.univ-thies.sn",   "etu009",   "ETUDIANT"},
                {"Samb",    "Sokhna",       "sokhna.samb@etu.univ-thies.sn",      "etu010",   "ETUDIANT"},
        };

        int[] uids = new int[users.length];
        PreparedStatement chk = conn.prepareStatement("SELECT id FROM utilisateurs WHERE email=?");
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO utilisateurs (nom,prenom,email,mot_de_passe,role) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < users.length; i++) {
            chk.setString(1, users[i][2]);
            ResultSet rs = chk.executeQuery();
            if (rs.next()) {
                uids[i] = rs.getInt("id");
            } else {
                ins.setString(1, users[i][0]); ins.setString(2, users[i][1]);
                ins.setString(3, users[i][2]); ins.setString(4, users[i][3]);
                ins.setString(5, users[i][4]);
                ins.executeUpdate();
                uids[i] = ins.getGeneratedKeys().getInt(1);
            }
        }

        String[][] etudiantsMeta = {
                {"ETU2024001","L2"}, {"ETU2024002","L2"}, {"ETU2024003","L2"},
                {"ETU2024004","L2"}, {"ETU2024005","L2"}, {"ETU2024006","L3"},
                {"ETU2024007","L3"}, {"ETU2024008","L3"}, {"ETU2024009","L1"},
                {"ETU2024010","L1"},
        };
        PreparedStatement ceCheck = conn.prepareStatement("SELECT id FROM etudiants WHERE id=?");
        PreparedStatement ceIns   = conn.prepareStatement("INSERT INTO etudiants(id,INE,niveau) VALUES(?,?,?)");
        for (int i = 0; i < etudiantsMeta.length; i++) {
            ceCheck.setInt(1, uids[7 + i]);
            if (!ceCheck.executeQuery().next()) {
                ceIns.setInt(1, uids[7 + i]);
                ceIns.setString(2, etudiantsMeta[i][0]);
                ceIns.setString(3, etudiantsMeta[i][1]);
                ceIns.executeUpdate();
            }
        }

        int b1 = getOrIns(conn, "batiments", "nom", "Amphi Lat Dior",
                "INSERT INTO batiments(nom,localisation,nombre_etages) VALUES('Amphi Lat Dior','Campus de Thies',1)");
        int b2 = getOrIns(conn, "batiments", "nom", "Bloc Pedagogique A",
                "INSERT INTO batiments(nom,localisation,nombre_etages) VALUES('Bloc Pedagogique A','Campus de Thies',3)");
        int b3 = getOrIns(conn, "batiments", "nom", "Bloc Pedagogique B",
                "INSERT INTO batiments(nom,localisation,nombre_etages) VALUES('Bloc Pedagogique B','Campus de Thies',2)");
        int b4 = getOrIns(conn, "batiments", "nom", "Batiment Informatique",
                "INSERT INTO batiments(nom,localisation,nombre_etages) VALUES('Batiment Informatique','Campus de Thies',2)");

        int s1  = getOrInsSalle(conn, "AMPHI-LD",  400, "AMPHI", b1);
        int s2  = getOrInsSalle(conn, "BPA-101",    40, "TD",    b2);
        int s3  = getOrInsSalle(conn, "BPA-102",    40, "TD",    b2);
        int s4  = getOrInsSalle(conn, "BPA-201",    35, "TD",    b2);
        int s5  = getOrInsSalle(conn, "BPA-202",    35, "TD",    b2);
        int s6  = getOrInsSalle(conn, "BPB-101",    30, "TD",    b3);
        int s7  = getOrInsSalle(conn, "BPB-102",    30, "TD",    b3);
        int s8  = getOrInsSalle(conn, "INFO-TP1",   30, "TP",    b4);
        int s9  = getOrInsSalle(conn, "INFO-TP2",   25, "TP",    b4);
        int s10 = getOrInsSalle(conn, "INFO-LABO",  20, "TP",    b4);

        getOrInsEquip(conn, "Videoprojecteur amphi", "Epson EB-X51",       "PROJECTEUR", s1);
        getOrInsEquip(conn, "Sono amphi",            "Systeme Bose",       "AUDIO",      s1);
        getOrInsEquip(conn, "Climatisation",         "Daikin 5000 BTU",    "CLIM",       s1);
        getOrInsEquip(conn, "Tableau numerique",     "SMART Board 6000",   "TABLEAU",    s2);
        getOrInsEquip(conn, "Videoprojecteur",       "BenQ MX550",         "PROJECTEUR", s3);
        getOrInsEquip(conn, "Videoprojecteur",       "Optoma EH200ST",     "PROJECTEUR", s4);
        getOrInsEquip(conn, "Tableau blanc",         "Nobo Classic",       "TABLEAU",    s5);
        getOrInsEquip(conn, "PC enseignant",         "Dell Optiplex 7090", "ORDINATEUR", s8);
        getOrInsEquip(conn, "PC enseignant",         "HP EliteDesk 800",   "ORDINATEUR", s9);
        getOrInsEquip(conn, "Serveur local",         "Dell PowerEdge T40", "SERVEUR",    s10);

        int m1 = getOrInsMat(conn, "Programmation Java",       "POO et design patterns",     30, 20);
        int m2 = getOrInsMat(conn, "Base de Donnees",          "SQL, MySQL, conception BDD", 20, 15);
        int m3 = getOrInsMat(conn, "Algorithmique",            "Structures de donnees",       25, 20);
        int m4 = getOrInsMat(conn, "Reseaux Informatiques",    "TCP/IP, routage, securite",  20, 15);
        int m5 = getOrInsMat(conn, "Systemes d exploitation",  "Linux, processus, memoire",  25, 20);
        int m6 = getOrInsMat(conn, "Mathematiques Discretes",  "Logique, combinatoire",       30, 15);
        int m7 = getOrInsMat(conn, "Developpement Web",        "HTML, CSS, JavaScript",       15, 25);
        int m8 = getOrInsMat(conn, "Intelligence Artificielle","ML et deep learning",         20, 20);

        int c1 = getOrInsCls(conn, "L1-INFO-A", "L1", 45);
        int c2 = getOrInsCls(conn, "L1-INFO-B", "L1", 42);
        int c3 = getOrInsCls(conn, "L2-INFO-A", "L2", 38);
        int c4 = getOrInsCls(conn, "L2-INFO-B", "L2", 35);
        int c5 = getOrInsCls(conn, "L3-INFO-A", "L3", 30);
        int c6 = getOrInsCls(conn, "L3-INFO-B", "L3", 28);

        int cr1  = getOrInsCren(conn, "Lundi",    8,  2);
        int cr2  = getOrInsCren(conn, "Lundi",    10, 2);
        int cr3  = getOrInsCren(conn, "Lundi",    14, 2);
        int cr4  = getOrInsCren(conn, "Lundi",    16, 2);
        int cr5  = getOrInsCren(conn, "Mardi",    8,  2);
        int cr6  = getOrInsCren(conn, "Mardi",    10, 2);
        int cr7  = getOrInsCren(conn, "Mardi",    14, 2);
        int cr8  = getOrInsCren(conn, "Mercredi", 8,  3);
        int cr9  = getOrInsCren(conn, "Mercredi", 14, 2);
        int cr10 = getOrInsCren(conn, "Jeudi",    8,  2);
        int cr11 = getOrInsCren(conn, "Jeudi",    10, 2);
        int cr12 = getOrInsCren(conn, "Jeudi",    14, 2);
        int cr13 = getOrInsCren(conn, "Vendredi", 8,  2);
        int cr14 = getOrInsCren(conn, "Vendredi", 10, 2);

        ResultSet rc = conn.createStatement().executeQuery("SELECT COUNT(*) FROM cours");
        if (rc.next() && rc.getInt(1) == 0) {
            insCours(conn, "PLANIFIE", "2026-03-16", m3, uids[3], c1, cr1,  s2);
            insCours(conn, "PLANIFIE", "2026-03-16", m6, uids[5], c1, cr2,  s3);
            insCours(conn, "PLANIFIE", "2026-03-16", m3, uids[3], c2, cr3,  s4);
            insCours(conn, "PLANIFIE", "2026-03-17", m6, uids[5], c2, cr5,  s5);
            insCours(conn, "EN_COURS", "2026-03-17", m1, uids[4], c1, cr6,  s8);
            insCours(conn, "PLANIFIE", "2026-03-16", m1, uids[3], c3, cr1,  s8);
            insCours(conn, "PLANIFIE", "2026-03-16", m2, uids[4], c3, cr2,  s9);
            insCours(conn, "PLANIFIE", "2026-03-17", m4, uids[5], c4, cr5,  s6);
            insCours(conn, "PLANIFIE", "2026-03-17", m5, uids[6], c4, cr6,  s10);
            insCours(conn, "TERMINE",  "2026-03-10", m2, uids[4], c3, cr7,  s2);
            insCours(conn, "TERMINE",  "2026-03-11", m1, uids[3], c4, cr8,  s8);
            insCours(conn, "PLANIFIE", "2026-03-18", m7, uids[6], c3, cr9,  s9);
            insCours(conn, "PLANIFIE", "2026-03-16", m8, uids[5], c5, cr3,  s10);
            insCours(conn, "PLANIFIE", "2026-03-16", m4, uids[6], c5, cr4,  s6);
            insCours(conn, "PLANIFIE", "2026-03-17", m7, uids[4], c6, cr7,  s7);
            insCours(conn, "PLANIFIE", "2026-03-18", m8, uids[5], c6, cr9,  s10);
            insCours(conn, "TERMINE",  "2026-03-09", m5, uids[6], c5, cr10, s6);
            insCours(conn, "PLANIFIE", "2026-03-19", m2, uids[3], c5, cr11, s9);
            insCours(conn, "PLANIFIE", "2026-03-18", m6, uids[5], c1, cr12, s1);
            insCours(conn, "PLANIFIE", "2026-03-20", m8, uids[5], c5, cr13, s1);
        }

        ResultSet rr = conn.createStatement().executeQuery("SELECT COUNT(*) FROM reservations");
        if (rr.next() && rr.getInt(1) == 0) {
            PreparedStatement ir = conn.prepareStatement(
                    "INSERT INTO reservations(motif,statut,date_reservation,salle_id,utilisateur_id) VALUES(?,?,?,?,?)");
            ir.setString(1,"Soutenance de projet L3");      ir.setString(2,"EN_ATTENTE"); ir.setString(3,"2026-03-25T09:00:00"); ir.setInt(4,s1);  ir.setInt(5,uids[3]); ir.executeUpdate();
            ir.setString(1,"Reunion pedagogique UFR");      ir.setString(2,"VALIDEE");    ir.setString(3,"2026-03-20T14:00:00"); ir.setInt(4,s2);  ir.setInt(5,uids[1]); ir.executeUpdate();
            ir.setString(1,"Examen de rattrapage L2");      ir.setString(2,"EN_ATTENTE"); ir.setString(3,"2026-03-27T08:00:00"); ir.setInt(4,s3);  ir.setInt(5,uids[4]); ir.executeUpdate();
            ir.setString(1,"Conference Numerique Senegal"); ir.setString(2,"VALIDEE");    ir.setString(3,"2026-03-22T10:00:00"); ir.setInt(4,s1);  ir.setInt(5,uids[2]); ir.executeUpdate();
            ir.setString(1,"Hackathon etudiant");           ir.setString(2,"EN_ATTENTE"); ir.setString(3,"2026-03-28T08:00:00"); ir.setInt(4,s8);  ir.setInt(5,uids[5]); ir.executeUpdate();
            ir.setString(1,"Atelier IA et Donnees");        ir.setString(2,"REFUSEE");    ir.setString(3,"2026-03-18T15:00:00"); ir.setInt(4,s10); ir.setInt(5,uids[6]); ir.executeUpdate();
            ir.setString(1,"Seance de tutorat L1");         ir.setString(2,"VALIDEE");    ir.setString(3,"2026-03-21T16:00:00"); ir.setInt(4,s4);  ir.setInt(5,uids[3]); ir.executeUpdate();
            ir.setString(1,"Presentation PFE Master");      ir.setString(2,"EN_ATTENTE"); ir.setString(3,"2026-03-30T09:00:00"); ir.setInt(4,s1);  ir.setInt(5,uids[4]); ir.executeUpdate();
        }
        System.out.println("Donnees demo senegalaises OK.");
    }

    private int getOrIns(Connection conn, String table, String col, String val, String insertSql) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE " + col + "=?");
        ps.setString(1, val);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        PreparedStatement ins = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
        ins.executeUpdate();
        return ins.getGeneratedKeys().getInt(1);
    }

    private int getOrInsSalle(Connection conn, String num, int cap, String type, int batId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM salles WHERE numero=?");
        ps.setString(1, num);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO salles(numero,capacite,type_salle,disponible,batiment_id) VALUES(?,?,?,1,?)",
                Statement.RETURN_GENERATED_KEYS);
        ins.setString(1, num); ins.setInt(2, cap); ins.setString(3, type); ins.setInt(4, batId);
        ins.executeUpdate();
        return ins.getGeneratedKeys().getInt(1);
    }

    private void getOrInsEquip(Connection conn, String nom, String desc, String type, int salleId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM equipements WHERE nom=? AND salle_id=?");
        ps.setString(1, nom); ps.setInt(2, salleId);
        if (ps.executeQuery().next()) return;
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO equipements(nom,description,etat,type_equipement,disponible,salle_id) VALUES(?,?,'BON',?,1,?)");
        ins.setString(1, nom); ins.setString(2, desc); ins.setString(3, type); ins.setInt(4, salleId);
        ins.executeUpdate();
    }

    private int getOrInsMat(Connection conn, String nom, String desc, int cm, int td) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM matieres WHERE nom=?");
        ps.setString(1, nom);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO matieres(nom,description,volume_cm,volume_td) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
        ins.setString(1, nom); ins.setString(2, desc); ins.setInt(3, cm); ins.setInt(4, td);
        ins.executeUpdate();
        return ins.getGeneratedKeys().getInt(1);
    }

    private int getOrInsCls(Connection conn, String nom, String niv, int eff) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM classes_pedago WHERE nom=?");
        ps.setString(1, nom);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO classes_pedago(nom,niveau,effectif) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
        ins.setString(1, nom); ins.setString(2, niv); ins.setInt(3, eff);
        ins.executeUpdate();
        return ins.getGeneratedKeys().getInt(1);
    }

    private int getOrInsCren(Connection conn, String jour, int heure, int duree) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM creneaux WHERE jour=? AND heure_debut=? AND duree=?");
        ps.setString(1, jour); ps.setInt(2, heure); ps.setInt(3, duree);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO creneaux(jour,heure_debut,duree) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
        ins.setString(1, jour); ins.setInt(2, heure); ins.setInt(3, duree);
        ins.executeUpdate();
        return ins.getGeneratedKeys().getInt(1);
    }

    private void insCours(Connection conn, String statut, String date,
                          int matId, int ensId, int clsId, int crenId, int salleId) throws SQLException {
        PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO cours(statut,date,matiere_id,enseignant_id,classe_id,creneau_id,salle_id) VALUES(?,?,?,?,?,?,?)");
        ins.setString(1, statut); ins.setString(2, date); ins.setInt(3, matId);
        ins.setInt(4, ensId); ins.setInt(5, clsId); ins.setInt(6, crenId); ins.setInt(7, salleId);
        ins.executeUpdate();
    }
}