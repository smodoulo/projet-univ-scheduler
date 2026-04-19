package com.univscheduler.service;

import com.univscheduler.dao.CoursDAO;
import com.univscheduler.dao.DemandeDisponibiliteDAO;
import com.univscheduler.dao.NotificationDAO;
import com.univscheduler.dao.UtilisateurDAO;
import com.univscheduler.model.Cours;
import com.univscheduler.model.DemandeDisponibilite;
import com.univscheduler.model.Notification;
import com.univscheduler.model.StatutDisponibilite;
import com.univscheduler.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service métier pour la gestion des demandes de disponibilité.
 *
 * Flux complet :
 *   ENSEIGNANT → soumettre()  → demande EN_ATTENTE
 *   GESTIONNAIRE → accepter() → cours.creneau_id mis à jour (si pas de conflit)
 *   GESTIONNAIRE → refuser()  → demande REFUSE + notification enseignant
 */
public class DisponibiliteService {

    private final DemandeDisponibiliteDAO demandeDAO     = new DemandeDisponibiliteDAO();
    private final CoursDAO                coursDAO       = new CoursDAO();
    private final NotificationDAO         notifDAO       = new NotificationDAO();
    private final UtilisateurDAO          utilisateurDAO = new UtilisateurDAO();

    // ════════════════════════════════════════════════════════════════
    //  1. ENSEIGNANT — soumettre une demande
    // ════════════════════════════════════════════════════════════════

    /**
     * Persiste la demande (statut EN_ATTENTE) et notifie les gestionnaires.
     */
    public void soumettre(DemandeDisponibilite demande) {
        demande.setStatut(StatutDisponibilite.EN_ATTENTE);
        if (demande.getDateDemande() == null)
            demande.setDateDemande(LocalDateTime.now());

        demandeDAO.save(demande);

        // Recharger pour enrichir matiereNom, classeNom, creneauInfo…
        DemandeDisponibilite enrichie = demandeDAO.findById(demande.getId());
        if (enrichie == null) enrichie = demande;

        final DemandeDisponibilite d = enrichie;

        // ── Notification in-app à chaque gestionnaire / admin ──────
        utilisateurDAO.findAll().stream()
                .filter(u -> "GESTIONNAIRE".equals(u.getRole())
                        || "ADMIN".equals(u.getRole()))
                .forEach(u -> {
                    Notification n = new Notification();
                    n.setUtilisateurId(u.getId());
                    n.setType("INFO");
                    n.setMessage("📅 Nouvelle demande de disponibilité #" + d.getId()
                            + " | Enseignant : " + nvl(d.getEnseignantNom())
                            + " | Cours : " + nvl(d.getMatiereNom())
                            + " (" + nvl(d.getClasseNom()) + ")"
                            + " | Créneau proposé : " + nvl(d.getCreneauInfo())
                            + (notEmpty(d.getCommentaire())
                            ? " | Commentaire : " + d.getCommentaire() : ""));
                    n.setDateEnvoi(LocalDateTime.now());
                    notifDAO.save(n);
                });

        // ── Email Gmail gestionnaires (thread background) ───────────
        final DemandeDisponibilite dFinal = enrichie;
        new Thread(() ->
                utilisateurDAO.findAll().stream()
                        .filter(u -> ("GESTIONNAIRE".equals(u.getRole())
                                || "ADMIN".equals(u.getRole()))
                                && isGmail(u))
                        .forEach(u -> EmailService.sendNotification(u,
                                "📅 Demande de disponibilité — "
                                        + nvl(dFinal.getMatiereNom())
                                        + " (" + nvl(dFinal.getClasseNom()) + ")",
                                buildEmailGestionnaire(dFinal, u))),
                "email-dispo-gests").start();
    }

    // ════════════════════════════════════════════════════════════════
    //  2. GESTIONNAIRE — accepter  (surcharge sans commentaire)
    // ════════════════════════════════════════════════════════════════

    /** Accepte sans commentaire additionnel. */
    public boolean accepter(int demandeId) {
        return accepter(demandeId, null);
    }

    /**
     * Accepte une demande :
     *  - vérifie conflit enseignant + salle
     *  - OK   → UPDATE cours.creneau_id + statut = ACCEPTE
     *  - KO   → statut = CONFLIT (cours non modifié)
     *
     * @return true si accepté sans conflit, false si conflit détecté
     */
    public boolean accepter(int demandeId, String commentaire) {
        DemandeDisponibilite demande = demandeDAO.findById(demandeId);
        if (demande == null) return false;

        Cours cours = coursDAO.findById(demande.getCoursId());
        if (cours == null) return false;

        boolean conflit = hasConflict(
                demande.getEnseignantId(),
                cours.getSalleId(),
                demande.getCreneauPropose(),
                cours.getDate() != null ? cours.getDate().toString() : "",
                demande.getCoursId());

        if (conflit) {
            demandeDAO.updateStatut(demandeId, StatutDisponibilite.CONFLIT);
            notifierEnseignant(demande,
                    "⚠️ Votre demande de disponibilité #" + demandeId
                            + " a été acceptée MAIS un conflit a été détecté"
                            + " sur le créneau " + nvl(demande.getCreneauInfo())
                            + ". Le créneau du cours n'a pas été modifié.",
                    "ALERTE");
            return false;
        }

        // Pas de conflit → mise à jour BDD
        coursDAO.updateCreneau(demande.getCoursId(), demande.getCreneauPropose());
        demandeDAO.updateStatut(demandeId, StatutDisponibilite.ACCEPTE);

        notifierEnseignant(demande,
                "✅ Votre demande de disponibilité #" + demandeId + " a été ACCEPTÉE !"
                        + " | Cours : " + nvl(demande.getMatiereNom())
                        + " (" + nvl(demande.getClasseNom()) + ")"
                        + " | Nouveau créneau : " + nvl(demande.getCreneauInfo())
                        + "\nVotre emploi du temps a été mis à jour automatiquement."
                        + (notEmpty(commentaire) ? "\n💬 Commentaire : " + commentaire : ""),
                "INFO");
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  3. GESTIONNAIRE — refuser
    // ════════════════════════════════════════════════════════════════

    /** Refuse sans motif. */
    public void refuser(int demandeId) {
        refuser(demandeId, null);
    }

    public void refuser(int demandeId, String motif) {
        DemandeDisponibilite demande = demandeDAO.findById(demandeId);
        if (demande == null) return;

        demandeDAO.updateStatut(demandeId, StatutDisponibilite.REFUSE);

        String msg = "❌ Votre demande de disponibilité #" + demandeId + " a été REFUSÉE."
                + " | Cours : " + nvl(demande.getMatiereNom())
                + " (" + nvl(demande.getClasseNom()) + ")"
                + " | Créneau proposé : " + nvl(demande.getCreneauInfo())
                + (notEmpty(motif) ? "\n💬 Motif : " + motif : "");

        notifierEnseignant(demande, msg, "ALERTE");
    }

    // ════════════════════════════════════════════════════════════════
    //  Lectures
    // ════════════════════════════════════════════════════════════════

    public List<DemandeDisponibilite> getMesDemandesEnseignant(int enseignantId) {
        return demandeDAO.findByEnseignant(enseignantId);
    }

    public List<DemandeDisponibilite> getDemandesEnAttente() {
        return demandeDAO.findEnAttente();
    }

    public List<DemandeDisponibilite> getToutesLesDemandes() {
        return demandeDAO.findAll();
    }

    public long countEnAttente() {
        return demandeDAO.countEnAttente();
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers privés
    // ════════════════════════════════════════════════════════════════

    private boolean hasConflict(int enseignantId, int salleId,
                                int creneauId, String date, int exclureCoursId) {
        return coursDAO.hasConflitEnseignant(enseignantId, creneauId, date, exclureCoursId)
                || coursDAO.hasConflitSalle(salleId, creneauId, date, exclureCoursId);
    }

    private void notifierEnseignant(DemandeDisponibilite demande,
                                    String message, String type) {
        // In-app
        Notification n = new Notification();
        n.setUtilisateurId(demande.getEnseignantId());
        n.setType(type);
        n.setMessage(message);
        n.setDateEnvoi(LocalDateTime.now());
        notifDAO.save(n);

        // Email Gmail (background)
        utilisateurDAO.findAll().stream()
                .filter(u -> u.getId() == demande.getEnseignantId() && isGmail(u))
                .findFirst()
                .ifPresent(u -> {
                    final String sujet = "📅 Demande de disponibilité #"
                            + demande.getId() + " — " + nvl(demande.getStatut());
                    new Thread(() -> EmailService.sendNotification(u, sujet,
                            "Bonjour " + u.getNomComplet() + ",\n\n"
                                    + message + "\n\nCordialement,\nUNIV-SCHEDULER"),
                            "email-dispo-ens").start();
                });
    }

    private String buildEmailGestionnaire(DemandeDisponibilite d, Utilisateur gestionnaire) {
        return "Bonjour " + gestionnaire.getNomComplet() + ",\n\n"
                + "Une nouvelle demande de disponibilité a été soumise :\n\n"
                + "  Enseignant      : " + nvl(d.getEnseignantNom()) + "\n"
                + "  Cours           : " + nvl(d.getMatiereNom())
                + " (" + nvl(d.getClasseNom()) + ")\n"
                + "  Créneau proposé : " + nvl(d.getCreneauInfo()) + "\n"
                + "  Commentaire     : " + (notEmpty(d.getCommentaire())
                ? d.getCommentaire() : "—") + "\n\n"
                + "Connectez-vous à UNIV-SCHEDULER pour traiter cette demande.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
    }

    private static boolean isGmail(Utilisateur u) {
        return u != null && u.getEmail() != null
                && u.getEmail().toLowerCase().endsWith("@gmail.com");
    }

    private static String nvl(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isBlank();
    }
}