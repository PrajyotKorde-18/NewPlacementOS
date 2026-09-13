package com.placementos.internship.service;

import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class NocPdfGeneratorService {

    private static final String NOC_DIR = "uploads/noc/";

    public String generateNocPdf(InternshipRequest request, User approver) throws IOException {
        File dir = new File(NOC_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String pdfPath = NOC_DIR + "NOC_Request_" + request.getId() + ".pdf";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                
                // Header
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(100, 750);
                contentStream.showText("RAMDEOBABA UNIVERSITY");
                contentStream.newLineAtOffset(0, -25);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.showText("CAREER DEVELOPMENT & PLACEMENT CELL (CDPC)");
                contentStream.newLineAtOffset(0, -25);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.showText("NO OBJECTION CERTIFICATE (NOC)");

                // Horizontal separator / spacing
                contentStream.newLineAtOffset(0, -40);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.showText("NOC Reference Number: NOC-REQ-" + request.getId());
                contentStream.newLineAtOffset(0, -20);
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDate = java.time.LocalDateTime.now().format(formatter);
                contentStream.showText("Date of Issue: " + formattedDate);

                contentStream.newLineAtOffset(0, -35);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 13);
                contentStream.showText("TO WHOMSOEVER IT MAY CONCERN");

                contentStream.newLineAtOffset(0, -30);
                contentStream.setFont(PDType1Font.HELVETICA, 11);

                String studentName = request.getStudent() != null && request.getStudent().getUser() != null
                        ? request.getStudent().getUser().getName() : "Student";
                String collegeId = request.getStudent() != null ? request.getStudent().getCollegeId() : "N/A";
                String dept = request.getStudent() != null ? request.getStudent().getDepartment() : "Engineering";

                contentStream.showText("This is to certify that " + studentName + " (College ID: " + collegeId + "),");
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("a student of Department of " + dept + ", has been granted NO OBJECTION");
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("by the Career Development & Placement Cell to pursue an internship with:");

                contentStream.newLineAtOffset(0, -30);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.showText("Organization: " + request.getCompanyName());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Role Title: " + request.getRoleTitle());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Duration: " + request.getDurationWeeks() + " Weeks");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Stipend: Rs. " + (request.getStipend() != null ? request.getStipend() : "0.00") + " per month");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Mode: " + request.getMode());

                contentStream.newLineAtOffset(0, -40);
                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.showText("The University has verified the internship offer details and grants permission.");

                // Signature section
                contentStream.newLineAtOffset(0, -60);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
                contentStream.showText("Approved & Digitally Signed By:");
                contentStream.newLineAtOffset(0, -18);
                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.showText(approver.getName() + " (" + approver.getRole() + ")");
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("Email: " + approver.getEmail());

                contentStream.endText();
            }

            document.save(pdfPath);
        }

        return pdfPath;
    }
}
