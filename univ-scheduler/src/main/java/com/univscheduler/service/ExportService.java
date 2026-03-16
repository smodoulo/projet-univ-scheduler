package com.univscheduler.service;

import com.univscheduler.model.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ========================= PDF =========================

    public void exportCoursAsPDF(List<Cours> cours, File out) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            // Title page header
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float y = 800;
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Header band
            cs.setNonStrokingColor(0.118f, 0.161f, 0.235f); // #1e293b
            cs.addRect(0, 820, 600, 42); cs.fill();

            cs.setNonStrokingColor(1f, 1f, 1f);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.beginText(); cs.newLineAtOffset(30, 828); cs.showText("UNIV-SCHEDULER v2.1 - Emploi du Temps"); cs.endText();

            cs.setNonStrokingColor(0.4f, 0.5f, 0.6f);
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.beginText(); cs.newLineAtOffset(30, 812);
            cs.showText("Genere le : " + LocalDateTime.now().format(FMT) + "  |  Total : " + cours.size() + " cours");
            cs.endText();

            // Column headers
            y = 780;
            float[] cx = {30, 95, 215, 315, 400, 465, 530};
            String[] hdr = {"Date", "Matiere", "Enseignant", "Classe", "Creneau", "Salle", "Statut"};
            cs.setNonStrokingColor(0.235f, 0.510f, 0.965f);
            cs.addRect(25, y - 4, 550, 16); cs.fill();
            cs.setNonStrokingColor(1f, 1f, 1f);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            for (int i = 0; i < hdr.length; i++) {
                cs.beginText(); cs.newLineAtOffset(cx[i], y); cs.showText(hdr[i]); cs.endText();
            }

            // Rows
            y -= 20;
            boolean alt = false;
            for (Cours c : cours) {
                if (y < 50) {
                    cs.close();
                    PDPage np = new PDPage(PDRectangle.A4); doc.addPage(np);
                    cs = new PDPageContentStream(doc, np);
                    y = 800;
                }
                if (alt) {
                    cs.setNonStrokingColor(0.949f, 0.961f, 0.973f);
                    cs.addRect(25, y - 3, 550, 14); cs.fill();
                }
                alt = !alt;
                cs.setNonStrokingColor(0.18f, 0.22f, 0.29f);
                cs.setFont(PDType1Font.HELVETICA, 8);
                String[] vals = {
                    c.getDate() != null ? c.getDate().toString() : "-",
                    trunc(c.getMatiereNom(), 18), trunc(c.getEnseignantNom(), 17),
                    trunc(c.getClasseNom(), 14), trunc(c.getCreneauInfo(), 14),
                    s(c.getSalleNumero()), s(c.getStatut())
                };
                for (int i = 0; i < vals.length; i++) {
                    cs.beginText(); cs.newLineAtOffset(cx[i], y); cs.showText(vals[i]); cs.endText();
                }
                y -= 14;
            }

            // Footer
            cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
            cs.beginText(); cs.newLineAtOffset(30, 25);
            cs.showText("UNIV-SCHEDULER - Document confidentiel"); cs.endText();
            cs.close();
            doc.save(out);
        }
    }

    public void exportReservationsAsPDF(List<Reservation> reservations, File out) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(0.118f, 0.161f, 0.235f);
                cs.addRect(0, 820, 600, 42); cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.beginText(); cs.newLineAtOffset(30, 828);
                cs.showText("UNIV-SCHEDULER - Historique des Reservations"); cs.endText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.beginText(); cs.newLineAtOffset(30, 812);
                cs.showText("Genere le : " + LocalDateTime.now().format(FMT));
                cs.endText();

                float y = 780;
                float[] cx = {30, 150, 240, 330, 430};
                String[] hdr = {"Demandeur", "Motif", "Salle", "Date", "Statut"};
                cs.setNonStrokingColor(0.063f, 0.725f, 0.506f);
                cs.addRect(25, y - 4, 550, 16); cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
                for (int i = 0; i < hdr.length; i++) {
                    cs.beginText(); cs.newLineAtOffset(cx[i], y); cs.showText(hdr[i]); cs.endText();
                }
                y -= 20;
                boolean alt = false;
                for (Reservation r : reservations) {
                    if (alt) {
                        cs.setNonStrokingColor(0.949f, 0.961f, 0.973f);
                        cs.addRect(25, y - 3, 550, 14); cs.fill();
                    }
                    alt = !alt;
                    cs.setNonStrokingColor(0.18f, 0.22f, 0.29f);
                    cs.setFont(PDType1Font.HELVETICA, 8);
                    String[] vals = {
                        trunc(r.getUtilisateurNom(), 17), trunc(r.getMotif(), 17),
                        s(r.getSalleNumero()),
                        r.getDateReservation() != null ? r.getDateReservation().toLocalDate().toString() : "-",
                        s(r.getStatut())
                    };
                    for (int i = 0; i < vals.length; i++) {
                        cs.beginText(); cs.newLineAtOffset(cx[i], y); cs.showText(vals[i]); cs.endText();
                    }
                    y -= 14;
                    if (y < 50) break;
                }
                cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                cs.beginText(); cs.newLineAtOffset(30, 25);
                cs.showText("UNIV-SCHEDULER - Document confidentiel"); cs.endText();
            }
            doc.save(out);
        }
    }

    public void exportRapportPDF(Map<String, Object> rapport, File out) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(0.118f, 0.161f, 0.235f);
                cs.addRect(0, 800, 600, 62); cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.beginText(); cs.newLineAtOffset(30, 840);
                cs.showText("UNIV-SCHEDULER"); cs.endText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.beginText(); cs.newLineAtOffset(30, 820);
                cs.showText(s((String) rapport.get("titre"))); cs.endText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.beginText(); cs.newLineAtOffset(30, 806);
                cs.showText(s((String) rapport.get("periode"))); cs.endText();

                float y = 770;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                cs.setNonStrokingColor(0.235f, 0.510f, 0.965f);
                cs.beginText(); cs.newLineAtOffset(30, y); cs.showText("Indicateurs Cles"); cs.endText();
                y -= 20;

                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.setNonStrokingColor(0.18f, 0.22f, 0.29f);
                for (Map.Entry<String, Object> e : rapport.entrySet()) {
                    if (e.getKey().equals("coursParJour") || e.getKey().equals("coursParStatut")
                            || e.getKey().equals("tauxOccupationParSalle")) continue;
                    cs.beginText(); cs.newLineAtOffset(30, y);
                    cs.showText(e.getKey() + " : " + e.getValue()); cs.endText();
                    y -= 18;
                    if (y < 50) break;
                }

                cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                cs.beginText(); cs.newLineAtOffset(30, 25);
                cs.showText("UNIV-SCHEDULER - Document genere automatiquement le "
                        + LocalDateTime.now().format(FMT)); cs.endText();
            }
            doc.save(out);
        }
    }

    // ========================= EXCEL =========================

    public void exportCoursAsExcel(List<Cours> cours, File out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Emploi du Temps");
            CellStyle hStyle = headerStyle(wb, new short[]{0x17, 0x29, 0x3B});
            CellStyle altStyle = altStyle(wb);

            String[] headers = {"ID", "Date", "Matiere", "Enseignant", "Classe", "Creneau", "Salle", "Statut"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hStyle);
            }
            int row = 1;
            for (Cours c : cours) {
                Row r = sheet.createRow(row);
                CellStyle st = (row % 2 == 0) ? altStyle : null;
                setCell(r, 0, c.getId(), st);
                setCell(r, 1, c.getDate() != null ? c.getDate().toString() : "", st);
                setCell(r, 2, s(c.getMatiereNom()), st);
                setCell(r, 3, s(c.getEnseignantNom()), st);
                setCell(r, 4, s(c.getClasseNom()), st);
                setCell(r, 5, s(c.getCreneauInfo()), st);
                setCell(r, 6, s(c.getSalleNumero()), st);
                setCell(r, 7, s(c.getStatut()), st);
                row++;
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(out)) { wb.write(fos); }
        }
    }

    public void exportReservationsAsExcel(List<Reservation> reservations, File out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reservations");
            CellStyle hStyle = headerStyle(wb, new short[]{0x06, 0x47, 0x32});
            CellStyle altStyle = altStyle(wb);

            String[] headers = {"ID", "Demandeur", "Motif", "Salle", "Date", "Statut"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hStyle);
            }
            int row = 1;
            for (Reservation r : reservations) {
                Row rx = sheet.createRow(row);
                CellStyle st = (row % 2 == 0) ? altStyle : null;
                setCell(rx, 0, r.getId(), st);
                setCell(rx, 1, s(r.getUtilisateurNom()), st);
                setCell(rx, 2, s(r.getMotif()), st);
                setCell(rx, 3, s(r.getSalleNumero()), st);
                setCell(rx, 4, r.getDateReservation() != null ? r.getDateReservation().toLocalDate().toString() : "", st);
                setCell(rx, 5, s(r.getStatut()), st);
                row++;
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(out)) { wb.write(fos); }
        }
    }

    public void exportOccupationAsExcel(Map<String, Double> taux, File out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Occupation");
            CellStyle hStyle = headerStyle(wb, new short[]{0x3B, 0x52, 0x82});
            CellStyle altStyle = altStyle(wb);

            Row hRow = sheet.createRow(0);
            String[] headers = {"Salle", "Taux Occupation (%)", "Niveau"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hStyle);
            }
            int row = 1;
            for (Map.Entry<String, Double> e : taux.entrySet()) {
                Row r = sheet.createRow(row);
                CellStyle st = (row % 2 == 0) ? altStyle : null;
                setCell(r, 0, e.getKey(), st);
                setCell(r, 1, e.getValue(), st);
                double v = e.getValue();
                setCell(r, 2, v >= 80 ? "CRITIQUE" : v >= 50 ? "ELEVE" : "NORMAL", st);
                row++;
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(out)) { wb.write(fos); }
        }
    }

    // ========================= HELPERS =========================

    private String s(String v) { return v != null ? v : ""; }
    private String trunc(String v, int max) {
        if (v == null) return "";
        return v.length() > max ? v.substring(0, max - 2) + ".." : v;
    }

    private CellStyle headerStyle(Workbook wb, short[] rgb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle altStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setCell(Row r, int col, Object val, CellStyle style) {
        Cell c = r.createCell(col);
        if (val instanceof Integer) c.setCellValue((Integer) val);
        else if (val instanceof Double) c.setCellValue((Double) val);
        else c.setCellValue(val != null ? val.toString() : "");
        if (style != null) c.setCellStyle(style);
    }
}
