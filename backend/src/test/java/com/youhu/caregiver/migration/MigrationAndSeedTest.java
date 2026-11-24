package com.youhu.caregiver.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.category.repository.CategoryRepository;
import com.youhu.caregiver.common.domain.RecordStatus;
import com.youhu.caregiver.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 Flyway 迁移与种子数据：空库启动后表结构与基础数据都应由脚本生成。
 */
@SpringBootTest
@ActiveProfiles("test")
class MigrationAndSeedTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CaregiverRepository caregiverRepository;

    @Test
    void seedDataIsLoaded() {
        assertThat(userRepository.count()).isEqualTo(4);
        assertThat(categoryRepository.count()).isEqualTo(5);
        assertThat(caregiverRepository.count()).isEqualTo(2);
        assertThat(categoryRepository.findByStatusOrderByBasePriceAsc(RecordStatus.ACTIVE))
                .extracting("name")
                .containsExactly("陪诊服务", "日常照料", "夜间陪护", "康复训练", "专业护理");
    }
}
