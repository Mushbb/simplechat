package com.example.simplechat.dto;

import com.example.simplechat.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponseDto {
    private Long userId;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String status; // e.g., PENDING_SENT, PENDING_RECEIVED, ACCEPTED

    public static FriendResponseDto from(User user, String status) {
        return new FriendResponseDto(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getProfile_image_url(),
            status
        );
    }
}
