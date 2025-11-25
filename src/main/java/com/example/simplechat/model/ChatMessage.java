package com.example.simplechat.model;

import com.example.simplechat.dto.ChatMessageRequestDto;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * 채팅방 내의 단일 채팅 메시지를 나타냅니다. 이 클래스는 메시지의 작성자, 내용, 유형 등 메시지에 대한 정보를 포함하는 핵심 데이터
 * 모델입니다.
 */
@Setter
@Getter
public class ChatMessage {

    /**
     * 채팅 메시지의 유형을 정의합니다. 클라이언트가 메시지를 적절하게 렌더링하는 데 도움이 됩니다.
     */
    public enum MsgType {
        /**
         * 표준 일반 텍스트 메시지입니다.
         */
        TEXT,
        /**
         * 이미지를 포함하는 메시지입니다. 내용은 일반적으로 이미지 URL입니다.
         */
        IMAGE,
        /**
         * 비디오를 포함하는 메시지입니다. 내용은 일반적으로 비디오 URL입니다.
         */
        VIDEO,
        /**
         * 일반 파일에 대한 링크를 포함하는 메시지입니다.
         */
        FILE,
        /**
         * 메시지가 삭제되었음을 나타내는 특수 유형입니다.
         */
        DELETE,
        /**
         * 메시지 내용이 업데이트되었음을 나타내는 특수 유형입니다.
         */
        UPDATE
    }

    /**
     * 메시지의 고유 식별자입니다.
     */
    private Long id;
    /**
     * 이 메시지가 속한 채팅방의 ID입니다.
     */
    private Long room_id;
    /**
     * 메시지를 보낸 사용자의 ID입니다.
     */
    private Long author_id;
    /**
     * 메시지를 보낸 사용자의 이름입니다.
     */
    private String author_name;
    /**
     * 메시지의 내용입니다. 일반 텍스트 또는 미디어 유형의 URL이 될 수 있습니다.
     */
    private String content;
    /**
     * 메시지가 생성된 타임스탬프입니다.
     */
    private String created_at;
    /**
     * {@link MsgType} 열거형에 의해 정의된 메시지 유형입니다.
     */
    private MsgType msg_type;
    /**
     * 스레딩 또는 답글에 사용되는 상위 메시지의 ID입니다.
     */
    private Long parent_msg_id;

    public ChatMessage() {
    }

    public ChatMessage(Long id, Long room_id, MsgType messageType) {
        this.id = id;
        this.room_id = room_id;
        this.msg_type = messageType;
    }

    public ChatMessage(Long author_id, Long room_id, String author_name) {
        this.author_id = author_id;
        this.room_id = room_id;
        this.author_name = author_name;
    }

    public ChatMessage(Long newId, Long author_id, String author_name, Long room_id) {
        this.id = newId;
        this.author_id = author_id;
        this.author_name = author_name;
        this.room_id = room_id;
    }

    public ChatMessage(Long room_id, Long author_id, String author_name, String content,
        MsgType msg_type) {
        this.room_id = room_id;
        this.author_id = author_id;
        this.author_name = author_name;
        this.content = content;
        this.msg_type = msg_type;
    }

    public ChatMessage(ChatMessageRequestDto dto, String AuthorName) {
        this.room_id = dto.roomId();
        this.author_id = dto.authorId();
        this.author_name = AuthorName;
        this.content = dto.content();
        this.msg_type = dto.messageType();
    }

    /**
     * 이 메시지를 이전 버전의 동일한 메시지와 비교하여 변경된 필드를 찾습니다. 부분 업데이트를 생성하는 데 유용합니다.
     *
     * @param oldMsg 비교할 이전 버전의 채팅 메시지입니다.
     * @return 변경된 필드의 이름을 키로, 새 값을 값으로 갖는 맵입니다.
     */
    public Map<String, Object> getChangedFields(ChatMessage oldMsg) {
        Map<String, Object> changes = new HashMap<>();

        if (!Objects.equals(this.content, oldMsg.content)) {
            changes.put("content", this.content);
        }

        return changes;
    }
}