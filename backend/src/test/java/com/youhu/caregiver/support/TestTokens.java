package com.youhu.caregiver.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 测试用登录工具：缓存令牌，避免多个测试类重复登录触发验证码频控。
 */
public final class TestTokens {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private TestTokens() {
    }

    public static String login(MockMvc mockMvc, String phone) throws Exception {
        String cached = CACHE.get(phone);
        if (cached != null) {
            return cached;
        }
        synchronized (TestTokens.class) {
            cached = CACHE.get(phone);
            if (cached != null) {
                return cached;
            }
            mockMvc.perform(post("/api/v1/auth/sms-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(("{\"phone\":\"" + phone + "\"}")
                                    .getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isOk());
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}")
                                    .getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andReturn();
            String token = JsonPath.read(
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                    "$.data.token");
            CACHE.put(phone, token);
            return token;
        }
    }
}
