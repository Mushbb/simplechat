package com.example.simplechat.controller;

import com.example.simplechat.dto.ChatRoomListDto;
import com.example.simplechat.dto.InviteRequestDto;
import com.example.simplechat.dto.RoomCreateDto;
import com.example.simplechat.dto.RoomEnterDto;
import com.example.simplechat.dto.RoomInitDataDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.ChatRoomService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 관리를 위한 REST 컨트롤러입니다. 채팅방 생성, 목록 조회, 참여 및 관리를 위한 엔드포인트를 제공합니다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/room")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * GET /room/list 사용 가능한 모든 채팅방 목록을 가져옵니다.
     *
     * @param session 현재 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @return {@link ChatRoomListDto} 객체 목록입니다.
     */
    @GetMapping("/list")
    public List<ChatRoomListDto> getRoomList(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return chatRoomService.getRoomList(userId);
    }

    /**
     * POST /room/create 새로운 채팅방을 생성합니다.
     *
     * @param roomcreateDto 새 방에 대한 세부 정보가 포함된 DTO입니다.
     * @param session 생성자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @return 새로 생성된 방의 ID입니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @PostMapping("/create")
    public Long createRoom(@RequestBody RoomCreateDto roomcreateDto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "먼저 로그인해주세요!");
        }
        return chatRoomService.createRoom(roomcreateDto, userId);
    }

    /**
     * POST /room/{roomId}/users 현재 사용자가 채팅방에 입장할 수 있도록 합니다.
     *
     * @param roomId 입장할 방의 ID입니다.
     * @param enterDto 필요한 경우 비밀번호가 포함된 DTO입니다.
     * @param session 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @return 방의 ID입니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @PostMapping("/{roomId}/users")
    public Long enterRoom(@PathVariable("roomId") Long roomId, @RequestBody RoomEnterDto enterDto,
        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "먼저 로그인해주세요!");
        }

        return chatRoomService.enterRoom(roomId, userId, enterDto.password());
    }

    /**
     * DELETE /room/{roomId}/users 현재 사용자가 채팅방에서 퇴장할 수 있도록 합니다.
     *
     * @param roomId 퇴장할 방의 ID입니다.
     * @param session 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @DeleteMapping("/{roomId}/users")
    public void exitRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatRoomService.exitRoom(roomId, userId);
    }

    /**
     * DELETE /room/{roomId} 채팅방을 삭제합니다. 이 작업은 방 소유자만 수행할 수 있습니다.
     *
     * @param roomId 삭제할 방의 ID입니다.
     * @param session 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @DeleteMapping("/{roomId}")
    public void deleteRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatRoomService.deleteRoom(roomId, userId);
    }

    /**
     * GET /room/my-rooms 현재 사용자가 참여한 채팅방 목록을 가져옵니다.
     *
     * @param session 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @return 방 목록 또는 인증되지 않은 상태를 포함하는 {@link ResponseEntity}입니다.
     */
    @GetMapping("/my-rooms")
    public ResponseEntity<List<ChatRoomListDto>> getMyRooms(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ChatRoomListDto> myRooms = chatRoomService.findRoomsByUserId(userId);
        return ResponseEntity.ok(myRooms);
    }

    /**
     * GET /room/{roomId}/init 채팅방의 초기 데이터(최근 메시지 및 사용자 목록 포함)를 가져옵니다.
     *
     * @param roomId 방의 ID입니다.
     * @param lines 가져올 최근 메시지의 수입니다.
     * @param session 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @return 초기 방 상태를 가진 {@link RoomInitDataDto}입니다.
     */
    @GetMapping("/{roomId}/init")
    public RoomInitDataDto initRoom(@PathVariable("roomId") Long roomId,
        @RequestParam(name = "lines", defaultValue = "20") int lines, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        return chatRoomService.initRoom(roomId, userId, lines);
    }

    /**
     * DELETE /room/{roomId}/users/{userId} 채팅방에서 사용자를 강제 퇴장시킵니다. 이 작업은 방 소유자만 수행할 수
     * 있습니다.
     *
     * @param roomId 방의 ID입니다.
     * @param userIdToKick 강제 퇴장될 사용자의 ID입니다.
     * @param session 강제 퇴장을 수행하는 사용자를 식별하는 데 사용되는 HTTP 세션입니다.
     * @throws RegistrationException 강제 퇴장을 수행하는 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @DeleteMapping("/{roomId}/users/{userId}")
    public void kickUserFromRoom(@PathVariable("roomId") Long roomId,
        @PathVariable("userId") Long userIdToKick, HttpSession session) {
        Long kickerId = (Long) session.getAttribute("userId");
        if (kickerId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatRoomService.kickUser(roomId, kickerId, userIdToKick);
    }

    /**
     * POST /room/{roomId}/invite 사용자를 채팅방에 초대합니다.
     *
     * @param roomId 방의 ID입니다.
     * @param inviteDto 초대할 사용자의 ID가 포함된 DTO입니다.
     * @param session 초대하는 사람을 식별하는 데 사용되는 HTTP 세션입니다.
     * @return OK 상태의 {@link ResponseEntity}입니다.
     * @throws RegistrationException 초대하는 사람이 로그인하지 않은 경우 발생합니다.
     */
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteUserToRoom(
        @PathVariable("roomId") Long roomId,
        @RequestBody InviteRequestDto inviteDto,
        HttpSession session) {

        Long inviterId = (Long) session.getAttribute("userId");
        if (inviterId == null) {
            throw new RegistrationException("UNAUTHORIZED", "로그인이 필요합니다.");
        }

        chatRoomService.inviteUserToRoom(roomId, inviterId, inviteDto.userId());

        return ResponseEntity.ok().build();
    }
}
