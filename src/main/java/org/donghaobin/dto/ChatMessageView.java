package org.donghaobin.dto;

import lombok.Getter;
import org.donghaobin.entity.ChatMessage;

import java.time.LocalDateTime;

@Getter
public class ChatMessageView {

    private final Long id;
    private final Long roomId;
    private final String sender;
    private final String content;
    private final LocalDateTime sentAt;

    public ChatMessageView(ChatMessage message) {
        this.id = message.getId();
        this.roomId = message.getRoomId();
        this.sender = message.getSenderUsername();
        this.content = message.getContent();
        this.sentAt = message.getSentAt();
    }
}
