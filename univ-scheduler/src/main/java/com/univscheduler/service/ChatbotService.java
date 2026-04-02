package com.univscheduler.service;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ChatbotService {

    private final CoursDAO        coursDAO        = new CoursDAO();
    private final ReservationDAO  reservationDAO  = new ReservationDAO();
    private final SalleDAO        salleDAO        = new SalleDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final SignalementDAO  signalDAO       = new SignalementDAO();
    private final UtilisateurDAO  utilisateurDAO  = new UtilisateurDAO();

    private Utilisateur currentUser;

    public void setUser(Utilisateur user) { this.currentUser = user; }

    public String repondre(String question) {
        if (question == null || question.trim().isEmpty())
            return "Veuillez saisir votre question.";

        String q    = question.toLowerCase().trim();
        String role = currentUser != null ? currentUser.getRole() : "INCONNU";

        if (contient(q, "bonjour","salut","hello","bonsoir","hi","coucou"))
            return getBienvenue();

        if (contient(q, "qui es-tu","qui etes","tu es","bot","assistant","presenté","présente"))
            return getPresentation();

        if (contient(q, "date","aujourd","heure","maintenant","quel jour"))
            return getDate();

        if (contient(q, "aide","help","que peux","commande","menu","quoi faire"))
            return getAide();

        if (contient(q, "merci","super","bravo","parfait","excellent","bien"))
            return "Avec plaisir ! Je suis la pour vous aider.\nN'hesitez pas si vous avez d'autres questions.";

        if (contient(q, "au revoir","bye","a bientot","fermer","quitter"))
            return "Au revoir ! Bonne journee.\nL'assistant reste disponible quand vous en avez besoin.";

        switch (role) {
            case "ETUDIANT":     return repondreEtudiant(q);
            case "ENSEIGNANT":   return repondreEnseignant(q);
            case "GESTIONNAIRE": return repondreGestionnaire(q);
            case "ADMIN":        return repondreAdmin(q);
            default:
                return "Je n'ai pas pu identifier votre role.\nVeuillez vous reconnecter.";
        }
    }

    // ═══════════════════════════════════════════════════
    // ETUDIANT
    // ═══════════════════════════════════════════════════
    private String repondreEtudiant(String q) {
        if (contient(q, "cours","emploi","planning","horaire","classe","matiere","programme"))
            return getCoursEtudiant();
        if (contient(q, "salle","libre","disponible","etude","travailler"))
            return getSallesLibres();
        if (contient(q, "notification","alerte","message","notif","avis"))
            return getNotifications();
        if (contient(q, "reservation","reserver","réservation"))
            return "────────────────────────\n"
                    + "Information Reservation\n"
                    + "────────────────────────\n"
                    + "En tant qu'etudiant, vous n'avez\n"
                    + "pas acces a la reservation de salles.\n\n"
                    + "Vous pouvez cependant :\n"
                    + "  - Consulter les salles libres\n"
                    + "  - Voir l'emploi du temps\n"
                    + "  - Contacter votre enseignant";
        if (contient(q, "profil","mon compte","information"))
            return getProfilEtudiant();
        return nonCompris();
    }

    // ═══════════════════════════════════════════════════
    // ENSEIGNANT
    // ═══════════════════════════════════════════════════
    private String repondreEnseignant(String q) {
        if (contient(q, "cours","emploi","planning","horaire","programme"))
            return getCoursEnseignant();
        if (contient(q, "prochain","prochains","suivant","bientot"))
            return getProchainCoursEnseignant();
        if (contient(q, "reservation","reserver","mes reserv","demande"))
            return getReservationsEnseignant();
        if (contient(q, "salle","libre","disponible"))
            return getSallesLibres();
        if (contient(q, "signalement","probleme","panne","signaler","defaut"))
            return getSignalementsEnseignant();
        if (contient(q, "notification","alerte","message","notif"))
            return getNotifications();
        if (contient(q, "profil","mon compte","information"))
            return getProfilEnseignant();
        return nonCompris();
    }

    // ═══════════════════════════════════════════════════
    // GESTIONNAIRE
    // ═══════════════════════════════════════════════════
    private String repondreGestionnaire(String q) {
        if (contient(q, "cours","emploi","planning","aujourd"))
            return getCoursDuJour();
        if (contient(q, "reservation","attente","valider","en attente","demande"))
            return getReservationsEnAttente();
        if (contient(q, "signalement","probleme","panne","maintenance"))
            return getSignalementsGestionnaire();
        if (contient(q, "stat","occupation","taux","rapport","bilan"))
            return getStatistiques();
        if (contient(q, "salle","libre","disponible","critique","occupee"))
            return getSallesCritiques();
        if (contient(q, "notification","alerte","message","notif"))
            return getNotifications();
        if (contient(q, "conflit","conflits","double"))
            return getConflits();
        return nonCompris();
    }

    // ═══════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════
    private String repondreAdmin(String q) {
        if (contient(q, "utilisateur","user","compte","combien","inscrit"))
            return getStatsUtilisateurs();
        if (contient(q, "salle","batiment","infrastructure","espace"))
            return getStatsSalles();
        if (contient(q, "cours","emploi","planning","programme"))
            return getStatsCours();
        if (contient(q, "stat","occupation","taux","rapport","global","bilan"))
            return getStatistiques();
        if (contient(q, "reservation","demande"))
            return getStatsReservations();
        if (contient(q, "notification","alerte","message","notif"))
            return getNotifications();
        if (contient(q, "signalement","probleme","panne"))
            return getSignalementsGestionnaire();
        return nonCompris();
    }

    // ═══════════════════════════════════════════════════
    // DONNEES ETUDIANT
    // ═══════════════════════════════════════════════════
    private String getCoursEtudiant() {
        try {
            List<Cours> tous = coursDAO.findAll();
            if (tous == null || tous.isEmpty())
                return "Aucun cours trouve dans le systeme.";
            List<Cours> planifies = tous.stream()
                    .filter(c -> "PLANIFIE".equals(c.getStatut())
                            || "EN_COURS".equals(c.getStatut()))
                    .collect(Collectors.toList());
            if (planifies.isEmpty())
                return "Aucun cours planifie pour le moment.";

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Emploi du Temps\n");
            sb.append("────────────────────────\n");
            sb.append("Total planifie : ").append(planifies.size()).append(" cours\n\n");

            int max = Math.min(planifies.size(), 6);
            for (int i = 0; i < max; i++) {
                Cours c = planifies.get(i);
                sb.append("Cours ").append(i + 1).append("\n");
                sb.append("  Matiere  : ").append(c.getMatiereNom() != null ? c.getMatiereNom() : "?").append("\n");
                sb.append("  Classe   : ").append(c.getClasseNom() != null ? c.getClasseNom() : "?").append("\n");
                sb.append("  Creneau  : ").append(c.getCreneauInfo() != null ? c.getCreneauInfo() : "?").append("\n");
                sb.append("  Salle    : ").append(c.getSalleNumero() != null ? c.getSalleNumero() : "?").append("\n");
                if (i < max - 1) sb.append("  --------\n");
            }
            if (planifies.size() > 6)
                sb.append("\n... et ").append(planifies.size() - 6).append(" autres cours.\n");
            sb.append("\nConsultez l'onglet Emploi du Temps\npour plus de details.");
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des cours.\nVeuillez reessayer.";
        }
    }

    private String getProfilEtudiant() {
        if (currentUser == null) return "Utilisateur non connecte.";
        return "────────────────────────\n"
                + "  Mon Profil\n"
                + "────────────────────────\n"
                + "Nom    : " + currentUser.getNom() + "\n"
                + "Prenom : " + currentUser.getPrenom() + "\n"
                + "Email  : " + currentUser.getEmail() + "\n"
                + "Role   : Etudiant\n"
                + "────────────────────────\n"
                + "Consultez l'onglet Mon Profil\npour plus d'informations.";
    }

    // ═══════════════════════════════════════════════════
    // DONNEES ENSEIGNANT
    // ═══════════════════════════════════════════════════
    private String getCoursEnseignant() {
        if (currentUser == null) return "Utilisateur non connecte.";
        try {
            List<Cours> cours = coursDAO.findByEnseignant(currentUser.getId());
            if (cours == null || cours.isEmpty())
                return "Vous n'avez aucun cours assigne actuellement.";

            long planifies = cours.stream().filter(c -> "PLANIFIE".equals(c.getStatut())).count();
            long realises  = cours.stream().filter(c -> "REALISE".equals(c.getStatut())).count();
            long annules   = cours.stream().filter(c -> "ANNULE".equals(c.getStatut())).count();

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Mes Cours\n");
            sb.append("────────────────────────\n");
            sb.append("Total     : ").append(cours.size()).append(" cours\n");
            sb.append("Planifies : ").append(planifies).append("\n");
            sb.append("Realises  : ").append(realises).append("\n");
            sb.append("Annules   : ").append(annules).append("\n");
            sb.append("────────────────────────\n");

            List<Cours> prochains = cours.stream()
                    .filter(c -> "PLANIFIE".equals(c.getStatut()))
                    .limit(4).collect(Collectors.toList());

            if (!prochains.isEmpty()) {
                sb.append("Prochains cours :\n\n");
                for (Cours c : prochains) {
                    sb.append("  Matiere : ").append(c.getMatiereNom() != null ? c.getMatiereNom() : "?").append("\n");
                    sb.append("  Classe  : ").append(c.getClasseNom() != null ? c.getClasseNom() : "?").append("\n");
                    sb.append("  Creneau : ").append(c.getCreneauInfo() != null ? c.getCreneauInfo() : "?").append("\n");
                    sb.append("  Date    : ").append(c.getDate() != null ? c.getDate() : "?").append("\n");
                    sb.append("  --------\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des cours.";
        }
    }

    private String getProchainCoursEnseignant() {
        if (currentUser == null) return "Utilisateur non connecte.";
        try {
            List<Cours> cours = coursDAO.findByEnseignant(currentUser.getId());
            if (cours == null || cours.isEmpty()) return "Aucun cours trouve.";
            LocalDate today = LocalDate.now();
            List<Cours> futurs = cours.stream()
                    .filter(c -> "PLANIFIE".equals(c.getStatut())
                            && c.getDate() != null
                            && !c.getDate().isBefore(today))
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .limit(3).collect(Collectors.toList());

            if (futurs.isEmpty())
                return "Aucun prochain cours planifie\npour les prochains jours.";

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Prochains Cours\n");
            sb.append("────────────────────────\n");
            for (int i = 0; i < futurs.size(); i++) {
                Cours c = futurs.get(i);
                sb.append("Cours ").append(i + 1).append("\n");
                sb.append("  Matiere : ").append(c.getMatiereNom()).append("\n");
                sb.append("  Classe  : ").append(c.getClasseNom()).append("\n");
                sb.append("  Date    : ").append(c.getDate()).append("\n");
                sb.append("  Creneau : ").append(c.getCreneauInfo()).append("\n");
                sb.append("  Salle   : ").append(c.getSalleNumero()).append("\n");
                if (i < futurs.size() - 1) sb.append("  --------\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des prochains cours.";
        }
    }

    private String getReservationsEnseignant() {
        if (currentUser == null) return "Utilisateur non connecte.";
        try {
            List<Reservation> reservations =
                    reservationDAO.findByUtilisateur(currentUser.getId());
            if (reservations == null || reservations.isEmpty())
                return "Vous n'avez aucune reservation enregistree.";

            long enAttente = reservations.stream().filter(r -> "EN_ATTENTE".equals(r.getStatut())).count();
            long validees  = reservations.stream().filter(r -> "VALIDEE".equals(r.getStatut())).count();
            long refusees  = reservations.stream().filter(r -> "REFUSEE".equals(r.getStatut())).count();

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Mes Reservations\n");
            sb.append("────────────────────────\n");
            sb.append("Total      : ").append(reservations.size()).append("\n");
            sb.append("En attente : ").append(enAttente).append("\n");
            sb.append("Validees   : ").append(validees).append("\n");
            sb.append("Refusees   : ").append(refusees).append("\n");
            sb.append("────────────────────────\n");

            reservations.stream().limit(4).forEach(r -> {
                String statut = "EN_ATTENTE".equals(r.getStatut()) ? "[En attente]"
                        : "VALIDEE".equals(r.getStatut())    ? "[Validee]"
                        : "[Refusee]";
                sb.append("Salle  : ").append(r.getSalleNumero() != null ? r.getSalleNumero() : "?").append("\n");
                sb.append("Motif  : ").append(r.getMotif() != null ? r.getMotif() : "?").append("\n");
                sb.append("Date   : ").append(r.getDateReservation() != null ? r.getDateReservation() : "?").append("\n");
                sb.append("Statut : ").append(statut).append("\n");
                sb.append("  --------\n");
            });
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des reservations.";
        }
    }

    private String getSignalementsEnseignant() {
        if (currentUser == null) return "Utilisateur non connecte.";
        try {
            List<Signalement> signalements =
                    signalDAO.findByEnseignant(currentUser.getId());
            if (signalements == null || signalements.isEmpty())
                return "Vous n'avez aucun signalement enregistre.";

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Mes Signalements\n");
            sb.append("────────────────────────\n");
            sb.append("Total : ").append(signalements.size()).append("\n\n");
            signalements.stream().limit(5).forEach(s -> {
                sb.append("Titre    : ").append(s.getTitre() != null ? s.getTitre() : "?").append("\n");
                sb.append("Priorite : ").append(s.getPriorite() != null ? s.getPriorite() : "?").append("\n");
                sb.append("Statut   : ").append(s.getStatut() != null ? s.getStatut() : "?").append("\n");
                sb.append("  --------\n");
            });
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des signalements.";
        }
    }

    private String getProfilEnseignant() {
        if (currentUser == null) return "Utilisateur non connecte.";
        return "────────────────────────\n"
                + "  Mon Profil\n"
                + "────────────────────────\n"
                + "Nom    : " + currentUser.getNom() + "\n"
                + "Prenom : " + currentUser.getPrenom() + "\n"
                + "Email  : " + currentUser.getEmail() + "\n"
                + "Role   : Enseignant\n";
    }

    // ═══════════════════════════════════════════════════
    // DONNEES GESTIONNAIRE
    // ═══════════════════════════════════════════════════
    private String getCoursDuJour() {
        try {
            List<Cours> tous = coursDAO.findAll();
            if (tous == null || tous.isEmpty())
                return "Aucun cours dans le systeme.";
            LocalDate today = LocalDate.now();
            List<Cours> duJour = tous.stream()
                    .filter(c -> today.equals(c.getDate()))
                    .collect(Collectors.toList());
            long totalPlanifies = tous.stream()
                    .filter(c -> "PLANIFIE".equals(c.getStatut())).count();

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Cours du Jour\n");
            sb.append("────────────────────────\n");
            sb.append("Aujourd'hui    : ").append(duJour.size()).append(" cours\n");
            sb.append("Total planifie : ").append(totalPlanifies).append("\n");
            sb.append("────────────────────────\n");

            if (!duJour.isEmpty()) {
                duJour.stream().limit(5).forEach(c -> {
                    sb.append("Matiere : ").append(c.getMatiereNom() != null ? c.getMatiereNom() : "?").append("\n");
                    sb.append("Classe  : ").append(c.getClasseNom() != null ? c.getClasseNom() : "?").append("\n");
                    sb.append("Salle   : ").append(c.getSalleNumero() != null ? c.getSalleNumero() : "?").append("\n");
                    sb.append("  --------\n");
                });
            } else {
                sb.append("Aucun cours programme aujourd'hui.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des cours.";
        }
    }

    private String getReservationsEnAttente() {
        try {
            List<Reservation> toutes = reservationDAO.findAll();
            if (toutes == null || toutes.isEmpty())
                return "Aucune reservation dans le systeme.";
            List<Reservation> enAttente = toutes.stream()
                    .filter(r -> "EN_ATTENTE".equals(r.getStatut()))
                    .collect(Collectors.toList());

            if (enAttente.isEmpty())
                return "────────────────────────\n"
                        + "  Reservations\n"
                        + "────────────────────────\n"
                        + "Aucune reservation en attente.\n"
                        + "Toutes les demandes ont ete traitees.";

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Reservations en Attente\n");
            sb.append("────────────────────────\n");
            sb.append("Nombre : ").append(enAttente.size()).append(" demande(s)\n\n");
            enAttente.stream().limit(4).forEach(r -> {
                sb.append("Demandeur : ").append(r.getUtilisateurNom() != null ? r.getUtilisateurNom() : "?").append("\n");
                sb.append("Salle     : ").append(r.getSalleNumero() != null ? r.getSalleNumero() : "?").append("\n");
                sb.append("Motif     : ").append(r.getMotif() != null ? r.getMotif() : "?").append("\n");
                sb.append("Date      : ").append(r.getDateReservation() != null ? r.getDateReservation() : "?").append("\n");
                sb.append("  --------\n");
            });
            if (enAttente.size() > 4)
                sb.append("... et ").append(enAttente.size() - 4).append(" autres demandes.\n");
            sb.append("\nAllez dans l'onglet Reservations\npour valider ou refuser.");
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des reservations.";
        }
    }

    private String getSignalementsGestionnaire() {
        try {
            List<Signalement> signalements = signalDAO.findAll();
            if (signalements == null || signalements.isEmpty())
                return "Aucun signalement recu pour le moment.";

            long enCours = signalements.stream()
                    .filter(s -> "EN_COURS".equals(s.getStatut())
                            || "EN_ATTENTE".equals(s.getStatut())).count();
            long resolus = signalements.stream()
                    .filter(s -> "RESOLU".equals(s.getStatut())).count();

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Signalements\n");
            sb.append("────────────────────────\n");
            sb.append("Total     : ").append(signalements.size()).append("\n");
            sb.append("En cours  : ").append(enCours).append("\n");
            sb.append("Resolus   : ").append(resolus).append("\n");
            sb.append("────────────────────────\n");
            sb.append("A traiter :\n\n");
            signalements.stream()
                    .filter(s -> !"RESOLU".equals(s.getStatut()))
                    .limit(4).forEach(s -> {
                        sb.append("Titre    : ").append(s.getTitre() != null ? s.getTitre() : "?").append("\n");
                        sb.append("Priorite : ").append(s.getPriorite() != null ? s.getPriorite() : "?").append("\n");
                        sb.append("Statut   : ").append(s.getStatut() != null ? s.getStatut() : "?").append("\n");
                        sb.append("  --------\n");
                    });
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des signalements.";
        }
    }

    private String getSallesCritiques() {
        try {
            List<Salle> salles = salleDAO.findAll();
            if (salles == null || salles.isEmpty())
                return "Aucune salle trouvee dans le systeme.";

            long dispos = salles.stream().filter(Salle::isDisponible).count();
            long indispos = salles.size() - dispos;

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Etat des Salles\n");
            sb.append("────────────────────────\n");
            sb.append("Total       : ").append(salles.size()).append(" salles\n");
            sb.append("Disponibles : ").append(dispos).append("\n");
            sb.append("Indisponibles: ").append(indispos).append("\n");
            sb.append("────────────────────────\n");
            sb.append("Liste :\n\n");
            salles.stream().limit(6).forEach(s -> {
                String dispo = s.isDisponible() ? "[Libre]" : "[Occupee]";
                sb.append("  ").append(s.getNumero() != null ? s.getNumero() : "?")
                        .append(" | cap:").append(s.getCapacite())
                        .append(" | ").append(s.getTypeSalle() != null ? s.getTypeSalle() : "?")
                        .append(" ").append(dispo).append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des salles.";
        }
    }

    private String getConflits() {
        try {
            List<Cours> tous = coursDAO.findAll();
            if (tous == null || tous.isEmpty())
                return "Aucun conflit detecte.";
            List<Cours> conflits = tous.stream()
                    .filter(c -> "CONFLIT".equals(c.getStatut()))
                    .collect(Collectors.toList());

            if (conflits.isEmpty())
                return "────────────────────────\n"
                        + "  Conflits\n"
                        + "────────────────────────\n"
                        + "Aucun conflit detecte.\n"
                        + "L'emploi du temps est coherent.";

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Conflits Detectes\n");
            sb.append("────────────────────────\n");
            sb.append("Nombre : ").append(conflits.size()).append(" conflit(s)\n\n");
            conflits.stream().limit(5).forEach(c -> {
                sb.append("Matiere : ").append(c.getMatiereNom() != null ? c.getMatiereNom() : "?").append("\n");
                sb.append("Classe  : ").append(c.getClasseNom() != null ? c.getClasseNom() : "?").append("\n");
                sb.append("Creneau : ").append(c.getCreneauInfo() != null ? c.getCreneauInfo() : "?").append("\n");
                sb.append("  --------\n");
            });
            sb.append("\nReglez les conflits dans\nl'onglet Emploi du Temps.");
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des conflits.";
        }
    }

    private String getStatistiques() {
        try {
            List<Cours> cours     = coursDAO.findAll();
            List<Salle> salles    = salleDAO.findAll();
            List<Reservation> res = reservationDAO.findAll();
            int totalCours  = cours  != null ? cours.size()  : 0;
            int totalSalles = salles != null ? salles.size() : 0;
            int totalRes    = res    != null ? res.size()    : 0;
            long planifies  = cours != null ? cours.stream()
                    .filter(c -> "PLANIFIE".equals(c.getStatut())).count() : 0;
            long enAttente  = res != null ? res.stream()
                    .filter(r -> "EN_ATTENTE".equals(r.getStatut())).count() : 0;
            long dispos     = salles != null ? salles.stream()
                    .filter(Salle::isDisponible).count() : 0;

            return "────────────────────────\n"
                    + "  Statistiques Globales\n"
                    + "────────────────────────\n"
                    + "Cours total    : " + totalCours   + "\n"
                    + "Planifies      : " + planifies    + "\n"
                    + "────────────────────\n"
                    + "Salles total   : " + totalSalles  + "\n"
                    + "Disponibles    : " + dispos       + "\n"
                    + "────────────────────\n"
                    + "Reservations   : " + totalRes     + "\n"
                    + "En attente     : " + enAttente    + "\n"
                    + "────────────────────────\n"
                    + "Consultez l'onglet Rapports\npour plus de details.";
        } catch (Exception e) {
            return "Erreur lors de la recuperation des statistiques.";
        }
    }

    // ═══════════════════════════════════════════════════
    // DONNEES ADMIN
    // ═══════════════════════════════════════════════════
    private String getStatsUtilisateurs() {
        try {
            List<Utilisateur> users = utilisateurDAO.findAll();
            if (users == null || users.isEmpty())
                return "Aucun utilisateur dans le systeme.";
            long admins = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
            long gests  = users.stream().filter(u -> "GESTIONNAIRE".equals(u.getRole())).count();
            long ensei  = users.stream().filter(u -> "ENSEIGNANT".equals(u.getRole())).count();
            long etuds  = users.stream().filter(u -> "ETUDIANT".equals(u.getRole())).count();

            return "────────────────────────\n"
                    + "  Utilisateurs\n"
                    + "────────────────────────\n"
                    + "Total          : " + users.size() + "\n"
                    + "Admins         : " + admins + "\n"
                    + "Gestionnaires  : " + gests  + "\n"
                    + "Enseignants    : " + ensei  + "\n"
                    + "Etudiants      : " + etuds  + "\n"
                    + "────────────────────────\n"
                    + "Gerez les comptes dans\nl'onglet Utilisateurs.";
        } catch (Exception e) {
            return "Erreur lors de la recuperation des utilisateurs.";
        }
    }

    private String getStatsSalles() {
        try {
            List<Salle> salles = salleDAO.findAll();
            if (salles == null || salles.isEmpty())
                return "Aucune salle configuree.";
            long dispos = salles.stream().filter(Salle::isDisponible).count();

            return "────────────────────────\n"
                    + "  Infrastructure\n"
                    + "────────────────────────\n"
                    + "Total salles   : " + salles.size() + "\n"
                    + "Disponibles    : " + dispos + "\n"
                    + "Indisponibles  : " + (salles.size() - dispos) + "\n"
                    + "────────────────────────\n"
                    + "Tapez 'salles' pour voir\nla liste complete.";
        } catch (Exception e) {
            return "Erreur lors de la recuperation des salles.";
        }
    }

    private String getStatsCours() {
        try {
            List<Cours> cours = coursDAO.findAll();
            if (cours == null || cours.isEmpty())
                return "Aucun cours dans le systeme.";
            long planifies = cours.stream().filter(c -> "PLANIFIE".equals(c.getStatut())).count();
            long realises  = cours.stream().filter(c -> "REALISE".equals(c.getStatut())).count();
            long annules   = cours.stream().filter(c -> "ANNULE".equals(c.getStatut())).count();
            long conflits  = cours.stream().filter(c -> "CONFLIT".equals(c.getStatut())).count();

            return "────────────────────────\n"
                    + "  Cours\n"
                    + "────────────────────────\n"
                    + "Total      : " + cours.size() + "\n"
                    + "Planifies  : " + planifies + "\n"
                    + "Realises   : " + realises  + "\n"
                    + "Annules    : " + annules   + "\n"
                    + "Conflits   : " + conflits  + "\n"
                    + "────────────────────────\n"
                    + "Gerez les cours dans\nl'onglet Emploi du Temps.";
        } catch (Exception e) {
            return "Erreur lors de la recuperation des cours.";
        }
    }

    private String getStatsReservations() {
        try {
            List<Reservation> res = reservationDAO.findAll();
            if (res == null || res.isEmpty())
                return "Aucune reservation dans le systeme.";
            long enAttente = res.stream().filter(r -> "EN_ATTENTE".equals(r.getStatut())).count();
            long validees  = res.stream().filter(r -> "VALIDEE".equals(r.getStatut())).count();
            long refusees  = res.stream().filter(r -> "REFUSEE".equals(r.getStatut())).count();

            return "────────────────────────\n"
                    + "  Reservations\n"
                    + "────────────────────────\n"
                    + "Total      : " + res.size()  + "\n"
                    + "En attente : " + enAttente   + "\n"
                    + "Validees   : " + validees    + "\n"
                    + "Refusees   : " + refusees    + "\n"
                    + "────────────────────────\n"
                    + "Gerez les demandes dans\nl'onglet Reservations.";
        } catch (Exception e) {
            return "Erreur lors de la recuperation des reservations.";
        }
    }

    // ═══════════════════════════════════════════════════
    // DONNEES COMMUNES
    // ═══════════════════════════════════════════════════
    private String getSallesLibres() {
        try {
            List<Salle> salles = salleDAO.findAll();
            if (salles == null || salles.isEmpty())
                return "Aucune salle trouvee.";
            List<Salle> libres = salles.stream()
                    .filter(Salle::isDisponible)
                    .collect(Collectors.toList());

            if (libres.isEmpty())
                return "────────────────────────\n"
                        + "  Salles Libres\n"
                        + "────────────────────────\n"
                        + "Aucune salle disponible\nen ce moment.";

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Salles Disponibles\n");
            sb.append("────────────────────────\n");
            sb.append("Nombre : ").append(libres.size()).append(" salle(s)\n\n");
            libres.stream().limit(7).forEach(s ->
                    sb.append("  ").append(s.getNumero() != null ? s.getNumero() : "?")
                            .append(" | cap:").append(s.getCapacite())
                            .append(" | ").append(s.getTypeSalle() != null ? s.getTypeSalle() : "?")
                            .append("\n")
            );
            if (libres.size() > 7)
                sb.append("  ... et ").append(libres.size() - 7).append(" autres.\n");
            sb.append("\nConsultez l'onglet Salles\npour reserver.");
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des salles.";
        }
    }

    private String getNotifications() {
        if (currentUser == null) return "Utilisateur non connecte.";
        try {
            List<Notification> notifs =
                    notificationDAO.findByUtilisateur(currentUser.getId());
            if (notifs == null || notifs.isEmpty())
                return "────────────────────────\n"
                        + "  Notifications\n"
                        + "────────────────────────\n"
                        + "Vous n'avez aucune\nnotification pour le moment.";

            long nonLues = notifs.stream().filter(n -> !n.isLu()).count();

            StringBuilder sb = new StringBuilder();
            sb.append("────────────────────────\n");
            sb.append("  Mes Notifications\n");
            sb.append("────────────────────────\n");
            sb.append("Total    : ").append(notifs.size()).append("\n");
            sb.append("Non lues : ").append(nonLues).append("\n");
            sb.append("────────────────────────\n");
            notifs.stream().limit(5).forEach(n -> {
                sb.append(n.isLu() ? "[Lu]    " : "[Nouveau] ");
                sb.append(n.getMessage() != null ? n.getMessage() : "?").append("\n");
                sb.append("  --------\n");
            });
            if (notifs.size() > 5)
                sb.append("... et ").append(notifs.size() - 5).append(" autres.\n");
            sb.append("\nConsultez l'onglet Notifications\npour les details.");
            return sb.toString();
        } catch (Exception e) {
            return "Erreur lors de la recuperation des notifications.";
        }
    }

    // ═══════════════════════════════════════════════════
    // AIDE & BIENVENUE
    // ═══════════════════════════════════════════════════
    private String getBienvenue() {
        String prenom = currentUser != null ? currentUser.getPrenom() : "";
        String role   = currentUser != null ? getRoleLabel() : "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMMM", java.util.Locale.FRENCH);
        String dateStr = LocalDate.now().format(fmt);
        return "Bonjour " + prenom + " !\n"
                + "Nous sommes le " + dateStr + ".\n\n"
                + "Je suis votre assistant personnel\nUNIV-SCHEDULER.\n\n"
                + "Vous etes connecte en tant que\n" + role + ".\n\n"
                + "Tapez 'aide' pour voir ce que\nje peux faire pour vous.";
    }

    private String getPresentation() {
        return "────────────────────────\n"
                + "  Assistant UNIV-SCHEDULER\n"
                + "────────────────────────\n"
                + "Je suis votre assistant intelligent\n"
                + "integre dans l'application.\n\n"
                + "Je peux vous aider a :\n"
                + "  - Consulter vos donnees\n"
                + "  - Obtenir des statistiques\n"
                + "  - Acceder a vos informations\n"
                + "  - Naviguer dans l'application\n\n"
                + "Tapez 'aide' pour commencer.";
    }

    private String getDate() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                "EEEE dd MMMM yyyy", java.util.Locale.FRENCH);
        return "────────────────────────\n"
                + "  Date et Heure\n"
                + "────────────────────────\n"
                + "Nous sommes le :\n"
                + LocalDate.now().format(fmt) + "\n"
                + "────────────────────────";
    }

    private String getAide() {
        if (currentUser == null)
            return "Connectez-vous pour acceder\na l'assistant.";
        StringBuilder sb = new StringBuilder();
        sb.append("────────────────────────\n");
        sb.append("  Commandes Disponibles\n");
        sb.append("────────────────────────\n");
        switch (currentUser.getRole()) {
            case "ETUDIANT":
                sb.append("Mon emploi du temps\n");
                sb.append("Salles libres\n");
                sb.append("Mes notifications\n");
                sb.append("Mon profil\n");
                break;
            case "ENSEIGNANT":
                sb.append("Mon emploi du temps\n");
                sb.append("Prochains cours\n");
                sb.append("Mes reservations\n");
                sb.append("Salles libres\n");
                sb.append("Mes signalements\n");
                sb.append("Mes notifications\n");
                sb.append("Mon profil\n");
                break;
            case "GESTIONNAIRE":
                sb.append("Cours du jour\n");
                sb.append("Reservations en attente\n");
                sb.append("Signalements\n");
                sb.append("Statistiques\n");
                sb.append("Salles critiques\n");
                sb.append("Conflits\n");
                sb.append("Notifications\n");
                break;
            case "ADMIN":
                sb.append("Utilisateurs\n");
                sb.append("Salles\n");
                sb.append("Cours\n");
                sb.append("Reservations\n");
                sb.append("Statistiques globales\n");
                sb.append("Signalements\n");
                sb.append("Notifications\n");
                break;
        }
        sb.append("────────────────────────\n");
        sb.append("Tapez une commande\nou posez votre question.");
        return sb.toString();
    }

    private String getRoleLabel() {
        if (currentUser == null) return "";
        switch (currentUser.getRole()) {
            case "ADMIN":        return "Administrateur";
            case "GESTIONNAIRE": return "Gestionnaire";
            case "ENSEIGNANT":   return "Enseignant";
            case "ETUDIANT":     return "Etudiant";
            default:             return currentUser.getRole();
        }
    }

    private String nonCompris() {
        return "────────────────────────\n"
                + "Je n'ai pas compris\nvotre demande.\n"
                + "────────────────────────\n"
                + "Essayez par exemple :\n"
                + "  > 'Mon emploi du temps'\n"
                + "  > 'Salles libres'\n"
                + "  > 'Mes notifications'\n\n"
                + "Ou tapez 'aide' pour voir\ntoutes les commandes.";
    }

    private boolean contient(String q, String... mots) {
        for (String mot : mots)
            if (q.contains(mot)) return true;
        return false;
    }
}