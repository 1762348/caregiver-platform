package com.youhu.caregiver.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.youhu.caregiver.support.TestTokens;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminApiTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String phone) throws Exception {
        return TestTokens.login(mockMvc, phone);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void adminCanListAndCreateCategories() throws Exception {
        String token = login("13800000000");

        mockMvc.perform(get("/api/v1/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].caregiverCount").isNumber());

        String body = """
                {"code":"TEST_CAT","name":"测试类别","description":"临时类别",
                 "basePrice":99.00,"status":"ACTIVE"}
                """;
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("TEST_CAT"))
                .andExpect(jsonPath("$.data.name").value("测试类别"));

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanCreateCaregiverAndSearch() throws Exception {
        String token = login("13800000000");
        String body = """
                {"phone":"13900008888","name":"测试护工","gender":"FEMALE","birth":"1990-01-01",
                 "code":"CG9999","categoryId":1,"qualification":"测试资质","intro":"测试简介",
                 "price":210.00,"status":"ACTIVE"}
                """;
        mockMvc.perform(post("/api/v1/admin/caregivers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CG9999"))
                .andExpect(jsonPath("$.data.phone").value("13900008888"));

        mockMvc.perform(get("/api/v1/admin/caregivers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("keyword", "CG9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void adminCanGenerateSchedulesInBatch() throws Exception {
        String token = login("13800000000");
        LocalDate from = LocalDate.now().plusDays(20);
        String body = """
                {"caregiverId":1,"from":"%s","to":"%s","totalQuota":2}
                """.formatted(from, from.plusDays(2));

        mockMvc.perform(post("/api/v1/admin/schedules/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(9));

        mockMvc.perform(get("/api/v1/admin/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("caregiverId", "1")
                        .param("from", from.toString())
                        .param("to", from.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[0].caregiverName").value("李秀兰"));
    }
}
