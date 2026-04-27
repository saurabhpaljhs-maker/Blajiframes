package com.balaji.controller;

import com.balaji.dto.PriceCalculationDTO;
import com.balaji.service.FileUploadService;
import com.balaji.service.FrameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/frame")
@RequiredArgsConstructor
public class FrameApiController {

    private final FrameService frameService;
    private final FileUploadService fileUploadService;

    // Calculate price dynamically (called via AJAX)
    @GetMapping("/price")
    public ResponseEntity<PriceCalculationDTO> calculatePrice(
            @RequestParam Long sizeId,
            @RequestParam Long beadingId,
            @RequestParam Long coverId) {
        PriceCalculationDTO dto = frameService.calculatePrice(sizeId, beadingId, coverId);
        return ResponseEntity.ok(dto);
    }

    // Upload photo, get back path
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestParam("file") MultipartFile file) {
        try {
            String path = fileUploadService.saveFile(file);
            return ResponseEntity.ok(Map.of("path", path, "status", "ok"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    // Get all sizes
    @GetMapping("/sizes")
    public ResponseEntity<?> getSizes() {
        return ResponseEntity.ok(frameService.getAllSizes());
    }

    // Get all beadings
    @GetMapping("/beadings")
    public ResponseEntity<?> getBeadings() {
        return ResponseEntity.ok(frameService.getAllBeadings());
    }

    // Get all covers
    @GetMapping("/covers")
    public ResponseEntity<?> getCovers() {
        return ResponseEntity.ok(frameService.getAllCovers());
    }
}
