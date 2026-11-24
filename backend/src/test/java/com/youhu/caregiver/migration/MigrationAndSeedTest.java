package com.youhu.caregiver.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.caregiver.repository.CaregiverBaseShiftRepository;
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
 *
 * <p>注意：其它测试可能通过接口注册新账号，因此这里只断言种子数据存在，不断言用户总数。</p>
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

    @Autowired
    private CaregiverBaseShiftRepository baseShiftRepository;

    @Test
    void seedDataIsLoaded() {
        assertThat(userRepository.findByPhone("13800000000")).isPresent();
        assertThat(userRepository.findByPhone("13800000001")).isPresent();
        assertThat(userRepository.findByPhone("13800000002")).isPresent();
        assertThat(userRepository.findByPhone("13800000003")).isPresent();

        assertThat(categoryRepository.count()).isEqualTo(5);
        assertThat(caregiverRepository.count()).isEqualTo(2);
        assertThat(baseShiftRepository.count()).isEqualTo(5);
        assertThat(categoryRepository.findByStatusOrderByBasePriceAsc(RecordStatus.ACTIVE))
                .extracting("name")
                .containsExactly("陪诊服务", "日常照料", "夜间陪护", "康复训练", "专业护理");
    }
}
