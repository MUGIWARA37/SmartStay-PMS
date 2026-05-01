package ma.ensa.khouribga.smartstay.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import ma.ensa.khouribga.smartstay.model.Invoice;
import ma.ensa.khouribga.smartstay.model.Invoice.InvoiceLine;
import ma.ensa.khouribga.smartstay.model.Reservation;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Generates a Bloody Samurai–themed PDF invoice scroll for a guest checkout.
 * Uses OpenPDF (a fork of iText 2.x, LGPL/MPL licensed).
 */
public class InvoiceExportService {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color COLOR_BG        = new Color(10, 10, 14);
    private static final Color COLOR_GOLD       = new Color(197, 160, 89);
    private static final Color COLOR_RED        = new Color(139, 0, 0);
    private static final Color COLOR_LIGHT      = new Color(224, 224, 224);
    private static final Color COLOR_MUTED      = new Color(140, 140, 140);
    private static final Color COLOR_SEPARATOR  = new Color(60, 60, 65);
    private static final Color COLOR_ROW_EVEN   = new Color(20, 20, 25);
    private static final Color COLOR_ROW_ODD    = new Color(28, 28, 34);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Exports a PDF invoice to the user's home directory.
     *
     * @param invoice     fully-populated Invoice (with lines)
     * @param reservation matching Reservation (for guest name, dates, room)
     * @return the generated File
     * @throws Exception on any PDF or I/O error
     */
    public static File export(Invoice invoice, Reservation reservation) throws Exception {
        String filename = "Invoice-" + invoice.getInvoiceNumber() + ".pdf";
        File out = new File(System.getProperty("user.home"), filename);

        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));

        // Dark background on every page
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onStartPage(PdfWriter w, Document d) {
                PdfContentByte canvas = w.getDirectContentUnder();
                canvas.setColorFill(COLOR_BG);
                canvas.rectangle(0, 0, d.getPageSize().getWidth(), d.getPageSize().getHeight());
                canvas.fill();
            }
        });

        doc.open();

        // ── Header ────────────────────────────────────────────────────────────
        Font kanji = new Font(Font.HELVETICA, 36, Font.BOLD, COLOR_RED);
        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, COLOR_GOLD);
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_MUTED);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_GOLD);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_LIGHT);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_GOLD);
        Font tableHeaderFont = new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_BG);
        Font tableBodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, COLOR_LIGHT);
        Font totalFont = new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_GOLD);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_MUTED);

        // Brand row
        PdfPTable brandTable = new PdfPTable(2);
        brandTable.setWidthPercentage(100);
        brandTable.setWidths(new float[]{1, 3});
        brandTable.setSpacingAfter(20);

        PdfPCell kanjiCell = cell(new Paragraph("侍", kanji), Element.ALIGN_CENTER);
        kanjiCell.setBackgroundColor(COLOR_BG);
        kanjiCell.setBorder(Rectangle.NO_BORDER);
        kanjiCell.setPaddingRight(16);

        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setBackgroundColor(COLOR_BG);
        brandCell.addElement(new Paragraph("SMARTSTAY PMS", titleFont));
        brandCell.addElement(new Paragraph("KHOURIBGA EDITION  ·  IMPERIAL INVOICE SCROLL", subtitleFont));

        brandTable.addCell(kanjiCell);
        brandTable.addCell(brandCell);
        doc.add(brandTable);

        // Red divider
        addHorizontalRule(doc, COLOR_RED, 2);

        // ── Invoice meta block ────────────────────────────────────────────────
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingBefore(14);
        metaTable.setSpacingAfter(14);

        addMetaRow(metaTable, "Invoice №",      invoice.getInvoiceNumber(),            labelFont, valueFont);
        addMetaRow(metaTable, "Status",         invoice.getStatus().name(),             labelFont, valueFont);
        addMetaRow(metaTable, "Issued",
                invoice.getIssuedAt() != null
                        ? invoice.getIssuedAt().toLocalDate().format(DATE_FMT) : "—",  labelFont, valueFont);
        addMetaRow(metaTable, "Reservation",    reservation.getReservationCode(),       labelFont, valueFont);
        addMetaRow(metaTable, "Guest",          reservation.getGuestFullName(),         labelFont, valueFont);
        addMetaRow(metaTable, "Room",
                "Room " + reservation.getRoomNumber()
                        + "  (" + reservation.getRoomTypeName() + ")",                 labelFont, valueFont);
        addMetaRow(metaTable, "Check-in",
                reservation.getCheckInDate() != null
                        ? reservation.getCheckInDate().format(DATE_FMT) : "—",        labelFont, valueFont);
        addMetaRow(metaTable, "Check-out",
                reservation.getCheckOutDate() != null
                        ? reservation.getCheckOutDate().format(DATE_FMT) : "—",       labelFont, valueFont);

        doc.add(metaTable);
        addHorizontalRule(doc, COLOR_SEPARATOR, 1);

        // ── Line items table ──────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        Paragraph linesTitle = new Paragraph("LINE ITEMS", sectionFont);
        linesTitle.setSpacingAfter(8);
        doc.add(linesTitle);

        PdfPTable linesTable = new PdfPTable(4);
        linesTable.setWidthPercentage(100);
        linesTable.setWidths(new float[]{4, 1, 2, 2});
        linesTable.setSpacingAfter(10);

        // Header row
        String[] headers = {"DESCRIPTION", "QTY", "UNIT PRICE", "TOTAL"};
        int[] headerAligns = {Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT};
        for (int i = 0; i < headers.length; i++) {
            PdfPCell hc = cell(new Paragraph(headers[i], tableHeaderFont), headerAligns[i]);
            hc.setBackgroundColor(COLOR_GOLD);
            hc.setPadding(7);
            hc.setBorder(Rectangle.NO_BORDER);
            linesTable.addCell(hc);
        }

        // Data rows
        int row = 0;
        for (InvoiceLine line : invoice.getLines()) {
            Color rowBg = (row % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;
            String typeTag = line.getLineType() == InvoiceLine.LineType.ROOM ? "🏯 " : "✦ ";

            addLineCell(linesTable, typeTag + line.getDescription(), tableBodyFont, rowBg, Element.ALIGN_LEFT);
            addLineCell(linesTable, String.valueOf(line.getQuantity()), tableBodyFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(linesTable, String.format("%.2f MAD", line.getUnitPrice()), tableBodyFont, rowBg, Element.ALIGN_RIGHT);
            addLineCell(linesTable, String.format("%.2f MAD", line.getLineTotal()), tableBodyFont, rowBg, Element.ALIGN_RIGHT);
            row++;
        }

        doc.add(linesTable);
        addHorizontalRule(doc, COLOR_SEPARATOR, 1);

        // ── Totals block ──────────────────────────────────────────────────────
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10);
        totalsTable.setSpacingAfter(20);

        if (invoice.getSubtotalAmount() > 0 && invoice.getTaxAmount() > 0) {
            addTotalRow(totalsTable, "Subtotal", String.format("%.2f MAD", invoice.getSubtotalAmount()), valueFont, valueFont);
            addTotalRow(totalsTable, "Tax", String.format("%.2f MAD", invoice.getTaxAmount()), valueFont, valueFont);
        }
        addTotalRow(totalsTable, "TOTAL DUE", String.format("%.2f MAD", invoice.getTotalAmount()), totalFont, totalFont);

        doc.add(totalsTable);

        // ── Footer ────────────────────────────────────────────────────────────
        addHorizontalRule(doc, COLOR_RED, 1);
        Paragraph footer = new Paragraph(
                "SmartStay PMS · Khouribga, Morocco · Thank you for your visit · 侍", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        doc.add(footer);

        doc.close();
        return out;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static PdfPCell cell(Paragraph p, int align) {
        PdfPCell c = new PdfPCell(p);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(COLOR_BG);
        c.setPadding(5);
        return c;
    }

    private static void addMetaRow(PdfPTable table, String label, String value,
                                   Font labelFont, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label + ":", labelFont));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(COLOR_BG);
        lc.setPadding(4);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(COLOR_BG);
        vc.setPadding(4);

        table.addCell(lc);
        table.addCell(vc);
    }

    private static void addLineCell(PdfPTable table, String text, Font font,
                                    Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(6);
        table.addCell(c);
    }

    private static void addTotalRow(PdfPTable table, String label, String value,
                                    Font labelFont, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(COLOR_BG);
        lc.setPadding(5);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(COLOR_BG);
        vc.setPadding(5);

        table.addCell(lc);
        table.addCell(vc);
    }

    private static void addHorizontalRule(Document doc, Color color, float thickness)
            throws DocumentException {
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(color);
        ls.setLineWidth(thickness);
        doc.add(new Chunk(ls));
    }
}