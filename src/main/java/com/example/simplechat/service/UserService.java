package com.example.simplechat.service;

import com.example.simplechat.dto.ProfileUpdateRequestDto;
import com.example.simplechat.dto.UserProfileDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FileRepository;
import com.example.simplechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Qualifier("profileFileRepository")
    private final FileRepository profileFileRepository;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    public UserProfileDto getUserProfile(Long userId) {
        // 1. UserRepository를 사용하여 프로필 정보 조회
        Map<String, Object> profileData =
                userRepository.findProfileById(userId)
                        .orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

        String imageUrl = (String) profileData.get("profile_image_url");

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = profileStaticUrlPrefix + "/default.png"; // 기본 이미지 경로
        } else {
            imageUrl = profileStaticUrlPrefix + "/" + imageUrl; // 저장된 이미지 경로
        }

        // 2. Map에서 DTO로 데이터 변환
        return new UserProfileDto(
                (Long) profileData.get("user_id"),
                (String) profileData.get("username"),
                (String) profileData.get("nickname"),
                (String) profileData.get("status_message"),
                imageUrl
        );
    }

    @Transactional
    public UserProfileDto changeUserProfile(ProfileUpdateRequestDto profileDto, Long userId) {
        // 1. DB에서 현재 사용자 정보를 가져옴
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

        // 2. DTO의 값으로 User 객체의 필드를 업데이트
        user.setNickname(profileDto.nickname());
        user.setStatus_message(profileDto.statusMessage());

        // 3. save 메서드를 호출하여 변경된 필드만 동적으로 업데이트
        userRepository.save(user);

        // 4. 변경된 최신 프로필 정보를 다시 조회하여 반환
        return getUserProfile(userId);
    }

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) {
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

        // 2. 기존 프로필 이미지가 있으면 삭제
        String oldFilename = user.getProfile_image_url();
        if (oldFilename != null && !oldFilename.isBlank()) {
            profileFileRepository.delete(oldFilename);
        }

        // 3. 새 파일 저장
        String newFilename = profileFileRepository.save(file);

        // 4. DB에 새 파일명 업데이트
        user.setProfile_image_url(newFilename);
        userRepository.save(user);

        // 5. 클라이언트가 접근할 수 있는 URL 경로 반환
        return profileStaticUrlPrefix + "/" + newFilename;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));
    }
}
