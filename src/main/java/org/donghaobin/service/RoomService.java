package org.donghaobin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.donghaobin.dto.BlacklistRequest;
import org.donghaobin.dto.ChatMessageView;
import org.donghaobin.dto.ChatRoomView;
import org.donghaobin.entity.ChatRoom;
import org.donghaobin.entity.RoomBlacklist;
import org.donghaobin.entity.RoomMember;
import org.donghaobin.entity.RoomRole;
import org.donghaobin.entity.UserAccount;
import org.donghaobin.mapper.ChatRoomMapper;
import org.donghaobin.mapper.RoomBlacklistMapper;
import org.donghaobin.mapper.RoomMemberMapper;
import org.donghaobin.mapper.UserAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final RoomMemberMapper roomMemberMapper;
    private final RoomBlacklistMapper roomBlacklistMapper;
    private final UserAccountMapper userAccountMapper;
    private final ChatMessageService chatMessageService;

    public RoomService(ChatRoomMapper chatRoomMapper,
                       RoomMemberMapper roomMemberMapper,
                       RoomBlacklistMapper roomBlacklistMapper,
                       UserAccountMapper userAccountMapper,
                       ChatMessageService chatMessageService) {
        this.chatRoomMapper = chatRoomMapper;
        this.roomMemberMapper = roomMemberMapper;
        this.roomBlacklistMapper = roomBlacklistMapper;
        this.userAccountMapper = userAccountMapper;
        this.chatMessageService = chatMessageService;
    }

    @Transactional
    public ChatRoomView createRoom(UserAccount user, String name) {
        String roomName = normalizeRoomName(name);
        ChatRoom room = new ChatRoom(roomName, user.getId());
        chatRoomMapper.insert(room);
        roomMemberMapper.insert(new RoomMember(room.getId(), user.getId(), RoomRole.ADMIN));
        return toView(room, user);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomView> activeRooms(UserAccount user) {
        return chatRoomMapper.selectList(new LambdaQueryWrapper<ChatRoom>()
                        .eq(ChatRoom::isActive, true)
                        .orderByDesc(ChatRoom::getCreatedAt))
                .stream()
                .map(room -> toView(room, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatRoomView> managedRooms(UserAccount user) {
        return chatRoomMapper.selectList(new LambdaQueryWrapper<ChatRoom>()
                        .eq(ChatRoom::getOwnerId, user.getId())
                        .orderByDesc(ChatRoom::getCreatedAt))
                .stream()
                .map(room -> toView(room, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatRoomView> joinedRooms(UserAccount user) {
        return roomMemberMapper.selectList(new LambdaQueryWrapper<RoomMember>()
                        .eq(RoomMember::getUserId, user.getId())
                        .eq(RoomMember::getRole, RoomRole.MEMBER)
                        .orderByDesc(RoomMember::getJoinedAt))
                .stream()
                .map(member -> chatRoomMapper.selectById(member.getRoomId()))
                .filter(room -> room != null)
                .map(room -> toView(room, user))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomView joinRoom(Long roomId, UserAccount user) {
        ChatRoom room = requireRoom(roomId);
        ensureRoomActive(room);
        ensureNotBlacklisted(room, user);

        if (!isMember(room, user)) {
            roomMemberMapper.insert(new RoomMember(room.getId(), user.getId(), RoomRole.MEMBER));
        }

        return toView(room, user);
    }

    @Transactional
    public ChatRoomView stopRoom(Long roomId, UserAccount user) {
        ChatRoom room = requireRoom(roomId);
        ensureAdmin(room, user);
        if (room.isActive()) {
            room.stop();
            chatRoomMapper.updateById(room);
        }
        return toView(room, user);
    }

    @Transactional
    public ChatRoomView blacklist(Long roomId, UserAccount admin, BlacklistRequest request) {
        ChatRoom room = requireRoom(roomId);
        ensureAdmin(room, admin);

        String username = request == null ? "" : request.getUsername();
        UserAccount target = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username == null ? "" : username.trim())
                .last("limit 1"));
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "被拉黑用户不存在");
        }

        if (target.getId().equals(room.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能拉黑聊天室创建者");
        }

        if (!isBlacklisted(room, target)) {
            String reason = request.getReason() == null ? "" : request.getReason().trim();
            roomBlacklistMapper.insert(new RoomBlacklist(room.getId(), target.getId(), admin.getId(), reason));
        }

        return toView(room, admin);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> latestMessages(Long roomId, UserAccount user) {
        ChatRoom room = requireRoom(roomId);
        ensureCanEnter(room, user);
        return chatMessageService.latestMessages(room);
    }

    @Transactional(readOnly = true)
    public ChatRoom requireRoom(Long roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "聊天室不存在");
        }
        return room;
    }

    @Transactional(readOnly = true)
    public void ensureCanEnter(ChatRoom room, UserAccount user) {
        ensureRoomActive(room);
        ensureNotBlacklisted(room, user);
        if (!isMember(room, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请先加入聊天室");
        }
    }

    private ChatRoomView toView(ChatRoom room, UserAccount user) {
        RoomMember member = findMember(room, user);
        RoomRole role = member == null ? null : member.getRole();
        UserAccount owner = userAccountMapper.selectById(room.getOwnerId());
        String ownerUsername = owner == null ? "未知用户" : owner.getUsername();
        return new ChatRoomView(room, ownerUsername, role, member != null, isBlacklisted(room, user));
    }

    private void ensureAdmin(ChatRoom room, UserAccount user) {
        RoomMember member = findMember(room, user);
        if (member == null || member.getRole() != RoomRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有聊天室管理员可以执行该操作");
        }
    }

    private void ensureRoomActive(ChatRoom room) {
        if (!room.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "聊天室已停用");
        }
    }

    private void ensureNotBlacklisted(ChatRoom room, UserAccount user) {
        if (isBlacklisted(room, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "你已被该聊天室拉黑");
        }
    }

    private boolean isMember(ChatRoom room, UserAccount user) {
        return findMember(room, user) != null;
    }

    private RoomMember findMember(ChatRoom room, UserAccount user) {
        return roomMemberMapper.selectOne(new LambdaQueryWrapper<RoomMember>()
                .eq(RoomMember::getRoomId, room.getId())
                .eq(RoomMember::getUserId, user.getId())
                .last("limit 1"));
    }

    private boolean isBlacklisted(ChatRoom room, UserAccount user) {
        Long count = roomBlacklistMapper.selectCount(new LambdaQueryWrapper<RoomBlacklist>()
                .eq(RoomBlacklist::getRoomId, room.getId())
                .eq(RoomBlacklist::getUserId, user.getId()));
        return count != null && count > 0;
    }

    private String normalizeRoomName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.length() < 2 || normalized.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "聊天室名称长度必须在 2 到 80 个字符之间");
        }
        return normalized;
    }
}
