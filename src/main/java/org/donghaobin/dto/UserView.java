package org.donghaobin.dto;

import lombok.Getter;
import org.donghaobin.entity.UserAccount;

@Getter
public class UserView {

    private final Long id;
    private final String username;

    public UserView(UserAccount user) {
        this.id = user.getId();
        this.username = user.getUsername();
    }
}
