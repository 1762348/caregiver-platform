package com.youhu.caregiver.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.youhu.caregiver.support.TestTokens;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 管理端与护工端接口的角色隔离 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String phone) throws Exception {
        return TestTokens.login(mockMvc, phone);
    }

    @Test
    void regularUserCannotAccessAdminApi() throws Exception {
        String token = login("13800000002");
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void caregiverCannotAccessAdminApiButCanAccessPortal() throws Exception {
        String token = login("13800000001");

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/caregiver/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CG0001"))
                .andExpect(jsonPath("$.data.categoryName").value("日常照料"));

        mockMvc.perform(get("/api/v1/caregiver/schedules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void regularUserCannotAccessCaregiverPortal() throws Exception {
        String token = login("13800000002");
        mockMvc.perform(get("/api/v1/caregiver/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminApi() throws Exception {
        String token = login("13800000000");
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
