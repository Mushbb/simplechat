package com.example.simplechat.service;

import com.example.simplechat.dto.ProfileUpdateRequestDto;
import com.example.simplechat.dto.UserProfileDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FileRepository;
import com.example.simplechat.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 주로 사용자 프로필 정보 조회, 변경, 프로필 이미지 업데이트 등의 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Qualifier("profileFileRepository")
    private final FileRepository profileFileRepository;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    /**
     * 특정 사용자의 프로필 정보를 조회합니다.
     * 프로필 이미지가 없는 경우 기본 이미지 경로를 반환합니다.
     *
     * @param userId 프로필을 조회할 사용자의 ID
     * @return 조회된 {@link UserProfileDto} 객체
     * @throws RegistrationException 해당 ID의 사용자를 찾을 수 없는 경우
     */
    public UserProfileDto getUserProfile(Long userId) {
        Map<String, Object> profileData =
                userRepository.findProfileById(userId)
                        .orElseThrow(() -> new RegistrationException("NOT_FOUND", "ID " + userId + "을(를) 가진 사용자를 찾을 수 없습니다."));

        String imageUrl = (String) profileData.get("profile_image_url");

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = profileStaticUrlPrefix + "/default.png"; // 기본 이미지 경로
        } else {
            imageUrl = profileStaticUrlPrefix + "/" + imageUrl; // 저장된 이미지 경로
        }

        return new UserProfileDto(
                (Long) profileData.get("user_id"),
                (String) profileData.get("username"),
                (String) profileData.get("nickname"),
                (String) profileData.get("status_message"),
                imageUrl
        );
    }

    /**
     * 사용자의 프로필 정보(닉네임, 상태 메시지)를 변경합니다.
     *
     * @param profileDto 변경할 프로필 정보를 담은 DTO
     * @param userId 프로필을 변경할 사용자의 ID
     * @return 변경된 최신 {@link UserProfileDto} 객체
     * @throws RegistrationException 해당 ID의 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public UserProfileDto changeUserProfile(ProfileUpdateRequestDto profileDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "ID " + userId + "을(를) 가진 사용자를 찾을 수 없습니다."));

        user.setNickname(profileDto.nickname());
        user.setStatus_message(profileDto.statusMessage());

        userRepository.save(user);

        return getUserProfile(userId);
    }

    /**
     * 사용자의 프로필 이미지를 업데이트합니다.
     * 기존 프로필 이미지가 있다면 삭제하고, 새 이미지를 저장한 후 데이터베이스에 파일명을 업데이트합니다.
     *
     * @param userId 프로필 이미지를 업데이트할 사용자의 ID
     * @param file 업로드할 새 프로필 이미지 파일
     * @return 클라이언트가 접근할 수 있는 새 프로필 이미지의 URL 경로
     * @throws RegistrationException 해당 ID의 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "ID " + userId + "을(를) 가진 사용자를 찾을 수 없습니다."));

        String oldFilename = user.getProfile_image_url();
        if (oldFilename != null && !oldFilename.isBlank()) {
            profileFileRepository.delete(oldFilename);
        }

        String newFilename = profileFileRepository.save(file);

        user.setProfile_image_url(newFilename);
        userRepository.save(user);

        return profileStaticUrlPrefix + "/" + newFilename;
    }

    /**
     * 사용자 ID를 기준으로 {@link User} 엔티티를 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 조회된 {@link User} 엔티티
     * @throws RuntimeException 해당 ID의 사용자를 찾을 수 없는 경우
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));
    }
}
