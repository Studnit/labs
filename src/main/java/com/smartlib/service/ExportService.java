package com.smartlib.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartlib.domain.Library;
import com.smartlib.domain.Payment;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {
    public record Table(String title, List<String> headers, List<List<Object>> rows) {}

    private static String str(Object o) { return o == null ? "" : o instanceof LocalDateTime t ? t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : o.toString(); }

    public byte[] xlsx(Table t) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(t.title().length() > 30 ? t.title().substring(0, 30) : t.title());
            CellStyle head = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font f = wb.createFont(); f.setBold(true); head.setFont(f);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row hr = sh.createRow(0);
            for (int c = 0; c < t.headers().size(); c++) { Cell cell = hr.createCell(c); cell.setCellValue(t.headers().get(c)); cell.setCellStyle(head); }
            int r = 1;
            for (List<Object> row : t.rows()) {
                Row xr = sh.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    Object v = row.get(c); Cell cell = xr.createCell(c);
                    if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                    else cell.setCellValue(sanitize(str(v)));
                }
            }
            for (int c = 0; c < t.headers().size(); c++) sh.autoSizeColumn(c);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Excel export failed", e); }
    }

    /** Prevents spreadsheet formula injection from user-supplied text. */
    private static String sanitize(String s) {
        return !s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+([.,]\\d+)?") ? "'" + s : s;
    }

    public byte[] pdf(Table t, String libraryName) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(t.headers().size() > 6 ? PageSize.A4.rotate() : PageSize.A4, 30, 30, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph(libraryName + " — " + t.title(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15)));
            doc.add(new Paragraph("Generated " + str(LocalDateTime.now()), FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(t.headers().size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font bf = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
            for (String h : t.headers()) {
                PdfPCell c = new PdfPCell(new Phrase(h, hf)); c.setBackgroundColor(new Color(33, 37, 41)); c.setPadding(5); table.addCell(c);
            }
            for (List<Object> row : t.rows()) for (Object v : row) { PdfPCell c = new PdfPCell(new Phrase(str(v), bf)); c.setPadding(4); table.addCell(c); }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("PDF export failed", e); }
    }

    public byte[] receiptPdf(Payment p) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Library lib = p.getLibrary();
            Document doc = new Document(PageSize.A5, 36, 36, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph(lib.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            if (lib.getAddress() != null) doc.add(new Paragraph(lib.getAddress(), FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("PAYMENT RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
            doc.add(new Paragraph(" "));
            PdfPTable t = new PdfPTable(new float[]{1.2f, 2.8f});
            t.setWidthPercentage(100);
            row(t, "Receipt no", p.getReceiptNo());
            row(t, "Date", str(p.getPaidAt()));
            row(t, "Received from", p.getMember().getFullName() + (p.getMember().getMemberCode() == null ? "" : " (" + p.getMember().getMemberCode() + ")"));
            row(t, "For", p.getPurpose() == null ? "Payment" : p.getPurpose());
            row(t, "Method", p.getMethod().name() + (p.getReference() == null || p.getReference().isBlank() ? "" : " — " + p.getReference()));
            row(t, "Amount", p.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            if (p.getSubscription() != null) row(t, "Valid", p.getSubscription().getStartDate() + " to " + p.getSubscription().getEndDate());
            doc.add(t);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("This is a computer-generated receipt.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Receipt generation failed", e); }
    }

    private static void row(PdfPTable t, String k, String v) {
        PdfPCell a = new PdfPCell(new Phrase(k, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        PdfPCell b = new PdfPCell(new Phrase(v == null ? "" : v, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        a.setPadding(6); b.setPadding(6); a.setBorderColor(Color.LIGHT_GRAY); b.setBorderColor(Color.LIGHT_GRAY);
        t.addCell(a); t.addCell(b);
    }
}
