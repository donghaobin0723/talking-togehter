package org.donghaobin.websocket;

import com.alibaba.fastjson2.JSON;
import org.donghaobin.dto.ChatMessageView;
import org.donghaobin.entity.ChatRoom;
import org.donghaobin.entity.ChatMessage;
import org.donghaobin.entity.UserAccount;
import org.donghaobin.service.AuthService;
import org.donghaobin.service.ChatMessageService;
import org.donghaobin.service.RoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String ROOM_ID_KEY = "roomId";
    private static final String USER_KEY = "user";

    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ChatMessageService chatMessageService;
    private final AuthService authService;
    private final RoomService roomService;

    public ChatWebSocketHandler(ChatMessageService chatMessageService,
                                AuthService authService,
                                RoomService roomService) {
        this.chatMessageService = chatMessageService;
        this.authService = authService;
        this.roomService = roomService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> query = parseQuery(session.getUri());
        Long roomId = parseRoomId(query.get("roomId"));
        UserAccount user = authService.authenticate(query.get("token"));
        ChatRoom room = roomService.requireRoom(roomId);
        roomService.ensureCanEnter(room, user);

        session.getAttributes().put(ROOM_ID_KEY, roomId);
        session.getAttributes().put(USER_KEY, user);
        roomSessions.computeIfAbsent(roomId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = (Long) session.getAttributes().get(ROOM_ID_KEY);
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.getOrDefault(roomId, Collections.emptySet());
            sessions.remove(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        ChatMessagePayload payload = JSON.parseObject(message.getPayload(), ChatMessagePayload.class);
        String content = normalize(payload.getContent());
        Long roomId = (Long) session.getAttributes().get(ROOM_ID_KEY);
        UserAccount user = (UserAccount) session.getAttributes().get(USER_KEY);

        if (content.isEmpty() || roomId == null || user == null) {
            return;
        }

        ChatRoom room = roomService.requireRoom(roomId);
        roomService.ensureCanEnter(room, user);
        ChatMessage savedMessage = chatMessageService.save(room, user, content);
        broadcast(roomId, JSON.toJSONString(new ChatMessageView(savedMessage)));
    }

    private void broadcast(Long roomId, String payload) throws IOException {
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : roomSessions.getOrDefault(roomId, Collections.emptySet())) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new HashMap<>();
        if (uri == null || uri.getQuery() == null) {
            return values;
        }
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0], parts[1]);
            }
        }
        return values;
    }

    private Long parseRoomId(String value) {
        try {
            return Long.parseLong(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("roomId 参数无效");
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            normalized = "";
        }
        return normalized.length() > 1000 ? normalized.substring(0, 1000) : normalized;
    }
}
