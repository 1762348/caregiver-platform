package com.youhu.caregiver.auth.service;

import com.youhu.caregiver.auth.dto.LoginResponse;
import com.youhu.caregiver.auth.dto.UserProfile;
import com.youhu.caregiver.auth.repository.RolePermissionRepository;
import com.youhu.caregiver.auth.sms.SmsCodeService;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.security.JwtService;
import com.youhu.caregiver.user.domain.User;
import com.youhu.caregiver.user.domain.UserRole;
import com.youhu.caregiver.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 手机号验证码登录；手机号未注册时自动创建普通用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER = "Bearer";

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SmsCodeService smsCodeService;
    private final JwtService jwtService;

    public void sendCode(String phone) {
        smsCodeService.send(phone);
    }

    @Transactional
    public LoginResponse login(String phone, String code) {
        smsCodeService.verify(phone, code);

        User user = userRepository.findByPhone(phone).orElseGet(() -> register(phone));
        List<String> permissions = rolePermissionRepository.findPermissionCodesByRole(user.getRole());
        String token = jwtService.issue(user, permissions);

        return new LoginResponse(token, BEARER, jwtService.expiresInSeconds(),
                toProfile(user, permissions));
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "账号不存在，请重新登录"));
        List<String> permissions = rolePermissionRepository.findPermissionCodesByRole(user.getRole());
        String token = jwtService.issue(user, permissions);
        return new LoginResponse(token, BEARER, jwtService.expiresInSeconds(),
                toProfile(user, permissions));
    }

    @Transactional(readOnly = true)
    public UserProfile me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "账号不存在，请重新登录"));
        return toProfile(user, rolePermissionRepository.findPermissionCodesByRole(user.getRole()));
    }

    private User register(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setRole(UserRole.USER);
        User saved = userRepository.save(user);
        log.info("新用户注册：id={}，手机号={}", saved.getId(), phone);
        return saved;
    }

    private UserProfile toProfile(User user, List<String> permissions) {
        return new UserProfile(user.getId(), user.getPhone(), user.getName(),
                user.getRole().name(), user.getAvatar(), permissions);
    }
}
