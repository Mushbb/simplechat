package com.example.simplechat.repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 시스템에 파일을 저장하고 관리하는 리포지토리 클래스입니다.
 * 업로드 디렉토리를 초기화하고, 파일을 저장 및 삭제하는 기능을 제공합니다.
 */
public class FileRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileRepository.class);
    private final Path rootLocation;

    /**
     * 지정된 업로드 디렉토리를 기반으로 FileRepository 인스턴스를 생성합니다.
     *
     * @param uploadDir 파일을 저장할 루트 디렉토리 경로
     */
    public FileRepository(String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 파일 저장 위치를 초기화합니다.
     * 지정된 루트 디렉토리가 존재하지 않으면 생성합니다.
     * 초기화에 실패하면 RuntimeException을 발생시킵니다.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            logger.info("업로드 디렉토리가 생성되었거나 이미 존재합니다: {}", rootLocation);
        } catch (IOException e) {
            logger.error("저장 위치 초기화에 실패했습니다: {}", rootLocation, e);
            throw new RuntimeException("Could not initialize storage location: " + rootLocation, e);
        }
    }

    /**
     * 파일을 저장하고 고유한 파일명을 반환합니다.
     *
     * @param file 저장할 MultipartFile
     * @return 저장된 고유 파일명 (확장자 포함)
     * @throws RuntimeException 파일 저장 실패 시
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("비어있는 파일을 저장할 수 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFilename = UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            Path destinationFile = this.rootLocation.resolve(storedFilename);

            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new RuntimeException("현재 디렉토리 외부에 파일을 저장할 수 없습니다.");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            logger.error("파일 {} 저장에 실패했습니다.", originalFilename, e);
            throw new RuntimeException("파일 저장 실패: " + originalFilename, e);
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
            logger.info("파일 {}이(가) 삭제되었습니다.", filename);
        } catch (IOException e) {
            logger.error("파일 {} 삭제에 실패했습니다.", filename, e);
        }
    }
}
