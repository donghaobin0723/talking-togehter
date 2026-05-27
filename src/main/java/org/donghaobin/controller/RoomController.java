package org.donghaobin.controller;

import org.donghaobin.dto.BlacklistRequest;
import org.donghaobin.dto.ChatMessageView;
import org.donghaobin.dto.ChatRoomView;
import org.donghaobin.dto.RoomCreateRequest;
import org.donghaobin.entity.UserAccount;
import org.donghaobin.service.CurrentUserService;
import org.donghaobin.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final CurrentUserService currentUserService;

    public RoomController(RoomService roomService, CurrentUserService currentUserService) {
        this.roomService = roomService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ChatRoomView> activeRooms(HttpServletRequest request) {
        return roomService.activeRooms(currentUserService.requireUser(request));
    }

    @PostMapping
    public ChatRoomView createRoom(@RequestBody RoomCreateRequest request, HttpServletRequest httpRequest) {
        UserAccount user = currentUserService.requireUser(httpRequest);
        return roomService.createRoom(user, request.getName());
    }

    @GetMapping("/managed")
    public List<ChatRoomView> managedRooms(HttpServletRequest request) {
        return roomService.managedRooms(currentUserService.requireUser(request));
    }

    @GetMapping("/joined")
    public List<ChatRoomView> joinedRooms(HttpServletRequest request) {
        return roomService.joinedRooms(currentUserService.requireUser(request));
    }

    @PostMapping("/{roomId}/join")
    public ChatRoomView joinRoom(@PathVariable Long roomId, HttpServletRequest request) {
        return roomService.joinRoom(roomId, currentUserService.requireUser(request));
    }

    @PostMapping("/{roomId}/stop")
    public ChatRoomView stopRoom(@PathVariable Long roomId, HttpServletRequest request) {
        return roomService.stopRoom(roomId, currentUserService.requireUser(request));
    }

    @PostMapping("/{roomId}/blacklist")
    public ChatRoomView blacklist(@PathVariable Long roomId,
                                  @RequestBody BlacklistRequest request,
                                  HttpServletRequest httpRequest) {
        return roomService.blacklist(roomId, currentUserService.requireUser(httpRequest), request);
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageView> latestMessages(@PathVariable Long roomId, HttpServletRequest request) {
        return roomService.latestMessages(roomId, currentUserService.requireUser(request));
    }
}
