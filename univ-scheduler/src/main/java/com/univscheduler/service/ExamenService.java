package com.univscheduler.service;

import com.univscheduler.dao.ExamenDAO;
import com.univscheduler.dao.NotificationDAO;
import com.univscheduler.dao.UtilisateurDAO;
import com.univscheduler.model.Examen;
import com.univscheduler.model.Notification;
import com.univscheduler.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service métier pour les examens, devoirs et contrôles.
 *
 * FLUX COMPLET :
 *  1. Enseignant → soumettre()
 *       → BDD statut EN_ATTENTE
 *       → Notif in-app + email → tous les gestionnaires
 *       → Notif in-app → responsable de la classe (enseignant principal)
 *
 *  2. Gestionnaire → valider()
 *       → BDD statut VALIDE
 *       → Notif in-app + email → enseignant auteur
 *       → Notif in-app → étudiants de la classe (si souhaité)
 *
 *  3. Gestionnaire → refuser()
 *       → BDD statut REFUSE + motif
 *       → Notif in-app + email → enseignant auteur
 */
public class ExamenService {

    private final ExamenDAO      dao            = new ExamenDAO();
    private final NotificationDAO notifDAO      = new NotificationDAO();
    private final UtilisateurDAO  utilisateurDAO = new UtilisateurDAO();

    // ════════════════════════════════════════════════════════════
    //  1. Enseignant soumet un examen / devoir
    // ════════════════════════════════════════════════════════════
    /**
     * @return true si succès, false si conflit salle détecté
     */
    public boolean soumettre(Examen examen) {
        try {
            // Vérif conflit salle avant sauvegarde
            if (examen.getSalleId() != null && examen.getSalleId() > 0) {
                if (dao.hasConflitSalle(examen.getSalleId(),
                        examen.getDateExamen(), examen.getDureeMinutes(), 0)) {
                    return false;  // ← conflit salle
                }
            }

            // Persistance
            dao.save(examen);

            // Message de base
            String msg = examen.getTypeIcon()
                    + " Nouvelle demande " + examen.getType().toLowerCase()
                    + " de " + nvl(examen.getEnseignantNom())
                    + " | " + nvl(examen.getTitre())
                    + " | Classe : " + nvl(examen.getClasseNom())
                    + " | Matière : " + nvl(examen.getMatiereNom())
                    + " | Date : " + formatDate(examen.getDateExamen())
                    + (examen.getSalleNumero() != null && !examen.getSalleNumero().isEmpty()
                    ? " | Salle : " + examen.getSalleNumero() : "");

            // ── Notifier tous les GESTIONNAIRES et ADMINS ────────────
            utilisateurDAO.findAll().stream()
                    .filter(u -> "GESTIONNAIRE".equals(u.getRole())
                            || "ADMIN".equals(u.getRole()))
                    .forEach(u -> {
                        saveNotif(u.getId(), "INFO", msg);

                        // Email si Gmail
                        if (isGmail(u)) {
                            new Thread(() -> EmailService.sendNotification(u,
                                    examen.getTypeIcon() + " Nouvelle demande — "
                                            + nvl(examen.getTitre()),
                                    buildEmailGestionnaire(examen, u)),
                                    "email-examen-gest").start();
                        }
                    });

            return true;

        } catch (Exception e) {
            System.err.println("[ExamenService] soumettre : " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  2. Gestionnaire valide
    // ════════════════════════════════════════════════════════════
    /**
     * @return true si succès, false si conflit salle détecté au moment de valider
     */
    public boolean valider(int examenId, String commentaire) {
        try {
            Examen e = dao.findById(examenId);
            if (e == null) return false;

            // Re-vérifier le conflit salle au moment de valider
            if (e.getSalleId() != null && e.getSalleId() > 0) {
                if (dao.hasConflitSalle(e.getSalleId(),
                        e.getDateExamen(), e.getDureeMinutes(), examenId)) {
                    // Marquer CONFLIT dans le commentaire mais ne pas bloquer
                    dao.updateStatut(examenId, Examen.STATUT_REFUSE,
                            "⚠️ Conflit salle détecté automatiquement. " + nvl(commentaire));
                    notifierEnseignant(e, false,
                            "⚠️ Conflit salle détecté — veuillez proposer une autre salle.");
                    return false;
                }
            }

            dao.updateStatut(examenId, Examen.STATUT_VALIDE, nvl(commentaire));

            // Notifier l'enseignant auteur
            notifierEnseignant(e, true, commentaire);

            return true;

        } catch (Exception ex) {
            System.err.println("[ExamenService] valider : " + ex.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  3. Gestionnaire refuse
    // ════════════════════════════════════════════════════════════
    public void refuser(int examenId, String motif) {
        try {
            Examen e = dao.findById(examenId);
            if (e == null) return;
            String motifFinal = (motif != null && !motif.isBlank())
                    ? motif : "Refusé par le gestionnaire.";
            dao.updateStatut(examenId, Examen.STATUT_REFUSE, motifFinal);
            notifierEnseignant(e, false, motifFinal);
        } catch (Exception ex) {
            System.err.println("[ExamenService] refuser : " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  4. Annuler (enseignant ou gestionnaire)
    // ════════════════════════════════════════════════════════════
    public void annuler(int examenId, String motif) {
        dao.updateStatut(examenId, Examen.STATUT_ANNULE, nvl(motif));
    }

    // ════════════════════════════════════════════════════════════
    //  Lectures
    // ════════════════════════════════════════════════════════════
    public List<Examen> getMesExamens(int enseignantId) {
        return dao.findByEnseignant(enseignantId);
    }

    public List<Examen> getTousLesExamens() {
        return dao.findAll();
    }

    public List<Examen> getEnAttente() {
        return dao.findEnAttente();
    }

    public List<Examen> getExamensPourClasse(int classeId, boolean validesSeulement) {
        return validesSeulement
                ? dao.findByClasseValide(classeId)
                : dao.findByClasse(classeId);
    }

    public long countEnAttente() {
        return dao.countEnAttente();
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers privés
    // ════════════════════════════════════════════════════════════

    /** Notifie l'enseignant auteur de la décision */
    private void notifierEnseignant(Examen e, boolean accepte, String commentaire) {
        Utilisateur enseignant = utilisateurDAO.findAll().stream()
                .filter(u -> u.getId() == e.getEnseignantId())
                .findFirst().orElse(null);
        if (enseignant == null) return;

        String icone = accepte ? "✅" : "❌";
        String statut = accepte ? "VALIDÉE" : "REFUSÉE";
        String msg = icone + " Votre demande d'" + e.getType().toLowerCase()
                + " a été " + statut
                + " | " + nvl(e.getTitre())
                + " | " + nvl(e.getClasseNom())
                + " | " + formatDate(e.getDateExamen())
                + (commentaire != null && !commentaire.isBlank()
                ? " | Commentaire : " + commentaire : "");

        saveNotif(enseignant.getId(), accepte ? "INFO" : "ALERTE", msg);

        if (isGmail(enseignant)) {
            final String sujet = icone + " Demande d'" + e.getType().toLowerCase()
                    + " " + statut.toLowerCase() + " — " + nvl(e.getTitre());
            final String corps = buildEmailEnseignant(e, enseignant, accepte, commentaire);
            new Thread(() -> EmailService.sendNotification(enseignant, sujet, corps),
                    "email-examen-ens").start();
        }
    }

    private void saveNotif(int userId, String type, String message) {
        Notification n = new Notification();
        n.setUtilisateurId(userId);
        n.setType(type);
        n.setMessage(message);
        n.setDateEnvoi(LocalDateTime.now());
        notifDAO.save(n);
    }

    // ── Corps emails ──────────────────────────────────────────────

    private String buildEmailGestionnaire(Examen e, Utilisateur u) {
        return "Bonjour " + u.getNomComplet() + ",\n\n"
                + "L'enseignant " + nvl(e.getEnseignantNom())
                + " a soumis une demande de "
                + e.getType().toLowerCase() + " :\n\n"
                + "  • Titre    : " + nvl(e.getTitre()) + "\n"
                + "  • Type     : " + e.getTypeIcon() + " " + e.getType() + "\n"
                + "  • Classe   : " + nvl(e.getClasseNom()) + "\n"
                + "  • Matière  : " + nvl(e.getMatiereNom()) + "\n"
                + "  • Date     : " + formatDate(e.getDateExamen()) + "\n"
                + "  • Durée    : " + e.getDureeMinutes() + " minutes\n"
                + "  • Salle    : " + (e.getSalleNumero() != null
                ? e.getSalleNumero() : "Non précisée") + "\n\n"
                + (e.getDescription() != null && !e.getDescription().isBlank()
                ? "Description : " + e.getDescription() + "\n\n" : "")
                + "Connectez-vous à UNIV-SCHEDULER pour valider ou refuser cette demande.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
    }

    private String buildEmailEnseignant(Examen e, Utilisateur ens,
                                        boolean accepte, String commentaire) {
        String statut = accepte ? "VALIDÉE ✅" : "REFUSÉE ❌";
        return "Bonjour " + ens.getNomComplet() + ",\n\n"
                + "Votre demande d'" + e.getType().toLowerCase()
                + " a été " + statut + ".\n\n"
                + "  • Titre    : " + nvl(e.getTitre()) + "\n"
                + "  • Classe   : " + nvl(e.getClasseNom()) + "\n"
                + "  • Matière  : " + nvl(e.getMatiereNom()) + "\n"
                + "  • Date     : " + formatDate(e.getDateExamen()) + "\n"
                + "  • Salle    : " + (e.getSalleNumero() != null
                ? e.getSalleNumero() : "Non précisée") + "\n\n"
                + (commentaire != null && !commentaire.isBlank()
                ? "Commentaire du gestionnaire : " + commentaire + "\n\n" : "")
                + "Cordialement,\nUNIV-SCHEDULER";
    }

    private String formatDate(String dateExamen) {
        if (dateExamen == null) return "—";
        return dateExamen.replace("T", " à ").replace("-", "/");
    }

    private boolean isGmail(Utilisateur u) {
        return u != null && u.getEmail() != null
                && u.getEmail().toLowerCase().endsWith("@gmail.com");
    }

    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "—"; }
}
