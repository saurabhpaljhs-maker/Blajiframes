package com.balaji.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String original = file.getOriginalFilename();
        String ext      = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";

        String fileName = UUID.randomUUID() + ext;
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File saved: {}", filePath);
        return "/uploads/" + fileName;
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null) return;
        try {
            Path path = Paths.get(uploadDir).resolve(
                    relativePath.replace("/uploads/", ""));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", relativePath);
        }
    }
}
