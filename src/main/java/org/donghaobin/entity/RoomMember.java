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
@TableName("room_members")
public class RoomMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private RoomRole role;
    private LocalDateTime joinedAt;

    public RoomMember(Long roomId, Long userId, RoomRole role) {
        this.roomId = roomId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }
}
