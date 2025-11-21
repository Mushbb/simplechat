package com.example.simplechat.service;

import com.example.simplechat.dto.LoginRequestDto;
import com.example.simplechat.dto.UserRegistrationRequestDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomUserRepository roomUserRepository;
    private final ChatRoomService chatRoomService;

    @Transactional
    public User register(UserRegistrationRequestDto requestDto) {
        if (requestDto.username().equals("system")) {
            throw new RegistrationException("INVALID_USERNAME", "Invalid Username");
        } else if (!userRepository.findByUsername(requestDto.username()).isEmpty()) {
            throw new RegistrationException("DUPLICATE_USERNAME", "Username is already exist");
        }

        return userRepository.save(new User(requestDto.username(), passwordEncoder.encode(requestDto.password()), requestDto.nickname()));
    }

    public User login(LoginRequestDto requestDto) {
        Optional<User> requested = userRepository.findByUsername(requestDto.username());
        if (requested.isEmpty()) {
            throw new RegistrationException("INVALID_USERNAME", "Username is not exist");
        } else if (!passwordEncoder.matches(requestDto.password(), requested.get().getPassword_hash())) {
            throw new RegistrationException("INVALID_PASSWORD", "Password is wrong");
        }

        return requested.get();
    }

    @Transactional
    public void deleteAccount(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId);
        }

        // 3. 방장이었던 방도 같이 삭제
        for (ChatRoom room : chatRoomService.findRoomsByOwnerId(userId)) {
            chatRoomService.deleteRoom(room.getId(), userId);
        }

        // 1. 사용자가 속한 모든 채팅방 멤버십 정보 삭제
        roomUserRepository.deleteByUserId(userId);

        // 2. 사용자 계정 자체를 삭제
        userRepository.deleteById(userId);
    }
}
