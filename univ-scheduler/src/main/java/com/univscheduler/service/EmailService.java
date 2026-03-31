package com.univscheduler.service;

import com.univscheduler.model.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static boolean simulationMode = true;
    private static String smtpHost     = "smtp.gmail.com";
    private static int    smtpPort     = 587;
    private static String smtpUser     = "";
    private static String smtpPassword = "";

    static {
        try {
            java.io.File f = new java.io.File("email.properties");
            if (f.exists()) {
                Properties p = new Properties();
                p.load(new java.io.FileInputStream(f));
                smtpHost      = p.getProperty("smtp.host",     "smtp.gmail.com");
                smtpPort      = Integer.parseInt(p.getProperty("smtp.port", "587"));
                smtpUser      = p.getProperty("smtp.user",     "");
                smtpPassword  = p.getProperty("smtp.password", "");
                simulationMode = !"true".equalsIgnoreCase(
                        p.getProperty("smtp.enabled", "false"));
            }
        } catch (Exception ignored) {}
    }

    // ========================= CORE SEND =========================

    public static void sendNotification(Utilisateur user,
                                        String subject, String body) {
        if (user == null || user.getEmail() == null
                || user.getEmail().isBlank()) return;

        if (simulationMode) {
            System.out.println(
                    "┌─ [EMAIL SIMULÉ] ────────────────────────────────────");
            System.out.println("│ À       : " + user.getEmail());
            System.out.println("│ Sujet   : " + subject);
            System.out.println("│ Corps   : "
                    + body.replace("\n", "\n│          "));
            System.out.println(
                    "└──────────────────────────────────────────────────────");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth",           "true");
            props.put("mail.smtp.starttls.enable","true");
            props.put("mail.smtp.host",            smtpHost);
            props.put("mail.smtp.port",            String.valueOf(smtpPort));

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(smtpUser, "UNIV-SCHEDULER"));
            msg.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(user.getEmail()));
            msg.setSubject(subject);

            MimeBodyPart textPart = new MimeBodyPart();
            String html = buildHtmlEmail(subject, body, user.getNomComplet());
            textPart.setContent(html, "text/html; charset=UTF-8");
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(textPart);
            msg.setContent(mp);

            Transport.send(msg);
            System.out.println("[EMAIL ENVOYÉ] À : " + user.getEmail());
        } catch (Exception e) {
            System.err.println("[EMAIL ERREUR] " + e.getMessage());
        }
    }

    // ========================= TYPED NOTIFICATIONS =========================

    /** Notification de validation de réservation */
    public static void notifierValidationReservation(Utilisateur user,
                                                     Reservation r) {
        String subject = "✅ Réservation validée — Salle " + r.getSalleNumero();
        String body = "Bonjour " + user.getNomComplet() + ",\n\n"
                + "Votre réservation a été validée :\n"
                + "  • Salle    : " + r.getSalleNumero() + "\n"
                + "  • Motif    : " + r.getMotif() + "\n"
                + "  • Date     : " + (r.getDateReservation() != null
                ? r.getDateReservation().toLocalDate() : "N/A") + "\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(user, subject, body);
    }

    /** Notification de refus de réservation */
    public static void notifierRefusReservation(Utilisateur user,
                                                Reservation r) {
        String subject = "❌ Réservation refusée — Salle " + r.getSalleNumero();
        String body = "Bonjour " + user.getNomComplet() + ",\n\n"
                + "Votre réservation de la salle " + r.getSalleNumero()
                + " a été refusée.\n"
                + "  • Motif demandé : " + r.getMotif() + "\n\n"
                + "Veuillez contacter la gestion pour plus d'informations.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(user, subject, body);
    }

    /** Notification de changement de salle pour un cours */
    public static void notifierChangementSalle(Utilisateur user,
                                               Cours cours,
                                               String ancienneSalle) {
        String subject = "🔔 Changement de salle — " + cours.getMatiereNom();
        String body = "Bonjour " + user.getNomComplet() + ",\n\n"
                + "La salle de votre cours a été modifiée :\n"
                + "  • Cours          : " + cours.getMatiereNom()
                + " — " + cours.getClasseNom() + "\n"
                + "  • Ancienne salle : " + ancienneSalle + "\n"
                + "  • Nouvelle salle : " + cours.getSalleNumero() + "\n"
                + "  • Créneau        : " + cours.getCreneauInfo() + "\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(user, subject, body);
    }

    /** Rappel de fin de réservation */
    public static void envoyerRappelFinReservation(Utilisateur user,
                                                   Reservation r) {
        String subject = "⏰ Rappel — Fin de réservation bientôt";
        String body = "Bonjour " + user.getNomComplet() + ",\n\n"
                + "Votre réservation de la salle " + r.getSalleNumero()
                + " se termine bientôt.\n"
                + "  • Motif  : " + r.getMotif() + "\n"
                + "  • Heure  : " + (r.getDateReservation() != null
                ? r.getDateReservation().toLocalTime() : "N/A") + "\n\n"
                + "Merci de libérer la salle à l'heure prévue.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(user, subject, body);
    }

    /** Alerte conflit de planification à l'admin */
    public static void alerterConflitAdmin(Utilisateur admin, String details) {
        String subject = "⚠️ Conflit de planification détecté";
        String body = "Bonjour " + admin.getNomComplet() + ",\n\n"
                + "Un conflit a été détecté lors de la planification :\n\n"
                + details + "\n\n"
                + "Veuillez intervenir dès que possible.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(admin, subject, body);
    }

    // ✅ Notifier un étudiant d'une nouvelle réservation de l'enseignant
    public static void notifierEtudiantReservation(Utilisateur etudiant,
                                                   Reservation reserv,
                                                   String enseignantNom) {
        String sujet = "📌 Nouvelle réservation de salle — "
                + reserv.getSalleNumero()
                + " le " + reserv.getDateReservation().toLocalDate();

        String corps = "Bonjour " + etudiant.getPrenom() + ",\n\n"
                + "Votre enseignant " + enseignantNom
                + " a effectué une réservation de salle :\n\n"
                + "  📍 Salle    : " + reserv.getSalleNumero()    + "\n"
                + "  📅 Date     : "
                + reserv.getDateReservation().toLocalDate()   + "\n"
                + "  ⏰ Horaire  : "
                + reserv.getDateReservation().toLocalTime().getHour()
                + "h00 → "
                + reserv.getDateFin().toLocalTime().getHour() + "h00\n"
                + "  📝 Motif    : " + reserv.getMotif()          + "\n\n"
                + "Cordialement,\nUniv-Scheduler";

        // ✅ sendNotification() au lieu de envoyerEmail() qui n'existe pas
        sendNotification(etudiant, sujet, corps);
    }

    // ========================= HTML TEMPLATE =========================

    private static String buildHtmlEmail(String subject, String body,
                                         String nomComplet) {
        String bodyHtml = body
                .replace("\n", "<br>")
                .replace("  •", "&nbsp;&nbsp;•");
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,"
                + "Arial,sans-serif;background:#f0f4f8;margin:0;padding:20px;'>"
                + "<div style='max-width:520px;margin:auto;background:white;"
                + "border-radius:12px;overflow:hidden;"
                + "box-shadow:0 4px 16px rgba(0,0,0,0.1);'>"
                + "<div style='background:#1e293b;padding:24px 32px;'>"
                + "<div style='color:#f8fafc;font-size:20px;font-weight:bold;'>"
                + "🎓 UNIV-SCHEDULER</div>"
                + "<div style='color:#94a3b8;font-size:13px;margin-top:4px;'>"
                + subject + "</div>"
                + "</div>"
                + "<div style='padding:32px;color:#1e293b;font-size:14px;"
                + "line-height:1.7;'>"
                + bodyHtml
                + "</div>"
                + "<div style='background:#f8fafc;padding:16px 32px;"
                + "color:#64748b;font-size:11px;"
                + "border-top:1px solid #e2e8f0;'>"
                + "Cet email a été envoyé automatiquement par UNIV-SCHEDULER."
                + " Ne pas répondre à cet email."
                + "</div></div></body></html>";
    }

    public static boolean isSimulationMode() { return simulationMode; }
}