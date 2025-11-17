package com.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file) {
        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("✅ Created upload directory: " + uploadPath.toAbsolutePath());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative URL path
            String imageUrl = "/uploads/" + filename;
            System.out.println("✅ File saved successfully: " + imageUrl);

            return imageUrl;

        } catch (IOException e) {
            System.err.println("❌ Failed to save file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.contains("/uploads/")) {
                // Extract filename from URL
                String filename = fileUrl.substring(fileUrl.lastIndexOf("/uploads/") + "/uploads/".length());
                Path filePath = Paths.get(uploadDir).resolve(filename);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    System.out.println("🗑️ File deleted: " + filename);
                } else {
                    System.out.println("⚠️ File not found: " + filename);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to delete file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}