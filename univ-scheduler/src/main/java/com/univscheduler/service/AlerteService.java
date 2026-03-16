package com.univscheduler.service;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * Service de fond qui tourne en parallèle de l'application.
 * Vérifie toutes les 15 minutes :
 *  - Les réservations qui expirent dans 1 heure → rappel email + notification in-app
 *  - Les conflits persistants → alerte admin
 */
public class AlerteService {

    private static final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AlerteService");
            t.setDaemon(true); // stops when JavaFX exits
            return t;
        });
    private static volatile boolean running = false;

    public static void start() {
        if (running) return;
        running = true;
        scheduler.scheduleAtFixedRate(AlerteService::tick, 1, 15, TimeUnit.MINUTES);
        System.out.println("[AlerteService] Démarré — vérification toutes les 15 min");
    }

    public static void stop() {
        scheduler.shutdownNow();
        running = false;
        System.out.println("[AlerteService] Arrêté");
    }

    /** Main tick — called every 15 minutes */
    private static void tick() {
        try {
            checkReservationsExpirant();
        } catch (Exception e) {
            System.err.println("[AlerteService] Erreur tick : " + e.getMessage());
        }
    }

    /**
     * Sends an in-app + email reminder for reservations expiring within 1 hour.
     */
    private static void checkReservationsExpirant() {
        ReservationDAO reservDAO  = new ReservationDAO();
        UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
        NotificationDAO notifDAO  = new NotificationDAO();

        List<Reservation> all = reservDAO.findAll();
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime inOneHour = now.plusHours(1);

        for (Reservation r : all) {
            if (!"VALIDEE".equals(r.getStatut())) continue;
            if (r.getDateReservation() == null) continue;
            // Reminder window: reservation ends within next hour
            if (r.getDateReservation().isAfter(now) && r.getDateReservation().isBefore(inOneHour)) {
                // In-app notification
                Notification n = new Notification();
                n.setMessage("⏰ Rappel : votre réservation salle " + r.getSalleNumero()
                    + " se termine à " + r.getDateReservation().toLocalTime());
                n.setType("RAPPEL");
                n.setUtilisateurId(r.getUtilisateurId());
                notifDAO.save(n);

                // Email
                utilisateurDAO.findAll().stream()
                    .filter(u -> u.getId() == r.getUtilisateurId())
                    .findFirst()
                    .ifPresent(u -> EmailService.envoyerRappelFinReservation(u, r));

                System.out.println("[AlerteService] Rappel envoyé pour réservation id=" + r.getId());
            }
        }
    }

    /**
     * Sends an immediate conflict alert (in-app + email) to the first admin found.
     * Called from GestionnaireDashboardController when a conflict is detected.
     */
    public static void alerterConflit(String details) {
        NotificationDAO notifDAO = new NotificationDAO();
        UtilisateurDAO  userDAO  = new UtilisateurDAO();

        // Notify all admins and gestionnaires
        userDAO.findAll().stream()
            .filter(u -> "ADMIN".equals(u.getRole()) || "GESTIONNAIRE".equals(u.getRole()))
            .forEach(u -> {
                Notification n = new Notification();
                n.setMessage("⚠️ CONFLIT DÉTECTÉ : " + details);
                n.setType("ALERTE");
                n.setUtilisateurId(u.getId());
                notifDAO.save(n);
                EmailService.alerterConflitAdmin(u, details);
            });
    }

    /**
     * Convenience: notify a single user by id with a custom message.
     */
    public static void notifierUtilisateur(int userId, String message, String type) {
        NotificationDAO notifDAO = new NotificationDAO();
        Notification n = new Notification();
        n.setMessage(message);
        n.setType(type);
        n.setUtilisateurId(userId);
        notifDAO.save(n);
    }
}
