package com.example.simplechat.service;

import com.example.simplechat.dto.ChatRoomUserDto;
import com.example.simplechat.dto.RoomCreateDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.repository.RoomRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 명령을 처리하는 서비스 클래스입니다.
 * 채팅방 관리, 사용자 조회, 시스템 정리 등 다양한 관리자 기능을 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;
//	private final MessageService messageService; // 현재 코드에서는 사용되지 않음
    private final FileCleanupService fileCleanupService;

    // 현재 선택된 채팅방을 저장하는 필드.
    // 멀티스레드 환경에서는 주의하여 사용해야 합니다 (단일 관리자 세션용으로 가정).
    private ChatRoom serverChat_room = null;

    /**
     * 관리자 명령 문자열을 파싱하고 실행합니다.
     *
     * @param command 실행할 관리자 명령 문자열 (예: "/rooms", "/create 방이름 -private 비밀번호")
     * @return 명령 실행 결과 메시지
     * @throws IllegalArgumentException 명령 파싱 오류 발생 시
     * @throws RegistrationException 채팅방 생성 또는 입장 실패 시
     */
    public String executeAdminCommand(String command) {
        CommandParser.ParseResult result = CommandParser.parse(command);
        
        switch (result.command) {
            case "rooms":
                List<ChatRoom> allRooms = roomRepository.findAll();
                if(allRooms.isEmpty()) {
                    return "채팅방이 없습니다.";
                } else {
                    StringBuilder sb = new StringBuilder("--- 채팅방 목록 ---\n");
                    sb.append(String.format("%-10s | %-20s | %-10s | %-15s%n", "ID", "방 이름", "유형", "생성 시간"));
                    for(ChatRoom room : allRooms) {
                        sb.append(String.format("%-10s | %-20s | %-10s | %-15s\n",
                                room.getId(),
                                room.getName(),
                                room.getRoom_type().name(),
                                room.getCreated_at()));
                        sb.append("------------------------------------------------------------\n");
                    }
                    return sb.toString();
                }
            case "create":
                ChatRoom.RoomType room_type;
                String pass_hash = null;
                if( result.options.containsKey("private") ) {
                    room_type = ChatRoom.RoomType.PRIVATE;
                    pass_hash = result.options.get("private");
                } else {
                    room_type = ChatRoom.RoomType.PUBLIC;
                }
                
                // 시스템 사용자(ID 0L)를 가정하여 채팅방 생성
                chatRoomService.createRoom(new RoomCreateDto(result.args, room_type, pass_hash), 0L);
                return "채팅방 '" + result.args + "'가 생성되었습니다.";
            case "enter":
                Optional<ChatRoom> room = roomRepository.findByName(result.args);
                if( room.isPresent() ) {
                    serverChat_room = room.get();
                    return "'" + room.get().getName() + "' 방에 입장했습니다.";
                } else {
                    return "해당하는 방이 없습니다.";
                }
            case "users":
                if( serverChat_room == null )
                    return "선택된 방이 없습니다. '/enter 방이름' 명령으로 방에 입장해주세요.";
                
                List<ChatRoomUserDto> allUsers = roomRepository.findUsersByRoomId(serverChat_room.getId());
                if( allUsers.isEmpty() ) {
                    return "이 방에는 사용자가 없습니다.";
                } else {
                    StringBuilder sb = new StringBuilder("--- 사용자 목록 ---\n");
                    sb.append(String.format("%-10s | %-20s | %-10s%n", "ID", "닉네임", "역할"));
                    for(ChatRoomUserDto user : allUsers) {
                        sb.append(String.format("%-10s | %-20s | %-10s\n",
                                user.userId(),
                                user.nickname(),
                                user.role()));
                        sb.append("------------------------------------------------------------\n");
                    }
                    return sb.toString();
                }
            case "cleanup":
                fileCleanupService.cleanupOldFiles(); // 파일 정리 서비스 호출
                return "수동 정리 작업이 완료되었습니다.";
            default:
                return "알 수 없는 명령입니다: " + result.command;
        }
    }

    /**
     * 명령줄 문자열을 파싱하여 명령, 인수 및 옵션으로 분리하는 정적 유틸리티 클래스입니다.
     * (이전 SimplechatService에서 복사됨)
     */
    public static class CommandParser {
        /**
         * 파싱된 명령의 결과를 나타내는 내부 클래스입니다.
         */
        public static class ParseResult {
            /** 실행될 명령 이름 (예: "rooms", "create") */
            public String command;
            /** 옵션 시작 전까지의 모든 인자 (공백 포함) */
            public String args;
            /** `-`로 시작하는 옵션과 그 값들의 맵 */
            public Map<String, String> options = new HashMap<>();

            @Override
            public String toString() {
                return "ParseResult{"
                        + "command='" + command + '\''
                        + ", args='" + args + '\''
                        + ", options=" + options
                        + '}';
            }
        }

        /**
         * 입력 문자열을 파싱하여 명령, 인수 및 옵션으로 분리합니다.
         * 명령은 `/`로 시작해야 하며, 옵션은 `-`로 시작합니다.
         *
         * @param input 파싱할 명령줄 입력 문자열
         * @return 파싱 결과를 담은 {@link ParseResult} 객체
         * @throws IllegalArgumentException 입력이 비어 있거나, 명령 형식이 올바르지 않은 경우
         */
        public static ParseResult parse(String input) {
            ParseResult result = new ParseResult();

            if (input == null || input.trim().isEmpty()) {
                throw new IllegalArgumentException("빈 입력입니다.");
            }

            String[] tokens = input.trim().split("\\s+");
            if (!tokens[0].startsWith("/")) {
                throw new IllegalArgumentException("명령어는 /로 시작해야 합니다.");
            }

            result.command = tokens[0].substring(1);

            StringBuilder argsBuilder = new StringBuilder();
            int i = 1;
            for (; i < tokens.length; i++) {
                if (tokens[i].startsWith("-")) {
                    break;
                }
                if (argsBuilder.length() > 0) {
                    argsBuilder.append(" ");
                }
                argsBuilder.append(tokens[i]);
            }
            result.args = argsBuilder.toString();

            while (i < tokens.length) {
                String token = tokens[i];
                if (!token.startsWith("-")) {
                    throw new IllegalArgumentException("옵션 이름은 -로 시작해야 합니다: " + token);
                }
                String optionName = token.substring(1);
                String optionValue = "true";

                if (i + 1 < tokens.length && !tokens[i + 1].startsWith("-")) {
                    optionValue = tokens[i + 1];
                    i++;
                }
                result.options.put(optionName, optionValue);
                i++;
            }

            return result;
        }
    }
}
