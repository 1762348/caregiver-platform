package com.youhu.caregiver.auth.web;

import com.youhu.caregiver.auth.dto.LoginRequest;
import com.youhu.caregiver.auth.dto.LoginResponse;
import com.youhu.caregiver.auth.dto.SendCodeRequest;
import com.youhu.caregiver.auth.dto.UserProfile;
import com.youhu.caregiver.auth.service.AuthService;
import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 发送登录验证码（开发环境为固定值，见配置 app.sms.fixed-code） */
    @PostMapping("/sms-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request.phone());
        return ApiResponse.ok();
    }

    /** 手机号 + 验证码登录，未注册自动创建账号 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.phone(), request.code()));
    }

    /** 续签令牌（需要携带有效令牌） */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(authService.refresh(user.id()));
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public ApiResponse<UserProfile> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(authService.me(user.id()));
    }
}
