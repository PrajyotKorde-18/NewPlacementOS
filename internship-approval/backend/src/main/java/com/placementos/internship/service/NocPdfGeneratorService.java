package com.placementos.internship.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class NocPdfGeneratorService {

    private static final String NOC_DIR = "uploads/noc/";

    @Autowired
    private NocVerificationTokenService tokenService;

    public String generateNocPdf(InternshipRequest request, User approver, String verificationToken) throws IOException {
        File dir = new File(NOC_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String pdfPath = NOC_DIR + "NOC_Request_" + request.getId() + ".pdf";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // 1. Text Content Stream
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();

                // Header
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(100, 750);
                contentStream.showText("RAMDEOBABA UNIVERSITY");

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(0, -25);
                contentStream.showText("CAREER DEVELOPMENT & PLACEMENT CELL (CDPC)");

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(0, -25);
                contentStream.showText("NO OBJECTION CERTIFICATE (NOC)");

                // Reference Number & Date
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(0, -40);
                contentStream.showText("NOC Reference Number: NOC-REQ-" + request.getId());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDate = java.time.LocalDateTime.now().format(formatter);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Date of Issue: " + formattedDate);

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 13);
                contentStream.newLineAtOffset(0, -35);
                contentStream.showText("TO WHOMSOEVER IT MAY CONCERN");

                String studentName = request.getStudent() != null && request.getStudent().getUser() != null
                        ? request.getStudent().getUser().getName() : "Student";
                String collegeId = request.getStudent() != null ? request.getStudent().getCollegeId() : "N/A";
                String dept = request.getStudent() != null ? request.getStudent().getDepartment() : "Engineering";

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(0, -30);
                contentStream.showText("This is to certify that " + studentName + " (College ID: " + collegeId + "),");

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("a student of Department of " + dept + ", has been granted NO OBJECTION");

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("by the Career Development & Placement Cell to pursue an internship with:");

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(0, -30);
                contentStream.showText("Organization: " + request.getCompanyName());

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Role Title: " + request.getRoleTitle());

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Duration: " + request.getDurationWeeks() + " Weeks");

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Stipend: Rs. " + (request.getStipend() != null ? request.getStipend() : "0.00") + " per month");

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Mode: " + request.getMode());

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(0, -40);
                contentStream.showText("The University has verified the internship offer details and grants permission.");

                // Signature section
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
                contentStream.newLineAtOffset(0, -60);
                contentStream.showText("Approved & Digitally Signed By:");

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText(approver.getName() + " (" + approver.getRole() + ")");

                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("Email: " + approver.getEmail());

                contentStream.endText();

                // 2. Draw QR Code on bottom-right if verification token exists
                if (verificationToken != null && !verificationToken.isBlank()) {
                    String verificationUrl = tokenService.buildVerificationUrl(verificationToken);

                    try {
                        QRCodeWriter qrCodeWriter = new QRCodeWriter();
                        BitMatrix bitMatrix = qrCodeWriter.encode(verificationUrl, BarcodeFormat.QR_CODE, 120, 120);

                        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                        byte[] pngBytes = pngOutputStream.toByteArray();

                        PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, pngBytes, "qr_code");

                        float qrWidth = 80f;
                        float qrHeight = 80f;
                        float qrX = page.getMediaBox().getWidth() - qrWidth - 50f;
                        float qrY = 60f;

                        contentStream.drawImage(qrImage, qrX, qrY, qrWidth, qrHeight);

                        // Verification caption under QR Code (calling setFont immediately before showText)
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 7);
                        contentStream.newLineAtOffset(qrX - 12, qrY - 12);
                        contentStream.showText("Scan QR to Verify NOC Authenticity");
                        contentStream.endText();
                    } catch (Exception e) {
                        // Log QR drawing error without breaking PDF generation
                    }
                }
            }

            document.save(pdfPath);
        }

        return pdfPath;
    }

    public String generateNocPdf(InternshipRequest request, User approver) throws IOException {
        return generateNocPdf(request, approver, null);
    }
}
