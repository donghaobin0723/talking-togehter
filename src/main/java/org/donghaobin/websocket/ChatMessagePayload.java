package org.donghaobin.websocket;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessagePayload {

    private String sender;
    private String content;

    public ChatMessagePayload(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }
}
