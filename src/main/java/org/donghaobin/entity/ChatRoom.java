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
@TableName("chat_rooms")
public class ChatRoom {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long ownerId;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime stoppedAt;

    public ChatRoom(String name, Long ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = LocalDateTime.now();
    }

    public void stop() {
        active = false;
        stoppedAt = LocalDateTime.now();
    }
}
