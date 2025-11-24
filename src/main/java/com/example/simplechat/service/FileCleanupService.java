package com.example.simplechat.service;

import com.example.simplechat.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class FileCleanupService {

    @Value("${file.chat-upload-dir}")
    private String chatUploadDir;

    @Value("${file.retention-days}")
    private long retentionDays;

    // FileRepository는 chatFileRepository만 필요
    @Autowired
    @Qualifier("chatFileRepository")
    private FileRepository chatFileRepository;


    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void cleanupOldFiles() {
        System.out.println("Starting scheduled cleanup of old files...");
        long retentionMillis = retentionDays * 24 * 60 * 60 * 1000;
        long cutoffTime = System.currentTimeMillis() - retentionMillis;

        deleteOldFiles(chatUploadDir, cutoffTime);

        System.out.println("Scheduled cleanup of old files finished.");
    }

    private void deleteOldFiles(String directoryPath, long cutoffTime) {
        try {
            Path directory = Paths.get(directoryPath);
            if (Files.exists(directory) && Files.isDirectory(directory)) {
                Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if (Files.getLastModifiedTime(file).toMillis() < cutoffTime) {
                                Files.delete(file);
                                System.out.println("Deleted old file: " + file);
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to delete file: " + file + " - " + e.getMessage());
                        }
                    });
            } else {
                System.out.println("Directory not found, skipping cleanup: " + directoryPath);
            }
        } catch (IOException e) {
            System.err.println("Error during file cleanup in directory: " + directoryPath + " - " + e.getMessage());
        }
    }
}
