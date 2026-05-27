package org.donghaobin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlacklistRequest {

    private String username;
    private String reason;
}
