package com.youhu.caregiver.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaregiverScheduleRepository scheduleRepository;

    @Test
    void categoriesArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("陪诊服务"));
    }

    @Test
    void caregiversArePublicAndPaged() throws Exception {
        mockMvc.perform(get("/api/v1/caregivers").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].name").isNotEmpty());
    }

    @Test
    void schedulesExposeCaregiverAndRemainingQuota() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        CaregiverSchedule schedule = new CaregiverSchedule();
        schedule.setCaregiverId(1L);
        schedule.setServiceDate(date);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setTotalQuota(3);
        schedule.setRemainingQuota(2);
        schedule.setPrice(new BigDecimal("200.00"));
        schedule.setStatus(ScheduleStatus.OPEN);
        scheduleRepository.saveAndFlush(schedule);

        mockMvc.perform(get("/api/v1/schedules").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].caregiverName").value("李秀兰"))
                .andExpect(jsonPath("$.data[0].timeRange").value("08:00-12:00"))
                .andExpect(jsonPath("$.data[0].remainingQuota").value(2));
    }

    @Test
    void ordersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }
}
