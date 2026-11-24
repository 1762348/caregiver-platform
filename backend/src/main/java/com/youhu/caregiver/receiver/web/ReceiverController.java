package com.youhu.caregiver.receiver.web;

import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.receiver.dto.ReceiverRequest;
import com.youhu.caregiver.receiver.dto.ReceiverView;
import com.youhu.caregiver.receiver.service.ReceiverService;
import com.youhu.caregiver.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receivers")
@RequiredArgsConstructor
public class ReceiverController {

    private final ReceiverService receiverService;

    @GetMapping
    public ApiResponse<List<ReceiverView>> list(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(receiverService.list(user.id()));
    }

    @PostMapping
    public ApiResponse<ReceiverView> create(@AuthenticationPrincipal AuthUser user,
                                            @Valid @RequestBody ReceiverRequest request) {
        return ApiResponse.ok(receiverService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReceiverView> update(@AuthenticationPrincipal AuthUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ReceiverRequest request) {
        return ApiResponse.ok(receiverService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        receiverService.delete(user.id(), id);
        return ApiResponse.ok();
    }
}
