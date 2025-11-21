package com.example.simplechat.controller;

import com.example.simplechat.dto.LoginRequestDto;
import com.example.simplechat.dto.LoginResponseDto;
import com.example.simplechat.dto.UserRegistrationRequestDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.User;
import com.example.simplechat.service.AuthService;
import com.example.simplechat.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public LoginResponseDto registerRequest(@RequestBody UserRegistrationRequestDto requestDto, HttpSession session) {
        User registered = authService.register(requestDto);

        // 2. Spring Security가 인식할 인증 토큰(공식 출입증) 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                registered.getUsername(),
                null,
                new ArrayList<>()
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        session.setAttribute("userId", registered.getId());
        session.setMaxInactiveInterval(180 * 60); // 180분 동안 비활성 시 세션 만료

        return new LoginResponseDto(
                registered.getId(),
                registered.getUsername(),
                registered.getNickname()
        );
    }

    @PostMapping("/login")
    public LoginResponseDto loginRequest(@RequestBody LoginRequestDto requestDto, HttpSession session) {
        User loggedIn = authService.login(requestDto);

        // 2. 인증 토큰(공식 출입증) 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loggedIn.getUsername(), // principal: 주로 사용자 ID나 객체를 넣습니다.
                null,                  // credentials: 비밀번호는 이미 검증했으므로 null 처리합니다.
                new ArrayList<>()      // authorities: 사용자의 권한 목록 (지금은 빈 리스트)
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        session.setAttribute("userId", loggedIn.getId());
        session.setMaxInactiveInterval(180 * 60); // 180분 동안 비활성 시 세션 만료

        return new LoginResponseDto(
                loggedIn.getId(),
                loggedIn.getUsername(),
                loggedIn.getNickname()
        );
    }

    @GetMapping("/session")
    public LoginResponseDto sessionCheck(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        System.out.println("UserId: " + userId);
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "Not Logged In");
        }

        User loggedIn = userService.getUserById(userId);
        return new LoginResponseDto(
                loggedIn.getId(),
                loggedIn.getUsername(),
                loggedIn.getNickname()
        );
    }

    @PostMapping("/logout")    // /logout은 예약엔드포인트였어..
    public Integer logoutRequest(HttpSession session) {
        System.out.println("Session Closed: " + session.getAttribute("userId"));
        session.invalidate(); // 세션 무효화
        return 1;
    }

    @DeleteMapping("/delete")
    public Integer deleteRequest(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        authService.deleteAccount(userId);
        System.out.println("Session Closed: " + userId);
        session.invalidate(); // 세션 무효화
        return 1;
    }
}
