package com.balaji.service;

import com.balaji.exception.BalajiExceptions.FileUploadEx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    // Loaded from env var UPLOAD_DIR
    // Dev:  /tmp/balaji-dev-uploads
    // Prod: /opt/balaji/uploads  (outside JAR — always writable)
    @Value("${app.upload.dir:/tmp/balaji-uploads}")
    private String uploadDir;

    private static final long   MAX_SIZE_BYTES = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    public String saveFile(MultipartFile file) {
        // ── Validations ───────────────────────────────────────────────────────
        if (file == null || file.isEmpty()) {
            throw new FileUploadEx("No file uploaded");
        }

        // 1. Size check
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new FileUploadEx("File too large. Maximum allowed size is 10MB");
        }

        // 2. MIME type check
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new FileUploadEx("Invalid file type. Only JPG, PNG, WEBP allowed");
        }

        // 3. Extension check (double validation)
        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new FileUploadEx("Invalid file extension. Only .jpg .png .webp allowed");
        }

        // ── Save file ─────────────────────────────────────────────────────────
        try {
            // Create upload directory if it doesn't exist
            // Works for both /tmp/balaji-dev (dev) and /opt/balaji/uploads (prod)
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }

            // Generate unique filename to avoid collisions
            String newFileName = UUID.randomUUID() + ext;
            Path destination  = uploadPath.resolve(newFileName);

            // Copy file to destination
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved: {} ({} bytes)", destination, file.getSize());

            // Return relative URL path for browser access
            return "/uploads/" + newFileName;

        } catch (IOException e) {
            log.error("File save failed: {}", e.getMessage(), e);
            throw new FileUploadEx("Could not save file. Please try again.");
        }
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            String fileName = relativePath.replace("/uploads/", "");
            Path path = Paths.get(uploadDir).resolve(fileName);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) log.info("File deleted: {}", path);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", relativePath);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}
