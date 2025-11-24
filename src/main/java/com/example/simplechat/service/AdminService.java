package com.example.simplechat.service;

import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.dto.ChatRoomListDto;
import com.example.simplechat.dto.ChatRoomUserDto;
import com.example.simplechat.dto.RoomCreateDto;
import com.example.simplechat.repository.RoomRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository; // To find users in room for /users command
    private final UserRepository userRepository; // To check user existence (if needed in commands)
    private final ChatRoomService chatRoomService; // For createRoom command
    private final MessageService messageService; // For cleanup (old simplechatService also had this)
    private final FileCleanupService fileCleanupService; // For cleanup command

    private ChatRoom serverChat_room = null; // Used for /enter command in old service

    public String executeAdminCommand(String command) {
        CommandParser.ParseResult result = CommandParser.parse(command);
        
        switch (result.command) {
            case "rooms":
                List<ChatRoom> allRooms = roomRepository.findAll();
                if(allRooms.isEmpty()) {
                    return "No chat rooms found.";
                } else {
                    StringBuilder sb = new StringBuilder("--- Chat Room List ---
");
                    sb.append(String.format("%-10s | %-20s | %-10s | %-15s%n", "ID", "Room Name", "Type", "Created At"));
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
                
                // Call the new ChatRoomService to create the room
                chatRoomService.createRoom(new RoomCreateDto(result.args, room_type, pass_hash), 0L); // Assuming system user is 0L
                return "Room '" + result.args + "' created.";
            case "enter":
                Optional<ChatRoom> room = roomRepository.findByName(result.args);
                if( room.isPresent() ) {
                    serverChat_room = room.get();
                    return "Entered room: " + room.get().getName();
                } else {
                    return "There is no such room.";
                }
            case "users":
                if( serverChat_room == null )
                    return "There is no selected room.";
                
                List<ChatRoomUserDto> allUsers = roomRepository.findUsersByRoomId(serverChat_room.getId());
                if( allUsers.isEmpty() ) {
                    return "There are no users in this room.";
                } else {
                    StringBuilder sb = new StringBuilder("--- User List ---
");
                    sb.append(String.format("%-10s | %-20s | %-10s%n", "ID", "Nickname", "Role"));
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
                fileCleanupService.cleanupOldFiles(); // Call the dedicated file cleanup service
                return "Manual cleanup finished.";
            default:
                return "Unknown command: " + result.command;
        }
    }

    // CommandLine Parser (Copied from old SimplechatService)
    public static class CommandParser {
        public static class ParseResult {
            public String command;
            public String args; // 옵션 전까지 띄어쓰기 포함 인자
            public Map<String, String> options = new HashMap<>();

            @Override
            public String toString() {
                return "ParseResult{"
                        + "command='" + command + '\''
                        + ", args='" + args + '\''
                        + ", options=" + options
                        + '}
';
            }
        }

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

            // 옵션(-) 시작 전까지 args 추출
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

            // 옵션 파싱
            while (i < tokens.length) {
                String token = tokens[i];
                if (!token.startsWith("-")) {
                    throw new IllegalArgumentException("옵션 이름은 -로 시작해야 합니다: " + token);
                }
                String optionName = token.substring(1);
                String optionValue = "true"; // 값이 없으면 true 처리

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
