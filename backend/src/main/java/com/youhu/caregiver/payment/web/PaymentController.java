package com.youhu.caregiver.payment.web;

import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.payment.dto.PaymentCallbackRequest;
import com.youhu.caregiver.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 第三方支付异步通知入口（公开接口，靠签名防伪造）。
     * Mock 支付走同步流程，此接口保留给真实网关接入。
     */
    @PostMapping("/callback")
    public ApiResponse<Void> callback(@Valid @RequestBody PaymentCallbackRequest request) {
        paymentService.handleCallback(request.paymentNo(), request.success(), request.signature());
        return ApiResponse.ok();
    }
}
