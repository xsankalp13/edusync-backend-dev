package com.project.edusync.finance.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    /**
     * Generates a PDF from a Thymeleaf template.
     * @param templateName The name of the HTML file in 'templates/' (e.g., "receipt")
     * @param data         A map of data to be injected into the template.
     * @return A byte array (byte[]) of the generated PDF.
     */
    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        try {
            // 1. Generate QR Code and add it to the data map
            String qrText = "ReceiptNo: " + data.getOrDefault("receiptNo", "N/A") +
                    "\nStudent: " + data.getOrDefault("studentName", "N/A") +
                    "\nAmount: " + data.getOrDefault("totalAmount", "0.00");
            String qrCodeBase64 = generateQrCodeBase64(qrText);
            data.put("qrCodeBase64", qrCodeBase64);

            // 2. Load and add the school logo as a Base64 string
            // This is the best way to embed images in PDFs
            String logoBase64 = loadSchoolLogoBase64();
            data.put("schoolLogoBase64", logoBase64);

            // 3. Process the HTML template with Thymeleaf
            Context context = new Context();
            context.setVariables(data);
            String rawHtml = templateEngine.process(templateName, context);

            // 4. Clean the HTML to be well-formed XML (required by Flying Saucer)
            Document document = Jsoup.parse(rawHtml);
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            String cleanHtml = document.html();

            // 5. Generate the PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            // Set the base URL for relative paths (e.g., for images)
            String baseUrl = FileSystems.getDefault()
                    .getPath("src", "main", "resources", "templates")
                    .toUri().toURL().toString();
            renderer.setDocumentFromString(cleanHtml, baseUrl);

            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error during PDF generation: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating PDF receipt", e);
        }
    }

    /**
     * Generates a QR code and returns it as a Base64 encoded PNG.
     */
    private String generateQrCodeBase64(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 100, 100);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }

    /**
     * Loads the school logo from 'resources' and encodes it in Base64.
     */
    private String loadSchoolLogoBase64() throws IOException {
        // We will hardcode a path to a logo file.
        // In a real app, this would come from a database or config.
        // Let's assume you add a "logo.png" to "src/main/resources/static/images/"
        try {
            ClassPathResource resource = new ClassPathResource("static/images/logo.png");
            if (!resource.exists()) {
                log.warn("logo.png not found. Using empty string.");
                return ""; // Return empty string if no logo
            }
            byte[] fileContent = resource.getInputStream().readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            log.warn("Could not load logo.png: {}", e.getMessage());
            return ""; // Fail gracefully
        }
    }
}