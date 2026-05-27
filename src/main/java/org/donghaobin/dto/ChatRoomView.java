package org.donghaobin.dto;

import lombok.Getter;
import org.donghaobin.entity.ChatRoom;
import org.donghaobin.entity.RoomRole;

import java.time.LocalDateTime;

@Getter
public class ChatRoomView {

    private final Long id;
    private final String name;
    private final String ownerUsername;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime stoppedAt;
    private final RoomRole role;
    private final boolean joined;
    private final boolean blacklisted;

    public ChatRoomView(ChatRoom room, String ownerUsername, RoomRole role, boolean joined, boolean blacklisted) {
        this.id = room.getId();
        this.name = room.getName();
        this.ownerUsername = ownerUsername;
        this.active = room.isActive();
        this.createdAt = room.getCreatedAt();
        this.stoppedAt = room.getStoppedAt();
        this.role = role;
        this.joined = joined;
        this.blacklisted = blacklisted;
    }
}
