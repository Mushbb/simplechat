package com.example.simplechat.service;

import com.example.simplechat.dto.LoginRequestDto;
import com.example.simplechat.dto.UserRegistrationRequestDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 인증 및 등록 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 사용자 계정 생성, 로그인, 계정 삭제 등의 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomUserRepository roomUserRepository;
    private final ChatRoomService chatRoomService;

    /**
     * 새로운 사용자를 등록합니다.
     * 사용자 이름 중복을 확인하고, 비밀번호를 암호화하여 저장합니다.
     *
     * @param requestDto 사용자 등록 요청 데이터를 담은 DTO
     * @return 등록된 {@link User} 객체
     * @throws RegistrationException 사용자 이름이 'system'이거나 이미 존재하는 경우
     */
    @Transactional
    public User register(UserRegistrationRequestDto requestDto) {
        if (requestDto.username().equals("system")) {
            throw new RegistrationException("INVALID_USERNAME", "유효하지 않은 사용자명입니다.");
        } else if (!userRepository.findByUsername(requestDto.username()).isEmpty()) {
            throw new RegistrationException("DUPLICATE_USERNAME", "사용자명이 이미 존재합니다.");
        }

        return userRepository.save(new User(requestDto.username(), passwordEncoder.encode(requestDto.password()), requestDto.nickname()));
    }

    /**
     * 사용자 로그인을 처리합니다.
     * 사용자명과 비밀번호를 검증하여 유효한 사용자인지 확인합니다.
     *
     * @param requestDto 로그인 요청 데이터를 담은 DTO
     * @return 로그인에 성공한 {@link User} 객체
     * @throws RegistrationException 사용자명이 존재하지 않거나 비밀번호가 틀린 경우
     */
    public User login(LoginRequestDto requestDto) {
        Optional<User> requested = userRepository.findByUsername(requestDto.username());
        if (requested.isEmpty()) {
            throw new RegistrationException("INVALID_USERNAME", "사용자명이 존재하지 않습니다.");
        } else if (!passwordEncoder.matches(requestDto.password(), requested.get().getPassword_hash())) {
            throw new RegistrationException("INVALID_PASSWORD", "비밀번호가 틀렸습니다.");
        }

        return requested.get();
    }

    /**
     * 사용자 계정을 삭제합니다.
     * 해당 사용자가 소유한 채팅방을 먼저 삭제하고, 모든 채팅방 멤버십 정보 및 사용자 계정 자체를 삭제합니다.
     *
     * @param userId 삭제할 사용자의 ID
     * @throws RuntimeException 해당 ID의 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteAccount(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId);
        }

        // 사용자가 방장이었던 방도 같이 삭제
        for (ChatRoom room : chatRoomService.findRoomsByOwnerId(userId)) {
            chatRoomService.deleteRoom(room.getId(), userId);
        }

        // 사용자가 속한 모든 채팅방 멤버십 정보 삭제
        roomUserRepository.deleteByUserId(userId);

        // 사용자 계정 자체를 삭제
        userRepository.deleteById(userId);
    }
}
