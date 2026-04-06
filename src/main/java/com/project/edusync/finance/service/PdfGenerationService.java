package com.project.edusync.finance.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.project.edusync.common.exception.finance.PdfGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    // 1x1 Solid Gray PNG pixel as placeholder (scales fine and doesn't break PDF renderer)
    private static final String PLACEHOLDER_IMAGE_BASE64 =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    private final TemplateEngine templateEngine;

    /**
     * Generates a PDF from a Thymeleaf template using OpenHTMLtoPDF.
     * Supports CSS3 including flexbox, gradients, border-radius, box-shadow.
     *
     * @param templateName The name of the HTML file in 'templates/' (e.g., "receipt")
     * @param data         A map of data to be injected into the template.
     * @return A byte array (byte[]) of the generated PDF.
     */
    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        try {
            ensureTemplateDataDefaults(data);

            Context context = new Context();
            context.setVariables(data);
            String rawHtml = templateEngine.process(templateName, context);

            Document jsoupDoc = Jsoup.parse(rawHtml);
            jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.useSVGDrawer(new BatikSVGDrawer());

                String baseUri = FileSystems.getDefault()
                        .getPath("src/main/resources/templates")
                        .toUri()
                        .toString();

                builder.withW3cDocument(w3cDoc, baseUri);
                builder.toStream(outputStream);
                builder.run();
                return outputStream.toByteArray();
            }
        } catch (Exception ex) {
            log.error("Failed to generate PDF for template {}", templateName, ex);
            throw new PdfGenerationException("Failed to generate PDF", ex);
        }
    }

    private void ensureTemplateDataDefaults(Map<String, Object> data) throws Exception {
        // For receipt backward compatibility — generate receipt QR if needed
        if (!data.containsKey("qrCodeBase64") || data.get("qrCodeBase64") == null) {
            String qrText = "ReceiptNo: " + data.getOrDefault("receiptNo", "N/A") +
                    "\nStudent: " + data.getOrDefault("studentName", "N/A") +
                    "\nAmount: " + data.getOrDefault("totalAmount", "0.00");
            String qrCodeBase64 = generateQrCodeBase64(qrText, 100);
            data.put("qrCodeBase64", qrCodeBase64);
        }

        // Load and add school logo as Base64 if not already set
        if (!data.containsKey("schoolLogoBase64") || data.get("schoolLogoBase64") == null
                || ((String) data.get("schoolLogoBase64")).isEmpty()) {
            String logoBase64 = loadSchoolLogoBase64();
            data.put("schoolLogoBase64", logoBase64);
        }
    }

    /**
     * Generates a QR code and returns it as a Base64 encoded PNG data URI.
     */
    public String generateQrCodeBase64(String text, int size) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }

    /**
     * Generates a Code128 barcode and returns it as a Base64 encoded PNG data URI.
     */
    public String generateBarcodeBase64(String text, int width, int height) throws Exception {
        com.google.zxing.oned.Code128Writer barcodeWriter = new com.google.zxing.oned.Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }

    /**
     * Loads the school logo from the classpath and encodes it in Base64.
     */
    public String loadSchoolLogoBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("static/images/logo.png");
            if (!resource.exists()) {
                log.warn("logo.png not found. Using empty string.");
                return "";
            }
            byte[] fileContent = resource.getInputStream().readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            log.warn("Could not load logo.png: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Fetches a remote image and returns it as a Base64-encoded data URI.
     * Returns a placeholder silhouette if the URL is blank or the fetch fails.
     */
    public String fetchRemoteImageAsBase64(String url) {
        return fetchRemoteImageAsBase64Internal(url, true);
    }

    /**
     * Fetches a remote image and returns it as a Base64-encoded data URI.
     * Returns empty string if URL is blank or the fetch fails.
     */
    public String fetchRemoteImageAsBase64OrEmpty(String url) {
        return fetchRemoteImageAsBase64Internal(url, false);
    }

    private String fetchRemoteImageAsBase64Internal(String url, boolean usePlaceholderOnFailure) {
        if (url == null || url.isBlank()) {
            return usePlaceholderOnFailure ? PLACEHOLDER_IMAGE_BASE64 : "";
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "image/*");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("Non-200 response ({}) fetching image from: {}", status, url);
                return usePlaceholderOnFailure ? PLACEHOLDER_IMAGE_BASE64 : "";
            }

            try (InputStream is = conn.getInputStream()) {
                BufferedImage image = ImageIO.read(is);
                if (image == null) {
                    log.warn("Unsupported image format from {}", url);
                    return usePlaceholderOnFailure ? PLACEHOLDER_IMAGE_BASE64 : "";
                }

                ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
                ImageIO.write(image, "png", pngBytes);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes.toByteArray());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch profile image from {}: {}", url, e.getMessage());
            return usePlaceholderOnFailure ? PLACEHOLDER_IMAGE_BASE64 : "";
        }
    }
}