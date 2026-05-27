package org.donghaobin.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum RoomRole {
    ADMIN("ADMIN"),
    MEMBER("MEMBER");

    @EnumValue
    private final String value;

    RoomRole(String value) {
        this.value = value;
    }
}
