package com.smarterp.modules.sales.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotePdfService {

    public byte[] generateQuotePdf(Quote quote) throws Exception {
        log.info("📄 Generando PDF para cotización: {}", quote.getQuoteNumber());

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            log.info("📝 Documento PDF abierto");

            addHeader(document, quote);
            log.info("✅ Header agregado");

            addCustomerInfo(document, quote);
            log.info("✅ Info del cliente agregada");

            addProductsTable(document, quote);
            log.info("✅ Tabla de productos agregada");

            addTotals(document, quote);
            log.info("✅ Totales agregados");

            addConditions(document, quote);
            log.info("✅ Condiciones agregadas");

            addFooter(document);
            log.info("✅ Footer agregado");

        } catch (Exception e) {
            log.error("❌ Error generando PDF: {}", e.getMessage());
            throw e;
        } finally {
            document.close();
            log.info("✅ Documento PDF cerrado");
        }

        byte[] pdfBytes = outputStream.toByteArray();
        log.info("✅ PDF generado: {} bytes", pdfBytes.length);
        return pdfBytes;
    }

    private void addHeader(Document document, Quote quote) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        Paragraph title = new Paragraph("COTIZACIÓN", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Font numberFont = FontFactory.getFont(FontFactory.HELVETICA, 14);
        Paragraph number = new Paragraph("N° " + quote.getQuoteNumber(), numberFont);
        number.setAlignment(Element.ALIGN_CENTER);
        number.setSpacingAfter(30);
        document.add(number);

        Paragraph separator = new Paragraph(" ");
        separator.setSpacingAfter(10);
        document.add(separator);

        Chunk line = new Chunk(new String(new char[80]).replace("\0", "-"),
                FontFactory.getFont(FontFactory.HELVETICA, 8));
        document.add(line);
        document.add(Chunk.NEWLINE);
    }

    private void addCustomerInfo(Document document, Quote quote) throws Exception {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new int[] { 30, 70 });
        infoTable.setSpacingBefore(20);
        infoTable.setSpacingAfter(20);

        infoTable.addCell(new Phrase("Cliente:", labelFont));
        infoTable.addCell(new Phrase(quote.getCustomerName(), valueFont));

        if (quote.getCustomerDocument() != null && !quote.getCustomerDocument().isEmpty()) {
            infoTable.addCell(new Phrase("Documento:", labelFont));
            infoTable.addCell(new Phrase(quote.getCustomerDocument(), valueFont));
        }

        infoTable.addCell(new Phrase("Fecha:", labelFont));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        infoTable.addCell(new Phrase(quote.getCreatedAt().format(formatter), valueFont));

        document.add(infoTable);
    }

    private void addProductsTable(Document document, Quote quote) throws Exception {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 15, 45, 20, 20 });
        table.setSpacingBefore(20);
        table.setSpacingAfter(20);

        String[] headers = { "Cant.", "Producto", "P. Unitario", "Subtotal" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (QuoteItem item : quote.getItems()) {
            table.addCell(new Phrase(String.valueOf(item.getQuantity()), cellFont));
            table.addCell(new Phrase(item.getProductName(), cellFont));
            table.addCell(new Phrase("S/ " + String.format("%.2f", item.getUnitPrice()), cellFont));
            table.addCell(new Phrase("S/ " + String.format("%.2f", item.getSubtotal()), cellFont));
        }

        document.add(table);
    }

    private void addTotals(Document document, Quote quote) throws Exception {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setWidths(new int[] { 60, 40 });
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10);

        totalsTable.addCell(new Phrase("Subtotal:", labelFont));
        totalsTable.addCell(new Phrase("S/ " + String.format("%.2f", quote.getSubtotal()), valueFont));

        totalsTable.addCell(new Phrase("IGV (18%):", labelFont));
        totalsTable.addCell(new Phrase("S/ " + String.format("%.2f", quote.getIgv()), valueFont));

        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL:", labelFont));
        totalLabel.setBackgroundColor(new Color(220, 220, 220));
        totalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalsTable.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase("S/ " + String.format("%.2f", quote.getTotal()), valueFont));
        totalValue.setBackgroundColor(new Color(220, 220, 220));
        totalValue.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalsTable.addCell(totalValue);

        document.add(totalsTable);
    }

    private void addConditions(Document document, Quote quote) throws Exception {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        Paragraph conditions = new Paragraph();
        conditions.setSpacingBefore(30);

        conditions.add(new Chunk("CONDICIONES:\n", boldFont));
        conditions.add(new Chunk("• Esta cotización es válida por 20 minutos a partir de su emisión.\n", font));
        conditions.add(new Chunk("• Los precios incluyen IGV.\n", font));
        conditions.add(new Chunk("• Sujeto a disponibilidad de stock.\n", font));
        conditions.add(new Chunk("• Forma de pago: Contado.\n", font));

        document.add(conditions);
    }

    private void addFooter(Document document) throws Exception {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8);

        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(40);
        footer.setAlignment(Element.ALIGN_CENTER);

        footer.add(new Chunk("Gracias por su preferencia\n", font));
        footer.add(new Chunk("Documento sujeto a aprobación de gerencia", font));

        document.add(footer);
    }
}