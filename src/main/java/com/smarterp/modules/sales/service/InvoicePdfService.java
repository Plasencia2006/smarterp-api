package com.smarterp.modules.sales.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    /**
     * 🧾 Generar PDF de Factura con OpenPDF
     */
    public byte[] generateInvoicePdf(Quote quote) {
        try {
            log.info("🧾 Generando factura PDF: {}", quote.getInvoiceNumber());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            // ✅ Encabezado - Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("FACTURA ELECTRÓNICA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // ✅ Información de la factura
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);
            infoTable.setSpacingAfter(20);

            // Número de factura
            infoTable.addCell(new Phrase("Número de Factura:", boldFont));
            infoTable.addCell(new Phrase(quote.getInvoiceNumber(), normalFont));

            // Fecha
            infoTable.addCell(new Phrase("Fecha de Emisión:", boldFont));
            String fechaEmision = quote.getPaidAt() != null
                    ? quote.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : "N/A";
            infoTable.addCell(new Phrase(fechaEmision, normalFont));

            // Cotización original
            infoTable.addCell(new Phrase("Cotización Original:", boldFont));
            infoTable.addCell(new Phrase(quote.getQuoteNumber(), normalFont));

            document.add(infoTable);

            // ✅ Información del cliente
            Paragraph clientSection = new Paragraph();
            clientSection.add(new Phrase("INFORMACIÓN DEL CLIENTE", boldFont));
            clientSection.setSpacingAfter(10);
            document.add(clientSection);

            PdfPTable clientTable = new PdfPTable(2);
            clientTable.setWidthPercentage(100);
            clientTable.setSpacingAfter(15);

            clientTable.addCell(new Phrase("Nombre:", boldFont));
            clientTable.addCell(new Phrase(quote.getCustomerName(), normalFont));

            if (quote.getCustomerDocument() != null) {
                clientTable.addCell(new Phrase("Documento:", boldFont));
                clientTable.addCell(new Phrase(quote.getCustomerDocument(), normalFont));
            }

            document.add(clientTable);

            // ✅ Productos
            Paragraph productsSection = new Paragraph();
            productsSection.add(new Phrase("DETALLE DE PRODUCTOS", boldFont));
            productsSection.setSpacingAfter(10);
            document.add(productsSection);

            // Tabla de productos
            PdfPTable productsTable = new PdfPTable(new float[] { 2.5f, 1f, 1.5f, 1.5f });
            productsTable.setWidthPercentage(100);
            productsTable.setSpacingAfter(15);

            // Headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            PdfPCell headerProducto = new PdfPCell(new Phrase("PRODUCTO", headerFont));
            headerProducto.setHorizontalAlignment(Element.ALIGN_CENTER);
            productsTable.addCell(headerProducto);

            PdfPCell headerCantidad = new PdfPCell(new Phrase("CANT.", headerFont));
            headerCantidad.setHorizontalAlignment(Element.ALIGN_CENTER);
            productsTable.addCell(headerCantidad);

            PdfPCell headerPUnit = new PdfPCell(new Phrase("P. UNIT.", headerFont));
            headerPUnit.setHorizontalAlignment(Element.ALIGN_RIGHT);
            productsTable.addCell(headerPUnit);

            PdfPCell headerSubtotal = new PdfPCell(new Phrase("SUBTOTAL", headerFont));
            headerSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            productsTable.addCell(headerSubtotal);

            // Items
            if (quote.getItems() != null) {
                for (QuoteItem item : quote.getItems()) {
                    productsTable.addCell(new Phrase(item.getProductName(), normalFont));

                    PdfPCell cantidadCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                    cantidadCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    productsTable.addCell(cantidadCell);

                    PdfPCell pUnitCell = new PdfPCell(
                            new Phrase(String.format("S/ %.2f", item.getUnitPrice()), normalFont));
                    pUnitCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    productsTable.addCell(pUnitCell);

                    PdfPCell subtotalCell = new PdfPCell(
                            new Phrase(String.format("S/ %.2f", item.getSubtotal()), normalFont));
                    subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    productsTable.addCell(subtotalCell);
                }
            }

            document.add(productsTable);

            // ✅ Totales
            PdfPTable totalsTable = new PdfPTable(new float[] { 3f, 1f, 1f });
            totalsTable.setWidthPercentage(60);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setSpacingAfter(20);

            // Subtotal
            totalsTable.addCell(new Phrase("SUBTOTAL:", boldFont));
            PdfPCell subtotalCell = new PdfPCell(new Phrase(String.format("S/ %.2f", quote.getSubtotal()), normalFont));
            subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            subtotalCell.setColspan(2);
            totalsTable.addCell(subtotalCell);

            // IGV
            totalsTable.addCell(new Phrase("IGV (18%):", boldFont));
            PdfPCell igvCell = new PdfPCell(new Phrase(String.format("S/ %.2f", quote.getIgv()), normalFont));
            igvCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            igvCell.setColspan(2);
            totalsTable.addCell(igvCell);

            // TOTAL
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            totalsTable.addCell(new Phrase("TOTAL:", totalFont));
            PdfPCell totalCell = new PdfPCell(new Phrase(String.format("S/ %.2f", quote.getTotal()), totalFont));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setColspan(2);
            totalsTable.addCell(totalCell);

            document.add(totalsTable);

            // ✅ Método de pago
            if (quote.getPaymentMethod() != null) {
                Paragraph paymentMethod = new Paragraph();
                paymentMethod.add(new Phrase("Método de Pago: ", boldFont));
                paymentMethod.add(new Phrase(quote.getPaymentMethod(), normalFont));
                paymentMethod.setSpacingAfter(10);
                document.add(paymentMethod);
            }

            // ✅ Pie de página
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(30);
            footer.add(new Phrase("\n\n", normalFont));
            footer.add(new Phrase("================================================================================\n",
                    FontFactory.getFont(FontFactory.HELVETICA, 8)));
            footer.add(new Phrase("¡GRACIAS POR SU COMPRA!\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            footer.add(new Phrase("================================================================================\n",
                    FontFactory.getFont(FontFactory.HELVETICA, 8)));
            footer.add(new Phrase("Esta factura es una representación electrónica de su compra.\n",
                    FontFactory.getFont(FontFactory.HELVETICA, 8)));
            footer.add(new Phrase(
                    "Fecha de impresión: "
                            + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    FontFactory.getFont(FontFactory.HELVETICA, 8)));

            document.add(footer);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            log.info(" Factura PDF generada exitosamente. Tamaño: {} bytes", pdfBytes.length);

            return pdfBytes;

        } catch (Exception e) {
            log.error("❌ Error al generar PDF de factura: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al generar factura PDF", e);
        }
    }
}




