package com.univscheduler.model;

import com.univscheduler.dao.NotificationDAO;
import com.univscheduler.dao.ReservationDAO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service de rappel automatique de fin de réservation.
 * Corrigé : le planificateur est recréé à chaque appel de demarrer()
 * pour éviter le RejectedExecutionException après une déconnexion/reconnexion.
 */
public class Servicerappel {

    // ── Singleton ─────────────────────────────────────────────────
    private static Servicerappel instance;

    public static synchronized Servicerappel getInstance() {
        if (instance == null) instance = new Servicerappel();
        return instance;
    }

    // ── Constantes ────────────────────────────────────────────────
    private static final int CHECK_INTERVALLE_MINUTES = 1;
    private static final int RAPPEL_AVANT_MINUTES     = 15;

    // ── Dépendances ───────────────────────────────────────────────
    // ✅ CORRIGÉ : planificateur non final — recréé à chaque demarrer()
    private ScheduledExecutorService planificateur;

    private final ReservationDAO  reservationDAO  = new ReservationDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // IDs déjà notifiés — remis à zéro à chaque nouvelle session
    private final Set<Integer> dejaNotifies = new HashSet<>();

    // ── Constructeur privé ────────────────────────────────────────
    private Servicerappel() {}

    // ── API publique ──────────────────────────────────────────────

    /**
     * Démarre le service.
     * ✅ CORRIGÉ : si le planificateur précédent est arrêté (après déconnexion),
     * on en crée un nouveau avant de planifier la tâche.
     */
    public void demarrer() {
        // ✅ Recréer le planificateur s'il est arrêté ou inexistant
        if (planificateur == null || planificateur.isShutdown() || planificateur.isTerminated()) {
            planificateur = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ServiceRappel");
                t.setDaemon(true);
                return t;
            });
            // ✅ Réinitialiser les réservations déjà notifiées pour la nouvelle session
            dejaNotifies.clear();
        }

        planificateur.scheduleAtFixedRate(
                this::verifierReservations,
                0,
                CHECK_INTERVALLE_MINUTES,
                TimeUnit.MINUTES
        );
        System.out.println("[ServiceRappel] Démarré — vérification toutes les "
                + CHECK_INTERVALLE_MINUTES + " min, rappel "
                + RAPPEL_AVANT_MINUTES + " min avant la fin.");
    }

    /**
     * Arrête proprement le service.
     * À appeler à la déconnexion.
     */
    public void arreter() {
        if (planificateur != null && !planificateur.isShutdown()) {
            planificateur.shutdown();
            System.out.println("[ServiceRappel] Arrêté.");
        }
    }

    // ── Logique interne ───────────────────────────────────────────

    private void verifierReservations() {
        try {
            List<Reservation> aNotifier =
                    reservationDAO.findReservationsTerminantBientot(RAPPEL_AVANT_MINUTES);

            for (Reservation r : aNotifier) {
                if (dejaNotifies.contains(r.getId())) continue;
                envoyerRappel(r);
                dejaNotifies.add(r.getId());
            }
        } catch (Exception e) {
            System.err.println("[ServiceRappel] Erreur : " + e.getMessage());
        }
    }

    private void envoyerRappel(Reservation r) {
        Notification n = new Notification();
        n.setUtilisateurId(r.getUtilisateurId());
        n.setType("RAPPEL");
        n.setMessage("⏰ Rappel : votre réservation de la salle "
                + r.getSalleNumero()
                + " (" + r.getMotif() + ")"
                + " se termine dans " + RAPPEL_AVANT_MINUTES + " minutes.");
        notificationDAO.save(n);
        System.out.println("[ServiceRappel] Rappel envoyé → réservation #"
                + r.getId() + " | utilisateur #" + r.getUtilisateurId()
                + " | salle " + r.getSalleNumero());
    }
}