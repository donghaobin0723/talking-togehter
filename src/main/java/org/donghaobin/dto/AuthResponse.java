package org.donghaobin.dto;

import lombok.Getter;
import org.donghaobin.entity.UserAccount;

@Getter
public class AuthResponse {

    private final String token;
    private final UserView user;

    public AuthResponse(String token, UserAccount user) {
        this.token = token;
        this.user = new UserView(user);
    }
}
