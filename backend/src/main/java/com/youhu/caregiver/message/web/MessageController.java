package com.youhu.caregiver.message.web;

import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.message.dto.MessageView;
import com.youhu.caregiver.message.service.MessageService;
import com.youhu.caregiver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ApiResponse<PageResult<MessageView>> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(messageService.list(user.id(), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(messageService.unreadCount(user.id()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        messageService.markRead(user.id(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> readAll(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(messageService.markAllRead(user.id()));
    }
}
