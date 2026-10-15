package com.placementos.tpms.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileUploadValidator {

    public static final long DEFAULT_MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    public static final List<String> ALLOWED_RESUME_EXTENSIONS = List.of("pdf", "docx", "txt");
    public static final List<String> ALLOWED_JD_EXTENSIONS = List.of("pdf", "txt");

    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] DOCX_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK\x03\x04

    public static void validateFile(MultipartFile file, List<String> allowedExtensions, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be null or empty.");
        }

        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new IllegalArgumentException("File size (" + file.getSize() + " bytes) exceeds maximum allowed limit of " + maxMb + "MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must have a valid filename.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension '." + extension + "'. Allowed types: " + allowedExtensions);
        }

        // Verify magic bytes
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new IllegalArgumentException("File is corrupted or too small to be valid.");
            }

            if ("pdf".equals(extension)) {
                if (!matchesHeader(header, PDF_MAGIC)) {
                    throw new IllegalArgumentException("File content does not match PDF format signature.");
                }
            } else if ("docx".equals(extension)) {
                if (!matchesHeader(header, DOCX_MAGIC)) {
                    throw new IllegalArgumentException("File content does not match DOCX format signature.");
                }
            } else if ("txt".equals(extension)) {
                for (int i = 0; i < read; i++) {
                    if (header[i] == 0) { // null byte
                        throw new IllegalArgumentException("Binary content detected in plain text file.");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file content: " + e.getMessage());
        }
    }

    private static boolean matchesHeader(byte[] header, byte[] magic) {
        if (header.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    public static String getFileExtension(String filename) {
        String base = Paths.get(filename).getFileName().toString();
        int dotIndex = base.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == base.length() - 1) {
            return "";
        }
        return base.substring(dotIndex + 1);
    }

    public static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "document.pdf";
        }
        String base = Paths.get(originalFilename).getFileName().toString();
        // Remove dangerous characters, allow only alphanumeric, underscores, hyphens, and dots
        String sanitized = base.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        // Prevent path traversal sequences
        sanitized = sanitized.replaceAll("\\.{2,}", ".");
        if (sanitized.isEmpty() || sanitized.equals(".")) {
            sanitized = "document.pdf";
        }
        return sanitized;
    }
}
