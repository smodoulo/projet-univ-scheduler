# 🎓 UNIV-SCHEDULER v2.1
**Application de Gestion Intelligente des Salles et Emplois du Temps**

## 🆕 Nouveautés v2.1 — Fonctionnalités ajoutées

### 4. Visualisation et rapports ✅
| Fonctionnalité | Description |
|---|---|
| 📊 Dashboard occupation | Taux d'occupation par salle (BarChart dynamique) |
| 🗺️ Carte interactive | Vue visuelle des bâtiments/salles colorée selon le taux (🟢🟡🔴) |
| ⚠️ Salles critiques | Tableau des salles avec occupation ≥ 70% |
| 📜 Historique réservations | Tableau complet de toutes les réservations |
| 📄 Export PDF | Emploi du temps et historique en PDF mis en page |
| 📊 Export Excel | Cours, réservations et taux d'occupation en .xlsx |
| 📅 Rapport hebdomadaire | Synthèse textuelle de la semaine courante |
| 📆 Rapport mensuel | Synthèse mensuelle avec tous les indicateurs |

### 5. Notifications et alertes ✅
| Fonctionnalité | Description |
|---|---|
| ⚠️ Alerte conflit planification | In-app + email automatique dès qu'un conflit est détecté |
| 🔔 Notif. changement de salle | Email à l'enseignant si la salle d'un cours est modifiée |
| ⏰ Rappel fin de réservation | Service de fond → rappel 1h avant la fin de la réservation |
| ✅/❌ Notif. validation/refus | Email envoyé lors de la décision sur une réservation |

---

## 🚀 Lancement rapide

### Prérequis
- Java 17+
- Maven 3.8+

```bash
mvn javafx:run
```

## 👥 Comptes de Démonstration
| Rôle | Email | Mot de passe |
|------|-------|--------------|
| Administrateur | admin@univ.fr | admin123 |
| Gestionnaire | marie.dupont@univ.fr | gest123 |
| Enseignant | jean.martin@univ.fr | ens123 |
| Étudiant | paul.leroy@univ.fr | etu123 |

## 📧 Configuration Email (optionnel)

Créez un fichier `email.properties` dans le répertoire racine du projet pour activer les vrais emails :

```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.user=votre@gmail.com
smtp.password=votre_mot_de_passe_app
smtp.enabled=true
```

> **Par défaut** : mode simulation — les emails sont affichés dans la console.

## 🏗 Architecture
```
com.univscheduler/
├── MainApp.java
├── model/           (14 entités métier)
├── dao/             (11 DAOs SQLite)
├── service/         (4 services) ← NOUVEAU
│   ├── RapportService.java     ← taux d'occupation, rapports
│   ├── ExportService.java      ← PDF (PDFBox) + Excel (POI)
│   ├── EmailService.java       ← notifications email SMTP
│   └── AlerteService.java      ← service de fond, rappels
└── controller/      (6 contrôleurs JavaFX)
```

## ✨ Guide des onglets par rôle

### 📋 Gestionnaire
| Onglet | Contenu |
|--------|---------|
| Emploi du Temps | CRUD cours + export PDF/Excel |
| Vue Calendrier | Grille hebdomadaire navigable |
| Réservations | Valider/refuser les demandes (email auto) |
| **Historique** ← NEW | Toutes les réservations + export |
| **Carte Salles** ← NEW | Carte visuelle colorée par taux d'occupation |
| **Rapports** ← NEW | Graphiques, salles critiques, rapports hebdo/mensuel |

### 👨‍🏫 Enseignant
- Export PDF et Excel de son emploi du temps (nouveau)
- Notifications email lors de changement de salle (nouveau)

### 🎓 Étudiant
- Export PDF de l'emploi du temps de sa classe (nouveau)
