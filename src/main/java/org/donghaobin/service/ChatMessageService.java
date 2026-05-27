package org.donghaobin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.donghaobin.dto.ChatMessageView;
import org.donghaobin.entity.ChatRoom;
import org.donghaobin.entity.ChatMessage;
import org.donghaobin.entity.UserAccount;
import org.donghaobin.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Transactional
    public ChatMessage save(ChatRoom room, UserAccount sender, String content) {
        ChatMessage message = new ChatMessage(room.getId(), sender.getId(), sender.getUsername(), content);
        chatMessageMapper.insert(message);
        return message;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> latestMessages(ChatRoom room) {
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, room.getId())
                .orderByDesc(ChatMessage::getSentAt)
                .last("limit 50"));
        Collections.reverse(messages);
        return messages.stream().map(ChatMessageView::new).collect(Collectors.toList());
    }
}
