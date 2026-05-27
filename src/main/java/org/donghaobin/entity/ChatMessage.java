package org.donghaobin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("chat_messages")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessage(Long roomId, Long senderId, String senderUsername, String content) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
}
