package com.example.simplechat.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Repository
public class FileRepository {

    private final Path rootLocation;

    public FileRepository(@Value("${file.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            System.out.println("Upload directory created or already exists: " + rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location: " + rootLocation, e);
        }
    }

    /**
     * 파일을 저장하고 고유한 파일명을 반환합니다.
     *
     * @param file 저장할 MultipartFile
     * @return 저장된 고유 파일명 (확장자 포함)
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }
        
        // 원본 파일의 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // UUID를 사용하여 고유한 파일명 생성
        String storedFilename = UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            Path destinationFile = this.rootLocation.resolve(storedFilename);

            if (!destinationFile.getParent().equals(this.rootLocation)) {
                // This is a security check to prevent directory traversal attacks
                throw new RuntimeException("Cannot store file outside current directory.");
            }
            
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        }
    }

    /**
     * 파일명으로 파일을 삭제합니다.
     *
     * @param filename 삭제할 파일명
     */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Path fileToDelete = rootLocation.resolve(filename);
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            // 파일 삭제 실패 시 로깅 또는 예외 처리. 
            // 여기서는 일단 에러를 출력하지만, 실제 운영 환경에서는 로거를 사용하는 것이 좋습니다.
            System.err.println("Failed to delete file: " + filename);
        }
    }
}
