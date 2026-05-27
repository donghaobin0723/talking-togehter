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
@TableName("room_blacklists")
public class RoomBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private Long adminId;
    private String reason;
    private LocalDateTime createdAt;

    public RoomBlacklist(Long roomId, Long userId, Long adminId, String reason) {
        this.roomId = roomId;
        this.userId = userId;
        this.adminId = adminId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
