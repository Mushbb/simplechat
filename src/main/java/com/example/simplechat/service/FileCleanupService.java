package com.example.simplechat.service;

import com.example.simplechat.repository.FileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 오래된 파일을 주기적으로 정리하는 서비스 클래스입니다.
 * 설정된 보존 기간을 초과한 파일을 파일 시스템에서 삭제합니다.
 */
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupService.class);

    @Value("${file.chat-upload-dir}")
    private String chatUploadDir;

    @Value("${file.retention-days}")
    private long retentionDays;

    @Autowired
    @Qualifier("chatFileRepository")
    private FileRepository chatFileRepository;


    /**
     * 매일 자정에 실행되도록 스케줄링된 메서드입니다.
     * 지정된 보존 기간을 초과한 오래된 채팅 파일을 정리합니다.
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void cleanupOldFiles() {
        logger.info("오래된 파일 정리 스케줄링 시작...");
        long retentionMillis = retentionDays * 24 * 60 * 60 * 1000;
        long cutoffTime = System.currentTimeMillis() - retentionMillis;

        deleteOldFiles(chatUploadDir, cutoffTime);

        logger.info("오래된 파일 정리 스케줄링 완료.");
    }

    /**
     * 지정된 디렉토리에서 특정 cutoff 시간을 초과한 오래된 파일들을 삭제합니다.
     *
     * @param directoryPath 파일을 정리할 디렉토리 경로
     * @param cutoffTime 이 시간보다 이전에 수정된 파일만 삭제됩니다. (밀리초 단위의 타임스탬프)
     */
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
                                logger.info("오래된 파일 삭제: {}", file);
                            }
                        } catch (IOException e) {
                            logger.error("파일 삭제 실패: {} - {}", file, e.getMessage());
                        }
                    });
            } else {
                logger.warn("디렉토리를 찾을 수 없습니다. 정리 작업을 건너뜥니다: {}", directoryPath);
            }
        } catch (IOException e) {
            logger.error("디렉토리 {}에서 파일 정리 중 오류 발생: {}", directoryPath, e.getMessage(), e);
        }
    }
}
