package com.univscheduler.service;

import com.univscheduler.dao.*;
import com.univscheduler.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service pour générer les statistiques d'occupation, les rapports
 * hebdomadaires/mensuels, et identifier les salles critiques.
 */
public class RapportService {

    private final CoursDAO coursDAO = new CoursDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final ReservationDAO reservDAO = new ReservationDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    /** Total number of schedulable slots per week: 5 days × 5 time slots */
    private static final int TOTAL_CRENEAUX_SEMAINE = 25;

    /**
     * Calculates the occupation rate (%) for each room.
     * @return Map: room number -> occupation rate (0.0 to 100.0)
     */
    public Map<String, Double> getTauxOccupation() {
        Map<String, Double> taux = new LinkedHashMap<>();
        List<Salle> salles = salleDAO.findAll();
        Map<Integer, Integer> coursParSalle = coursDAO.countBySalle();

        for (Salle s : salles) {
            int nbCours = coursParSalle.getOrDefault(s.getId(), 0);
            double occupation = (nbCours / (double) TOTAL_CRENEAUX_SEMAINE) * 100.0;
            taux.put(s.getNumero(), Math.min(Math.round(occupation * 10.0) / 10.0, 100.0));
        }
        return taux;
    }

    /**
     * Returns the overall campus occupation rate (%).
     */
    public double getTauxOccupationGlobal() {
        Map<String, Double> taux = getTauxOccupation();
        if (taux.isEmpty()) return 0.0;
        double sum = taux.values().stream().mapToDouble(Double::doubleValue).sum();
        return Math.round((sum / taux.size()) * 10.0) / 10.0;
    }

    /**
     * Returns rooms with occupation >= threshold (default 70%).
     */
    public List<Salle> getSallesCritiques(double threshold) {
        List<Salle> critiques = new ArrayList<>();
        Map<String, Double> taux = getTauxOccupation();
        List<Salle> salles = salleDAO.findAll();
        for (Salle s : salles) {
            double t = taux.getOrDefault(s.getNumero(), 0.0);
            if (t >= threshold) critiques.add(s);
        }
        return critiques;
    }

    public List<Salle> getSallesCritiques() {
        return getSallesCritiques(70.0);
    }

    /**
     * Returns a map of salle number -> occupation rate for all rooms.
     * Used for the interactive map coloring.
     */
    public Map<String, Double> getTauxParSalleNumero() {
        return getTauxOccupation();
    }

    /**
     * Generates a weekly usage report as a structured map.
     */
    public Map<String, Object> getRapportHebdomadaire() {
        Map<String, Object> rapport = new LinkedHashMap<>();
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        rapport.put("titre", "Rapport Hebdomadaire");
        rapport.put("periode", "Du " + monday.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " au " + sunday.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        rapport.put("totalCours", coursDAO.count());
        rapport.put("tauxOccupationGlobal", getTauxOccupationGlobal() + "%");
        rapport.put("sallesCritiques", getSallesCritiques().size());
        rapport.put("coursParJour", coursDAO.countByJour());
        rapport.put("coursParStatut", coursDAO.countByStatut());
        rapport.put("totalSalles", salleDAO.count());
        rapport.put("sallesDisponibles", salleDAO.countDisponibles());
        rapport.put("reservationsEnAttente", getReservationsEnAttente());
        return rapport;
    }

    /**
     * Generates a monthly usage report.
     */
    public Map<String, Object> getRapportMensuel() {
        Map<String, Object> rapport = new LinkedHashMap<>();
        rapport.put("titre", "Rapport Mensuel");
        rapport.put("mois", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        rapport.put("totalCours", coursDAO.count());
        rapport.put("tauxOccupationGlobal", getTauxOccupationGlobal() + "%");
        rapport.put("sallesCritiques", getSallesCritiques().size());
        rapport.put("coursParJour", coursDAO.countByJour());
        rapport.put("coursParStatut", coursDAO.countByStatut());
        rapport.put("tauxOccupationParSalle", getTauxOccupation());
        rapport.put("totalUtilisateurs", utilisateurDAO.findAll().size());
        rapport.put("enseignants", utilisateurDAO.countByRole("ENSEIGNANT"));
        rapport.put("etudiants", utilisateurDAO.countByRole("ETUDIANT"));
        return rapport;
    }

    private long getReservationsEnAttente() {
        return reservDAO.findAll().stream()
                .filter(r -> "EN_ATTENTE".equals(r.getStatut())).count();
    }

    /**
     * Returns a summary line for a room, suitable for display.
     */
    public String getResumeSalle(Salle salle) {
        Map<String, Double> taux = getTauxOccupation();
        double t = taux.getOrDefault(salle.getNumero(), 0.0);
        String niveau = t >= 80 ? "🔴 CRITIQUE" : t >= 50 ? "🟡 ÉLEVÉ" : "🟢 NORMAL";
        return salle.getNumero() + " | " + salle.getTypeSalle() + " | cap:" + salle.getCapacite()
                + " | Occupation: " + t + "% " + niveau;
    }
}
