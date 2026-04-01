package com.univscheduler.service;

import com.univscheduler.model.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static boolean simulationMode = false; // ← DÉSACTIVÉ par défaut
    private static String smtpHost     = "smtp.gmail.com";
    private static int    smtpPort     = 587;
    private static String smtpUser     = "lo065657@gmail.com";       // ← hardcodé
    private static String smtpPassword = "dcysswrxhnddsklq"; // ← remplacez ici

    static {
        System.out.println(">>> [EmailService] Répertoire de travail : "
                + new java.io.File(".").getAbsolutePath());

        try {
            java.io.File f = new java.io.File("email.properties");
            System.out.println(">>> [EmailService] Fichier email.properties trouvé : " + f.exists());
            System.out.println(">>> [EmailService] Chemin absolu : " + f.getAbsolutePath());

            if (f.exists()) {
                Properties p = new Properties();
                p.load(new java.io.FileInputStream(f));
                smtpHost      = p.getProperty("smtp.host",     smtpHost);
                smtpPort      = Integer.parseInt(p.getProperty("smtp.port", String.valueOf(smtpPort)));
                smtpUser      = p.getProperty("smtp.user",     smtpUser);
                smtpPassword  = p.getProperty("smtp.password", smtpPassword);
                simulationMode = !"true".equalsIgnoreCase(
                        p.getProperty("smtp.enabled", "true"));
                System.out.println(">>> [EmailService] Config chargée depuis email.properties");
            } else {
                System.out.println(">>> [EmailService] Fichier absent — utilisation des valeurs hardcodées");
            }
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Erreur chargement config : " + e.getMessage());
        }

        System.out.println(">>> [EmailService] Mode simulation : " + simulationMode);
        System.out.println(">>> [EmailService] Utilisateur SMTP : " + smtpUser);
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
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host",             smtpHost);
            props.put("mail.smtp.port",             String.valueOf(smtpPort));

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
            System.out.println("[EMAIL ENVOYÉ] ✅ À : " + user.getEmail());
        } catch (Exception e) {
            System.err.println("[EMAIL ERREUR] ❌ " + e.getMessage());
            e.printStackTrace(); // ← pour voir l'erreur complète si ça échoue
        }
    }

    // ========================= TYPED NOTIFICATIONS =========================

    public static void notifierValidationReservation(Utilisateur user, Reservation r) {
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

    public static void notifierRefusReservation(Utilisateur user, Reservation r) {
        String subject = "❌ Réservation refusée — Salle " + r.getSalleNumero();
        String body = "Bonjour " + user.getNomComplet() + ",\n\n"
                + "Votre réservation de la salle " + r.getSalleNumero()
                + " a été refusée.\n"
                + "  • Motif demandé : " + r.getMotif() + "\n\n"
                + "Veuillez contacter la gestion pour plus d'informations.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(user, subject, body);
    }

    public static void notifierChangementSalle(Utilisateur user,
                                               Cours cours, String ancienneSalle) {
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

    public static void envoyerRappelFinReservation(Utilisateur user, Reservation r) {
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

    public static void alerterConflitAdmin(Utilisateur admin, String details) {
        String subject = "⚠️ Conflit de planification détecté";
        String body = "Bonjour " + admin.getNomComplet() + ",\n\n"
                + "Un conflit a été détecté lors de la planification :\n\n"
                + details + "\n\n"
                + "Veuillez intervenir dès que possible.\n\n"
                + "Cordialement,\nUNIV-SCHEDULER";
        sendNotification(admin, subject, body);
    }

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
                + "  📅 Date     : " + reserv.getDateReservation().toLocalDate() + "\n"
                + "  ⏰ Horaire  : "
                + reserv.getDateReservation().toLocalTime().getHour()
                + "h00 → "
                + reserv.getDateFin().toLocalTime().getHour() + "h00\n"
                + "  📝 Motif    : " + reserv.getMotif() + "\n\n"
                + "Cordialement,\nUniv-Scheduler";
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